package com.erman.usurf.application.data

import com.erman.usurf.application.utils.INITIAL_SHORTCUT_LIST
import com.erman.usurf.home.data.Shortcut
import com.erman.usurf.utils.StoragePaths
import com.erman.usurf.utils.loge
import io.realm.Realm
import io.realm.kotlin.createObject
import java.io.File

class ApplicationDao(val realm: Realm) {
    fun addInitialShortcuts() {
        val directory = StoragePaths().getStorageDirectories().first()

        for (shortcutName in INITIAL_SHORTCUT_LIST) {
            val shortcutPath = directory + File.separator + shortcutName

            if (File(shortcutPath).exists()) {
                realm.beginTransaction()
                try {
                    val shortcut: Shortcut = realm.createObject<Shortcut>(shortcutPath)
                    shortcut.name = shortcutName
                    shortcut.path = shortcutPath
                } catch (err: Error) {
                    loge("addInitialShortcuts $err")
                } finally {
                    realm.commitTransaction()
                }
            }
        }
    }
}