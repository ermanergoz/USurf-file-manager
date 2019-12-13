package com.erman.drawerfm.activities

import DirectoryData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.erman.drawerfm.R
import com.erman.drawerfm.dialogs.AboutDrawerFMDialog
import com.erman.drawerfm.dialogs.RenameDialog
import com.erman.drawerfm.fragments.ListDirFragment
import kotlinx.android.synthetic.main.activity_fragment.*
import java.io.File

class FragmentActivity : AppCompatActivity(), ListDirFragment.OnItemClickListener,
    RenameDialog.DialogRenameFileListener {
    lateinit var path: String
    private lateinit var filesListFragment: ListDirFragment
    private val fragmentManager: FragmentManager = supportFragmentManager
    var openedDirectories = mutableListOf<String>()

    var newFileName = ""
    lateinit var selectedDirectory: DirectoryData

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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(R.layout.activity_fragment)
        this.path = intent.getStringExtra("path")
        sideNavigationView.isVisible = false
        launchFragment(path)

        sideNavigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.action_copy ->
                    Log.e("Copy file at", selectedDirectory.path)

                R.id.action_paste -> {
                    Log.e("Paste file to", selectedDirectory.path)
                    //sideNavigationView.isVisible = false
                }

                R.id.action_move ->
                    Log.e("Move file", selectedDirectory.path)

                R.id.action_cut ->
                    Log.e("Cut", selectedDirectory.path)

                R.id.action_rename -> {
                    Log.e("Rename file", selectedDirectory.path)
                    val newFragment = RenameDialog()
                    newFragment.show(supportFragmentManager, "")
                }
            }
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->
                backButtonPressed()
        }
        return super.onOptionsItemSelected(item)
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
        sideNavigationView.isVisible = true
        selectedDirectory = directoryData
        Log.e("item is", "long clicked")
    }

    private fun backButtonPressed() {
        when {
            sideNavigationView.isVisible -> sideNavigationView.isVisible = false
            openedDirectories.size > 1 -> {
                fragmentManager.popBackStack(
                    openedDirectories[openedDirectories.size - 1],
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
                openedDirectories.removeAt(openedDirectories.size - 1)
                path = openedDirectories[openedDirectories.size - 1]
            }
            else -> {
                fragmentManager.popBackStack()
                super.onBackPressed()
            }
        }
    }

    override fun onBackPressed() {
        backButtonPressed()
    }

    override fun dialogRenameFileListener(newFileName: String) {
        this.newFileName = newFileName
        renameFie()
    }

    private fun renameFie() {
        Log.e(newFileName, "")
        var prev = File("/storage/emulated/0/Download/joker-laugh.jpg")
/*
        var name = ""

        for (i in selectedPath.length - 1 downTo 1) {
            if (selectedPath[i] != '/')
                name = selectedPath[i] + name
            else
                break
        }

        selectedPath.replaceRange(name.length, selectedPath.length, newFileName)
*/
        var new = File("/storage/emulated/0/Download/aaaaaaajoker.jpg")

        var isSuccess = prev.renameTo(new)

        Log.e(isSuccess.toString(), "")
    }
}