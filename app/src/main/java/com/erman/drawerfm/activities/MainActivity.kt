package com.erman.drawerfm.activities

import CreateShortcutDialog
import ShortcutRecyclerViewAdapter
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.erman.drawerfm.R
import com.erman.drawerfm.dialogs.ErrorDialog
import getStorageDirectories
import getUsedStoragePercentage
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.ArrayList

class MainActivity : AppCompatActivity(), CreateShortcutDialog.DialogCreateShortcutListener {

    override fun dialogCreateShortcutListener(shortcutPath: String, shortcutName: String) {
        addShortcut(shortcutPath, shortcutName)
    }

    lateinit var layoutManager: GridLayoutManager
    lateinit var adapter: ShortcutRecyclerViewAdapter

    var storageProgressBarHeight = 3f
    var buttonSideMargin = 7
    var storageProgressBarColor: Int = 0

    private var buttonBorder: Int = 0

    private lateinit var buttons: MutableList<Button>
    private lateinit var storageBars: MutableList<ProgressBar>
    private lateinit var storageDirectories: ArrayList<String>
    private var screenWidth = R.drawable.button_style_light

    var shortcuts: MutableMap<String, String> = mutableMapOf(
        "DCIM" to "/storage/emulated/0/DCIM",
        "Download" to "/storage/emulated/0/Download",
        "Pictures" to "/storage/emulated/0/Pictures",
        "Movies" to "/storage/emulated/0/Movies",
        "Music" to "/storage/emulated/0/Music"
    )

    private fun setButtonBorderColor() {
        if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
            storageProgressBarColor = Color.parseColor("#168DDA")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                this.buttonBorder = R.drawable.button_style_dark
            }
        } else {
            storageProgressBarColor = Color.parseColor("#99CBFD")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                this.buttonBorder = R.drawable.button_style_light
            }
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 1
        )
    }

    private fun nameStorageButtons(button: Button) {
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

        button.text = name
    }

    private fun createButtons() {
        buttons = mutableListOf()

        for (i in 0 until storageDirectories.size) {
            buttons.add(Button(this))
            buttons[i].tag = storageDirectories[i]
            nameStorageButtons(buttons[i])
            buttons[i].isSingleLine = true
            buttons[i].setBackgroundResource(buttonBorder)
        }
    }

    private fun createStorageBar() {
        storageBars = mutableListOf()

        for (i in 0 until buttons.size) {
            storageBars.add(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal))
            storageBars[i].scaleY = storageProgressBarHeight

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                storageBars[i].progressDrawable.colorFilter =
                    BlendModeColorFilter(storageProgressBarColor, BlendMode.SRC_ATOP)
            } else {
                storageBars[i].progressDrawable.setColorFilter(
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
            (ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        buttonLayoutParams.setMargins(buttonSideMargin, 0, buttonSideMargin, 0)

        for (i in 0 until storageDirectories.size) {
            storageUsageBarLayout.addView(storageBars[i], buttonLayoutParams)
        }
    }

    private fun displayUsedSpace() {
        for (i in 0 until storageDirectories.size) {
            storageBars[i].progress = getUsedStoragePercentage(storageDirectories[i])
        }
    }

    private fun createShortcutGrid() {
        layoutManager = GridLayoutManager(this, 2/*number of columns*/)
        shortcutRecyclerView.layoutManager = layoutManager

        adapter = ShortcutRecyclerViewAdapter(buttonBorder)

        shortcutRecyclerView.adapter = adapter
        adapter.updateData(shortcuts)
    }

    private fun setClickListener() {
        for (i in 0 until buttons.size) {
            buttons[i].setOnClickListener {
                startFragmentActivity(buttons[i].tag.toString())
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
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        requestPermissions()

        storageDirectories = getStorageDirectories(this)

        setButtonBorderColor()
        createShortcutGrid()
        createButtons()
        createStorageBar()
        addItemsToActivity()
        displayUsedSpace()
        setClickListener()

        addShortcut.setOnClickListener {
            val newFragment = CreateShortcutDialog()
            newFragment.show(supportFragmentManager, "")
        }
    }

    private fun startFragmentActivity(path: String) {
        val intent = Intent(this, FragmentActivity::class.java)
        intent.putExtra("path", path)
        startActivity(intent)
    }
}