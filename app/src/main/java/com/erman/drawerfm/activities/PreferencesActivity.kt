package com.erman.drawerfm.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.erman.drawerfm.R
import com.erman.drawerfm.fragments.PreferencesFragment

class PreferencesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)


        supportFragmentManager.beginTransaction()
            .replace(R.id.preferencesContainer, PreferencesFragment()).commit()
    }
}
