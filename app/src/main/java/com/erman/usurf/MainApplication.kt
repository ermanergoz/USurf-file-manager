package com.erman.usurf

import android.app.Application
import android.content.Context
import com.erman.usurf.utils.REALM_CONFIG_FILE_NAME
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
        // Initialize Realm
        //Realm.init(this)
        //val config = RealmConfiguration.Builder().name(REALM_CONFIG_FILE_NAME).initialData(Realm.Transaction { realm ->
        //    realm.insert(FTPUser())
        //}).deleteRealmIfMigrationNeeded().build()
        //Realm.setDefaultConfiguration(config)
        // Get a Realm instance for this thread
        //realm = Realm.getDefaultInstance()
    }
}