package com.erman.usurf.directory.model

import com.erman.usurf.preference.domain.RootAccessChecker
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import com.stericson.RootShell.RootShell
import com.stericson.RootShell.exceptions.RootDeniedException
import com.stericson.RootShell.execution.Command
import com.stericson.RootShell.execution.Shell
import com.stericson.RootTools.RootTools
import java.io.File
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

private const val EXIT_CODE_SUCCESS: Int = 0

// How long the shell lets a command run before it ends it: a listing, a new folder or file, a rename or a remount
// answers at once, so 20 s (the shell's own default) still ends one that hangs; a delete, copy or move of a large
// tree may run for minutes.
private const val COMMAND_TIMEOUT_MS: Int = 20_000
private const val TRANSFER_TIMEOUT_MS: Int = 600_000

// A caller waits this much longer than the shell lets the command run, so the shell is what ends a slow command and
// only a shell that never answers at all runs into the caller's bound.
private const val LATCH_MARGIN_MS: Long = 30_000

// Longer than the longest superuser prompt countdown (60 s in Magisk), so the manager's own grant,
// denial or timeout answers first; only a manager that never answers runs into this bound.
private const val ROOT_SHELL_REQUEST_TIMEOUT_MS: Int = 70_000

// RootShell retries a failed request three times by default, and every retry opens another prompt.
private const val ROOT_SHELL_REQUEST_RETRIES: Int = 0

/**
 * The su request, with a bounded wait: it returns the open shell or throws TimeoutException, RootDeniedException or
 * IOException. It blocks until the prompt is answered, never call it on main.
 */
internal class RootShellRequest<S : Any>(
    private val open: () -> S,
    private val isClosed: (S) -> Boolean,
    private val closeRootShell: () -> Unit
) {
    // One request at a time: a caller that comes while a prompt is up waits for it and then gets the shell it opened,
    // so requests started together share one prompt and one su process.
    @Synchronized
    fun openHealthy(): S {
        val shell: S = open()
        if (!isClosed(shell)) return shell
        try {
            closeRootShell()
        } catch (err: IOException) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
        }
        return open()
    }
}

private val rootShellRequest: RootShellRequest<Shell> =
    RootShellRequest(
        open = { RootTools.getShell(true, ROOT_SHELL_REQUEST_TIMEOUT_MS, Shell.defaultContext, ROOT_SHELL_REQUEST_RETRIES) },
        isClosed = { shell -> shell.isClosed },
        closeRootShell = Shell::closeRootShell
    )

/**
 * One command line for the root shell that its caller can wait for, which the shell ends after [timeoutMs]. With the
 * library's handlers off for the whole app ([RootHandler.configureShell]), the shell calls it back on the shell's own
 * thread, never through the Looper of the thread that built it, which may be the very thread waiting. The latch makes
 * the exit code and the output readable only after the shell finished or ended the command.
 */
private class AwaitedCommand(id: Int, timeoutMs: Int, line: String) : Command(id, timeoutMs, line) {
    private val finished: CountDownLatch = CountDownLatch(1)
    private val output: MutableList<String> = CopyOnWriteArrayList()

    @Volatile
    private var exitCode: Int? = null

    override fun commandOutput(id: Int, line: String) {
        super.commandOutput(id, line)
        output.add(line)
    }

    override fun commandCompleted(id: Int, exitcode: Int) {
        super.commandCompleted(id, exitcode)
        exitCode = exitcode
        finished.countDown()
    }

    override fun commandTerminated(id: Int, reason: String) {
        super.commandTerminated(id, reason)
        loge(reason)
        finished.countDown()
    }

    /** The lines the command printed when it exited 0 within [timeoutMs]; null when it failed, was ended or timed out. */
    fun outputOnSuccess(timeoutMs: Long): List<String>? {
        val hasFinished =
            try {
                finished.await(timeoutMs, TimeUnit.MILLISECONDS)
            } catch (err: InterruptedException) {
                loge(err.localizedMessage ?: UNKNOWN_ERROR)
                Thread.currentThread().interrupt()
                false
            }
        if (!hasFinished) terminate()
        return output.toList().takeIf { hasFinished && exitCode == EXIT_CODE_SUCCESS }
    }
}

