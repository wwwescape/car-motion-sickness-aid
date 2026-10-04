package com.wwwescape.carmotionsicknessaid.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wwwescape.carmotionsicknessaid.data.settings.AppSettings
import com.wwwescape.carmotionsicknessaid.data.settings.SettingsRepository
import com.wwwescape.carmotionsicknessaid.motion.SensorAvailability
import com.wwwescape.carmotionsicknessaid.overlay.CueOverlayService
import com.wwwescape.carmotionsicknessaid.overlay.CueOverlayState
import com.wwwescape.carmotionsicknessaid.overlay.CueStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val settings: StateFlow<AppSettings> = SettingsRepository.settingsFlow(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val status: StateFlow<CueStatus> = CueOverlayState.status

    val sensors: SensorAvailability = SensorAvailability.of(application)

    fun start() {
        CueOverlayService.start(getApplication())
    }

    fun stop() = CueOverlayService.stop(getApplication())

    fun togglePause() = CueOverlayService.togglePause(getApplication())
}
