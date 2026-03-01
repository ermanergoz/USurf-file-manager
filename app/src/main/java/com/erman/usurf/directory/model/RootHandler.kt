package com.erman.usurf.directory.model

import com.erman.usurf.directory.utils.PATH_SEPARATOR_CHAR
import com.erman.usurf.directory.utils.SUFFIX_LENGTH
import com.erman.usurf.utils.UNKNOWN_ERROR
import com.erman.usurf.utils.loge
import com.stericson.RootShell.execution.Command
import com.stericson.RootTools.RootTools
import java.io.File

private const val ROOT_COMMAND_WAIT_TIME_MS: Long = 50
private const val ROOT_COMMAND_ID_DEFAULT: Int = 0
private const val EXIT_CODE_SUCCESS: Int = 0

class RootHandler {
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
                err.localizedMessage?.let { loge("createFile $it") } ?: UNKNOWN_ERROR
            }
        }
    }

    fun isDeviceRooted(): Boolean {
        return RootTools.isRootAvailable()
    }

    fun isRootAccessGiven(): Boolean {
        return try {
            RootTools.getShell(true)
            true
        } catch (err: Exception) {
            err.localizedMessage?.let { loge(it) } ?: UNKNOWN_ERROR
            false
        }
    }

    fun remountRootDirAs(mountMode: String) {
        val mntCommand: String = when (mountMode) {
            MountOption.READ_WRITE.option -> "mount -o rw,remount /"
            MountOption.READ.option -> "mount -o ro,remount /"
            else -> return
        }

        val command: Command =
            object : Command(ROOT_COMMAND_ID_DEFAULT, "mount -o rw,remount /") {
                override fun commandTerminated(
                    id: Int,
                    reason: String,
                ) {
                    super.commandTerminated(id, reason)
                    loge("remountRootDirAs $reason")
                }
            }
        RootTools.getShell(true).add(command)
        waitForFinish(command)
    }

    fun getFileList(path: String): List<String> {
        val fileList: MutableList<String> = mutableListOf()
        // -p adds the trailing slash on directories to distinguish folders from files
        // -a is to display hidden files
        val command: Command =
            object : Command(ROOT_COMMAND_ID_DEFAULT, "cd '$path'", "ls -p -a") {
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
        RootTools.getShell(true).add(command)
        waitForFinish(command)

        return fileList
    }

    fun delete(selectedDirectories: List<FileModel>): Boolean {
        var isSuccess = false

        for (source in selectedDirectories) {
            val command: Command =
                object : Command(ROOT_COMMAND_ID_DEFAULT, "rm -r '${source.path}'") {
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
            RootTools.getShell(true).add(command)
            waitForFinish(command)
        }
        return isSuccess
    }

    private fun doesFileExist(
        path: String,
        name: String,
        isDirectory: Boolean,
    ): Boolean {
        var fileName = name

        if (isDirectory) {
            fileName = "$name$PATH_SEPARATOR_CHAR"
        }

        for (file in getFileList(path)) {
            if (file == fileName) {
                return true
            }
        }
        return false
    }

    fun createFolder(
        path: String,
        folderName: String,
    ): Boolean {
        var isSuccess = false

        if (!doesFileExist(path, folderName, true)) {
            val command: Command =
                object : Command(ROOT_COMMAND_ID_DEFAULT, "cd '$path'", "mkdir '$folderName'") {
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
            RootTools.getShell(true).add(command)
            waitForFinish(command)
        }
        return isSuccess
    }

    fun createFile(
        path: String,
        fileName: String,
    ): Boolean {
        var isSuccess = false

        if (!doesFileExist(path, fileName, false)) {
            val command: Command =
                object : Command(ROOT_COMMAND_ID_DEFAULT, "cd '$path'", "> '$fileName'") {
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
                        loge("createFile $reason")
                    }
                }
            RootTools.getShell(true).add(command)
            waitForFinish(command)
        }
        return isSuccess
    }

    fun renameFile(
        selectedFile: FileModel,
        newName: String,
    ): Boolean {
        var isSuccess = false
        val parentDir = getParentPath(selectedFile)

        if (!doesFileExist(parentDir, newName, true)) {
            val command: Command =
                object : Command(
                    ROOT_COMMAND_ID_DEFAULT,
                    "cd '$parentDir'",
                    "mv '${selectedFile.path}' '${"$parentDir${File.separator}$newName"}'",
                ) {
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
            RootTools.getShell(true).add(command)
            waitForFinish(command)
        }
        return isSuccess
    }

    fun copyFile(
        selectedDirectories: List<FileModel>,
        copyOrMoveDestination: String,
    ): Boolean {
        var isSuccess = false

        for (source in selectedDirectories) {
            val sourcePath =
                if (source.path.last() == PATH_SEPARATOR_CHAR) {
                    source.path.dropLast(SUFFIX_LENGTH)
                } else {
                    source.path
                }

            val command: Command =
                object : Command(ROOT_COMMAND_ID_DEFAULT, "cp -a '$sourcePath' '$copyOrMoveDestination'") {
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
            RootTools.getShell(true).add(command)
            waitForFinish(command)
        }
        return isSuccess
    }

    fun moveFile(
        selectedDirectories: List<FileModel>,
        copyOrMoveDestination: String,
    ): Boolean {
        var isSuccess = false

        for (source in selectedDirectories) {
            val sourcePath =
                if (source.path.last() == PATH_SEPARATOR_CHAR) {
                    source.path.dropLast(SUFFIX_LENGTH)
                } else {
                    source.path
                }

            val command: Command =
                object : Command(ROOT_COMMAND_ID_DEFAULT, "mv '$sourcePath' '$copyOrMoveDestination'") {
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
            RootTools.getShell(true).add(command)
            waitForFinish(command)
        }
        return isSuccess
    }
}
