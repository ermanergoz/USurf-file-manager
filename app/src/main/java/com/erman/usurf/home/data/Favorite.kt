package com.erman.usurf.home.data

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Favorite(
    @PrimaryKey var id: String = "",
    var name: String = "",
    var path: String = ""
) : RealmObject()
