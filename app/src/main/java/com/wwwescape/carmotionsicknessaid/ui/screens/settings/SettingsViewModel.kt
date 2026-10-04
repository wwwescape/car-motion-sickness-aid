package com.wwwescape.carmotionsicknessaid.ui.screens.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wwwescape.carmotionsicknessaid.auto.VehicleDetection
import com.wwwescape.carmotionsicknessaid.data.settings.AppSettings
import com.wwwescape.carmotionsicknessaid.data.settings.ColorTheme
import com.wwwescape.carmotionsicknessaid.data.settings.CueColor
import com.wwwescape.carmotionsicknessaid.data.settings.CueDensity
import com.wwwescape.carmotionsicknessaid.data.settings.CueSize
import com.wwwescape.carmotionsicknessaid.data.settings.CueStyle
import com.wwwescape.carmotionsicknessaid.data.settings.SensorMode
import com.wwwescape.carmotionsicknessaid.data.settings.SettingsRepository
import com.wwwescape.carmotionsicknessaid.data.settings.ThemeContrast
import com.wwwescape.carmotionsicknessaid.data.settings.ThemeMode
import com.wwwescape.carmotionsicknessaid.motion.SensorAvailability
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val settings: StateFlow<AppSettings> = SettingsRepository.settingsFlow(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val sensors: SensorAvailability = SensorAvailability.of(application)

    private fun save(block: suspend (Context) -> Unit) {
        viewModelScope.launch { block(getApplication()) }
    }

    fun setThemeMode(mode: ThemeMode) = save { SettingsRepository.setThemeMode(it, mode) }
    fun setDynamicColor(enabled: Boolean) = save { SettingsRepository.setDynamicColor(it, enabled) }
    fun setColorTheme(theme: ColorTheme) = save { SettingsRepository.setColorTheme(it, theme) }
    fun setThemeContrast(contrast: ThemeContrast) = save { SettingsRepository.setThemeContrast(it, contrast) }
    fun setPureDark(enabled: Boolean) = save { SettingsRepository.setPureDark(it, enabled) }
    fun setAbsoluteDark(enabled: Boolean) = save { SettingsRepository.setAbsoluteDark(it, enabled) }
    fun setCueStyle(style: CueStyle) = save { SettingsRepository.setCueStyle(it, style) }
    fun setCueColor(color: CueColor) = save { SettingsRepository.setCueColor(it, color) }
    fun setCueSize(size: CueSize) = save { SettingsRepository.setCueSize(it, size) }
    fun setCueDensity(density: CueDensity) = save { SettingsRepository.setCueDensity(it, density) }
    fun setCueOpacity(opacity: Float) = save { SettingsRepository.setCueOpacity(it, opacity) }
    fun setCueSensitivity(value: Float) = save { SettingsRepository.setCueSensitivity(it, value) }
    fun setCueSmoothing(value: Float) = save { SettingsRepository.setCueSmoothing(it, value) }
    fun setHideWhenStill(enabled: Boolean) = save { SettingsRepository.setHideWhenStill(it, enabled) }
    fun setSensorMode(mode: SensorMode) = save { SettingsRepository.setSensorMode(it, mode) }
    fun setPauseWhenScreenOff(enabled: Boolean) = save { SettingsRepository.setPauseWhenScreenOff(it, enabled) }

    /** Callers must already hold the activity-recognition permission when enabling. */
    fun setAutoStartInVehicle(enabled: Boolean) = save {
        SettingsRepository.setAutoStartInVehicle(it, enabled)
        if (enabled) VehicleDetection.register(it) else VehicleDetection.unregister(it)
    }
}
