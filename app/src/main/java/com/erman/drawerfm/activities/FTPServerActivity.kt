package com.erman.drawerfm.activities

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import com.erman.drawerfm.R
import com.erman.drawerfm.utilities.fuckinshit
import com.erman.drawerfm.utilities.getStorageDirectories
import kotlinx.android.synthetic.main.activity_ftpserver.*

class FTPServerActivity : AppCompatActivity() {

    lateinit var chosenPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ftpserver)

        var storageDirectories = getStorageDirectories(this)

        for (i in storageDirectories.indices) {
            val radioButton = RadioButton(this)
            radioButton.id = i
            radioButton.setText(storageDirectories[i])
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
            fuckinshit(chosenPath).execute()
        }
    }
}
