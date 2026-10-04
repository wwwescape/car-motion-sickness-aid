package com.wwwescape.carmotionsicknessaid.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CueStatus { STOPPED, RUNNING, PAUSED }

/** Process-wide view of the overlay service, observed by the Home screen and the Quick Settings
 * tile. Only [CueOverlayService] writes it. */
object CueOverlayState {
    private val _status = MutableStateFlow(CueStatus.STOPPED)
    val status: StateFlow<CueStatus> = _status.asStateFlow()

    /** Whether the current session was started by vehicle detection rather than the user — only
     * those sessions are stopped automatically when the user leaves the vehicle. */
    @Volatile
    var startedAutomatically: Boolean = false
        internal set

    internal fun set(status: CueStatus) {
        _status.value = status
    }
}
