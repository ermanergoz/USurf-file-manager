package com.erman.usurf.application

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.pm.ActivityInfo
import android.os.Bundle
import com.erman.usurf.application.data.ApplicationDao
import com.erman.usurf.application.di.appModule
import com.erman.usurf.application.domain.ApplicationPreferencesRepository
import com.erman.usurf.directory.model.RootHandler
import io.realm.Realm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class MainApplication : Application() {
    private val applicationPreferencesRepository: ApplicationPreferencesRepository by inject()
    private val applicationDao: ApplicationDao by inject()

    // Work the process does for itself, outside any screen: it lasts as long as the process.
    private val applicationScope: CoroutineScope = MainScope()

    override fun onCreate() {
        super.onCreate()

        RootHandler.configureShell()
        Realm.init(this)
        startKoin {
            androidContext(this@MainApplication)
            modules(appModule)
        }

        if (applicationPreferencesRepository.getIsFirstLaunch()) {
            applicationScope.launch {
                applicationDao.addInitialFavorites()
                applicationPreferencesRepository.setIsFirstLaunch(false)
            }
        }

        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                @SuppressLint("SourceLockedOrientationActivity")
                override fun onActivityCreated(p0: Activity, p1: Bundle?) {
                    p0.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }

                override fun onActivityStarted(p0: Activity) = Unit

                override fun onActivityResumed(p0: Activity) = Unit

                override fun onActivityPaused(p0: Activity) = Unit

                override fun onActivityStopped(p0: Activity) = Unit

                override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) = Unit

                override fun onActivityDestroyed(p0: Activity) = Unit
            }
        )
    }
}
