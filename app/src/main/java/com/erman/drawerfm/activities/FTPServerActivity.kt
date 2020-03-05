package com.erman.drawerfm.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.FragmentManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.erman.drawerfm.R
import com.erman.drawerfm.common.*
import com.erman.drawerfm.dialogs.EditDialog
import com.erman.drawerfm.dialogs.EditPasswordDialog
import com.erman.drawerfm.dialogs.EditPortDialog
import com.erman.drawerfm.services.FTPServer
import com.erman.drawerfm.utilities.FTPServerBroadcastListener
import com.erman.drawerfm.utilities.getStorageDirectories
import kotlinx.android.synthetic.main.activity_ftpserver.*

class FTPServerActivity : AppCompatActivity(), EditDialog.EditDialogListener, EditPasswordDialog.EditPasswordDialogListener,
    EditPortDialog.EditPortDialogListener {

    lateinit var chosenPath: String
    private lateinit var ftpBroadcastListener: FTPServerBroadcastListener
    private lateinit var preferences: SharedPreferences
    lateinit var preferencesEditor: SharedPreferences.Editor
    private var port: Int = 0
    private val fragmentManager: FragmentManager = supportFragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ftpserver)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        preferences = this.getSharedPreferences(SHARED_PREF_FILE, AppCompatActivity.MODE_PRIVATE)
        port = getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getInt(PORT_KEY, DEFAULT_PORT)
        ftpBroadcastListener = FTPServerBroadcastListener() { updateServiceStatus() }

        val storageDirectories = getStorageDirectories(this)

        updateInfo()

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

            restartService()
        }

        connectButton.setOnClickListener {
            if (!getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getBoolean(KEY_INTENT_IS_SERVICE_ACTIVE,
                                                                                         false)) startService(chosenPath)
            else stopService(Intent(this, FTPServer()::class.java))
        }

        userNameTextView.setOnLongClickListener {
            val newFragment = EditDialog(getString(R.string.edit_username))
            newFragment.show(fragmentManager, "")
            true
        }

        passwordTextView.setOnLongClickListener {
            val newFragment = EditPasswordDialog(getString(R.string.edit_password))
            newFragment.show(fragmentManager, "")
            true
        }

        portTextView.setOnLongClickListener {
            val newFragment = EditPortDialog(getString(R.string.edit_port))
            newFragment.show(fragmentManager, "")
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

    private fun restartService() {
        stopService(Intent(this, FTPServer()::class.java))
        startService(chosenPath)
    }

    private fun getIpAddress(): String {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager  //applicationContext is to avoid memory leak
        val wifiInfo = wifiManager.connectionInfo
        val ip = wifiInfo.ipAddress
        return String.format("%d.%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
        //Formatter.formatIpAddress is deprecated beacuse it doesnt work with ipv6
    }

    private fun updateInfo() {
        urlTextView.text = "ftps://" + getIpAddress()
        portTextView.text = getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getInt(PORT_KEY, DEFAULT_PORT).toString()
        userNameTextView.text = getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getString(USERNAME_KEY, DEFAULT_USER_NAME)!!
        passwordTextView.text = getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getString(PASSWORD_KEY, PASSWORD_DEF_VAL)!!
    }

    private fun removeNotification() {
        NotificationManagerCompat.from(this).cancel(1)
    }

    private fun displayNotification() {
        createNotificationChannel()
        val builder =
            NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.logo_transparent).setContentTitle(getString(R.string.ftp_is_running))
                .setContentText(getIpAddress() + ":" + port).setPriority(NotificationCompat.PRIORITY_DEFAULT).setOngoing(true)

        with(NotificationManagerCompat.from(this)) {
            notify(1, builder.build())
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateServiceStatus() {
        preferencesEditor = preferences.edit()
        if (!getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE).getBoolean(KEY_INTENT_IS_SERVICE_ACTIVE, false)) {
            connectButton.text = getString(R.string.disconnect)
            preferencesEditor.putBoolean(KEY_INTENT_IS_SERVICE_ACTIVE, true)
            preferencesEditor.apply()
            displayNotification()
        } else {
            connectButton.text = getString(R.string.connect)
            preferencesEditor.putBoolean(KEY_INTENT_IS_SERVICE_ACTIVE, false)
            preferencesEditor.apply()
            removeNotification()
        }
    }

    override fun editDialogListener(stringInput: String) {
        preferencesEditor = preferences.edit()
        preferencesEditor.putString(USERNAME_KEY, stringInput)
        preferencesEditor.apply()
        updateInfo()
    }

    override fun editPasswordDialogListener(stringInput: String) {
        preferencesEditor = preferences.edit()
        preferencesEditor.putString(PASSWORD_KEY, stringInput)
        preferencesEditor.apply()
        updateInfo()
    }

    override fun editPortDialogListener(numberInput: Int) {
        preferencesEditor = preferences.edit()
        preferencesEditor.putInt(PORT_KEY, numberInput)
        preferencesEditor.apply()
        updateInfo()
    }
}