class RootHandler(
    private val isSuBinaryFound: () -> Boolean = RootTools::isRootAvailable,
    private val isRootShellOpen: () -> Boolean = { !rootShellRequest.openHealthy().isClosed },
    private val submitToShell: (Command) -> Unit = { command -> rootShellRequest.openHealthy().add(command) },
    private val latchWaitMs: (commandTimeoutMs: Int) -> Long = { commandTimeoutMs -> commandTimeoutMs + LATCH_MARGIN_MS }
) : RootAccessChecker {
    companion object {
        /**
         * Sets the root shell library up for the whole app, once, before any root command is built: no command posts its
         * callbacks to the Looper of the thread that built it, so a command built on a thread that then waits for it
         * still sees its answer.
         */
        fun configureShell() {
            RootShell.handlerEnabled = false
        }
    }

    private val commandIdCounter: AtomicInteger = AtomicInteger(0)

    private fun nextCommandId(): Int = commandIdCounter.getAndIncrement()

    private fun getParentPath(fileModel: FileModel): String {
        val parent = fileModel.path
        return parent.removeSuffix(File.separator + fileModel.name)
    }

    override fun isDeviceRooted(): Boolean = isSuBinaryFound()

    // A denied request, a request that outlives its bound and a su that cannot start all mean no root access.
    override fun isRootAccessGiven(): Boolean = try {
        isRootShellOpen()
    } catch (err: TimeoutException) {
        loge(err.localizedMessage ?: UNKNOWN_ERROR)
        false
    } catch (err: RootDeniedException) {
        loge(err.localizedMessage ?: UNKNOWN_ERROR)
        false
    } catch (err: IOException) {
        loge(err.localizedMessage ?: UNKNOWN_ERROR)
        false
    }

    // A remount the root file system refuses, as a verity-protected one does, or one the shell cannot run at all, leaves
    // the action to succeed or fail alone.
    fun remountRootDirAs(mountOption: MountOption) {
        try {
            run(RootCommands.remountRoot(mountOption))
        } catch (err: FileOperationException) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
        }
    }

    // An entry named by a bare newline prints as an empty line, which names nothing to show or open.
    fun getFileList(path: String): List<String> {
        val lines = run(RootCommands.list(path)) ?: throw FileOperationException("The folder could not be listed as root")
        return lines.filter { it.isNotEmpty() }
    }

    fun delete(selectedDirectories: List<FileModel>): Boolean = selectedDirectories.all { source ->
        executeRootCommand(RootCommands.delete(source.path), TRANSFER_TIMEOUT_MS)
    }

    fun createFolder(path: String, folderName: String): Boolean =
        executeRootCommand(RootCommands.createFolder(path, folderName)) || refuseIfTaken(File(path, folderName).path)

    fun createFile(path: String, fileName: String): Boolean =
        executeRootCommand(RootCommands.createFile(path, fileName)) || refuseIfTaken(File(path, fileName).path)

    fun renameFile(selectedFile: FileModel, newName: String): Boolean {
        val newPath = "${getParentPath(selectedFile)}${File.separator}$newName"
        return executeRootCommand(RootCommands.rename(selectedFile.path, newPath)) || refuseIfTaken(newPath)
    }

    /**
     * After a write that did not succeed: throws [OperationError.AlreadyExists] when something is at [target], so a
     * taken name is reported as one; otherwise false, a failure with no reason of its own.
     */
    private fun refuseIfTaken(target: String): Boolean {
        requireFree(target)
        return false
    }

    /** Throws [OperationError.AlreadyExists] when something is at [target]. */
    private fun requireFree(target: String) {
        if (run(RootCommands.isTaken(target)) != null) {
            throw FileOperationException("An item with this name already exists", OperationError.AlreadyExists)
        }
    }

    /**
     * Runs [line] in the root shell, which ends it after [timeoutMs], and gives what it printed when the shell reports
     * exit code 0 in time, else null.
     */
    private fun run(line: String, timeoutMs: Int = COMMAND_TIMEOUT_MS): List<String>? {
        val command = AwaitedCommand(nextCommandId(), timeoutMs, line)
        submit(command)
        return command.outputOnSuccess(latchWaitMs(timeoutMs))
    }

    /**
     * Hands [command] to the root shell. Throws [FileOperationException] when the shell cannot take it: su was denied,
     * its prompt was not answered in time, su could not start, or the shell closed before the command reached it.
     */
    private fun submit(command: Command) {
        val refusal: Exception? =
            try {
                submitToShell(command)
                null
            } catch (err: TimeoutException) {
                err
            } catch (err: RootDeniedException) {
                err
            } catch (err: IOException) {
                err
            } catch (err: IllegalStateException) {
                err
            }
        if (refusal != null) throw FileOperationException("Root access is not available", cause = refusal)
    }

    private fun executeRootCommand(line: String, timeoutMs: Int = COMMAND_TIMEOUT_MS): Boolean = run(line, timeoutMs) != null

    // Each item stops at a name already taken in the destination, so neither a copy nor a move replaces anything there.
    // The name is checked before the transfer: a transfer that fails midway can leave part of its item at the name,
    // which is then no name taken by something else.
    fun copyFile(selectedDirectories: List<FileModel>, copyOrMoveDestination: String): Boolean = selectedDirectories.all { source ->
        requireFree(File(copyOrMoveDestination, File(source.path).name).path)
        executeRootCommand(RootCommands.copyInto(source.path, copyOrMoveDestination), TRANSFER_TIMEOUT_MS)
    }

    fun moveFile(selectedDirectories: List<FileModel>, copyOrMoveDestination: String): Boolean = selectedDirectories.all { source ->
        requireFree(File(copyOrMoveDestination, File(source.path).name).path)
        executeRootCommand(RootCommands.moveInto(source.path, copyOrMoveDestination), TRANSFER_TIMEOUT_MS)
    }
}
