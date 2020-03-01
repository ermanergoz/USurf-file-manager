package com.erman.drawerfm.activities

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.erman.drawerfm.R
import com.erman.drawerfm.common.*
import com.erman.drawerfm.utilities.FTPServer
import com.erman.drawerfm.utilities.FTPServerBroadcastListener
import com.erman.drawerfm.utilities.getStorageDirectories
import kotlinx.android.synthetic.main.activity_ftpserver.*

class FTPServerActivity : AppCompatActivity() {

    lateinit var chosenPath: String
    private lateinit var ftpBroadcastListener: FTPServerBroadcastListener
    private lateinit var preferences: SharedPreferences
    lateinit var preferencesEditor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ftpserver)

        preferences = this.getSharedPreferences(SHARED_PREF_FILE, AppCompatActivity.MODE_PRIVATE)
        ftpBroadcastListener = FTPServerBroadcastListener() { updateServiceStatus() }

        var storageDirectories = getStorageDirectories(this)

        for (i in storageDirectories.indices) {
            val radioButton = RadioButton(this)
            radioButton.id = i
            radioButton.text = storageDirectories[i]
            radioButtonGroup.addView(radioButton)

            if (i == 0) {
                radioButton.isChecked = true
                chosenPath = storageDirectories[i]
            }
        }

        radioButtonGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == 0) chosenPath = getStorageDirectories(this)[0]
            if (checkedId == 1) chosenPath = getStorageDirectories(this)[1]
        }

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager  //applicationContext is to avoid memory leak
        val wifiInfo = wifiManager.connectionInfo
        val ip = wifiInfo.ipAddress
        val ipAddress = String.format("%d.%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
        //Formatter.formatIpAddress is deprecated beacuse it doesnt work with ipv6

        urlTextView.text = "ftps://" + ipAddress

        connectButton.setOnClickListener {
            preferencesEditor = preferences.edit()
            if (!getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getBoolean(KEY_INTENT_IS_SERVICE_ACTIVE, false)) {
                preferencesEditor.putBoolean(KEY_INTENT_IS_SERVICE_ACTIVE, true)
                preferencesEditor.apply()
                startService(chosenPath)
            } else {
                preferencesEditor.putBoolean(KEY_INTENT_IS_SERVICE_ACTIVE, false)
                preferencesEditor.apply()
                stopService(Intent(this, FTPServer()::class.java))
            }
        }

        userNameTextView.setOnLongClickListener {
            true
        }

        passwordTextView.setOnLongClickListener {
            true
        }

        portTextView.setOnLongClickListener {
            true
        }
    }

    private fun startService(chosenPath: String) {
        Intent(this, FTPServer()::class.java).also { intent ->
            intent.putExtra(KEY_INTENT_CHOSEN_PATH, chosenPath)
            startService(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        this.let {
            LocalBroadcastManager.getInstance(it).registerReceiver(ftpBroadcastListener, IntentFilter(getString(R.string.file_broadcast_receiver)))
        }
    }

    override fun onPause() {
        super.onPause()

        this.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(ftpBroadcastListener)
        }
    }

    private fun updateServiceStatus() {
        if (getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getBoolean(KEY_INTENT_IS_SERVICE_ACTIVE, false)) connectButton.text =
            getString(R.string.disconnect)
        else connectButton.text = getString(R.string.connect)
    }
}