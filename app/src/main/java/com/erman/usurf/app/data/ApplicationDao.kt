package com.erman.usurf.app.data

import com.erman.usurf.app.utils.INITIAL_SHORTCUT_LIST
import com.erman.usurf.home.data.Shortcut
import com.erman.usurf.utils.StoragePaths
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import java.io.File

class ApplicationDao(val realm: Realm) {
    fun addInitialShortcuts() {
        val directory = StoragePaths().getStorageDirectories().first()

        for (shortcutName in INITIAL_SHORTCUT_LIST) {
            val shortcutPath = directory + File.separator + shortcutName

            if (File(shortcutPath).exists()) {
                realm.beginTransaction()
                val shortcut: Shortcut = realm.createObject<Shortcut>((realm.where<Shortcut>().findAll().size) + 1)
                shortcut.name = shortcutName
                shortcut.path = shortcutPath
                realm.commitTransaction()
            }
        }
    }
}