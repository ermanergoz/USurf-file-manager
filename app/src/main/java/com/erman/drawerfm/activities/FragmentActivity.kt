package com.erman.drawerfm.activities

import DirectoryData
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.erman.drawerfm.R

class FragmentActivity : AppCompatActivity(), ListDirFragment.OnItemClickListener {
    lateinit var path: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment)

        this.path = intent.getStringExtra("path")

        var filesListFragment = ListDirFragment.buildFragment(path)

        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, filesListFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onClick(directoryData: DirectoryData) {
        if (directoryData.isFolder) {
            Log.e("clicked item is a", "folder")
        } else {
            Log.e("clicked item is a", "file")
        }
    }

    override fun onLongClick(directoryData: DirectoryData) {
        Log.e("item is", "long clicked")

    }

    override fun onBackPressed() {
        finish()
    }
}
