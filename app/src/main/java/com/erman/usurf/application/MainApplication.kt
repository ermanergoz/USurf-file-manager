package com.erman.usurf.application

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.app.Application
import com.erman.usurf.application.data.ApplicationDao
import com.erman.usurf.application.di.appModule
import com.erman.usurf.application.domain.ApplicationPreferencesRepository
import io.realm.Realm
import io.realm.RealmConfiguration
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

private const val REALM_CONFIG_FILE_NAME = "usurf.realm"

class MainApplication : Application() {
    private val applicationPreferencesRepository: ApplicationPreferencesRepository by inject()
    private val applicationDao: ApplicationDao by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MainApplication)
            modules(appModule)
        }

        Realm.init(this)
        val config =
            RealmConfiguration.Builder()
                .name(REALM_CONFIG_FILE_NAME)
                .deleteRealmIfMigrationNeeded()
                .allowWritesOnUiThread(true)
                .build()

        Realm.setDefaultConfiguration(config)

        if (applicationPreferencesRepository.getIsFirstLaunch()) {
            applicationDao.addInitialFavorites()
            applicationPreferencesRepository.setIsFirstLaunch(false)
        }

        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                @SuppressLint("SourceLockedOrientationActivity")
                override fun onActivityCreated(
                    p0: Activity,
                    p1: Bundle?,
                ) {
                    p0.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }

                override fun onActivityStarted(p0: Activity) = Unit

                override fun onActivityResumed(p0: Activity) = Unit

                override fun onActivityPaused(p0: Activity) = Unit

                override fun onActivityStopped(p0: Activity) = Unit

                override fun onActivitySaveInstanceState(
                    p0: Activity,
                    p1: Bundle,
                ) = Unit

                override fun onActivityDestroyed(p0: Activity) = Unit
            },
        )
    }
}
