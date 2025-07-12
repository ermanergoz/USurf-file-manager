package com.erman.usurf.directory.model

import com.erman.usurf.directory.utils.FOLDER_SUFFIX
import com.erman.usurf.directory.utils.ROOT_COMMAND_WAIT_TIME_MS
import com.erman.usurf.utils.loge
import com.stericson.RootShell.execution.Command
import com.stericson.RootTools.RootTools
import java.io.File

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
                loge("waitForFinish $err")
            }
        }
    }

    fun isDeviceRooted(): Boolean {
        return RootTools.isRootAvailable()
    }

    fun isRootAccessGiven(): Boolean {
        return RootTools.isAccessGiven()
    }

    fun remountRootDirAs(mountMode: String) {
        var mntCommand = ""

        when (mountMode) {
            MountOption.READ_WRITE.option -> mntCommand = "mount -o rw,remount /"
            MountOption.READ.option -> mntCommand = "mount -o ro,remount /"
        }

        val command: Command =
            object : Command(0, mntCommand) {
                override fun commandTerminated(
                    id: Int,
                    reason: String,
                ) {
                    super.commandTerminated(id, reason)
                    loge("delete commandTerminated $reason")
                }
            }
        RootTools.getShell(true).add(command)
    }

    fun getFileList(path: String): List<String> {
        val fileList: MutableList<String> = mutableListOf()
        // -p adds the trailing slash on directories to distinguish folders from files
        // -a is to display hidden files
        val command: Command =
            object : Command(0, "cd '$path'", "ls -p -a") {
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
                    loge("getFileList commandTerminated $reason")
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
                object : Command(0, "rm -r '${source.path}'") {
                    override fun commandCompleted(
                        id: Int,
                        exitcode: Int,
                    ) {
                        super.commandCompleted(id, exitcode)
                        isSuccess = true
                    }

                    override fun commandTerminated(
                        id: Int,
                        reason: String,
                    ) {
                        super.commandTerminated(id, reason)
                        isSuccess = false
                        loge("delete commandTerminated $reason")
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
            fileName = "$name$FOLDER_SUFFIX"
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
                object : Command(0, "cd '$path'", "mkdir '$folderName'") {
                    override fun commandCompleted(
                        id: Int,
                        exitcode: Int,
                    ) {
                        super.commandCompleted(id, exitcode)
                        isSuccess = true
                    }

                    override fun commandTerminated(
                        id: Int,
                        reason: String,
                    ) {
                        super.commandTerminated(id, reason)
                        isSuccess = false
                        loge("createFolder commandTerminated $reason")
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
                object : Command(0, "cd '$path'", "> '$fileName'") {
                    override fun commandCompleted(
                        id: Int,
                        exitcode: Int,
                    ) {
                        super.commandCompleted(id, exitcode)
                        isSuccess = true
                    }

                    override fun commandTerminated(
                        id: Int,
                        reason: String,
                    ) {
                        super.commandTerminated(id, reason)
                        isSuccess = false
                        loge("createFile commandTerminated $reason")
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
                object : Command(0, "cd $parentDir", "mv '${selectedFile.path}' '${"$parentDir/$newName"}'") {
                    override fun commandCompleted(
                        id: Int,
                        exitcode: Int,
                    ) {
                        super.commandCompleted(id, exitcode)
                        isSuccess = true
                    }

                    override fun commandTerminated(
                        id: Int,
                        reason: String,
                    ) {
                        super.commandTerminated(id, reason)
                        isSuccess = false
                        loge("renameFile commandTerminated $reason")
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
                if (source.path.last() == '/') {
                    source.path.dropLast(1)
                } else {
                    source.path
                }

            val command: Command =
                object : Command(0, "cp -a '$sourcePath' '$copyOrMoveDestination'") {
                    override fun commandCompleted(
                        id: Int,
                        exitcode: Int,
                    ) {
                        super.commandCompleted(id, exitcode)
                        isSuccess = true
                    }

                    override fun commandTerminated(
                        id: Int,
                        reason: String,
                    ) {
                        super.commandTerminated(id, reason)
                        isSuccess = false
                        loge("copyFile commandTerminated $reason")
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
                if (source.path.last() == FOLDER_SUFFIX) {
                    source.path.dropLast(1)
                } else {
                    source.path
                }

            val command: Command =
                object : Command(0, "mv '$sourcePath' '$copyOrMoveDestination'") {
                    override fun commandCompleted(
                        id: Int,
                        exitcode: Int,
                    ) {
                        super.commandCompleted(id, exitcode)
                        isSuccess = true
                    }

                    override fun commandTerminated(
                        id: Int,
                        reason: String,
                    ) {
                        super.commandTerminated(id, reason)
                        isSuccess = false
                        loge("copyFile commandTerminated $reason")
                    }
                }
            RootTools.getShell(true).add(command)
            waitForFinish(command)
        }
        return isSuccess
    }
}
