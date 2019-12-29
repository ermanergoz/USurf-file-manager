package com.erman.drawerfm.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.erman.drawerfm.R
import com.erman.drawerfm.dialogs.CreateFileDialog
import com.erman.drawerfm.dialogs.CreateFolderDialog
import com.erman.drawerfm.dialogs.RenameDialog
import com.erman.drawerfm.fragments.ListDirFragment
import com.erman.drawerfm.utilities.*
import kotlinx.android.synthetic.main.activity_fragment.*
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
        if (optionButtonBar.isVisible)
            optionButtonBar.isVisible = false

        filesListFragment = ListDirFragment.buildFragment(
            path,
            getSharedPreferences(
                "com.erman.draverfm",
                Context.MODE_PRIVATE
            ).getBoolean("marquee choice", true)
        )
        openedDirectories.add(path)
        pathTextView.text = path

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
        optionButtonBar.isVisible = false
        newFileFloatingButton.isVisible = false
        newFolderFloatingButton.isVisible = false

        launchFragment(path)

        copyButton.setOnClickListener {
            isCopyOperation = true
            showConfirmationButtons()
        }

        moveButton.setOnClickListener {
            isMoveOperation = true
            showConfirmationButtons()
        }

        renameButton.setOnClickListener {
            val newFragment = RenameDialog(getString(R.string.rename_file))
            newFragment.show(fragmentManager, "")
        }

        deleteButton.setOnClickListener {
            delete(multipleSelectionList) { finishAndUpdate() }
        }

        OKButton.setOnClickListener {
            if (isCopyOperation)
                copyFile(multipleSelectionList, path) { finishAndUpdate() }
            if (isMoveOperation) {
                moveFile(multipleSelectionList, path) { finishAndUpdate() }
                isMoveOperation = false
            }
            updateFragment()
            optionButtonBar.isVisible = false
        }

        cancelButton.setOnClickListener {
            finishAndUpdate()
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
            updateFragment()
        }
    }

    private fun finishAndUpdate() {
        isMoveOperation = false
        isCopyOperation = false
        multipleSelectionList.clear()
        isMultipleSelection = false
        optionButtonBar.isVisible = false

        multipleSelectionList.clear()
        updateFragment()
    }

    private fun showConfirmationButtons() {
        OKButton.isVisible = true
        cancelButton.isVisible = true
        copyButton.isVisible = false
        moveButton.isVisible = false
        renameButton.isVisible = false
        deleteButton.isVisible = false
        zipButton.isVisible = false
        unzipButton.isVisible = false
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
            if (multipleSelectionList.contains(directoryData)) {
                multipleSelectionList.removeAt(multipleSelectionList.indexOf(directoryData))
            } else {
                multipleSelectionList.add(directoryData)
            }

            if (multipleSelectionList.isEmpty()) {
                finishAndUpdate()
            }
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

    private fun showOptionButtons(isExtensionZip: Boolean) {
        optionButtonBar.isVisible = true
        OKButton.isVisible = false
        cancelButton.isVisible = false
        if (isExtensionZip)
            zipButton.isVisible = false
        else
            unzipButton.isVisible = false
    }

    override fun onLongClick(directoryData: File) {
        isMultipleSelection = true



        if (multipleSelectionList.contains(directoryData)) {
            multipleSelectionList.removeAt(multipleSelectionList.indexOf(directoryData))
        } else {
            multipleSelectionList.add(directoryData)
        }
        showOptionButtons(directoryData.extension == "zip")
    }

    private fun backButtonPressed() {
        if (optionButtonBar.isVisible && !isMoveOperation && !isCopyOperation) {
            finishAndUpdate()
        } else if (newFileFloatingButton.isVisible && newFolderFloatingButton.isVisible) {
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
        pathTextView.text = path
    }

    override fun dialogRenameFileListener(newFileName: String) {
        rename(multipleSelectionList, newFileName) { finishAndUpdate() }
    }

    override fun dialogCreateFileListener(newFileName: String) {
        createFile(
            openedDirectories[openedDirectories.size - 1], newFileName
        ) { updateFragment() }
        newFileFloatingButton.isVisible = false
        newFolderFloatingButton.isVisible = false
    }

    override fun dialogCreateFolderListener(newFileName: String) {
        createFolder(
            openedDirectories[openedDirectories.size - 1],
            newFileName
        ) { updateFragment() }
        newFileFloatingButton.isVisible = false
        newFolderFloatingButton.isVisible = false
    }

    private fun updateFragment() {
        val broadcastIntent = Intent()
        broadcastIntent.action = applicationContext.getString(R.string.file_broadcast_receiver)
        broadcastIntent.putExtra(
            "path for broadcast",
            openedDirectories[openedDirectories.size - 1]
        )
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
    }
}