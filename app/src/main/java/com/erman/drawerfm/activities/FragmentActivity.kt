package com.erman.drawerfm.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.erman.drawerfm.R

class FragmentActivity : AppCompatActivity() {

    lateinit var path: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment)

        this.path=intent.getStringExtra("path")

        var filesListFragment = ListDirFragment.buildFragment(path)

        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, filesListFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onBackPressed() {
        finish()
    }
}
