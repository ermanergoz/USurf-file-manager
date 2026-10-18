package com.erman.usurf.directory.model

import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
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

// The longest a caller waits for one command. The shell ends a command it has run for 20 s by itself, so only a
// shell that never answers at all runs into this bound.
private const val ROOT_COMMAND_MAX_WAIT_MS: Long = 600_000
private const val EXIT_CODE_SUCCESS: Int = 0

// Longer than the longest superuser prompt countdown (60 s in Magisk), so the manager's own grant,
// denial or timeout answers first; only a manager that never answers runs into this bound.
private const val ROOT_SHELL_REQUEST_TIMEOUT_MS: Int = 70_000

// RootShell retries a failed request three times by default, and every retry opens another prompt.
private const val ROOT_SHELL_REQUEST_RETRIES: Int = 0

// One su request with a bounded wait: it returns the open shell or throws TimeoutException,
// RootDeniedException or IOException. It blocks until the prompt is answered, never call it on main.
private object RootShellRequest {
    private fun open(): Shell = RootTools.getShell(true, ROOT_SHELL_REQUEST_TIMEOUT_MS, Shell.defaultContext, ROOT_SHELL_REQUEST_RETRIES)

    fun openHealthy(): Shell {
        val shell: Shell = open()
        if (!shell.isClosed) return shell
        try {
            Shell.closeRootShell()
        } catch (err: Exception) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
        }
        return open()
    }
}

/**
 * One command line for the root shell that its caller can wait for. Its handler is off, so the shell calls it back on
 * the shell's own thread, never through the Looper of the thread that built it, which may be the very thread waiting.
 * The latch makes the exit code and the output readable only after the shell finished or ended the command.
 */
private class AwaitedCommand(id: Int, line: String) : Command(id, false, line) {
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
    private val isRootShellOpen: () -> Boolean = { !RootShellRequest.openHealthy().isClosed },
    private val submitToShell: (Command) -> Unit = { command -> RootShellRequest.openHealthy().add(command) },
    private val commandTimeoutMs: Long = ROOT_COMMAND_MAX_WAIT_MS
) {
    private val commandIdCounter: AtomicInteger = AtomicInteger(0)

    private fun nextCommandId(): Int = commandIdCounter.getAndIncrement()

    private fun getParentPath(fileModel: FileModel): String {
        val parent = fileModel.path
        return parent.removeSuffix(File.separator + fileModel.name)
    }

    fun isDeviceRooted(): Boolean = isSuBinaryFound()

    // A denied request, a request that outlives its bound and a su that cannot start all mean no root access.
    fun isRootAccessGiven(): Boolean = try {
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

    // A remount the root file system refuses, as a verity-protected one does, leaves the action to succeed or fail alone.
    fun remountRootDirAs(mountOption: MountOption) {
        run(RootCommands.remountRoot(mountOption))
    }

    fun getFileList(path: String): List<String> =
        run(RootCommands.list(path)) ?: throw FileOperationException("The folder could not be listed as root")

    fun delete(selectedDirectories: List<FileModel>): Boolean = selectedDirectories.all { source ->
        executeRootCommand(RootCommands.delete(source.path))
    }

    fun createFolder(path: String, folderName: String): Boolean = executeRootCommand(RootCommands.createFolder(path, folderName))

    fun createFile(path: String, fileName: String): Boolean = executeRootCommand(RootCommands.createFile(path, fileName))

    fun renameFile(selectedFile: FileModel, newName: String): Boolean {
        val newPath = "${getParentPath(selectedFile)}${File.separator}$newName"
        return executeRootCommand(RootCommands.rename(selectedFile.path, newPath))
    }

    /** Runs [line] in the root shell and gives what it printed when the shell reports exit code 0 in time, else null. */
    private fun run(line: String): List<String>? {
        val command = AwaitedCommand(nextCommandId(), line)
        submitToShell(command)
        return command.outputOnSuccess(commandTimeoutMs)
    }

    private fun executeRootCommand(line: String): Boolean = run(line) != null

    // Each item stops at a name already taken in the destination, so neither a copy nor a move replaces anything there.
    fun copyFile(selectedDirectories: List<FileModel>, copyOrMoveDestination: String): Boolean = selectedDirectories.all { source ->
        executeRootCommand(RootCommands.copyInto(source.path, copyOrMoveDestination))
    }

    fun moveFile(selectedDirectories: List<FileModel>, copyOrMoveDestination: String): Boolean = selectedDirectories.all { source ->
        executeRootCommand(RootCommands.moveInto(source.path, copyOrMoveDestination))
    }
}
