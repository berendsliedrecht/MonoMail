package com.fsck.k9.ui.managefolders

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.BaseActivity
import com.fsck.k9.ui.base.extensions.findNavController
import net.thunderbird.core.android.account.LegacyAccountDto

class ManageFoldersActivity : BaseActivity() {
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayout(R.layout.activity_manage_folders)
        setTitle(R.string.folders_action)

        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT) ?: error("Missing Intent extra '$EXTRA_ACCOUNT'")

        initializeNavController(accountUuid)
    }

    private fun initializeNavController(accountUuid: String) {
        navController = findNavController(R.id.nav_host_fragment)

        val fragmentArguments = bundleOf(
            ManageFoldersFragment.EXTRA_ACCOUNT to accountUuid,
        )
        navController.setGraph(R.navigation.navigation_manage_folders, fragmentArguments)

        // MonoMail: allow deep-linking straight into one folder's settings. The
        // legacy folder list is popped from the back stack so pressing back
        // returns to the e-ink UI instead of the old screen.
        val folderId = intent.getLongExtra(EXTRA_FOLDER_ID, -1L)
        if (folderId != -1L) {
            navController.navigate(
                R.id.folderSettingsScreen,
                bundleOf(
                    FolderSettingsFragment.EXTRA_ACCOUNT to accountUuid,
                    FolderSettingsFragment.EXTRA_FOLDER_ID to folderId,
                ),
                navOptions {
                    popUpTo(R.id.manageFoldersScreen) { inclusive = true }
                },
            )
        }

        val appBarConfiguration = AppBarConfiguration(topLevelDestinationIds = emptySet())
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp() || finishActivity()
    }

    private fun finishActivity(): Boolean {
        finish()
        return true
    }

    companion object {
        private const val EXTRA_ACCOUNT = "account"
        private const val EXTRA_FOLDER_ID = "folderId"

        @JvmStatic
        fun launch(activity: Activity, account: LegacyAccountDto) {
            val intent = Intent(activity, ManageFoldersActivity::class.java).apply {
                putExtra(EXTRA_ACCOUNT, account.uuid)
            }
            activity.startActivity(intent)
        }

        // MonoMail: open one folder's settings directly (e-ink folder manager).
        @JvmStatic
        fun launchFolderSettings(activity: Activity, account: LegacyAccountDto, folderId: Long) {
            val intent = Intent(activity, ManageFoldersActivity::class.java).apply {
                putExtra(EXTRA_ACCOUNT, account.uuid)
                putExtra(EXTRA_FOLDER_ID, folderId)
            }
            activity.startActivity(intent)
        }
    }
}
