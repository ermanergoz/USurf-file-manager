package com.erman.usurf.home.data

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Shortcut (
    @PrimaryKey var id: Int = 0,
    var name: String = "",
    var path: String = ""
) : RealmObject()
