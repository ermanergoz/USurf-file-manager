package com.erman.usurf.utils

import android.content.res.Resources
import java.text.SimpleDateFormat
import java.util.Date

private const val MODIFICATION_DATE_PATTERN: String = "dd MMMM | HH:mm:ss"

/** When an item last changed, [millis] after the epoch, as a row and its Information show it, in this language. */
fun Resources.modificationDateText(millis: Long): String = SimpleDateFormat(MODIFICATION_DATE_PATTERN, locale()).format(Date(millis))
