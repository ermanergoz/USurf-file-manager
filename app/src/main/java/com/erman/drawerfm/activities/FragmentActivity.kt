package com.erman.drawerfm.activities

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.erman.drawerfm.R
import com.erman.drawerfm.fragments.FileSearchFragment
import com.erman.drawerfm.fragments.ListDirFragment
import com.erman.drawerfm.utilities.*
import kotlinx.android.synthetic.main.activity_fragment.*
import java.io.File
import android.view.inputmethod.InputMethodManager
import com.erman.drawerfm.dialogs.*

class FragmentActivity : AppCompatActivity(), ListDirFragment.OnItemClickListener,
    FileSearchFragment.OnItemClickListener,
    RenameDialog.DialogRenameFileListener, CreateFileDialog.DialogCreateFileListener,
    CreateFolderDialog.DialogCreateFolderListener, SearchView.OnQueryTextListener {
    lateinit var path: String
    lateinit var longClickedFile: File
    private lateinit var filesListFragment: ListDirFragment
    private lateinit var filesSearchFragment: FileSearchFragment
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

        filesListFragment = ListDirFragment.buildFragment(path)
        openedDirectories.add(path)
        pathTextView.text = path

        fragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, filesListFragment)
            .addToBackStack(path)
            .commit()
    }

    private fun launchSearchFragment(path: String, fileSearchQuery: String) {
        if (optionButtonBar.isVisible)
            optionButtonBar.isVisible = false

        pathTextView.text = getString(R.string.results_for) + " " + fileSearchQuery

        filesSearchFragment = FileSearchFragment.buildSearchFragment(
            getSearchedFiles(path, fileSearchQuery)
        )

        fragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, filesSearchFragment)
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
        moreOptionButtonBar.isVisible = false
        confirmationButtonBar.isVisible = false
        newFileFloatingButton.isVisible = false
        newFolderFloatingButton.isVisible = false

        launchFragment(path)

        copyButton.setOnClickListener {
            isCopyOperation = true
            showConfirmationButtons()
        }

        moveButton.setOnClickListener {
            isMoveOperation = true
            deactivateMultipleSelectionMode()
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

        moreButton.setOnClickListener {
            if (moreOptionButtonBar.isVisible) {
                moreOptionButtonBar.isVisible = false
                moreButton.text = getString(R.string.more)
            } else {
                moreOptionButtonBar.isVisible = true
                moreButton.text = getString(R.string.collapse)
            }
        }

        informationButton.setOnClickListener {
            val newFragment = FileInformationDialog(longClickedFile)
            newFragment.show(supportFragmentManager, "")
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
    }

    private fun deactivateMultipleSelectionMode() {
        isMultipleSelection = false
        moreButton.text = getString(R.string.more)
    }

    private fun finishAndUpdate() {
        isMoveOperation = false
        isCopyOperation = false
        multipleSelectionList.clear()
        deactivateMultipleSelectionMode()
        optionButtonBar.isVisible = false
        moreOptionButtonBar.isVisible = false
        confirmationButtonBar.isVisible = false

        multipleSelectionList.clear()
        updateFragment()
    }

    private fun showConfirmationButtons() {
        optionButtonBar.isVisible = false
        moreOptionButtonBar.isVisible = false
        confirmationButtonBar.isVisible = true
    }

    private fun displayErrorDialog(errorMessage: String) {
        val newFragment = ErrorDialog(errorMessage)
        newFragment.show(supportFragmentManager, "")
    }

    override fun onClick(directory: File) {
        if (isMultipleSelection) {
            if (multipleSelectionList.contains(directory)) {
                multipleSelectionList.removeAt(multipleSelectionList.indexOf(directory))
            } else {
                multipleSelectionList.add(directory)
            }
            if (multipleSelectionList.isEmpty()) {
                finishAndUpdate()
            }
        } else {
            path = directory.path

            if (directory.isDirectory) {
                launchFragment(directory.path)
            } else {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data =
                    FileProvider.getUriForFile(this, "com.erman.drawerfm", File(directory.path))
                try {
                    intent.flags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION.or(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    startActivity(intent)
                } catch (err: ActivityNotFoundException) {
                    displayErrorDialog("This extension is not supported.")
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_fragment_activity, menu)

        val searchManager =
            getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val search =
            menu!!.findItem(R.id.fileSearch).actionView as SearchView
        search.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        search.setOnQueryTextListener(this)

        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->
                backButtonPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showOptionButtons(isExtensionZip: Boolean) {
        optionButtonBar.isVisible = true
        confirmationButtonBar.isVisible = false
        if (isExtensionZip)
            compressButton.isVisible = false
        else
            extractButton.isVisible = false
    }

    fun triggerStorageAccessFramework() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        this.startActivityForResult(intent, 3);
    }

    override fun onLongClick(directory: File) {
        isMultipleSelection = true

        longClickedFile=directory

        if (multipleSelectionList.contains(directory)) {
            multipleSelectionList.removeAt(multipleSelectionList.indexOf(directory))
        } else {
            multipleSelectionList.add(directory)
        }
        showOptionButtons(directory.extension == "zip")
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
            pathTextView.text = path
        } else {
            fragmentManager.popBackStack()
            super.onBackPressed()
        }
    }

    override fun onBackPressed() {
        backButtonPressed()
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

        // On Android 5, trigger storage access framework.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //TODO:this function should be moved to proper location before calling this function which launches the files app
            //TODO:we need to saved the data we need to do that operatiın we want by assigning them to the variables
            //triggerStorageAccessFramework()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //TODO:this lifecycle func is called after launching the files app for ext storage
        //TODO: and we need to do the operations on those files here
        //TODO:and also check if we have values for the variables

        super.onActivityResult(requestCode, resultCode, data)

        //TODO:WE NEED TO make the variables we assigned in line 315 we need to reset them after we are done with the operation
    }

    private fun hideKeyboard() {
        val inputManager: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            currentFocus?.windowToken,
            InputMethodManager.SHOW_FORCED
        )
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        if (query != null) {
            launchSearchFragment(path, query)
            hideKeyboard()
        }
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return true
    }
}