import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.*

/*
private val DEFAULT_FALLBACK_STORAGE_PATH = "/storage/sdcard0"
val DIR_SEPARATOR = Pattern.compile("/")

fun getStorageDirectories(context: Context): ArrayList<String> {
    // Final set of paths
    val rv = ArrayList<String>()
    // Primary physical SD-CARD (not emulated)
    val rawExternalStorage = System.getenv("EXTERNAL_STORAGE")

    // All Secondary SD-CARDs (all exclude primary) separated by ":"
    val rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE")
    // Primary emulated SD-CARD
    val rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET")
    if (TextUtils.isEmpty(rawEmulatedStorageTarget)) {
        // Device has physical external storage; use plain paths.
        if (TextUtils.isEmpty(rawExternalStorage)) {
            // EXTERNAL_STORAGE undefined; falling back to default.
            // Check for actual existence of the directory before adding to list
            if (File(DEFAULT_FALLBACK_STORAGE_PATH).exists()) {
                rv.add(DEFAULT_FALLBACK_STORAGE_PATH)
            } else {
                //We know nothing else, use Environment's fallback
                rv.add(Environment.getExternalStorageDirectory().absolutePath)
            }
        } else {
            if (rawExternalStorage != null) {
                rv.add(rawExternalStorage)    //////////////////////////// /storage path////////
            }
        }
    } else {
        // Device has emulated storage; external storage paths should have
        // userId burned into them.
        val rawUserId: String
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            rawUserId = ""
        } else {
            val path = Environment.getExternalStorageDirectory().absolutePath
            val folders = DIR_SEPARATOR.split(path)
            val lastFolder = folders[folders.size - 1]
            var isDigit = false
            try {
                Integer.valueOf(lastFolder)
                isDigit = true
            } catch (ignored: NumberFormatException) {
            }

            rawUserId = if (isDigit) lastFolder else ""
        }
        // /storage/emulated/0[1,2,...]
        if (TextUtils.isEmpty(rawUserId)) {
            if (rawEmulatedStorageTarget != null) {
                rv.add(rawEmulatedStorageTarget)
            }
        } else {
            rv.add(rawEmulatedStorageTarget + File.separator + rawUserId)
        }
    }
    // Add all secondary storages
    if (!TextUtils.isEmpty(rawSecondaryStoragesStr)) {
        // All Secondary SD-CARDs splited into array
        val rawSecondaryStorages =
            rawSecondaryStoragesStr!!.split(File.pathSeparator.toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
        Collections.addAll(rv, *rawSecondaryStorages)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkStoragePermission(context))
        rv.clear()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

        val strings = getExtSdCardPathsForActivity(context)
        for (s in strings) {
            val f = File(s)
            if (!rv.contains(s) && canListFiles(f))
                rv.add(s)
        }
    }

    val usb = getUsbDrive()
    if (usb != null && !rv.contains(usb!!.getPath())) rv.add(usb!!.getPath())
/*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (SingletonUsbOtg.getInstance().isDeviceConnected()) {
                    rv.add(OTGUtil.PREFIX_OTG + "/")
                }
            }
*/
    return rv
}

fun getUsbDrive(): File? {
    var parent = File("/storage")

    try {
        for (f in parent.listFiles()!!)
            if (f.exists() && f.name.toLowerCase().contains("usb") && f.canExecute())
                return f
    } catch (e: Exception) {
    }

    parent = File("/mnt/sdcard/usbStorage")
    if (parent.exists() && parent.canExecute())
        return parent
    parent = File("/mnt/sdcard/usb_storage")
    return if (parent.exists() && parent.canExecute()) parent else null

}
*/
fun getExtSdCardPathsForActivity(context: Context): Array<String> {
    val paths = ArrayList<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        for (file in context.getExternalFilesDirs("external")) {
            if (file != null) {
                val index = file.absolutePath.lastIndexOf("/Android/data")
                if (index < 0) {
                    Log.w("Log, ", "Unexpected external file dir: " + file.absolutePath)
                } else {
                    var path = file.absolutePath.substring(0, index)
                    try {
                        path = File(path).canonicalPath

                    } catch (e: IOException) {
                        // Keep non-canonical path.
                    }
                    paths.add(path)
                }
            }
        }
    }
    if (paths.isEmpty()) paths.add("/storage/sdcard1")
    return paths.toTypedArray()
}

