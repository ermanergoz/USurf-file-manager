package com.erman.usurf.home.data

import io.realm.DynamicRealm
import io.realm.RealmConfiguration
import io.realm.RealmMigration

/** The file every release has kept the favourites in. */
private const val FAVORITES_REALM_FILE_NAME: String = "usurf.realm"

/**
 * The version of the favourites' stored schema. A change to a stored class raises it by one and adds its step to
 * [FavoritesMigration], so an update keeps the favourites a user already has.
 */
const val FAVORITES_SCHEMA_VERSION: Long = 1

/**
 * How the favourites' Realm is opened: the app's file at [FAVORITES_SCHEMA_VERSION], an older one migrated, never
 * deleted; a write on the main thread is refused. Realm must be initialised first.
 */
fun favoritesRealm(): RealmConfiguration.Builder = RealmConfiguration.Builder()
    .name(FAVORITES_REALM_FILE_NAME)
    .schemaVersion(FAVORITES_SCHEMA_VERSION)
    .migration(FavoritesMigration)
    .allowWritesOnUiThread(false)

/**
 * The steps from each older schema version to [FAVORITES_SCHEMA_VERSION]. Releases 1.2.1 to 2.1.0 stored version 0,
 * whose schema is today's (a favourite's id, name and path), so the step from 0 to 1 changes nothing: the version
 * moves so that a later change to the schema has a step of its own and no update deletes the file.
 */
object FavoritesMigration : RealmMigration {
    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) = Unit
}
