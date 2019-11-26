package com.erman.drawerfm.activities

import DirectoryData
import com.erman.drawerfm.fragments.ListDirFragment
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import com.erman.drawerfm.R
import java.io.File

class FragmentActivity : AppCompatActivity(), ListDirFragment.OnItemClickListener {
    lateinit var path: String
    private lateinit var filesListFragment: ListDirFragment
    lateinit var initialPath: String

    private fun setTheme() {
        when {
            getSharedPreferences(
                "com.erman.draverfm", Context.MODE_PRIVATE
            ).getString("theme choice", "System default") == "Dark theme" -> {
                setTheme(R.style.DarkTheme)
            }
            getSharedPreferences(
                "com.erman.draverfm", Context.MODE_PRIVATE
            ).getString("theme choice", "System default") == "Light theme" -> {
                setTheme(R.style.LightTheme)
            }
            else -> {
                if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                    setTheme(R.style.DarkTheme)
                } else {
                    setTheme(R.style.LightTheme)
                }
            }
        }
    }

    private fun launchFragment(path: String) {
        this.supportFragmentManager.popBackStack()

        filesListFragment = ListDirFragment.buildFragment(path)

        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, filesListFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme()

        setContentView(R.layout.activity_fragment)

        this.path = intent.getStringExtra("path")
        initialPath = path

        launchFragment(path)
    }

    override fun onClick(directoryData: DirectoryData) {
        if (directoryData.isFolder) {
            Log.e("path of clicked item is", directoryData.path)
        } else {
            Log.e("path of clicked item is", directoryData.path)
        }
        path = directoryData.path
        launchFragment(path)
    }

    override fun onLongClick(directoryData: DirectoryData) {
        Log.e("item is", "long clicked")
    }

    override fun onBackPressed() {
        //TODO: Change this piece of shit and deal with stacks instead!!

        if (initialPath == path)
            finish()

        for (i in path.length - 1 downTo 1) {
            if (path[i] != '/') {
                path = path.replaceRange(i, i + 1, "")
            } else {
                path = path.replaceRange(i, i + 1, "")
                Log.e("path", path)

                if (File(path).canRead()) {
                    launchFragment(path)
                } else
                    finish()
                break
            }
        }
    }
}