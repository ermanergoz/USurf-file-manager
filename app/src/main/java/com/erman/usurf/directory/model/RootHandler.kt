package com.erman.usurf.directory.model

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
private const val EXIT_CODE_SUCCESS: Int = 0

class RootHandler {
    private val commandIdCounter: AtomicInteger = AtomicInteger(0)

    private fun nextCommandId(): Int = commandIdCounter.getAndIncrement()

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
        while (!command.isFinished) {
            // A workaround to wait for the command to finish since waitForFinish()
            // is not available in latest version of RootTools library
            try {
                Thread.sleep(ROOT_COMMAND_WAIT_TIME_MS)
            } catch (err: InterruptedException) {
                loge(err.localizedMessage ?: UNKNOWN_ERROR)
            }
        }
    }

    fun isDeviceRooted(): Boolean {
        return RootTools.isRootAvailable()
    }

    fun isRootAccessGiven(): Boolean {
        return try {
            val shell: Shell = getHealthyShell()
            !shell.isClosed
        } catch (err: Exception) {
            loge(err.localizedMessage ?: UNKNOWN_ERROR)
            false
        }
    }

    fun remountRootDirAs(mountMode: String) {
        val command: Command =
            object : Command(nextCommandId(), "mount -o $mountMode,remount /") {
                override fun commandTerminated(
                    id: Int,
                    reason: String,
                ) {
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
            object : Command(nextCommandId(), "cd '$path'", "ls -p -a") {
                override fun commandOutput(
                    id: Int,
                    line: String,
                ) {
                    super.commandOutput(id, line)
                    fileList.add(line)
                }

                override fun commandTerminated(
                    id: Int,
                    reason: String,
                ) {
                    super.commandTerminated(id, reason)
                    loge(reason)
                }
            }
        getHealthyShell().add(command)
        waitForFinish(command)

        return fileList
    }

    fun delete(selectedDirectories: List<FileModel>): Boolean {
        return selectedDirectories.all { source ->
            executeRootCommand("rm -r '${source.path}'")
        }
    }

    private fun doesFileExist(
        path: String,
        name: String,
        isDirectory: Boolean,
    ): Boolean {
        val fileName: String = if (isDirectory) "$name$PATH_SEPARATOR_CHAR" else name
        return getFileList(path).any { it == fileName }
    }

    fun createFolder(
        path: String,
        folderName: String,
    ): Boolean {
        if (doesFileExist(path, folderName, true)) return false
        return executeRootCommand("cd '$path'", "mkdir '$folderName'")
    }

    fun createFile(
        path: String,
        fileName: String,
    ): Boolean {
        if (doesFileExist(path, fileName, false)) return false
        return executeRootCommand("cd '$path'", "> '$fileName'")
    }

    fun renameFile(
        selectedFile: FileModel,
        newName: String,
    ): Boolean {
        val parentDir = getParentPath(selectedFile)
        if (doesFileExist(parentDir, newName, true)) return false
        val newPath = "$parentDir${File.separator}$newName"
        return executeRootCommand("cd '$parentDir'", "mv '${selectedFile.path}' '$newPath'")
    }

    private fun sanitizeSourcePath(path: String): String =
        if (path.last() == PATH_SEPARATOR_CHAR) {
            path.dropLast(SUFFIX_LENGTH)
        } else {
            path
        }

    private fun executeRootCommand(vararg commands: String): Boolean {
        var isSuccess = false
        val command: Command =
            object : Command(nextCommandId(), *commands) {
                override fun commandCompleted(
                    id: Int,
                    exitcode: Int,
                ) {
                    super.commandCompleted(id, exitcode)
                    isSuccess = exitcode == EXIT_CODE_SUCCESS
                }

                override fun commandTerminated(
                    id: Int,
                    reason: String,
                ) {
                    super.commandTerminated(id, reason)
                    isSuccess = false
                    loge(reason)
                }
            }
        getHealthyShell().add(command)
        waitForFinish(command)
        return isSuccess
    }

    fun copyFile(
        selectedDirectories: List<FileModel>,
        copyOrMoveDestination: String,
    ): Boolean {
        return selectedDirectories.all { source ->
            val sourcePath: String = sanitizeSourcePath(source.path)
            executeRootCommand("cp -a '$sourcePath' '$copyOrMoveDestination'")
        }
    }

    fun moveFile(
        selectedDirectories: List<FileModel>,
        copyOrMoveDestination: String,
    ): Boolean {
        return selectedDirectories.all { source ->
            val sourcePath: String = sanitizeSourcePath(source.path)
            executeRootCommand("mv '$sourcePath' '$copyOrMoveDestination'")
        }
    }
}
