package com.androidfung.linkscrub.util

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Helper class to manage Google Play In-App Updates lifecycle.
 * Supports flexible updates with a callback/StateFlow when downloaded,
 * as well as resuming downloaded updates and immediate updates.
 */
class InAppUpdateManager(
    activity: ComponentActivity,
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity),
) {

    private val _isUpdateDownloaded = MutableStateFlow(false)
    val isUpdateDownloaded: StateFlow<Boolean> = _isUpdateDownloaded.asStateFlow()

    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                // Update flow was cancelled or failed
                AnalyticsHelper.recordBreadcrumb("In-app update flow cancelled or failed: ${result.resultCode}")
            }
        }

    private val installStateUpdatedListener = InstallStateUpdatedListener { state: InstallState ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            _isUpdateDownloaded.value = true
            AnalyticsHelper.recordBreadcrumb("In-app update status: DOWNLOADED")
        }
    }

    init {
        appUpdateManager.registerListener(installStateUpdatedListener)
    }

    /**
     * Checks for available updates.
     * Prefers Flexible update by default; can fall back or enforce Immediate update if needed.
     */
    fun checkForUpdate(preferImmediate: Boolean = false) {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                val updateType = if (preferImmediate && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    AppUpdateType.IMMEDIATE
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    AppUpdateType.FLEXIBLE
                } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    AppUpdateType.IMMEDIATE
                } else {
                    null
                }

                if (updateType != null) {
                    startUpdateFlow(appUpdateInfo, updateType)
                }
            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                _isUpdateDownloaded.value = true
            }
        }.addOnFailureListener { exception ->
            AnalyticsHelper.recordException(exception)
        }
    }

    /**
     * Resumes check on Activity onResume to handle uncompleted immediate updates
     * or already downloaded flexible updates.
     */
    fun onResumeCheck() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                _isUpdateDownloaded.value = true
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // If an immediate update is already in progress, resume it
                startUpdateFlow(appUpdateInfo, AppUpdateType.IMMEDIATE)
            }
        }
    }

    private fun startUpdateFlow(appUpdateInfo: AppUpdateInfo, updateType: Int) {
        try {
            val options = AppUpdateOptions.newBuilder(updateType).build()
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                updateLauncher,
                options
            )
        } catch (e: Exception) {
            AnalyticsHelper.recordException(e)
        }
    }

    /**
     * Completes flexible update by restarting the app.
     */
    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    /**
     * Unregisters listeners. Should be called when activity is destroyed.
     */
    fun onDestroy() {
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }
}
