package com.wwwescape.carmotionsicknessaid.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import android.view.Display
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.wwwescape.carmotionsicknessaid.MainActivity
import com.wwwescape.carmotionsicknessaid.R
import com.wwwescape.carmotionsicknessaid.data.settings.ActivationSettings
import com.wwwescape.carmotionsicknessaid.data.settings.SettingsRepository
import com.wwwescape.carmotionsicknessaid.motion.MotionEngine
import com.wwwescape.carmotionsicknessaid.tile.CuesTileService
import kotlinx.coroutines.launch

/**
 * Foreground service that draws the motion cues over every app via a non-touchable
 * `TYPE_APPLICATION_OVERLAY` window. Touches always pass straight through: the window is
 * `FLAG_NOT_TOUCHABLE` and its alpha never exceeds 0.8, the Android 12+ limit above which the
 * system would block touches beneath an untrusted overlay.
 */
class CueOverlayService : LifecycleService() {

    private lateinit var engine: MotionEngine
    private var overlayView: CueOverlayView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var activation = ActivationSettings()
    private var userPaused = false
    private var screenOff = false

    private val windowManager by lazy { getSystemService(WindowManager::class.java) }
    private val defaultDisplay: Display? by lazy {
        getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            screenOff = intent.action == Intent.ACTION_SCREEN_OFF
            applyRunState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        engine = MotionEngine(this) { defaultDisplay?.rotation ?: 0 }
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Must reach startForeground() promptly on every start, even one we're about to abandon.
        goForeground()

        when (intent?.action) {
            ACTION_STOP -> {
                shutDown()
                return START_NOT_STICKY
            }
            ACTION_STOP_IF_AUTOMATIC -> {
                if (CueOverlayState.startedAutomatically || overlayView == null) shutDown()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_PAUSE -> {
                userPaused = !userPaused
                applyRunState()
                return START_STICKY
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            shutDown()
            return START_NOT_STICKY
        }
        // A manual start "adopts" an automatic session, so leaving the vehicle won't end it.
        val automatic = intent?.getBooleanExtra(EXTRA_AUTOMATIC, false) == true
        if (overlayView == null) {
            CueOverlayState.startedAutomatically = automatic
            attachOverlay()
        } else if (!automatic) {
            CueOverlayState.startedAutomatically = false
        }
        userPaused = false
        applyRunState()
        return START_STICKY
    }

    private fun attachOverlay() {
        val view = CueOverlayView(this, engine)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            alpha = MAX_WINDOW_ALPHA
            title = getString(R.string.app_name)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                fitInsetsTypes = 0
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        runCatching { windowManager.addView(view, params) }.onFailure {
            shutDown()
            return
        }
        overlayView = view
        layoutParams = params

        lifecycleScope.launch {
            SettingsRepository.settingsFlow(this@CueOverlayService).collect { settings ->
                activation = settings.activation
                engine.mode = settings.cues.sensorMode
                view.settings = settings.cues
                params.alpha = settings.cues.opacity.coerceIn(0.1f, MAX_WINDOW_ALPHA)
                runCatching { windowManager.updateViewLayout(view, params) }
                view.refresh()
                applyRunState()
            }
        }
    }

    /** Sensors and drawing run only while the user hasn't paused and — unless "pause when screen
     * off" is turned off — the screen is on. Screen-off pauses aren't shown as "Paused" since
     * they resume by themselves. */
    private fun applyRunState() {
        val view = overlayView ?: return
        val paused = userPaused || (screenOff && activation.pauseWhenScreenOff)
        if (paused) {
            engine.stop()
            view.stopTicking()
            view.visibility = View.GONE
        } else {
            view.visibility = View.VISIBLE
            engine.start()
            view.startTicking()
        }
        CueOverlayState.set(if (userPaused) CueStatus.PAUSED else CueStatus.RUNNING)
        updateNotification()
        CuesTileService.requestUpdate(this)
    }

    private fun goForeground() {
        createChannel()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val pauseLabel = if (userPaused) R.string.action_resume else R.string.action_pause
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_cues)
            .setContentTitle(getString(if (userPaused) R.string.notification_title_paused else R.string.notification_title_running))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, getString(pauseLabel), servicePendingIntent(ACTION_TOGGLE_PAUSE, 1))
            .addAction(0, getString(R.string.action_stop), servicePendingIntent(ACTION_STOP, 2))
            .build()
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this,
        requestCode,
        Intent(this, CueOverlayService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            },
        )
    }

    private fun shutDown() {
        removeOverlay()
        CueOverlayState.startedAutomatically = false
        CueOverlayState.set(CueStatus.STOPPED)
        CuesTileService.requestUpdate(this)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun removeOverlay() {
        engine.stop()
        overlayView?.let { view ->
            view.stopTicking()
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        layoutParams = null
    }

    override fun onDestroy() {
        removeOverlay()
        runCatching { unregisterReceiver(screenReceiver) }
        CueOverlayState.set(CueStatus.STOPPED)
        CuesTileService.requestUpdate(this)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "motion_cues"
        private const val NOTIFICATION_ID = 1
        private const val MAX_WINDOW_ALPHA = 0.8f

        const val ACTION_START = "com.wwwescape.carmotionsicknessaid.action.START"
        const val ACTION_STOP = "com.wwwescape.carmotionsicknessaid.action.STOP"
        const val ACTION_STOP_IF_AUTOMATIC = "com.wwwescape.carmotionsicknessaid.action.STOP_IF_AUTOMATIC"
        const val ACTION_TOGGLE_PAUSE = "com.wwwescape.carmotionsicknessaid.action.TOGGLE_PAUSE"
        const val EXTRA_AUTOMATIC = "automatic"

        fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

        /** Returns false if the system refused the start (a background-start restriction). */
        fun start(context: Context, automatic: Boolean = false): Boolean = send(
            context,
            Intent(context, CueOverlayService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_AUTOMATIC, automatic),
        )

        fun stop(context: Context) {
            if (CueOverlayState.status.value == CueStatus.STOPPED) return
            send(context, Intent(context, CueOverlayService::class.java).setAction(ACTION_STOP))
        }

        fun stopIfAutomatic(context: Context) {
            if (CueOverlayState.status.value == CueStatus.STOPPED) return
            send(context, Intent(context, CueOverlayService::class.java).setAction(ACTION_STOP_IF_AUTOMATIC))
        }

        fun togglePause(context: Context) {
            if (CueOverlayState.status.value == CueStatus.STOPPED) return
            send(context, Intent(context, CueOverlayService::class.java).setAction(ACTION_TOGGLE_PAUSE))
        }

        private fun send(context: Context, intent: Intent): Boolean =
            runCatching { ContextCompat.startForegroundService(context, intent) }.isSuccess
    }
}
