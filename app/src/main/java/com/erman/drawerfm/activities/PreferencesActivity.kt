package com.erman.drawerfm.activities

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.erman.drawerfm.R
import com.erman.drawerfm.fragments.FileListPreferencesFragment
import com.erman.drawerfm.fragments.MainPreferencesFragment

class PreferencesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_preferences)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if(intent.getBooleanExtra("isMainActivity", true))
            supportFragmentManager.beginTransaction().replace(R.id.preferencesContainer, MainPreferencesFragment()).commit()
        else
            supportFragmentManager.beginTransaction().replace(R.id.preferencesContainer, FileListPreferencesFragment()).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}