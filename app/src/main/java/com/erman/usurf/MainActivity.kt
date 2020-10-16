package com.erman.usurf

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.navigation.NavigationView
import androidx.core.app.ActivityCompat
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.DisplayMetrics
import android.view.Menu
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.erman.usurf.dialog.ui.SearchDialog
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.home.model.FinishActivity
import com.erman.usurf.utils.*
import java.io.File

class MainActivity : AppCompatActivity(), ShowDialog, FinishActivity, RefreshNavDrawer, StorageAccessFramework, HomeStorageButton {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var directoryViewModel: DirectoryViewModel
    private lateinit var viewModelFactory: ViewModelFactory

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            logd("Request read and write permissions")
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        viewModelFactory = ViewModelFactory()
        directoryViewModel = ViewModelProvider(this, viewModelFactory).get(DirectoryViewModel::class.java)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)

        setupNavDrawer(navView, navController, drawerLayout)
        addStoragesToDrawer(navView, navController, drawerLayout)
        requestPermissions()

        if (intent.getBooleanExtra(KEY_INTENT_IS_FTP_NOTIFICATION_CLICKED, INTENT_IS_FTP_NOTIFICATION_CLICKED_DEF_VAL))
            navController.navigate(R.id.global_action_to_nav_ftp)
    }

    private fun setupNavDrawer(navView: NavigationView, navController: NavController, drawerLayout: DrawerLayout) {
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_directory,
                R.id.nav_preferences,
                R.id.nav_ftp,
                R.id.nav_info
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        navView.setNavigationItemSelectedListener {
             when (it.itemId) {
                R.id.nav_home -> navController.navigate(R.id.global_action_nav_home)
                R.id.nav_preferences -> navController.navigate(R.id.global_action_nav_preferences)
                R.id.nav_ftp -> navController.navigate(R.id.global_action_to_nav_ftp)
                R.id.nav_info -> navController.navigate(R.id.global_action_nav_info)
                R.id.nav_device_wide_search -> {
                    val bundle = bundleOf(KEY_BUNDLE_SEARCH_FILE to true)
                    navController.navigate(R.id.global_action_nav_directory, bundle)
                }
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun addStoragesToDrawer(navView: NavigationView, navController: NavController, drawerLayout: DrawerLayout) {
        val storageDirectories = StoragePaths().getStorageDirectories()

        for (path in storageDirectories) {
            val storage = navView.menu.add(R.id.storage, Menu.NONE, 0, path)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                storage.icon = ContextCompat.getDrawable(this, R.drawable.ic_hdd)
            }
            storage.setOnMenuItemClickListener {
                onStorageButtonClick(path, navController)
                drawerLayout.closeDrawers()
                true
            }
        }
    }

    private fun refreshNavDrawer(navView: NavigationView, navController: NavController, drawerLayout: DrawerLayout) {
        navView.menu.removeGroup(R.id.storage)
        addStoragesToDrawer(navView, navController, drawerLayout)
    }

    private fun onStorageButtonClick(path: String, navController: NavController) {
        directoryViewModel.setPath(path)
        navController.navigate(R.id.global_action_nav_directory)
        if (!File(path).canWrite() && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            launchSAF()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            logd("Get read and write permissions")
            data?.data?.let { treeUri ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    this.contentResolver.takePersistableUriPermission(treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                DirectoryPreferenceProvider().editChosenUri(treeUri.toString())
            }
        } else {
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun showDialog(dialog: DialogFragment) {
        logd("Show a dialog")
        dialog.show(supportFragmentManager, "")
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawers()
        else
            super.onBackPressed()
    }

    override fun finishActivity() {
        finish()
    }

    override fun refreshStorageButtons() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)

        refreshNavDrawer(navView, navController, drawerLayout)
    }

    override fun launchSAF() {
        //https://developer.android.com/reference/android/support/v4/provider/DocumentFile
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            //If you really do need full access to an entire subtree of documents,
            this.startActivityForResult(intent, 2)
        }
    }

    override fun autoSizeButtonDimensions(storageButtonCount: Int, sideMargin: Int): Pair<Int, Int> {
        val displayMetrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            this.display?.getRealMetrics(displayMetrics)
        else
            windowManager.defaultDisplay.getMetrics(displayMetrics)

        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        return Pair(((screenWidth - ((sideMargin * 2) * storageButtonCount)) / storageButtonCount),
            (screenHeight / (8 + storageButtonCount)))
    }
}
