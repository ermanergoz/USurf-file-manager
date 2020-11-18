package com.erman.usurf.application

import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import com.erman.usurf.application.data.ApplicationDao
import com.erman.usurf.application.data.ApplicationPreferenceProvider
import com.erman.usurf.application.utils.REALM_CONFIG_FILE_NAME
import com.erman.usurf.preference.data.PreferenceProvider
import com.erman.usurf.pushNotification.model.PushNotificationBroadcastReceiver
import com.erman.usurf.pushNotification.utils.INTERVAL_FIVE_MINUTES
import io.realm.Realm
import io.realm.RealmConfiguration

class MainApplication : Application() {
    companion object {
        lateinit var appContext: Context
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this.applicationContext

        //Documentation: https://realm.io/docs/kotlin/latest/#realms
        Realm.init(this)
        val config = RealmConfiguration.Builder()
            .name(REALM_CONFIG_FILE_NAME)
            .deleteRealmIfMigrationNeeded()
            .build()

        Realm.setDefaultConfiguration(config)
        val realm = Realm.getDefaultInstance()
        val preferenceProvider = ApplicationPreferenceProvider()

        if (preferenceProvider.getIsFirstLaunch()) {
            ApplicationDao(realm).addInitialFavorites()
            preferenceProvider.editIsFirstLaunch(false)
        }

        schedulePushNotifications()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(p0: Activity, p1: Bundle?) {
                p0.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

            override fun onActivityStarted(p0: Activity) = Unit

            override fun onActivityResumed(p0: Activity) = Unit

            override fun onActivityPaused(p0: Activity) = Unit

            override fun onActivityStopped(p0: Activity) = Unit

            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) = Unit

            override fun onActivityDestroyed(p0: Activity) = Unit
        })
    }

    private fun schedulePushNotifications() {
        if(PreferenceProvider().getCleanStorageReminderPreference()) {
            val notifyIntent = Intent(this, PushNotificationBroadcastReceiver::class.java)
            var pendingIntent =
                PendingIntent.getBroadcast(this, 2, notifyIntent, PendingIntent.FLAG_NO_CREATE)

            val alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (pendingIntent == null) {
                pendingIntent = PendingIntent.getBroadcast(
                    this,
                    2,
                    notifyIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
                // start it only it wasn't running already
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP, /*calendar.timeInMillis*/ System.currentTimeMillis(),
                    /*AlarmManager.INTERVAL_DAY*/INTERVAL_FIVE_MINUTES, pendingIntent
                )
            }
        }
    }
}