package com.wwwescape.carmotionsicknessaid.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.wwwescape.carmotionsicknessaid.MainActivity
import com.wwwescape.carmotionsicknessaid.R
import com.wwwescape.carmotionsicknessaid.overlay.CueOverlayService
import com.wwwescape.carmotionsicknessaid.overlay.CueOverlayState
import com.wwwescape.carmotionsicknessaid.overlay.CueStatus

/** Quick Settings tile that toggles the motion cues without opening the app. Falls back to
 * opening the app when the overlay permission is missing or the system won't let us start the
 * service from here. */
class CuesTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        if (CueOverlayState.status.value != CueStatus.STOPPED) {
            CueOverlayService.stop(this)
        } else if (!CueOverlayService.canDrawOverlays(this) || !CueOverlayService.start(this)) {
            openApp()
        }
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val status = CueOverlayState.status.value
        tile.state = if (status == CueStatus.STOPPED) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.label = getString(R.string.tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                when (status) {
                    CueStatus.STOPPED -> R.string.tile_state_off
                    CueStatus.RUNNING -> R.string.tile_state_on
                    CueStatus.PAUSED -> R.string.tile_state_paused
                },
            )
        }
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        fun requestUpdate(context: Context) {
            runCatching {
                requestListeningState(context, ComponentName(context, CuesTileService::class.java))
            }
        }
    }
}
