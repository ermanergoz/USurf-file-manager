package com.erman.usurf.utils

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import com.erman.usurf.R
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private const val BYTES_PER_KB: Long = 1024L
private const val BYTES_PER_MB: Long = BYTES_PER_KB * 1024L
private const val BYTES_PER_GB: Long = BYTES_PER_MB * 1024L
private const val BYTES_PER_TB: Long = BYTES_PER_GB * 1024L
private const val MAX_FILE_SIZE_DECIMALS: Int = 2
private const val STORAGE_SIZE_FORMAT: String = "%.1f %s"

// The units a size reads in, from the largest down.
private val SIZE_UNITS: List<Pair<Long, Int>> =
    listOf(BYTES_PER_TB to R.string.tb, BYTES_PER_GB to R.string.gb, BYTES_PER_MB to R.string.mb, BYTES_PER_KB to R.string.kb)

/** Sizes as the screens show them: the number in the language of the resources given, then its unit. */
object SizeFormatter {
    /** A file's or a folder's size: in the largest unit it is more than one of, with one or two decimals; else in bytes. */
    fun fileSize(resources: Resources, bytes: Long): String {
        val locale = resources.locale()
        val unit = SIZE_UNITS.firstOrNull { (unitBytes, _) -> bytes > unitBytes }
            ?: return "${NumberFormat.getIntegerInstance(locale).withoutGrouping().format(bytes)} ${resources.getString(R.string.bytes)}"
        val (unitBytes, unitName) = unit
        val number =
            NumberFormat.getNumberInstance(locale).withoutGrouping().apply {
                minimumFractionDigits = 1
                maximumFractionDigits = MAX_FILE_SIZE_DECIMALS
                roundingMode = RoundingMode.HALF_UP
            }
        return "${number.format(bytes.toDouble() / unitBytes)} ${resources.getString(unitName)}"
    }

    /** A volume's size: in the largest unit it reaches, with one decimal; below a kilobyte, in kilobytes. */
    fun storageSize(resources: Resources, bytes: Long): String {
        val locale = resources.locale()
        if (bytes <= 0L) return "${NumberFormat.getIntegerInstance(locale).format(0)} ${resources.getString(R.string.bytes)}"
        val (unitBytes, unitName) = SIZE_UNITS.firstOrNull { (unitBytes, _) -> bytes >= unitBytes } ?: SIZE_UNITS.last()
        return String.format(locale, STORAGE_SIZE_FORMAT, bytes.toDouble() / unitBytes, resources.getString(unitName))
    }
}

/** The language these resources show text in: the first of the configuration's locales. */
internal fun Resources.locale(): Locale = ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault()

private fun NumberFormat.withoutGrouping(): NumberFormat = apply { isGroupingUsed = false }
