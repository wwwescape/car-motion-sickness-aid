package com.wwwescape.carmotionsicknessaid.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Mirrors [ThemeMode] outside DataStore, which is Flow-only/async. [MainActivity] reads this
 * synchronously in `attachBaseContext`, before any Compose or DataStore code can run, so the
 * window's initial (pre-Compose) theme resolution honors the user's choice instead of always
 * following the raw system day/night setting. */
const val THEME_PREFS_NAME = "theme_prefs_sync"
const val THEME_MODE_PREF_KEY = "theme_mode"

object SettingsRepository {

    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
    private val COLOR_THEME_KEY = stringPreferencesKey("color_theme")
    private val THEME_CONTRAST_KEY = stringPreferencesKey("theme_contrast")
    private val PURE_DARK_KEY = booleanPreferencesKey("pure_dark")
    private val ABSOLUTE_DARK_KEY = booleanPreferencesKey("absolute_dark")
    private val CUE_STYLE_KEY = stringPreferencesKey("cue_style")
    private val CUE_COLOR_KEY = stringPreferencesKey("cue_color")
    private val CUE_SIZE_KEY = stringPreferencesKey("cue_size")
    private val CUE_DENSITY_KEY = stringPreferencesKey("cue_density")
    private val CUE_OPACITY_KEY = floatPreferencesKey("cue_opacity")
    private val CUE_SENSITIVITY_KEY = floatPreferencesKey("cue_sensitivity")
    private val CUE_SMOOTHING_KEY = floatPreferencesKey("cue_smoothing")
    private val HIDE_WHEN_STILL_KEY = booleanPreferencesKey("hide_when_still")
    private val SENSOR_MODE_KEY = stringPreferencesKey("sensor_mode")
    private val AUTO_START_KEY = booleanPreferencesKey("auto_start_in_vehicle")
    private val PAUSE_SCREEN_OFF_KEY = booleanPreferencesKey("pause_when_screen_off")

    fun settingsFlow(context: Context): Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[THEME_MODE_KEY].toEnumOrDefault(ThemeMode.SYSTEM),
            useDynamicColor = prefs[DYNAMIC_COLOR_KEY] ?: true,
            colorTheme = prefs[COLOR_THEME_KEY].toEnumOrDefault(ColorTheme.DEFAULT),
            themeContrast = prefs[THEME_CONTRAST_KEY].toEnumOrDefault(ThemeContrast.STANDARD),
            pureDark = prefs[PURE_DARK_KEY] ?: false,
            absoluteDark = prefs[ABSOLUTE_DARK_KEY] ?: false,
            cues = CueSettings(
                style = prefs[CUE_STYLE_KEY].toEnumOrDefault(CueStyle.EDGE_DOTS),
                color = prefs[CUE_COLOR_KEY].toEnumOrDefault(CueColor.ADAPTIVE),
                size = prefs[CUE_SIZE_KEY].toEnumOrDefault(CueSize.MEDIUM),
                density = prefs[CUE_DENSITY_KEY].toEnumOrDefault(CueDensity.MEDIUM),
                opacity = prefs[CUE_OPACITY_KEY] ?: 0.6f,
                sensitivity = prefs[CUE_SENSITIVITY_KEY] ?: 0.5f,
                smoothing = prefs[CUE_SMOOTHING_KEY] ?: 0.5f,
                hideWhenStill = prefs[HIDE_WHEN_STILL_KEY] ?: false,
                sensorMode = prefs[SENSOR_MODE_KEY].toEnumOrDefault(SensorMode.AUTO),
            ),
            activation = ActivationSettings(
                autoStartInVehicle = prefs[AUTO_START_KEY] ?: false,
                pauseWhenScreenOff = prefs[PAUSE_SCREEN_OFF_KEY] ?: true,
            ),
        )
    }

    suspend fun current(context: Context): AppSettings = settingsFlow(context).first()

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.settingsDataStore.edit { it[THEME_MODE_KEY] = mode.name }
        context.getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(THEME_MODE_PREF_KEY, mode.name).apply()
    }

    suspend fun setDynamicColor(context: Context, enabled: Boolean) = put(context, DYNAMIC_COLOR_KEY, enabled)
    suspend fun setColorTheme(context: Context, theme: ColorTheme) = put(context, COLOR_THEME_KEY, theme.name)
    suspend fun setThemeContrast(context: Context, contrast: ThemeContrast) = put(context, THEME_CONTRAST_KEY, contrast.name)
    suspend fun setPureDark(context: Context, enabled: Boolean) = put(context, PURE_DARK_KEY, enabled)
    suspend fun setAbsoluteDark(context: Context, enabled: Boolean) = put(context, ABSOLUTE_DARK_KEY, enabled)
    suspend fun setCueStyle(context: Context, style: CueStyle) = put(context, CUE_STYLE_KEY, style.name)
    suspend fun setCueColor(context: Context, color: CueColor) = put(context, CUE_COLOR_KEY, color.name)
    suspend fun setCueSize(context: Context, size: CueSize) = put(context, CUE_SIZE_KEY, size.name)
    suspend fun setCueDensity(context: Context, density: CueDensity) = put(context, CUE_DENSITY_KEY, density.name)
    suspend fun setCueOpacity(context: Context, opacity: Float) = put(context, CUE_OPACITY_KEY, opacity)
    suspend fun setCueSensitivity(context: Context, sensitivity: Float) = put(context, CUE_SENSITIVITY_KEY, sensitivity)
    suspend fun setCueSmoothing(context: Context, smoothing: Float) = put(context, CUE_SMOOTHING_KEY, smoothing)
    suspend fun setHideWhenStill(context: Context, enabled: Boolean) = put(context, HIDE_WHEN_STILL_KEY, enabled)
    suspend fun setSensorMode(context: Context, mode: SensorMode) = put(context, SENSOR_MODE_KEY, mode.name)
    suspend fun setAutoStartInVehicle(context: Context, enabled: Boolean) = put(context, AUTO_START_KEY, enabled)
    suspend fun setPauseWhenScreenOff(context: Context, enabled: Boolean) = put(context, PAUSE_SCREEN_OFF_KEY, enabled)

    private suspend fun <T> put(context: Context, key: Preferences.Key<T>, value: T) {
        context.settingsDataStore.edit { it[key] = value }
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
        this?.let { name -> runCatching { enumValueOf<T>(name) }.getOrNull() } ?: default
}
