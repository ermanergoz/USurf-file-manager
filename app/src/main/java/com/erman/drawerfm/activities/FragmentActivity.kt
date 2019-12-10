package com.erman.drawerfm.activities

import DirectoryData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentManager
import com.erman.drawerfm.R
import com.erman.drawerfm.fragments.ListDirFragment
import java.io.File

class FragmentActivity : AppCompatActivity(), ListDirFragment.OnItemClickListener {
    lateinit var path: String
    private lateinit var filesListFragment: ListDirFragment
    private val fragmentManager: FragmentManager = supportFragmentManager
    var openedDirectories = mutableListOf<String>()

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
        filesListFragment = ListDirFragment.buildFragment(
            path,
            getSharedPreferences(
                "com.erman.draverfm",
                Context.MODE_PRIVATE
            ).getBoolean("marquee choice", true)
        )

        openedDirectories.add(path)

        fragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, filesListFragment)
            .addToBackStack(path)
            .commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme()
        setContentView(R.layout.activity_fragment)

        this.path = intent.getStringExtra("path")

        launchFragment(path)
    }

    override fun onClick(directoryData: DirectoryData) {
        path = directoryData.path

        if (directoryData.isFolder) {
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

    override fun onLongClick(directoryData: DirectoryData) {
        Log.e("item is", "long clicked")
    }

    override fun onBackPressed() {
        if (openedDirectories.size > 1) {
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
}