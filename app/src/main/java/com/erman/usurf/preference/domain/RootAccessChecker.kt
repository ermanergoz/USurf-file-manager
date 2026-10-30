package com.erman.usurf.preference.domain

/** Whether root access can be turned on: the device has su and the user lets the app use it. */
interface RootAccessChecker {
    fun isDeviceRooted(): Boolean

    /** Asks for a root shell, which may wait for the user to answer the superuser prompt; never call it on main. */
    fun isRootAccessGiven(): Boolean
}
