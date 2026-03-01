package com.erman.usurf.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Menu
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.window.layout.WindowMetricsCalculator
import com.erman.usurf.MobileNavigationDirections
import com.erman.usurf.R
import com.erman.usurf.activity.model.RefreshNavDrawer
import com.erman.usurf.activity.model.ShowDialog
import com.erman.usurf.databinding.ActivityMainBinding
import com.erman.usurf.dialog.model.ManageAllFilesRequestCallbacks
import com.erman.usurf.dialog.model.SafAccessRequestCallbacks
import com.erman.usurf.dialog.ui.ManageAllFilesRequestDialog
import com.erman.usurf.dialog.ui.SafAccessRequestDialog
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.home.model.HomeStorageButton
import com.erman.usurf.home.model.StorageAccessFramework
import com.erman.usurf.storage.domain.StorageDirectoryRepository
import com.erman.usurf.utils.KEY_INTENT_IS_FTP_NOTIFICATION_CLICKED
import com.google.android.material.navigation.NavigationView
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val INTENT_IS_FTP_NOTIFICATION_CLICKED_DEF_VAL = false
private const val URI_SCHEME = "package"
private const val READ_AND_WRITE_PERMISSION_REQUEST_CODE = 1
private const val SIDE_MARGIN_MULTIPLIER = 2
private const val STORAGE_BUTTON_HEIGHT_DIVISOR_OFFSET = 8
private const val STORAGE_MENU_ITEM_ORDER_FIRST = 0

class MainActivity :
    AppCompatActivity(),
    ShowDialog,
    RefreshNavDrawer,
    StorageAccessFramework,
    HomeStorageButton {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val storageDirectoryRepository: StorageDirectoryRepository by inject()
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var binding: ActivityMainBinding
    private var destination: NavDirections? = null
    private val mainViewModel by viewModel<MainViewModel>()
    private val directoryViewModel by viewModel<DirectoryViewModel>()

    @SuppressLint("ObsoleteSdkInt")
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ),
                READ_AND_WRITE_PERMISSION_REQUEST_CODE,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val dialog = ManageAllFilesRequestDialog()
                dialog.callbacks = ManageAllFilesRequestCallbacks { manageAllFilesRequest() }
                showDialog(dialog)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setContentView(binding.root)
        setSupportActionBar(binding.incAppBarMain.toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)

        setupNavDrawer(navView, navController, drawerLayout)
        mainViewModel.storagePaths.observe(this) { paths ->
            populateStorageDrawer(navView, drawerLayout, paths)
        }
        requestPermissions()

        if (intent.getBooleanExtra(
                KEY_INTENT_IS_FTP_NOTIFICATION_CLICKED,
                INTENT_IS_FTP_NOTIFICATION_CLICKED_DEF_VAL,
            )
        ) {
            navController.navigate(R.id.global_action_to_nav_ftp)
        }

        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val data: Intent? = result.data
                data?.data?.let { treeUri ->
                    this.contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                    storageDirectoryRepository.setChosenUri(treeUri.toString())
                }
            }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawers()
                        return
                    }
                    if (navController.currentDestination?.id == R.id.nav_directory) {
                        // Let DirectoryViewModel try to handle back (go up a directory, exit modes, etc.)
                        if (directoryViewModel.onBackPressed()) {
                            return
                        }
                        // If DirectoryViewModel cannot handle it, navigate up in the nav graph
                        if (navController.popBackStack()) {
                            return
                        }
                        finish()
                        return
                    }
                    // For all other destinations, rely on Navigation back stack first
                    if (navController.popBackStack()) {
                        return
                    }
                    finish()
                }
            },
        )
    }

    private fun setupNavDrawer(
        navView: NavigationView,
        navController: NavController,
        drawerLayout: DrawerLayout,
    ) {
        appBarConfiguration =
            AppBarConfiguration(
                setOf(
                    R.id.nav_home,
                    R.id.nav_directory,
                    R.id.nav_preferences,
                    R.id.nav_ftp,
                    R.id.nav_info,
                ),
                drawerLayout,
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
            object : ActionBarDrawerToggle(
                this,
                drawerLayout,
                binding.incAppBarMain.toolbar,
                R.string.drawer_open,
                R.string.drawer_close,
            ) {
                override fun onDrawerClosed(view: View) {
                    super.onDrawerClosed(view)
                    // This whole thing is a workaround to fix nav drawer lag issue.
                    // Goes to the destination after it closes instead of right after the click.
                    destination?.let { navController.navigate(it) }
                }
            }
        drawerToggle.isDrawerIndicatorEnabled = true
        drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
    }

    private fun populateStorageDrawer(
        navView: NavigationView,
        drawerLayout: DrawerLayout,
        paths: List<String>,
    ) {
        navView.menu.removeGroup(R.id.storage)
        for (path in paths) {
            val storage = navView.menu.add(R.id.storage, Menu.NONE, STORAGE_MENU_ITEM_ORDER_FIRST, path)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                storage.icon = ContextCompat.getDrawable(this, R.drawable.ic_hdd)
            }
            storage.setOnMenuItemClickListener {
                val result = mainViewModel.onStorageSelected(path)
                directoryViewModel.setPath(path)
                destination = MobileNavigationDirections.globalActionNavDirectory()
                if (result.showSafDialog) {
                    launchSAF()
                }
                drawerLayout.closeDrawers()
                true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun showDialog(dialog: DialogFragment) {
        dialog.show(supportFragmentManager, "")
    }

    override fun refreshStorageButtons() {
        mainViewModel.refreshStoragePaths()
    }

    override fun launchSAF() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val dialog = SafAccessRequestDialog()
            dialog.callbacks = SafAccessRequestCallbacks { safAccessRequest() }
            showDialog(dialog)
        }
    }

    override fun autoSizeButtonDimensions(
        storageButtonCount: Int,
        sideMargin: Int,
    ): Pair<Int, Int> {
        // to calculate the storage buttons so that they will fill the screen vertically
        val windowMetrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
        val currentBounds = windowMetrics.bounds // E.g. [0 0 1350 1800]
        val screenWidth = currentBounds.width()
        val screenHeight = currentBounds.height()

        return Pair(
            ((screenWidth - ((sideMargin * SIDE_MARGIN_MULTIPLIER) * storageButtonCount)) / storageButtonCount),
            (screenHeight / (STORAGE_BUTTON_HEIGHT_DIVISOR_OFFSET + storageButtonCount)),
        )
    }

    @SuppressLint("InlinedApi") // Version check already exists
    private fun manageAllFilesRequest() {
        val intent = Intent()
        intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
        val uri: Uri = Uri.fromParts(URI_SCHEME, this.packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    @SuppressLint("InlinedApi") // Version check already exists
    private fun safAccessRequest() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        resultLauncher.launch(intent)
    }
}
