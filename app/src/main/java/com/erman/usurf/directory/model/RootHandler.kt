package com.erman.usurf.directory.model

import android.os.SystemClock
import com.erman.usurf.directory.utils.PATH_SEPARATOR_CHAR
import com.erman.usurf.directory.utils.SUFFIX_LENGTH
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import com.stericson.RootShell.execution.Command
import com.stericson.RootShell.execution.Shell
import com.stericson.RootTools.RootTools
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

private const val ROOT_COMMAND_WAIT_TIME_MS: Long = 50
private const val ROOT_COMMAND_MAX_WAIT_MS: Long = 600_000
private const val EXIT_CODE_SUCCESS: Int = 0

class RootHandler {
    private val commandIdCounter: AtomicInteger = AtomicInteger(0)

    private fun nextCommandId(): Int = commandIdCounter.getAndIncrement()

    // Wraps an arbitrary file name or path so it is a single literal argument in a POSIX shell,
    // preventing quotes/metacharacters in a name from breaking out of the command (root injection).
    private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    private fun getHealthyShell(): Shell {
        val shell: Shell = RootTools.getShell(true)
        if (!shell.isClosed) return shell
        try {
            Shell.closeRootShell()
        } catch (err: Exception) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
        }
        return RootTools.getShell(true)
    }

    private fun getParentPath(fileModel: FileModel): String {
        val parent = fileModel.path
        return parent.removeSuffix(File.separator + fileModel.name)
    }

    private fun waitForFinish(command: Command) {
        // A workaround to wait for the command to finish since waitForFinish()
        // is not available in latest version of RootTools library. Bounded by a
        // deadline so a stuck command can never block the calling thread forever.
        val deadline = SystemClock.elapsedRealtime() + ROOT_COMMAND_MAX_WAIT_MS
        while (!command.isFinished && SystemClock.elapsedRealtime() < deadline) {
            try {
                Thread.sleep(ROOT_COMMAND_WAIT_TIME_MS)
            } catch (err: InterruptedException) {
                loge(err.localizedMessage ?: UNKNOWN_ERROR)
                Thread.currentThread().interrupt()
                return
            }
        }
    }

    fun isDeviceRooted(): Boolean = RootTools.isRootAvailable()

    fun isRootAccessGiven(): Boolean = try {
        val shell: Shell = getHealthyShell()
        !shell.isClosed
    } catch (err: Exception) {
        loge(err.localizedMessage ?: UNKNOWN_ERROR)
        false
    }

    fun remountRootDirAs(mountMode: String) {
        val command: Command =
            object : Command(nextCommandId(), "mount -o $mountMode,remount /") {
                override fun commandTerminated(id: Int, reason: String) {
                    super.commandTerminated(id, reason)
                    loge(reason)
                }
            }
        getHealthyShell().add(command)
        waitForFinish(command)
    }

    fun getFileList(path: String): List<String> {
        val fileList: MutableList<String> = mutableListOf()
        // -p adds the trailing slash on directories to distinguish folders from files
        // -a is to display hidden files
        val command: Command =
            object : Command(nextCommandId(), "cd ${shellQuote(path)}", "ls -p -a") {
                override fun commandOutput(id: Int, line: String) {
                    super.commandOutput(id, line)
                    fileList.add(line)
                }

                override fun commandTerminated(id: Int, reason: String) {
                    super.commandTerminated(id, reason)
                    loge(reason)
                }
            }
        getHealthyShell().add(command)
        waitForFinish(command)

        return fileList
    }

    fun delete(selectedDirectories: List<FileModel>): Boolean = selectedDirectories.all { source ->
        executeRootCommand("rm -r ${shellQuote(source.path)}")
    }

    private fun doesFileExist(path: String, name: String, isDirectory: Boolean): Boolean {
        val fileName: String = if (isDirectory) "$name$PATH_SEPARATOR_CHAR" else name
        return getFileList(path).any { it == fileName }
    }

    fun createFolder(path: String, folderName: String): Boolean {
        if (doesFileExist(path, folderName, true)) return false
        return executeRootCommand("cd ${shellQuote(path)}", "mkdir ${shellQuote(folderName)}")
    }

    fun createFile(path: String, fileName: String): Boolean {
        if (doesFileExist(path, fileName, false)) return false
        return executeRootCommand("cd ${shellQuote(path)}", "> ${shellQuote(fileName)}")
    }

    fun renameFile(selectedFile: FileModel, newName: String): Boolean {
        val parentDir = getParentPath(selectedFile)
        if (doesFileExist(parentDir, newName, true)) return false
        val newPath = "$parentDir${File.separator}$newName"
        return executeRootCommand("cd ${shellQuote(parentDir)}", "mv ${shellQuote(selectedFile.path)} ${shellQuote(newPath)}")
    }

    private fun sanitizeSourcePath(path: String): String = if (path.last() == PATH_SEPARATOR_CHAR) {
        path.dropLast(SUFFIX_LENGTH)
    } else {
        path
    }

    private fun executeRootCommand(vararg commands: String): Boolean {
        var isSuccess = false
        val command: Command =
            object : Command(nextCommandId(), *commands) {
                override fun commandCompleted(id: Int, exitcode: Int) {
                    super.commandCompleted(id, exitcode)
                    isSuccess = exitcode == EXIT_CODE_SUCCESS
                }

                override fun commandTerminated(id: Int, reason: String) {
                    super.commandTerminated(id, reason)
                    isSuccess = false
                    loge(reason)
                }
            }
        getHealthyShell().add(command)
        waitForFinish(command)
        return isSuccess
    }

    fun copyFile(selectedDirectories: List<FileModel>, copyOrMoveDestination: String): Boolean = selectedDirectories.all { source ->
        val sourcePath: String = sanitizeSourcePath(source.path)
        executeRootCommand("cp -a ${shellQuote(sourcePath)} ${shellQuote(copyOrMoveDestination)}")
    }

    fun moveFile(selectedDirectories: List<FileModel>, copyOrMoveDestination: String): Boolean = selectedDirectories.all { source ->
        val sourcePath: String = sanitizeSourcePath(source.path)
        executeRootCommand("mv ${shellQuote(sourcePath)} ${shellQuote(copyOrMoveDestination)}")
    }
}