/*
fun canListFiles(f: File): Boolean {
    return f.canRead() && f.isDirectory
}

fun checkStoragePermission(context: Context): Boolean {
    // Verify that all required contact permissions have been granted.
    return ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
}*/
fun getStorageDirectories(context: Context): ArrayList<String> {
    val paths = ArrayList<String>()

    var isEmulatedFound = false

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        paths.addAll(getExtSdCardPathsForActivity(context))
    }

    for (i in 0 until paths.size) {
        if (paths[i] == "/storage/emulated/0") {
            isEmulatedFound = true
            break
        }
    }
    if (File("/").exists() && File("/").canRead()) {
        paths.add("/")
    }

    //Datas
    if (File("/data/sdext4").exists() && File("/data/sdext4").canRead()) {
        paths.add("/data/sdext4")
    }
    if (File("/data/sdext3").exists() && File("/data/sdext3").canRead()) {
        paths.add("/data/sdext3")
    }
    if (File("/data/sdext2").exists() && File("/data/sdext2").canRead()) {
        paths.add("/data/sdext2")
    }
    if (File("/data/sdext1").exists() && File("/data/sdext1").canRead()) {
        paths.add("/data/sdext1")
    }
    if (File("/data/sdext").exists() && File("/data/sdext").canRead()) {
        paths.add("/data/sdext")
    }

    //MNTS
    if (File("mnt/sdcard/external_sd").exists() && File("mnt/sdcard/external_sd").canRead()) {
        paths.add("mnt/sdcard/external_sd")
    }
    if (File("mnt/extsdcard").exists() && File("mnt/extsdcard").canRead()) {
        paths.add("mnt/extsdcard")
    }
    if (File("mnt/external_sd").exists() && File("mnt/external_sd").canRead()) {
        paths.add("mnt/external_sd")
    }
    if (File("mnt/externalsd").exists() && File("mnt/externalsd").canRead()) {
        paths.add("mnt/externalsd")
    }
    if (File("mnt/emmc").exists() && File("mnt/emmc").canRead()) {
        paths.add("mnt/emmc")
    }
    if (File("mnt/sdcard0").exists() && File("mnt/sdcard0").canRead() && !isEmulatedFound) {
        paths.add("mnt/sdcard0")
    }
    if (File("mnt/sdcard1").exists() && File("mnt/sdcard1").canRead() && !isEmulatedFound) {
        paths.add("mnt/sdcard1")
    }
    if (File("mnt/sdcard").exists() && File("mnt/sdcard").canRead() && !isEmulatedFound) {
        paths.add("mnt/sdcard")
    }

    //Storages
    if (File("/storage/removable/sdcard1").exists() && File("/storage/removable/sdcard1").canRead()) {
        paths.add("/storage/removable/sdcard1")
    }
    if (File("/storage/external_SD").exists() && File("/storage/external_SD").canRead()) {
        paths.add("/storage/external_SD")
    }
    if (File("/storage/ext_sd").exists() && File("/storage/ext_sd").canRead()) {
        paths.add("/storage/ext_sd")
    }
    if (File("/storage/extsd").exists() && File("/storage/extsd").canRead()) {
        paths.add("/storage/extsd")
    }
    if (File("/storage/sdcard1").exists() && File("/storage/sdcard1").canRead() && !isEmulatedFound) {
        paths.add("/storage/sdcard1")
    }
    if (File("/storage/sdcard0").exists() && File("/storage/sdcard0").canRead() && !isEmulatedFound) {
        paths.add("/storage/sdcard0")
    }
    if (File("/storage/sdcard").exists() && File("/storage/sdcard").canRead() && !isEmulatedFound) {
        paths.add("/storage/sdcard")
    }
    if (paths.size == 0) {
        paths.add(Environment.getExternalStorageDirectory().absolutePath)
    }

    return paths
}