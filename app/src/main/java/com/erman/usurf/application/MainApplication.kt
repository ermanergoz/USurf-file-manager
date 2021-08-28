package com.erman.usurf.application

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import com.erman.usurf.application.data.ApplicationDao
import com.erman.usurf.application.data.ApplicationPreferenceProvider
import com.erman.usurf.application.utils.REALM_CONFIG_FILE_NAME
import io.realm.Realm
import io.realm.RealmConfiguration

class MainApplication : Application() {
    companion object {
        lateinit var appContext: Context
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this.applicationContext

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
}