package com.erman.usurf.directory.model

import android.content.Context
import com.erman.usurf.preference.domain.PreferencesRepository
import com.erman.usurf.storage.domain.StorageDirectoryRepository
import com.erman.usurf.storage.domain.StoragePathsProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DirectoryModelTest {
    private lateinit var tempDir: File
    private lateinit var directoryModel: DirectoryModel

    @Before
    fun setUp() {
        tempDir =
            File.createTempFile("usurf_dm_test_", null).apply {
                delete()
                mkdir()
            }
        val context: Context = RuntimeEnvironment.getApplication()
        val storagePaths: Set<String> = setOf(tempDir.absolutePath, tempDir.absolutePath)
        directoryModel =
            DirectoryModel(
                preferencesRepository = fakePreferencesRepository(),
                storageDirectoryRepository = fakeStorageDirectoryRepository(),
                rootHandler = RootHandler(),
                context = context,
                storagePathsProvider = fakeStoragePathsProvider(storagePaths),
            )
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun getFileModelsFromDirectory_emptyDirectory_returnsEmptyList() =
        runTest {
            val result = directoryModel.getFileModelsFromDirectory(tempDir.absolutePath)
            assertTrue(result.isEmpty())
        }

    @Test
    fun createFolder_folderExistsOnDisk() =
        runTest {
            directoryModel.createFolder(tempDir.absolutePath, "NewFolder")
            val dir = File(tempDir, "NewFolder")
            assertTrue(dir.exists())
            assertTrue(dir.isDirectory)
        }

    @Test
    fun createFile_fileExistsOnDisk() =
        runTest {
            directoryModel.createFile(tempDir.absolutePath, "new_file.txt")
            val file = File(tempDir, "new_file.txt")
            assertTrue(file.exists())
            assertTrue(file.isFile)
        }

    @Test
    fun delete_fileNoLongerExists() =
        runTest {
            val file = File(tempDir, "to_delete.txt")
            file.writeText("content")
            val fileModel =
                FileModel(
                    path = file.absolutePath,
                    name = file.name,
                    isDirectory = false,
                )
            directoryModel.delete(listOf(fileModel))
            assertFalse(file.exists())
        }

    @Test
    fun delete_directoryRemovedRecursively() =
        runTest {
            val dir = File(tempDir, "to_delete_dir")
            dir.mkdir()
            File(dir, "nested.txt").writeText("nested")
            val fileModel =
                FileModel(
                    path = dir.absolutePath,
                    name = dir.name,
                    isDirectory = true,
                )
            directoryModel.delete(listOf(fileModel))
            assertFalse(dir.exists())
        }

    @Test
    fun rename_fileAppearsWithNewName() =
        runTest {
            val original = File(tempDir, "old_name.txt")
            original.writeText("data")
            val fileModel =
                FileModel(
                    path = original.absolutePath,
                    name = original.name,
                    isDirectory = false,
                )
            directoryModel.rename(fileModel, "new_name.txt")
            assertFalse(original.exists())
            val renamed = File(tempDir, "new_name.txt")
            assertTrue(renamed.exists())
            assertEquals("data", renamed.readText())
        }

    @Test
    fun copyFile_fileExistsInDestinationAndOriginalRemains() =
        runTest {
            val sourceFile = File(tempDir, "source.txt")
            sourceFile.writeText("copy me")
            val destDir = File(tempDir, "dest")
            destDir.mkdir()
            val fileModel =
                FileModel(
                    path = sourceFile.absolutePath,
                    name = sourceFile.name,
                    isDirectory = false,
                )
            directoryModel.copyFile(listOf(fileModel), destDir.absolutePath)
            assertTrue(sourceFile.exists())
            val copied = File(destDir, "source.txt")
            assertTrue(copied.exists())
            assertEquals("copy me", copied.readText())
        }

    @Test
    fun copyFile_directoryCopiedRecursively() =
        runTest {
            val sourceDir = File(tempDir, "source_dir")
            sourceDir.mkdir()
            File(sourceDir, "inner.txt").writeText("inner content")
            val destDir = File(tempDir, "dest_dir")
            destDir.mkdir()
            val fileModel =
                FileModel(
                    path = sourceDir.absolutePath,
                    name = sourceDir.name,
                    isDirectory = true,
                )
            directoryModel.copyFile(listOf(fileModel), destDir.absolutePath)
            val copiedDir = File(destDir, "source_dir")
            assertTrue(copiedDir.exists())
            assertTrue(copiedDir.isDirectory)
            val copiedFile = File(copiedDir, "inner.txt")
            assertTrue(copiedFile.exists())
            assertEquals("inner content", copiedFile.readText())
        }

    @Test
    fun moveFile_fileExistsInDestinationAndOriginalRemoved() =
        runTest {
            val sourceFile = File(tempDir, "to_move.txt")
            sourceFile.writeText("move me")
            val destDir = File(tempDir, "move_dest")
            destDir.mkdir()
            val fileModel =
                FileModel(
                    path = sourceFile.absolutePath,
                    name = sourceFile.name,
                    isDirectory = false,
                )
            directoryModel.moveFile(listOf(fileModel), destDir.absolutePath)
            assertFalse(sourceFile.exists())
            val moved = File(destDir, "to_move.txt")
            assertTrue(moved.exists())
            assertEquals("move me", moved.readText())
        }

    @Test
    fun moveFile_directoryMovedRecursively() =
        runTest {
            val sourceDir = File(tempDir, "move_source_dir")
            sourceDir.mkdir()
            File(sourceDir, "file.txt").writeText("data")
            val destDir = File(tempDir, "move_dest_dir")
            destDir.mkdir()
            val fileModel =
                FileModel(
                    path = sourceDir.absolutePath,
                    name = sourceDir.name,
                    isDirectory = true,
                )
            directoryModel.moveFile(listOf(fileModel), destDir.absolutePath)
            assertFalse(sourceDir.exists())
            val movedDir = File(destDir, "move_source_dir")
            assertTrue(movedDir.exists())
            val movedFile = File(movedDir, "file.txt")
            assertTrue(movedFile.exists())
            assertEquals("data", movedFile.readText())
        }

    private fun fakePreferencesRepository(): PreferencesRepository =
        object : PreferencesRepository {
            override fun getRootAccessPreference(): Boolean = false

            override fun setRootAccessPreference(value: Boolean) {}

            override fun getFileSortPreference(): String? = "Sort by name"

            override fun setFileSortPreference(value: String) {}

            override fun getShowHiddenPreference(): Boolean = true

            override fun setShowHiddenPreference(value: Boolean) {}

            override fun getShowThumbnailsPreference(): Boolean = true

            override fun setShowThumbnailsPreference(value: Boolean) {}

            override fun setAscendingOrderPreference(value: Boolean) {}

            override fun setDescendingOrderPreference(value: Boolean) {}

            override fun getDescendingOrderPreference(): Boolean = false
        }

    private fun fakeStorageDirectoryRepository(): StorageDirectoryRepository =
        object : StorageDirectoryRepository {
            override fun getChosenUri(): String? = null

            override fun setChosenUri(uri: String) {}
        }

    private fun fakeStoragePathsProvider(paths: Set<String>): StoragePathsProvider =
        object : StoragePathsProvider {
            override fun getStorageDirectories(): Set<String> = paths
        }
}
