package com.erman.drawerfm.activities

import CreateShortcutDialog
import com.erman.drawerfm.adapters.ShortcutRecyclerViewAdapter
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.erman.drawerfm.R
import com.erman.drawerfm.dialogs.AboutDrawerFMDialog
import com.erman.drawerfm.dialogs.ErrorDialog
import getStorageDirectories
import getUsedStoragePercentage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.storage_button.view.*
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity(), CreateShortcutDialog.DialogCreateShortcutListener {

    override fun dialogCreateShortcutListener(shortcutPath: String, shortcutName: String) {
        addShortcut(shortcutPath, shortcutName)
    }

    companion object {
        lateinit var firsActivity: Activity
    }

    private lateinit var layoutManager: GridLayoutManager
    lateinit var adapter: ShortcutRecyclerViewAdapter

    //TODO: implement SharedPreferences.OnSharedPreferenceChangeListener

    var storageProgressBarHeight = 20f
    var buttonSideMargin = 7
    private var storageProgressBarColor: Int = 0

    private var buttonBorder: Int = 0

    private lateinit var storageButtons: MutableList<View>
    private lateinit var storageDirectories: ArrayList<String>
    private var screenWidth = 0

    private var shortcuts: MutableMap<String, String> = mutableMapOf(
        "DCIM" to "/storage/emulated/0/DCIM",
        "Download" to "/storage/emulated/0/Download",
        "Pictures" to "/storage/emulated/0/Pictures",
        "Movies" to "/storage/emulated/0/Movies",
        "Music" to "/storage/emulated/0/Music"
    )

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 1
        )
    }

    private fun setTheme() {
        val chosenTheme = getSharedPreferences(
            "com.erman.draverfm", Context.MODE_PRIVATE
        ).getString("theme choice", "System default")

        when {
            chosenTheme == "Dark theme" -> {
                setTheme(R.style.DarkTheme)
                storageProgressBarColor =
                    ResourcesCompat.getColor(resources, R.color.darkBlue, null)
            }
            chosenTheme == "Light theme" -> {
                setTheme(R.style.LightTheme)
                storageProgressBarColor =
                    ResourcesCompat.getColor(resources, R.color.lightBlue, null)
            }
            else -> {
                setTheme(R.style.AppTheme)
                storageProgressBarColor = if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                    ResourcesCompat.getColor(resources, R.color.darkBlue, null)
                } else {
                    ResourcesCompat.getColor(resources, R.color.lightBlue, null)
                }
            }
        }
    }

    private fun setStorageButtonName(button: View) {
        var name = ""

        for (i in button.tag.toString().length - 1 downTo 1) {
            if (button.tag.toString()[i] != '/')
                name = button.tag.toString()[i] + name
            else
                break
        }
        if (name == "0")
            name = "emulated/0"
        else if (name == "")
            name = "/"

        button.linkText.text = name
    }

    private fun createStorageButtons() {
        storageButtons = mutableListOf()

        for (i in 0 until storageDirectories.size) {
            val layoutInflater: LayoutInflater = LayoutInflater.from(this)
            val storageButtonLayout: View =
                layoutInflater.inflate(R.layout.storage_button, null, false)

            storageButtons.add(storageButtonLayout)
            storageButtons[i].tag = storageDirectories[i]
            setStorageButtonName(storageButtons[i])
            storageButtons[i].linkText.isSingleLine = true
            storageButtons[i].setBackgroundResource(buttonBorder)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                storageButtons[i].progressBar.progressDrawable.colorFilter =
                    BlendModeColorFilter(storageProgressBarColor, BlendMode.SRC_ATOP)
            } else {
                storageButtons[i].progressBar.progressDrawable.setColorFilter(
                    storageProgressBarColor, PorterDuff.Mode.SRC_ATOP
                )
            }
        }
    }

    private fun addItemsToActivity() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenWidth = displayMetrics.widthPixels

        val buttonLayoutParams = FrameLayout.LayoutParams(
            ((screenWidth - ((buttonSideMargin * 2) * storageDirectories.size)) / storageDirectories.size),
            (170)
            //TODO: Change button height in such way that  it will look nice on different screen sizes
        )
        buttonLayoutParams.setMargins(buttonSideMargin, 0, buttonSideMargin, 0)

        for (i in 0 until storageDirectories.size) {
            storageButtons[i].progressBar.scaleY = storageProgressBarHeight
            storageUsageBarLayout.addView(storageButtons[i], buttonLayoutParams)
        }
    }

    private fun displayUsedSpace() {
        for (i in 0 until storageDirectories.size) {
            storageButtons[i].progressBar.progress =
                getUsedStoragePercentage(storageDirectories[i])
        }
    }

    private fun createShortcutGrid() {
        layoutManager = GridLayoutManager(this, 2/*number of columns*/)
        shortcutRecyclerView.layoutManager = layoutManager
        adapter = ShortcutRecyclerViewAdapter()
        shortcutRecyclerView.adapter = adapter
        adapter.updateData(shortcuts)
    }

    private fun setClickListener() {
        for (i in 0 until storageButtons.size) {
            storageButtons[i].setOnClickListener {
                startFragmentActivity(storageButtons[i].tag.toString())
            }
        }
    }

    private fun displayErrorDialog(errorMessage: String) {
        val newFragment = ErrorDialog(errorMessage)
        newFragment.show(supportFragmentManager, "")
    }

    private fun addShortcut(shortcutPath: String, shortcutName: String) {
        when {
            shortcutPath in shortcuts.values -> displayErrorDialog(getString(R.string.duplicate_shortcut))
            File(shortcutPath).exists() -> {
                shortcuts[shortcutName] = shortcutPath
                adapter.updateData(shortcuts)
            }
            else -> displayErrorDialog(getString(R.string.invalid_path))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        requestPermissions()

        storageDirectories = getStorageDirectories(this)

        createShortcutGrid()
        createStorageButtons()
        addItemsToActivity()
        displayUsedSpace()
        setClickListener()

        addShortcut.setOnClickListener {
            val newFragment = CreateShortcutDialog()
            newFragment.show(supportFragmentManager, "")
        }
        firsActivity = this

        eben.text = getSharedPreferences(
            "com.erman.draverfm", Context.MODE_PRIVATE
        ).getString("theme choice", "System default")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_activity_option_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.deviceWideSearch ->
                Log.e("option", "deviceWideSearch")

            R.id.settings ->
                startSettingsActivity()

            R.id.about ->
                AboutDrawerFMDialog().show(supportFragmentManager, "")
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startFragmentActivity(path: String) {
        val intent = Intent(this, FragmentActivity::class.java)
        intent.putExtra("path", path)
        startActivity(intent)
    }

    private fun startSettingsActivity() {
        val intent = Intent(this, PreferencesActivity::class.java)
        startActivity(intent)
    }
}