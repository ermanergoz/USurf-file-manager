package com.erman.drawerfm.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
