package com.erman.drawerfm.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.erman.drawerfm.R
import com.erman.drawerfm.dialogs.CreateFileDialog
import com.erman.drawerfm.dialogs.CreateFolderDialog
import com.erman.drawerfm.dialogs.RenameDialog
import com.erman.drawerfm.fragments.ListDirFragment
import copyFile
import createFile
import createFolder
import delete
import kotlinx.android.synthetic.main.activity_fragment.*
import moveFile
import rename
import java.io.File

class FragmentActivity : AppCompatActivity(), ListDirFragment.OnItemClickListener,
    RenameDialog.DialogRenameFileListener, CreateFileDialog.DialogCreateFileListener,
    CreateFolderDialog.DialogCreateFolderListener {
    lateinit var path: String
    private lateinit var filesListFragment: ListDirFragment
    private val fragmentManager: FragmentManager = supportFragmentManager
    private var openedDirectories = mutableListOf<String>()
    var isMoveOperation = false
    var isCopyOperation = false
    var isMultipleSelection = false
    var multipleSelectionList = mutableListOf<File>()

    private fun setTheme() {
        val chosenTheme = getSharedPreferences(
            "com.erman.draverfm", Context.MODE_PRIVATE
        ).getString("theme choice", "System default")

        when (chosenTheme) {
            "Dark theme" -> {
                setTheme(R.style.DarkTheme)
            }
            "Light theme" -> {
                setTheme(R.style.LightTheme)
            }
            else -> {
                setTheme(R.style.AppTheme)
            }
        }
    }

    private fun launchFragment(path: String) {
        if (sideNavigationView.isVisible)
            sideNavigationView.isVisible = false

        filesListFragment = ListDirFragment.buildFragment(
            path,
            getSharedPreferences(
                "com.erman.draverfm",
                Context.MODE_PRIVATE
            ).getBoolean("marquee choice", true)
        )
        openedDirectories.add(path)
        supportActionBar?.title = path

        fragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, filesListFragment)
            .addToBackStack(path)
            .commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(R.layout.activity_fragment)
        this.path = intent.getStringExtra("path")
        sideNavigationView.isVisible = false
        bottomConfNavView.isVisible = false
        newFileFloatingButton.isVisible = false
        newFolderFloatingButton.isVisible = false

        launchFragment(path)
        sideNavigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.action_copy -> {
                    isCopyOperation = true
                    bottomConfNavView.isVisible = true
                }
                R.id.action_move -> {
                    isMoveOperation = true
                    bottomConfNavView.isVisible = true
                }
                R.id.action_rename -> {
                    val newFragment = RenameDialog(getString(R.string.rename_file))
                    newFragment.show(fragmentManager, "")
                }
                R.id.action_delete -> {
                    delete(multipleSelectionList, filesListFragment)
                }
            }
            sideNavigationView.isVisible = false
            isMultipleSelection = false
            true
        }

        bottomConfNavView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.action_OK -> {
                    if (isCopyOperation)
                        copyFile(multipleSelectionList, path, filesListFragment)
                    if (isMoveOperation) {
                        moveFile(multipleSelectionList, path, filesListFragment)
                        isMoveOperation = false
                    }
                }

                R.id.action_cancel -> {
                    isMoveOperation = false
                    isCopyOperation = false
                    bottomConfNavView.isVisible = false
                }
            }
            true
        }
        createNewFloatingButton.setOnClickListener {
            if (newFileFloatingButton.isVisible && newFolderFloatingButton.isVisible) {
                newFileFloatingButton.isVisible = false
                newFolderFloatingButton.isVisible = false
            } else {
                newFileFloatingButton.isVisible = true
                newFolderFloatingButton.isVisible = true
            }
        }
        newFolderFloatingButton.setOnClickListener {
            val newFragment = CreateFolderDialog(getString(R.string.new_directory_name))
            newFragment.show(fragmentManager, "")
        }
        newFileFloatingButton.setOnClickListener {
            val newFragment = CreateFileDialog(getString(R.string.new_file_name))
            newFragment.show(fragmentManager, "")
        }
        tempRefreshButton.setOnClickListener {
            filesListFragment.updateData()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->
                backButtonPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(directoryData: File) {
        if (isMultipleSelection) {
            multipleSelectionList.add(directoryData)
        } else {
            path = directoryData.path

            if (directoryData.isDirectory) {
                launchFragment(directoryData.path)
            } else {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data =
                    FileProvider.getUriForFile(this, "com.erman.drawerfm", File(directoryData.path))
                intent.flags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION.or(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                startActivity(intent)
            }
        }
    }

    override fun onLongClick(directoryData: File) {
        isMultipleSelection = true
        multipleSelectionList.add(directoryData)
        sideNavigationView.isVisible = true
        //selectedDirectory = directoryData
    }

    private fun backButtonPressed() {
        if (sideNavigationView.isVisible) sideNavigationView.isVisible = false
        if (newFileFloatingButton.isVisible && newFolderFloatingButton.isVisible) {
            newFileFloatingButton.isVisible = false
            newFolderFloatingButton.isVisible = false
        } else if (openedDirectories.size > 1) {
            fragmentManager.popBackStack(
                openedDirectories[openedDirectories.size - 1],
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            openedDirectories.removeAt(openedDirectories.size - 1)
            path = openedDirectories[openedDirectories.size - 1]
        } else {
            fragmentManager.popBackStack()
            super.onBackPressed()
        }
    }

    override fun onBackPressed() {
        backButtonPressed()
    }

    override fun dialogRenameFileListener(newFileName: String) {
        rename(multipleSelectionList, newFileName, filesListFragment)
    }

    override fun dialogCreateFileListener(newFileName: String) {
        createFile(
            openedDirectories[openedDirectories.size - 1],
            newFileName,
            filesListFragment
        )
        newFileFloatingButton.isVisible = false
        newFolderFloatingButton.isVisible = false
    }

    override fun dialogCreateFolderListener(newFileName: String) {
        createFolder(
            openedDirectories[openedDirectories.size - 1],
            newFileName,
            filesListFragment
        )
        newFileFloatingButton.isVisible = false
        newFolderFloatingButton.isVisible = false
    }
}