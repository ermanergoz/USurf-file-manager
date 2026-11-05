package com.erman.usurf.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.erman.usurf.MobileNavigationDirections
import com.erman.usurf.R
import com.erman.usurf.databinding.ActivityMainBinding
import com.erman.usurf.directory.ui.DirectoryViewModel
import com.erman.usurf.utils.EventObserver
import com.erman.usurf.utils.KEY_INTENT_IS_FTP_NOTIFICATION_CLICKED
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val INTENT_IS_FTP_NOTIFICATION_CLICKED_DEF_VAL: Boolean = false
private const val STORAGE_MENU_ITEM_ORDER_FIRST: Int = 0

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var destination: NavDirections? = null
    private val mainViewModel by viewModel<MainViewModel>()

    // Held here only to ask whether leaving would stop an operation, and to retry the write a new grant was asked for.
    private val directoryViewModel by viewModel<DirectoryViewModel>()
    private lateinit var permissionHandler: PermissionHandler

    // Added while the drawer is open, after any screen's own handler, so Back closes the drawer before a screen sees it.
    private val drawerBackCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                binding.drawerLayout.closeDrawers()
            }
        }

    private val navController: NavController
        get() = findNavController(R.id.nav_host_fragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Drawn edge to edge on every version, as Android 15 and later draw it anyway. The status bar's icons are light,
        // over the brand-blue app bar of both themes; each screen keeps its content clear of the navigation bar.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        permissionHandler =
            PermissionHandler(this) { uri: String ->
                mainViewModel.onSafUriChosen(uri)
                directoryViewModel.onSafAccessGranted()
            }
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(binding.incAppBarMain.toolbar)
        permissionHandler.rootView = binding.root

        configureAppBar()
        bindDrawerItems()
        installDrawerToggle()
        observeViewModel()
        permissionHandler.requestInitialPermissions()

        if (savedInstanceState == null) {
            handleFtpNotificationIntent(intent)
        }
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = handleBackPressed()
            }
        )
    }

    override fun onResume() {
        super.onResume()
        // A drawer the system reopened after a recreation gets its Back handler too.
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) onDrawerOpenChanged(isOpen = true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleFtpNotificationIntent(intent)
    }

    // What no screen used: the drawer closes, the screen below comes back, or the app is left.
    private fun handleBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawers()
            return
        }
        if (navController.popBackStack()) return
        exitOrConfirm()
    }

    private fun handleFtpNotificationIntent(intent: Intent) {
        val fromFtpNotification =
            intent.getBooleanExtra(
                KEY_INTENT_IS_FTP_NOTIFICATION_CLICKED,
                INTENT_IS_FTP_NOTIFICATION_CLICKED_DEF_VAL
            )
        if (fromFtpNotification) {
            intent.removeExtra(KEY_INTENT_IS_FTP_NOTIFICATION_CLICKED)
            navController.navigate(R.id.global_action_to_nav_ftp)
        }
    }

    private fun configureAppBar() {
        appBarConfiguration =
            AppBarConfiguration(
                setOf(
                    R.id.nav_home,
                    R.id.nav_directory,
                    R.id.nav_preferences,
                    R.id.nav_ftp,
                    R.id.nav_info
                ),
                binding.drawerLayout
            )
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    // A destination is opened once the drawer has closed, so the drawer does not slide over the new screen.
    private fun bindDrawerItems() {
        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> destination = MobileNavigationDirections.globalActionNavHome()
                R.id.nav_preferences -> destination = MobileNavigationDirections.globalActionNavPreferences()
                R.id.nav_ftp -> destination = MobileNavigationDirections.globalActionToNavFtp()
                R.id.nav_info -> destination = MobileNavigationDirections.globalActionNavInfo()
            }
            binding.drawerLayout.closeDrawers()
            true
        }
    }

    private fun installDrawerToggle() {
        val drawerToggle: ActionBarDrawerToggle =
            object : ActionBarDrawerToggle(
                this,
                binding.drawerLayout,
                binding.incAppBarMain.toolbar,
                R.string.drawer_open,
                R.string.drawer_close
            ) {
                override fun onDrawerOpened(drawerView: View) {
                    super.onDrawerOpened(drawerView)
                    onDrawerOpenChanged(isOpen = true)
                }

                override fun onDrawerClosed(view: View) {
                    super.onDrawerClosed(view)
                    onDrawerOpenChanged(isOpen = false)
                    destination?.let {
                        navController.navigate(it)
                        destination = null
                    }
                }
            }
        drawerToggle.isDrawerIndicatorEnabled = true
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
    }

    private fun onDrawerOpenChanged(isOpen: Boolean) {
        drawerBackCallback.remove()
        if (isOpen) onBackPressedDispatcher.addCallback(this, drawerBackCallback)
    }

    private fun observeViewModel() {
        mainViewModel.storagePaths.observe(this, ::populateStorageDrawer)
        mainViewModel.uiEvents.observe(
            this,
            EventObserver { event ->
                when (event) {
                    MainUiEvent.LaunchSafPicker -> permissionHandler.requestSafAccess()
                }
            }
        )
    }

    private fun populateStorageDrawer(paths: List<String>) {
        val menu: Menu = binding.navView.menu
        menu.removeGroup(R.id.storage)
        for (path in paths) {
            val storage = menu.add(R.id.storage, Menu.NONE, STORAGE_MENU_ITEM_ORDER_FIRST, path)
            storage.icon = ContextCompat.getDrawable(this, R.drawable.ic_hdd)
            storage.setOnMenuItemClickListener {
                destination = MobileNavigationDirections.globalActionNavDirectory(path)
                mainViewModel.onStorageSelected(path)
                binding.drawerLayout.closeDrawers()
                true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean = navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()

    private fun exitOrConfirm() {
        if (directoryViewModel.shouldConfirmExit()) {
            showExitConfirmationDialog()
        } else {
            finish()
        }
    }

    private fun showExitConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.exit_confirmation_message)
            .setPositiveButton(R.string.ok) { _, _ -> finish() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
