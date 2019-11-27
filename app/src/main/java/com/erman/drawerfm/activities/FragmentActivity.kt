package com.erman.drawerfm.activities

import DirectoryData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.erman.drawerfm.R
import com.erman.drawerfm.fragments.ListDirFragment
import java.io.File


class FragmentActivity : AppCompatActivity(), ListDirFragment.OnItemClickListener {
    lateinit var path: String
    private lateinit var filesListFragment: ListDirFragment
    lateinit var initialPath: String

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
        this.supportFragmentManager.popBackStack()

        filesListFragment = ListDirFragment.buildFragment(
            path,
            getSharedPreferences(
                "com.erman.draverfm",
                Context.MODE_PRIVATE
            ).getBoolean("marquee choice", true)
        )

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
            launchFragment(directoryData.path)
        } else {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = FileProvider.getUriForFile(this, "com.erman.drawerfm", File(directoryData.path))
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION.or(Intent.FLAG_GRANT_WRITE_URI_PERMISSION )
            startActivity(intent)
        }
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

                if (File(path).canRead()) {
                    launchFragment(path)
                } else
                    finish()
                break
            }
        }
    }
}