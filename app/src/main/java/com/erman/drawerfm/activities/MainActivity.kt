package com.erman.drawerfm.activities

import CreateShortcutDialog
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.erman.drawerfm.R
import com.erman.drawerfm.adapters.ShortcutRecyclerViewAdapter
import com.erman.drawerfm.dialogs.AboutDrawerFMDialog
import com.erman.drawerfm.dialogs.ErrorDialog
import com.erman.drawerfm.dialogs.ShortcutOptionsDialog
import com.erman.drawerfm.interfaces.OnShortcutClickListener
import com.erman.drawerfm.utilities.getStorageDirectories
import com.erman.drawerfm.utilities.getUsedStoragePercentage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.storage_button.view.*
import java.io.File

class MainActivity : AppCompatActivity(), CreateShortcutDialog.DialogCreateShortcutListener,
    ShortcutOptionsDialog.ShortcutOptionListener, OnShortcutClickListener {

    override fun dialogCreateShortcutListener(shortcutName: String, isCanceled: Boolean) {
        if (File(newShortcutPath).exists() && !isCanceled) {
            addShortcut(newShortcutPath, shortcutName)
        }
        isCreateShortcutMode = false
    }

    companion object {
        lateinit var mainActivity: Activity
    }

    private var newShortcutPath = ""
    var isCreateShortcutMode = false
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var adapter: ShortcutRecyclerViewAdapter
    private var storageProgressBarHeight = 20f
    private var buttonSideMargin = 7
    private var storageProgressBarColor: Int = 0
    private var buttonBorder: Int = R.drawable.storage_button_style
    private lateinit var storageButtons: MutableList<View>
    private lateinit var storageDirectories: Set<String>
    private var screenWidth = 0
    private var screenHeight = 0
    //-----------------------------------------------------------------
    private lateinit var preferences: SharedPreferences
    private var sharedPrefFile: String = "com.erman.draverfm"
    lateinit var preferencesEditor: SharedPreferences.Editor
    //-----------------------------------------------------------------

    private var shortcutNames: MutableSet<String> = mutableSetOf("DCIM", "Download")

    private var shortcutPaths: MutableSet<String> =
        mutableSetOf("/storage/emulated/0/Download")

    private fun saveShortcuts() {
        preferences = this.getSharedPreferences(sharedPrefFile, AppCompatActivity.MODE_PRIVATE)

        preferencesEditor = preferences.edit()
        preferencesEditor.putStringSet("shortcut names", shortcutNames)
        preferencesEditor.putStringSet("shortcut paths", shortcutPaths)
        preferencesEditor.apply()
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
                                          arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                                                  Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                          1)
    }

    private fun setTheme() {
        val chosenTheme =
            getSharedPreferences("com.erman.draverfm", Context.MODE_PRIVATE).getString("theme choice", "System default")

        when (chosenTheme) {
            "Dark theme" -> {
                setTheme(R.style.DarkTheme)
                storageProgressBarColor = ResourcesCompat.getColor(resources, R.color.darkBlue, null)
            }
            "Light theme" -> {
                setTheme(R.style.LightTheme)
                storageProgressBarColor = ResourcesCompat.getColor(resources, R.color.lightBlue, null)
            }
            else -> {
                setTheme(R.style.AppTheme)
                storageProgressBarColor =
                    if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) ResourcesCompat.getColor(
                        resources,
                        R.color.darkBlue,
                        null)
                    else ResourcesCompat.getColor(resources, R.color.lightBlue, null)
            }
        }
    }

    private fun setStorageButtonName(button: View) {
        var name = ""

        for (i in button.tag.toString().length - 1 downTo 1) {
            if (button.tag.toString()[i] != '/') name = button.tag.toString()[i] + name
            else break
        }
        if (name == "0") name = "emulated/0"
        else if (name == "") name = "/"

        button.linkText.text = name
    }

    private fun createStorageButtons() {
        storageButtons = mutableListOf()

        for (i in storageDirectories.indices) {
            val layoutInflater: LayoutInflater = LayoutInflater.from(this)
            val storageButtonLayout: View = layoutInflater.inflate(R.layout.storage_button, null, false)

            storageButtons.add(storageButtonLayout)
            storageButtons[i].tag = storageDirectories.elementAt(i)
            setStorageButtonName(storageButtons[i])
            storageButtons[i].linkText.isSingleLine = true
            storageButtons[i].setBackgroundResource(buttonBorder)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                storageButtons[i].progressBar.progressDrawable.colorFilter =
                    BlendModeColorFilter(storageProgressBarColor, BlendMode.SRC_ATOP)
            } else {
                storageButtons[i].progressBar.progressDrawable.setColorFilter(storageProgressBarColor,
                                                                              PorterDuff.Mode.SRC_ATOP)
            }
        }
    }

    private fun addItemsToActivity() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels

        val buttonLayoutParams =
            FrameLayout.LayoutParams(((screenWidth - ((buttonSideMargin * 2) * storageDirectories.size)) / storageDirectories.size),
                                     (screenHeight / (8 + storageButtons.size)))
        buttonLayoutParams.setMargins(buttonSideMargin, 0, buttonSideMargin, 0)

        for (i in storageDirectories.indices) {
            storageButtons[i].progressBar.scaleY = storageProgressBarHeight
            storageUsageBarLayout.addView(storageButtons[i], buttonLayoutParams)
        }
    }

    private fun displayUsedSpace() {
        for (i in storageDirectories.indices) {
            storageButtons[i].progressBar.progress = getUsedStoragePercentage(storageDirectories.elementAt(i))
        }
    }

    private fun createShortcutGrid() {
        layoutManager = GridLayoutManager(this, 2/*number of columns*/)
        shortcutRecyclerView.layoutManager = layoutManager
        adapter = ShortcutRecyclerViewAdapter(this)
        shortcutRecyclerView.adapter = adapter
        adapter.updateData(shortcutNames, shortcutPaths)
    }

    private fun setClickListener() {
        for (i in 0 until storageButtons.size) {
            storageButtons[i].setOnClickListener {
                startFragmentActivity(storageButtons[i].tag.toString(), isCreateShortcutMode)
            }
        }
    }

    private fun displayErrorDialog(errorMessage: String) {
        val newFragment = ErrorDialog(errorMessage)
        newFragment.show(supportFragmentManager, "")
    }

    private fun addShortcut(shortcutPath: String, shortcutName: String) {
        when {
            shortcutPath in shortcutPaths -> displayErrorDialog(getString(R.string.duplicate_shortcut))

            File(shortcutPath).exists() -> {
                shortcutNames.add(shortcutName)
                shortcutPaths.add(shortcutPath)

                adapter.updateData(shortcutNames, shortcutPaths)
                saveShortcuts()
            }
            else -> displayErrorDialog(getString(R.string.invalid_path))
        }
    }

    private fun removeShortcut(shortcut: TextView) {
        shortcutNames.remove(shortcut.text)
        shortcutPaths.remove(shortcut.tag)
        adapter.updateData(shortcutNames, shortcutPaths)
        saveShortcuts()
    }

    private fun renameShortcut(shortcut: TextView, newName: String) {
        val pathTemp = shortcut.tag.toString()

        shortcutNames.remove(shortcut.text)
        shortcutPaths.remove(pathTemp)

        shortcutNames.add(newName)
        shortcutPaths.add(pathTemp)

        shortcut.text = newName

        adapter.updateData(shortcutNames, shortcutPaths)
        saveShortcuts()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        shortcutNames.addAll(getSharedPreferences("com.erman.draverfm",
                                                  Context.MODE_PRIVATE).getStringSet("shortcut names", emptySet())!!)

        shortcutPaths.addAll(getSharedPreferences("com.erman.draverfm",
                                                  Context.MODE_PRIVATE).getStringSet("shortcut paths", emptySet())!!)
        setTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        requestPermissions()

        storageDirectories = getStorageDirectories(this)

        createShortcutGrid()
        createStorageButtons()
        addItemsToActivity()
        displayUsedSpace()
        setClickListener()

        addShortcut.setOnClickListener {
            isCreateShortcutMode = true
            Toast.makeText(this, getString(R.string.new_shortcut_instruction), Toast.LENGTH_LONG).show()
        }
        mainActivity = this

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.deviceWideSearch -> Log.e("option", "deviceWideSearch")
            R.id.settings -> startSettingsActivity()
            R.id.about -> AboutDrawerFMDialog().show(supportFragmentManager, "")
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startFragmentActivity(path: String, isCreateShortcutMode: Boolean) {
        val intent = Intent(this, FragmentActivity::class.java)
        intent.putExtra("path", path)
        if (isCreateShortcutMode) {
            intent.putExtra("isCreateShortcutMode", isCreateShortcutMode)
            startActivityForResult(intent, 1)
        } else startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //in case of new shortcut creation
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                newShortcutPath = data!!.getStringExtra("newShortcutPath")

                val newFragment = CreateShortcutDialog()
                newFragment.show(supportFragmentManager, "")
            }
        }
    }

    private fun startSettingsActivity() {
        val intent = Intent(this, PreferencesActivity::class.java)
        startActivity(intent)
    }

    override fun shortcutOptionListener(isDelete: Boolean, isRename: Boolean, shortcut: TextView, newName: String) {
        if (isDelete) removeShortcut(shortcut)
        if (isRename) renameShortcut(shortcut, newName)
    }

    override fun onClick(shortcut: TextView) {
        startFragmentActivity(shortcut.tag.toString(), isCreateShortcutMode)
    }

    override fun onLongClick(shortcut: TextView) {
        val newFragment = ShortcutOptionsDialog(shortcut)
        newFragment.show(supportFragmentManager, "")
    }

    override fun onBackPressed() {
        if (isCreateShortcutMode) {
            isCreateShortcutMode = false
            Toast.makeText(this, getString(R.string.canceled), Toast.LENGTH_SHORT).show()
        } else finish()
    }
}