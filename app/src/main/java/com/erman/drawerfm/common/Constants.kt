package com.erman.drawerfm.common

const val SHARED_PREF_FILE: String = "com.erman.draverfm"
const val THEME_CHOICE_KEY: String = "theme choice"
const val THEME_DEF_VAL: String = "System default"
const val DARK_THEME: String = "Dark theme"
const val LIGHT_THEME: String = "Light theme"
const val STORAGE_PROGRESSBAR_HEIGHT: Float = 20f
const val GRID_LAYOUT_COLUMN_NUMBER: Int = 2
const val KEY_INTENT_PATH: String = "path"
const val KEY_INTENT_IS_EXTCARD: String = "isExtSdCard"
const val KEY_INTENT_IS_CREATE_SHORTCUT_MODE: String = "isCreateShortcutMode"
const val KEY_INTENT_SEARCH_QUERY: String = "searchQuery"
const val KEY_INTENT_IS_DEVICE_WIDE_SEARCH_MODE: String = "isDeviceWideSearchMode"
const val KEY_INTENT_STORAGE_DIRECTORIES: String = "storageDirectories"
const val KEY_INTENT_NEW_SHORTCUT_PATH: String = "newShortcutPath"
const val KEY_INTENT_IS_MAIN_ACTIVITY: String = "isMainActivity"
const val KEY_INTENT_CURRENT_PATH: String = "currentPath"
const val KEY_INTENT_PATH_FOR_BROADCAST: String = "path for broadcast"
const val KEY_INTENT_EXTCARD_CHOSEN_URI: String = "extSdCardChosenUri"
const val REALM_FIELD_NAME_PATH: String = "path"
const val REALM_CONFIG_FILE_NAME: String = "drawerfm.realm"

const val CREATE_NEW_FOLDER: String = "folder"
const val CREATE_NEW_FILE: String = "file"
const val CREATE_NEW_ZIP: String = "zip"

const val SIMPLE_DATE_FORMAT_PATTERN: String = "dd MMMM | HH:mm:ss"
const val KEY_INTENT_GRID_VIEW: String = "grid view"
const val MARQUEE_CHOICE_KEY: String = "marquee choice"
const val MARQUEE_REPEAT_LIM: Int = -1  //-1 for infinite

//FileListPreferencesFragment
const val KEY_SORT_FILES_LIST_PREFERENCE: String = "sortListPreference"
const val KEY_SHOW_HIDDEN_SWITCH: String = "showHiddenFileSwitch"
const val KEY_SHOW_FILES_ONLY_SWITCH: String = "showFilesOnlySwitch"
const val KEY_SHOW_FOLDERS_ONLY_SWITCH: String = "showFoldersOnlySwitch"
const val KEY_ASCENDING_ORDER_CHECKBOX: String = "ascendingOrderPreference"
const val KEY_DESCENDING_ORDER_CHECKBOX: String = "descendingOrderPreference"
const val KEY_FILES_ON_TOP_CHECKBOX: String = "showFilesOnTopPreference"
const val KEY_FOLDERS_ON_TOP_CHECKBOX: String = "showFoldersOnTopPreference"
const val PREFERENCE_FILE_SORT: String = "sortFileMode"
const val PREFERENCE_SHOW_HIDDEN: String = "showHidden"
const val PREFERENCE_SHOW_FILES_ONLY: String = "showFilesOnly"
const val PREFERENCE_SHOW_FOLDERS_ONLY: String = "showFoldersOnly"
const val PREFERENCE_ASCENDING_ORDER: String = "ascendingOrder"
const val PREFERENCE_DESCENDING_ORDER: String = "descendingOrder"
const val PREFERENCE_FILES_ON_TOP: String = "showFilesOnTop"
const val PREFERENCE_FOLDERS_ON_TOP: String = "showFoldersOnTop"
const val DEFAULT_FILE_SORT_MODE: String = "Sort by name"

//FTP Server
const val DEFAULT_PORT: Int = 2221
const val DEFAULT_USER_NAME: String = "anonymous"

//FTPServerActivity
const val KEY_INTENT_CHOSEN_PATH: String = "chosenPath"
const val KEY_INTENT_IS_SERVICE_ACTIVE: String = "isServiceActive"
const val CHANNEL_ID: String = "notificationChannel"
const val PORT_KEY: String = "port"
const val PASSWORD_KEY: String = "password"
const val USERNAME_KEY: String = "username"
const val PASSWORD_DEF_VAL: String = ""