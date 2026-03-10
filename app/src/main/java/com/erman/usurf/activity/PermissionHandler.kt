package com.erman.usurf.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.erman.usurf.R
import com.erman.usurf.dialog.ui.ManageAllFilesRequestDialog
import com.erman.usurf.dialog.ui.SafAccessRequestDialog
import com.erman.usurf.utils.MANAGE_ALL_FILES_DIALOG_TAG
import com.erman.usurf.utils.MANAGE_ALL_FILES_REQUEST_KEY
import com.erman.usurf.utils.SAF_ACCESS_DIALOG_TAG
import com.erman.usurf.utils.SAF_ACCESS_REQUEST_KEY
import com.google.android.material.snackbar.Snackbar

private const val URI_SCHEME: String = "package"

class PermissionHandler(
    private val activity: AppCompatActivity,
    private val onSafUriGranted: (String) -> Unit,
) {
    lateinit var rootView: View

    private val storagePermissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { grants: Map<String, Boolean> ->
            handleStoragePermissionResult(grants)
        }

    private val notificationPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            handleNotificationPermissionResult(isGranted)
        }

    private val manageAllFilesLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) {
            handleManageAllFilesResult()
        }

    private val safLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            result.data?.data?.let { treeUri ->
                activity.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                onSafUriGranted(treeUri.toString())
            }
        }

    init {
        activity.supportFragmentManager.setFragmentResultListener(
            MANAGE_ALL_FILES_REQUEST_KEY,
            activity,
        ) { _, _ -> launchManageAllFilesSettings() }
        activity.supportFragmentManager.setFragmentResultListener(
            SAF_ACCESS_REQUEST_KEY,
            activity,
        ) { _, _ -> launchSafPicker() }
    }

    fun requestInitialPermissions() {
        requestStoragePermissions()
        requestNotificationPermission()
    }

    fun requestSafAccess() {
        SafAccessRequestDialog()
            .show(activity.supportFragmentManager, SAF_ACCESS_DIALOG_TAG)
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            val permissions: Array<String> =
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                )
            val isGranted: Boolean =
                permissions.all {
                    ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
                }
            if (isGranted) return
            val needsRationale: Boolean =
                permissions.any {
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
                }
            if (needsRationale) {
                Snackbar.make(rootView, R.string.storage_permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok) { storagePermissionLauncher.launch(permissions) }
                    .show()
            } else {
                storagePermissionLauncher.launch(permissions)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                ManageAllFilesRequestDialog()
                    .show(activity.supportFragmentManager, MANAGE_ALL_FILES_DIALOG_TAG)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted: Boolean =
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            if (!isGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handleStoragePermissionResult(grants: Map<String, Boolean>) {
        val allGranted: Boolean = grants.values.all { it }
        if (!allGranted) {
            showSnackbar(R.string.storage_permission_not_granted)
        }
    }

    private fun handleNotificationPermissionResult(isGranted: Boolean) {
        if (!isGranted) {
            showSnackbar(R.string.notification_permission_not_granted)
        }
    }

    private fun handleManageAllFilesResult() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            showSnackbar(R.string.storage_permission_not_granted)
        }
    }

    @SuppressLint("InlinedApi")
    private fun launchManageAllFilesSettings() {
        val intent: Intent =
            Intent().apply {
                action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                data = Uri.fromParts(URI_SCHEME, activity.packageName, null)
            }
        manageAllFilesLauncher.launch(intent)
    }

    @SuppressLint("InlinedApi")
    private fun launchSafPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        safLauncher.launch(intent)
    }

    private fun showSnackbar(messageResId: Int) {
        Snackbar.make(rootView, messageResId, Snackbar.LENGTH_LONG).show()
    }
}
