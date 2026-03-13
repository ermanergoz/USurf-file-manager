package com.erman.usurf.activity

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
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
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.home.model.HomeStorageButton
import com.erman.usurf.home.model.StorageAccessFramework
import com.erman.usurf.storage.domain.StorageDirectoryRepository
import com.erman.usurf.utils.KEY_INTENT_IS_FTP_NOTIFICATION_CLICKED
import com.google.android.material.navigation.NavigationView
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val INTENT_IS_FTP_NOTIFICATION_CLICKED_DEF_VAL: Boolean = false
private const val SIDE_MARGIN_MULTIPLIER: Int = 2
private const val STORAGE_BUTTON_HEIGHT_DIVISOR_OFFSET: Int = 8
private const val STORAGE_MENU_ITEM_ORDER_FIRST: Int = 0

class MainActivity :
    AppCompatActivity(),
    ShowDialog,
    RefreshNavDrawer,
    StorageAccessFramework,
    HomeStorageButton {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val storageDirectoryRepository: StorageDirectoryRepository by inject()
    private lateinit var binding: ActivityMainBinding
    private var destination: NavDirections? = null
    private val mainViewModel by viewModel<MainViewModel>()
    private val directoryViewModel by viewModel<DirectoryViewModel>()
    private lateinit var permissionHandler: PermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHandler =
            PermissionHandler(this) { uri: String ->
                storageDirectoryRepository.setChosenUri(uri)
            }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setContentView(binding.root)
        setSupportActionBar(binding.incAppBarMain.toolbar)
        permissionHandler.rootView = binding.root

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController: NavController = findNavController(R.id.nav_host_fragment)

        setupNavDrawer(navView, navController, drawerLayout)
        mainViewModel.storagePaths.observe(this) { paths: List<String> ->
            populateStorageDrawer(navView, drawerLayout, paths)
        }
        permissionHandler.requestInitialPermissions()

        if (intent.getBooleanExtra(
                KEY_INTENT_IS_FTP_NOTIFICATION_CLICKED,
                INTENT_IS_FTP_NOTIFICATION_CLICKED_DEF_VAL,
            )
        ) {
            navController.navigate(R.id.global_action_to_nav_ftp)
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
                        if (directoryViewModel.onBackPressed()) {
                            return
                        }
                        if (navController.popBackStack()) {
                            return
                        }
                        finish()
                        return
                    }
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
            storage.icon = ContextCompat.getDrawable(this, R.drawable.ic_hdd)
            storage.setOnMenuItemClickListener {
                val result = mainViewModel.onStorageSelected(path)
                destination = MobileNavigationDirections.globalActionNavDirectory(path)
                if (result.showSafDialog) {
                    launchSAF()
                }
                drawerLayout.closeDrawers()
                true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController: NavController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun showDialog(dialog: DialogFragment) {
        dialog.show(supportFragmentManager, "")
    }

    override fun refreshStorageButtons() {
        mainViewModel.refreshStoragePaths()
    }

    override fun launchSAF() {
        permissionHandler.requestSafAccess()
    }

    override fun autoSizeButtonDimensions(
        storageButtonCount: Int,
        sideMargin: Int,
        containerHorizontalPadding: Int,
    ): Pair<Int, Int> {
        val windowMetrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
        val currentBounds = windowMetrics.bounds
        val availableWidth: Int = currentBounds.width() - containerHorizontalPadding
        val screenHeight: Int = currentBounds.height()
        return Pair(
            ((availableWidth - ((sideMargin * SIDE_MARGIN_MULTIPLIER) * storageButtonCount)) / storageButtonCount),
            (screenHeight / (STORAGE_BUTTON_HEIGHT_DIVISOR_OFFSET + storageButtonCount)),
        )
    }
}
