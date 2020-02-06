package com.erman.drawerfm.activities

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.erman.drawerfm.fragments.FileSearchFragment
import com.erman.drawerfm.fragments.ListDirFragment
import com.erman.drawerfm.utilities.*
import kotlinx.android.synthetic.main.activity_fragment.*
import java.io.File
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.erman.drawerfm.dialogs.*
import com.erman.drawerfm.interfaces.OnFileClickListener
import android.app.Activity
import android.content.SharedPreferences
import androidx.core.view.isGone
import com.erman.drawerfm.R
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream

class FragmentActivity : AppCompatActivity(), OnFileClickListener, FileSearchFragment.OnItemClickListener, RenameDialog.DialogRenameFileListener,
    CreateNew.DialogCreateFolderListener, SearchView.OnQueryTextListener {
    private var newShortcutPath = ""
    private var isCreateShortcutMode = false
    lateinit var path: String
    lateinit var longClickedFile: File
    private lateinit var filesListFragment: ListDirFragment
    private lateinit var filesSearchFragment: FileSearchFragment
    private val fragmentManager: FragmentManager = supportFragmentManager
    var isMoveOperation = false
    var isCopyOperation = false
    var isMultipleSelection = false
    var multipleSelectionList = mutableListOf<File>()
    var isExtSdCard = false
    private lateinit var preferences: SharedPreferences
    lateinit var preferencesEditor: SharedPreferences.Editor
    private var sharedPrefFile: String = "com.erman.draverfm"

    private fun setTheme() {
        val chosenTheme = getSharedPreferences("com.erman.draverfm", Context.MODE_PRIVATE).getString("theme choice", "System default")

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
        if (optionButtonBar.isVisible) optionButtonBar.isVisible = false

        filesListFragment = ListDirFragment.buildFragment(path)
        pathTextView.text = path

        fragmentManager.beginTransaction().add(R.id.fragmentContainer, filesListFragment).addToBackStack(path).commit()
    }

    private fun launchSearchFragment(path: String, fileSearchQuery: String) {
        if (optionButtonBar.isVisible) optionButtonBar.isVisible = false

        pathTextView.text = getString(R.string.results_for) + " " + fileSearchQuery

        filesSearchFragment = FileSearchFragment.buildSearchFragment(getSearchedFiles(path, fileSearchQuery))

        fragmentManager.beginTransaction().add(R.id.fragmentContainer, filesSearchFragment).addToBackStack(path).commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(R.layout.activity_fragment)
        this.path = intent.getStringExtra("path")
        if (!File(path).isDirectory) {  //if user adds a file to shorcuts instead of a folder and tries to open it from there
            openFile(File(path))
            finish()
        }
        this.isCreateShortcutMode = intent.getBooleanExtra("isCreateShortcutMode", false)
        this.isExtSdCard = intent.getBooleanExtra("isExtSdCard", false)
        optionButtonBar.isVisible = false
        moreOptionButtonBar.isVisible = false
        confirmationButtonBar.isVisible = false
        newFileFloatingButton.isVisible = false
        newFolderFloatingButton.isVisible = false

        launchFragment(path)

        if (isExtSdCard) {  //hafıza kartı
            triggerStorageAccessFramework()
        }

        copyButton.setOnClickListener {
            isCopyOperation = true
            deactivateMultipleSelectionMode()
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
            delete(this, multipleSelectionList, isExtSdCard) { finishAndUpdate() }
        }

        OKButton.setOnClickListener {
            if (isCopyOperation) {
                copyFile(this, multipleSelectionList, path, isExtSdCard) { finishAndUpdate() }
                isMoveOperation = false
            }
            if (isMoveOperation) {
                moveFile(this, multipleSelectionList, path, isExtSdCard) { finishAndUpdate() }
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
            val newFragment = CreateNew(getString(R.string.new_directory_name), "folder")
            newFragment.show(fragmentManager, "")
        }
        newFileFloatingButton.setOnClickListener {
            val newFragment = CreateNew(getString(R.string.new_file_name), "file")
            newFragment.show(fragmentManager, "")
        }

        shareButton.setOnClickListener {
            val fileUris: ArrayList<Uri> = arrayListOf()

            for (i in 0 until multipleSelectionList.size) {
                fileUris.add(FileProvider.getUriForFile(this, "com.erman.drawerfm", //(use your app signature + ".provider" )
                                                        multipleSelectionList[i]))  //used this instead of File().toUri to avoid FileUriExposedException
            }

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
                type = "*/*"
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
        }

        compressButton.setOnClickListener {
            val newFragment = CreateNew(getString(R.string.name), "zip")
            newFragment.show(fragmentManager, "")
        }

        extractButton.setOnClickListener {
            unzip(this, multipleSelectionList) {finishAndUpdate()}
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

    private fun openFile(directory: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = FileProvider.getUriForFile(this, "com.erman.drawerfm", File(directory.path))
        try {
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION.or(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            startActivity(intent)
        } catch (err: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.file_unsupported_or_no_application), Toast.LENGTH_LONG).show()
        }
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
                openFile(directory)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_fragment_activity, menu)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val search = menu!!.findItem(R.id.fileSearch).actionView as SearchView
        search.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        search.setOnQueryTextListener(this)

        return super.onCreateOptionsMenu(menu)
    }

    private fun startSettingsActivity() {
        val intent = Intent(this, PreferencesActivity::class.java)
        intent.putExtra("isMainActivity", false)
        intent.putExtra("currentPath", path)
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) backButtonPressed()
        if (item.itemId == R.id.subMenu) {
            startSettingsActivity()
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showOptionButtons(isExtensionZip: Boolean) {
        optionButtonBar.isGone = false
        confirmationButtonBar.isGone = true
        if (isExtensionZip) {
            compressButton.isGone = true
            extractButton.isGone = false
        } else {
            extractButton.isGone = true
            compressButton.isGone = false
        }
    }

    override fun onLongClick(directory: File) {
        if (isCreateShortcutMode) {
            newShortcutPath = directory.path

            val intent = Intent()
            intent.putExtra("newShortcutPath", newShortcutPath)
            setResult(RESULT_OK, intent)
            finish()
        } else {
            isMultipleSelection = true

            longClickedFile = directory

            if (multipleSelectionList.contains(directory)) {
                multipleSelectionList.removeAt(multipleSelectionList.indexOf(directory))
            } else {
                multipleSelectionList.add(directory)
            }
            showOptionButtons(directory.extension == "zip")
        }
    }

    private fun backButtonPressed() {
        if (optionButtonBar.isVisible && !isMoveOperation && !isCopyOperation) {
            finishAndUpdate()
        } else if (newFileFloatingButton.isVisible && newFolderFloatingButton.isVisible) {
            newFileFloatingButton.isVisible = false
            newFolderFloatingButton.isVisible = false

        } else if (fragmentManager.backStackEntryCount > 1) {
            fragmentManager.popBackStack(path, FragmentManager.POP_BACK_STACK_INCLUSIVE)

            path = File(path).parent //move up in the directory

            pathTextView.text = path
        } else if (fragmentManager.backStackEntryCount == 1 && File(File(path).parent).canWrite()) {
            path = File(path).parent
            pathTextView.text = path
            launchFragment(path)
        } else {
            fragmentManager.popBackStackImmediate()
            super.onBackPressed()
        }
    }

    override fun onBackPressed() {
        backButtonPressed()
    }

    override fun dialogRenameFileListener(newFileName: String) {
        rename(this, multipleSelectionList, newFileName, isExtSdCard) { finishAndUpdate() }
    }

    override fun dialogCreateNewListener(newFileName: String, whatToCreate: String) {
        if (whatToCreate == "folder") createFolder(this, path, newFileName, isExtSdCard) { finishAndUpdate() }

        if (whatToCreate == "file") createFile(this, path, newFileName, isExtSdCard) { finishAndUpdate() }

        if (whatToCreate == "zip") zipFile(this, multipleSelectionList, newFileName) { finishAndUpdate() }

        newFileFloatingButton.isVisible = false
        newFolderFloatingButton.isVisible = false
    }

    private fun updateFragment() {
        val broadcastIntent = Intent()
        broadcastIntent.action = applicationContext.getString(R.string.file_broadcast_receiver)
        broadcastIntent.putExtra("path for broadcast", path)
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
    }


    fun triggerStorageAccessFramework() {   //https://developer.android.com/reference/android/support/v4/provider/DocumentFile
        // On Android 5, trigger storage access framework.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)   //If you really do need full access to an entire subtree of documents,
            this.startActivityForResult(intent, 3)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode === Activity.RESULT_OK) {    //TODO: Change this. It is deprecated!
            val treeUri = data!!.data
            //val pickedDir = DocumentFile.fromTreeUri(this, treeUri!!)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                contentResolver.takePersistableUriPermission(treeUri!!,
                                                             Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            preferences = this.getSharedPreferences(sharedPrefFile, AppCompatActivity.MODE_PRIVATE)
            preferencesEditor = preferences.edit()
            preferencesEditor.putString("extSdCardChosenUri", treeUri.toString())
            preferencesEditor.apply()
        } else {
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun hideKeyboard() {
        val inputManager: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(currentFocus?.windowToken, InputMethodManager.SHOW_FORCED)
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