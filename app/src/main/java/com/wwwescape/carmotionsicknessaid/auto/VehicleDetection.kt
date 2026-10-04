package com.wwwescape.carmotionsicknessaid.auto

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.wwwescape.carmotionsicknessaid.data.settings.SettingsRepository
import com.wwwescape.carmotionsicknessaid.overlay.CueOverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Starts the cues when Google Play services' Activity Recognition reports the user getting into a
 * vehicle, and stops them (if they were started this way) on leaving it. Activity-transition
 * broadcasts are one of Android's documented exemptions that allow a foreground service to start
 * from the background.
 */
object VehicleDetection {

    private const val REQUEST_CODE = 42

    fun hasPermission(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACTIVITY_RECOGNITION
        } else {
            "com.google.android.gms.permission.ACTIVITY_RECOGNITION"
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun register(context: Context) {
        if (!hasPermission(context)) return
        val transitions = listOf(ActivityTransition.ACTIVITY_TRANSITION_ENTER, ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .map { type ->
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.IN_VEHICLE)
                    .setActivityTransition(type)
                    .build()
            }
        runCatching {
            ActivityRecognition.getClient(context)
                .requestActivityTransitionUpdates(ActivityTransitionRequest(transitions), pendingIntent(context))
        }
    }

    @SuppressLint("MissingPermission")
    fun unregister(context: Context) {
        if (!hasPermission(context)) return
        runCatching { ActivityRecognition.getClient(context).removeActivityTransitionUpdates(pendingIntent(context)) }
    }

    /** Play services fills in the transition result as extras, so this must be mutable. */
    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, VehicleTransitionReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0,
    )
}

class VehicleTransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val latest = result.transitionEvents.lastOrNull { it.activityType == DetectedActivity.IN_VEHICLE } ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (!SettingsRepository.current(context).activation.autoStartInVehicle) return@launch
                when (latest.transitionType) {
                    ActivityTransition.ACTIVITY_TRANSITION_ENTER ->
                        if (CueOverlayService.canDrawOverlays(context)) CueOverlayService.start(context, automatic = true)
                    ActivityTransition.ACTIVITY_TRANSITION_EXIT -> CueOverlayService.stopIfAutomatic(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Activity-transition registrations don't survive a reboot or an app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (SettingsRepository.current(context).activation.autoStartInVehicle) VehicleDetection.register(context)
            } finally {
                pending.finish()
            }
        }
    }
}
