package com.erman.drawerfm.activities

import DirectoryData
import ListDirFragment
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.erman.drawerfm.R

class FragmentActivity : AppCompatActivity(), ListDirFragment.OnItemClickListener {
    lateinit var path: String
    private lateinit var filesListFragment: ListDirFragment

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
        setContentView(R.layout.activity_fragment)

        this.path = intent.getStringExtra("path")

        launchFragment(path)
    }

    override fun onClick(directoryData: DirectoryData) {
        if (directoryData.isFolder) {
            Log.e("path of clicked item is", directoryData.path)
        } else {
            Log.e("path of clicked item is", directoryData.path)
        }
        launchFragment(directoryData.path)
    }

    override fun onLongClick(directoryData: DirectoryData) {
        Log.e("item is", "long clicked")
    }

    /* override fun onBackPressed() {
         finish()
     }*/
}
