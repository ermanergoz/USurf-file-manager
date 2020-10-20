package com.erman.usurf.home.data

import android.widget.TextView
import com.erman.usurf.home.utils.REALM_FIELD_NAME_PATH
import com.erman.usurf.utils.logd
import com.erman.usurf.utils.loge
import io.realm.Realm
import io.realm.exceptions.RealmPrimaryKeyConstraintException
import io.realm.kotlin.createObject
import io.realm.kotlin.where

class ShortcutDao(var realm: Realm) {
    fun getShortcuts(): RealmLiveData<Shortcut> {
        logd("Get shortcuts")
        return realm.where(Shortcut::class.java).findAllAsync().asLiveData()
    }

    fun addShortcut(shortcutPath: String, shortcutName: String): Boolean {
        logd("Add shortcut")
        realm.beginTransaction()
        try {
            val shortcut: Shortcut = realm.createObject<Shortcut>(shortcutPath)
            shortcut.name = shortcutName
            shortcut.path = shortcutPath
        } catch (err: RealmPrimaryKeyConstraintException) {
            loge("addShortcut $err")
            return false
        } finally {
            realm.commitTransaction()
        }
        return true
    }

    fun removeShortcut(shortcut: TextView): Boolean {
        logd("Remove shortcut")
        try {
            val results = realm.where<Shortcut>().equalTo(REALM_FIELD_NAME_PATH, shortcut.tag.toString()).findAllAsync()
            realm.executeTransaction {
                results.deleteFirstFromRealm()
            }
        } catch (err: Error) {
            loge("removeShortcut $err")
            return false
        }
        return true
    }

    fun renameShortcut(shortcut: TextView, newName: String): Boolean {
        logd("Rename shortcut")
        try {
            val shortcut = realm.where<Shortcut>().equalTo(REALM_FIELD_NAME_PATH, shortcut.tag.toString()).findFirst()
            realm.beginTransaction()
            shortcut?.let { it.name = newName }
        } catch (err: Error) {
            loge("renameShortcut $err")
            return false
        } finally {
            realm.commitTransaction()
        }
        return true
    }
}