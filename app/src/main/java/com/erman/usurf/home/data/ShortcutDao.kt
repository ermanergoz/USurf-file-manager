package com.erman.usurf.home.data

import android.widget.TextView
import android.widget.Toast
import com.erman.usurf.R
import com.erman.usurf.application.MainApplication.Companion.appContext
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
            displayToast(R.string.unable_to_create_shortcut)
            return false
        } finally {
            realm.commitTransaction()
        }
        displayToast(R.string.shortcut_created)
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
            displayToast(R.string.unable_to_delete_shortcut)
            return false
        }
        displayToast(R.string.shortcut_deleted)
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
            displayToast(R.string.unable_to_rename_shortcut)
            return false
        } finally {
            realm.commitTransaction()
        }
        displayToast(R.string.shortcut_renamed)
        return true
    }

    private fun displayToast(messageId: Int) {
        Toast.makeText(appContext, appContext.getString(messageId), Toast.LENGTH_LONG).show()
    }
}