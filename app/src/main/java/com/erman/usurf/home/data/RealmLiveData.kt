package com.erman.usurf.home.data

import androidx.lifecycle.LiveData
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmModel
import io.realm.RealmResults

/**
 * What [query] finds in the Realm [configuration] opens, as copies any thread may read, kept current while observed.
 * The Realm is opened when the first observer arrives and closed when the last one leaves, on the main thread, where
 * Realm delivers the changes of a write made on any other.
 */
class RealmLiveData<T : RealmModel>(private val configuration: RealmConfiguration, private val query: (Realm) -> RealmResults<T>) :
    LiveData<List<T>>() {
    private var openQuery: OpenQuery<T>? = null

    override fun onActive() {
        val realm: Realm = Realm.getInstance(configuration)
        val results: RealmResults<T> = query(realm)
        results.addChangeListener { found -> value = realm.copyFromRealm(found) }
        openQuery = OpenQuery(realm, results)
    }

    override fun onInactive() {
        openQuery?.close()
        openQuery = null
    }

    private class OpenQuery<T : RealmModel>(private val realm: Realm, private val results: RealmResults<T>) {
        fun close() {
            results.removeAllChangeListeners()
            realm.close()
        }
    }
}
