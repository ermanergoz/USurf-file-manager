package com.erman.usurf.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.erman.usurf.directory.utils.MimeUtils
import java.io.File

private const val APK_EXTENSION = "apk"

object FileOpenUtils {
    fun openFile(
        context: Context,
        path: String,
        grantWritePermission: Boolean = false,
        bringToFront: Boolean = false,
    ): Boolean {
        val file = File(path)
        val uri =
            FileProvider.getUriForFile(
                context,
                context.packageName,
                file,
            )
        if (path.endsWith(".$APK_EXTENSION", ignoreCase = true)) {
            return openApkForInstall(context, uri)
        }
        val mimeType: String = MimeUtils.getMimeTypeForPath(path)
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    if (grantWritePermission) {
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    } else {
                        0
                    }
                if (bringToFront) {
                    addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                }
            }
        val packageManager = context.packageManager
        val resolveInfo = intent.resolveActivity(packageManager)
        if (resolveInfo != null) {
            context.startActivity(intent)
            return true
        }
        return false
    }

    private fun openApkForInstall(
        context: Context,
        uri: Uri,
    ): Boolean {
        val intent =
            Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        val resolveInfo = context.packageManager.resolveActivity(intent, 0)
        if (resolveInfo != null) {
            context.startActivity(intent)
            return true
        }
        return false
    }
}
