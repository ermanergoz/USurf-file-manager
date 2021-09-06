package com.erman.usurf.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Menu
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.erman.usurf.MobileNavigationDirections
import com.erman.usurf.R
import com.erman.usurf.activity.data.StorageDirectoryPreferenceProvider
import com.erman.usurf.activity.model.RefreshNavDrawer
import com.erman.usurf.activity.model.ShowDialog
import com.erman.usurf.activity.utils.*
import com.erman.usurf.databinding.ActivityMainBinding
import com.erman.usurf.dialog.model.DialogListener
import com.erman.usurf.dialog.ui.ManageAllFilesRequestDialog
import com.erman.usurf.dialog.ui.SafAccessRequestDialog
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.ftp.utils.KEY_INTENT_IS_FTP_NOTIFICATION_CLICKED
import com.erman.usurf.home.model.FinishActivity
import com.erman.usurf.home.model.HomeStorageButton
import com.erman.usurf.home.model.StorageAccessFramework
import com.erman.usurf.utils.StoragePaths
import com.erman.usurf.utils.ViewModelFactory
import com.erman.usurf.utils.logd
import com.google.android.material.navigation.NavigationView
import java.io.File

class MainActivity : AppCompatActivity(), ShowDialog, FinishActivity, RefreshNavDrawer, StorageAccessFramework, HomeStorageButton, DialogListener {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var directoryViewModel: DirectoryViewModel
    private lateinit var viewModelFactory: ViewModelFactory
    private lateinit var storageDirectoryPreferenceProvider: StorageDirectoryPreferenceProvider
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var binding: ActivityMainBinding
    private var destination: NavDirections? = null

    @SuppressLint("ObsoleteSdkInt")
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            logd("Request read and write permissions")
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE),
                READ_AND_WRITE_PERMISSION_REQUEST_CODE)

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            logd("Request access to manage all files")
            if (!Environment.isExternalStorageManager()) {
                showDialog(ManageAllFilesRequestDialog())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setContentView(binding.root)
        setSupportActionBar(binding.incAppBarMain.toolbar)

        viewModelFactory = ViewModelFactory()
        directoryViewModel = ViewModelProvider(this, viewModelFactory).get(DirectoryViewModel::class.java)
        storageDirectoryPreferenceProvider = StorageDirectoryPreferenceProvider()

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)

        setupNavDrawer(navView, navController, drawerLayout)
        addStoragesToDrawer(navView, navController, drawerLayout)
        requestPermissions()

        if (intent.getBooleanExtra(KEY_INTENT_IS_FTP_NOTIFICATION_CLICKED, INTENT_IS_FTP_NOTIFICATION_CLICKED_DEF_VAL))
            navController.navigate(R.id.global_action_to_nav_ftp)

        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            data?.data?.let { treeUri ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    this.contentResolver.takePersistableUriPermission(treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                storageDirectoryPreferenceProvider.editChosenUri(treeUri.toString())
            }
        }
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
                R.id.nav_home -> destination = MobileNavigationDirections.globalActionNavHome()
                R.id.nav_preferences -> destination = MobileNavigationDirections.globalActionNavPreferences()
                R.id.nav_ftp -> destination = MobileNavigationDirections.globalActionToNavFtp()
                R.id.nav_info -> destination = MobileNavigationDirections.globalActionNavInfo()
            }
            drawerLayout.closeDrawers()
            true
        }

        val drawerToggle: ActionBarDrawerToggle =
            object : ActionBarDrawerToggle(this, drawerLayout, binding.incAppBarMain.toolbar, R.string.drawer_open, R.string.drawer_close) {
                override fun onDrawerClosed(view: View) {
                    super.onDrawerClosed(view)
                    //This whole thing is a workaround to fix nav drawer lag issue.
                    //Goes to the destination after it closes instead of right after the click.
                    destination?.let { navController.navigate(it) }
                }
            }

        drawerToggle.isDrawerIndicatorEnabled = true
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
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
        destination = MobileNavigationDirections.globalActionNavDirectory()
        if (path != ROOT_DIRECTORY && !File(path).canWrite() && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
            && storageDirectoryPreferenceProvider.getChosenUri() == EMPTY_STR) {
            launchSAF()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun showDialog(dialog: DialogFragment) {
        logd("Show a dialog")
        dialog.show(supportFragmentManager, EMPTY_STR)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            showDialog(SafAccessRequestDialog())
        }
    }

    override fun autoSizeButtonDimensions(storageButtonCount: Int, sideMargin: Int): Pair<Int, Int> {
        //to calculate the storage buttons so that they will fill the screen vertically
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        return Pair(((screenWidth - ((sideMargin * 2) * storageButtonCount)) / storageButtonCount),
            (screenHeight / (8 + storageButtonCount)))
    }

    @SuppressLint("InlinedApi") //Version check already exists
    override fun manageAllFilesRequestListener() {
        val intent = Intent()
        intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
        val uri: Uri = Uri.fromParts(URI_SCHEME, this.packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    @SuppressLint("InlinedApi") //Version check already exists
    override fun safAccessRequestListener() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        resultLauncher.launch(intent)
    }
}
