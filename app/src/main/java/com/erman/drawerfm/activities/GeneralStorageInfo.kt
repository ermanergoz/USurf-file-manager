package com.erman.drawerfm.activities

import android.content.Context
import android.content.res.Configuration
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.erman.drawerfm.R
import com.erman.drawerfm.utilities.getUsedStoragePercentage
import kotlinx.android.synthetic.main.activity_general_storage_info.*
import kotlinx.android.synthetic.main.storage_button.view.*
import java.util.ArrayList

class GeneralStorageInfo : AppCompatActivity() {

    private lateinit var storageDirectories: ArrayList<String>
    private lateinit var storageProgressBars: MutableList<View>
    private var buttonBorder: Int = R.drawable.storage_button_style
    private var storageProgressBarColor: Int = 0
    private var storageProgressBarHeight = 20f

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
                    if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                        ResourcesCompat.getColor(resources, R.color.darkBlue, null)
                    } else {
                        ResourcesCompat.getColor(resources, R.color.lightBlue, null)
                    }
            }
        }
    }


    private fun createStorageButtons() {
        storageProgressBars = mutableListOf()

        for (i in 0 until storageDirectories.size) {
            val layoutInflater: LayoutInflater = LayoutInflater.from(this)
            val storageButtonLayout: View = layoutInflater.inflate(R.layout.storage_button, null, false)

            storageProgressBars.add(storageButtonLayout)
            storageProgressBars[i].tag = storageDirectories[i]
            storageProgressBars[i].linkText.text = storageDirectories[i]
            storageProgressBars[i].linkText.isSingleLine = true
            storageProgressBars[i].setBackgroundResource(buttonBorder)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                storageProgressBars[i].progressBar.progressDrawable.colorFilter =
                    BlendModeColorFilter(storageProgressBarColor, BlendMode.SRC_ATOP)
            } else {
                storageProgressBars[i].progressBar.progressDrawable.setColorFilter(storageProgressBarColor,
                                                                                   PorterDuff.Mode.SRC_ATOP)
            }
        }
    }

    private fun displayUsedSpace() {
        for (i in 0 until storageDirectories.size) {
            storageProgressBars[i].progressBar.progress = getUsedStoragePercentage(storageDirectories[i])
        }
    }

    private fun addItemsToActivity() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        for (i in 0 until storageDirectories.size) {
            storageProgressBars[i].progressBar.scaleY = storageProgressBarHeight
            storageUsageDataContainer.addView(storageProgressBars[i])
        }
        //TODO: Implement adapter for listView and modify this function accordingly
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_general_storage_info)

        this.storageDirectories = intent.getStringArrayListExtra("storageDirectories")

        createStorageButtons()
        displayUsedSpace()
        addItemsToActivity()
    }
}
