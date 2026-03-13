package com.erman.usurf.home.data

import android.widget.TextView
import androidx.lifecycle.LiveData
import com.erman.usurf.home.utils.REALM_FIELD_NAME_PATH
import com.erman.usurf.utils.logd
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where

class ShortcutDao(var realm: Realm) {
    fun getShortcuts(): RealmLiveData<Shortcut> {
        logd("Get shortcuts")
        return realm.where(Shortcut::class.java).findAllAsync().asLiveData()
    }

    fun addShortcut(shortcutPath: String, shortcutName: String) {
        logd("Add shortcut")
        realm.beginTransaction()
        val shortcut: Shortcut = realm.createObject<Shortcut>((realm.where<Shortcut>().findAll().size) + 1)
        shortcut.name = shortcutName
        shortcut.path = shortcutPath
        realm.commitTransaction()
    }

    fun removeShortcut(shortcut: TextView) {
        logd("Remove shortcut")
        val results = realm.where<Shortcut>().equalTo(REALM_FIELD_NAME_PATH, shortcut.tag.toString()).findAll()
        realm.executeTransaction {
            results.deleteFirstFromRealm()
        }
    }

    fun renameShortcut(shortcut: TextView, newName: String) {
        logd("Rename shortcut")
        val path = shortcut.tag.toString()
        removeShortcut(shortcut)
        addShortcut(path, newName)
    }
}