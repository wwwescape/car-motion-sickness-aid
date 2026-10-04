package com.wwwescape.carmotionsicknessaid.ui.screens.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoSizeSelectSmall
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.carmotionsicknessaid.R
import com.wwwescape.carmotionsicknessaid.auto.VehicleDetection
import com.wwwescape.carmotionsicknessaid.data.settings.CueDensity
import com.wwwescape.carmotionsicknessaid.data.settings.CueSize
import com.wwwescape.carmotionsicknessaid.data.settings.CueStyle
import com.wwwescape.carmotionsicknessaid.data.settings.SensorMode
import com.wwwescape.carmotionsicknessaid.data.settings.ThemeContrast
import com.wwwescape.carmotionsicknessaid.data.settings.ThemeMode
import com.wwwescape.carmotionsicknessaid.tile.CuesTileService
import com.wwwescape.carmotionsicknessaid.ui.components.NavigationRow
import com.wwwescape.carmotionsicknessaid.ui.components.ReadableWidth
import com.wwwescape.carmotionsicknessaid.ui.components.RowDivider
import com.wwwescape.carmotionsicknessaid.ui.components.SectionLabel
import com.wwwescape.carmotionsicknessaid.ui.components.SettingsGroupCard
import com.wwwescape.carmotionsicknessaid.ui.components.SliderRow
import com.wwwescape.carmotionsicknessaid.ui.components.ToggleRow
import com.wwwescape.carmotionsicknessaid.util.openUrl

private const val PRIVACY_POLICY_URL = "https://www.ericppereira.co.in/apps/car-motion-sickness-aid/privacy-policy.html"

private enum class SettingsDialog { THEME, COLOR_THEME, CONTRAST, CUE_STYLE, CUE_COLOR, CUE_SIZE, CUE_DENSITY, SENSOR_MODE }

@Composable
fun SettingsScreen(
    onNavigateToLicenses: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "—"
    }
    val isDarkTheme = when (settings.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> if (granted) viewModel.setAutoStartInVehicle(true) },
    )
    val cues = settings.cues
    val sensors = viewModel.sensors

    ReadableWidth(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionLabel(stringResource(R.string.section_cues))
            SettingsGroupCard {
                NavigationRow(
                    icon = Icons.Rounded.BlurOn,
                    title = stringResource(R.string.setting_cue_style),
                    subtitle = cues.style.label(),
                    onClick = { dialog = SettingsDialog.CUE_STYLE },
                )
                RowDivider()
                NavigationRow(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.setting_cue_color),
                    subtitle = cues.color.label(),
                    onClick = { dialog = SettingsDialog.CUE_COLOR },
                )
                RowDivider()
                NavigationRow(
                    icon = Icons.Rounded.PhotoSizeSelectSmall,
                    title = stringResource(R.string.setting_cue_size),
                    subtitle = cues.size.label(),
                    onClick = { dialog = SettingsDialog.CUE_SIZE },
                )
                if (cues.style != CueStyle.HORIZON) {
                    RowDivider()
                    NavigationRow(
                        icon = Icons.Rounded.GridOn,
                        title = stringResource(R.string.setting_cue_density),
                        subtitle = cues.density.label(),
                        onClick = { dialog = SettingsDialog.CUE_DENSITY },
                    )
                }
                RowDivider()
                SliderRow(
                    icon = Icons.Rounded.Opacity,
                    title = stringResource(R.string.setting_cue_opacity),
                    value = cues.opacity,
                    valueRange = 0.2f..0.8f,
                    onValueChange = viewModel::setCueOpacity,
                )
                RowDivider()
                ToggleRow(
                    icon = Icons.Rounded.VisibilityOff,
                    title = stringResource(R.string.setting_hide_when_still),
                    subtitle = stringResource(R.string.setting_hide_when_still_subtitle),
                    checked = cues.hideWhenStill,
                    onCheckedChange = viewModel::setHideWhenStill,
                )
            }

            SectionLabel(stringResource(R.string.section_motion))
            SettingsGroupCard {
                SliderRow(
                    icon = Icons.Rounded.Speed,
                    title = stringResource(R.string.setting_sensitivity),
                    value = cues.sensitivity,
                    onValueChange = viewModel::setCueSensitivity,
                )
                RowDivider()
                SliderRow(
                    icon = Icons.Rounded.Animation,
                    title = stringResource(R.string.setting_smoothing),
                    value = cues.smoothing,
                    onValueChange = viewModel::setCueSmoothing,
                )
                RowDivider()
                NavigationRow(
                    icon = Icons.Rounded.Sensors,
                    title = stringResource(R.string.setting_sensor_mode),
                    subtitle = cues.sensorMode.label(),
                    onClick = { dialog = SettingsDialog.SENSOR_MODE },
                )
            }

            SectionLabel(stringResource(R.string.section_activation))
            SettingsGroupCard {
                ToggleRow(
                    icon = Icons.Rounded.DirectionsCar,
                    title = stringResource(R.string.setting_auto_start),
                    subtitle = stringResource(R.string.setting_auto_start_subtitle),
                    checked = settings.activation.autoStartInVehicle,
                    onCheckedChange = { enabled ->
                        when {
                            !enabled -> viewModel.setAutoStartInVehicle(false)
                            VehicleDetection.hasPermission(context) -> viewModel.setAutoStartInVehicle(true)
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                                activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                            // Pre-Q the Play services permission is granted at install time.
                            else -> viewModel.setAutoStartInVehicle(true)
                        }
                    },
                )
                RowDivider()
                ToggleRow(
                    icon = Icons.Rounded.ScreenLockPortrait,
                    title = stringResource(R.string.setting_pause_screen_off),
                    subtitle = stringResource(R.string.setting_pause_screen_off_subtitle),
                    checked = settings.activation.pauseWhenScreenOff,
                    onCheckedChange = viewModel::setPauseWhenScreenOff,
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    RowDivider()
                    NavigationRow(
                        icon = Icons.Rounded.Widgets,
                        title = stringResource(R.string.setting_add_tile),
                        subtitle = stringResource(R.string.setting_add_tile_subtitle),
                        onClick = { requestAddTile(context) },
                    )
                }
            }

            SectionLabel(stringResource(R.string.section_appearance))
            SettingsGroupCard {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ToggleRow(
                        icon = Icons.Rounded.Palette,
                        title = stringResource(R.string.setting_dynamic_color),
                        subtitle = stringResource(R.string.setting_dynamic_color_subtitle),
                        checked = settings.useDynamicColor,
                        onCheckedChange = viewModel::setDynamicColor,
                    )
                    RowDivider()
                }
                NavigationRow(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.setting_color_theme),
                    subtitle = settings.colorTheme.label(),
                    onClick = { dialog = SettingsDialog.COLOR_THEME },
                )
                RowDivider()
                NavigationRow(
                    icon = Icons.Rounded.DarkMode,
                    title = stringResource(R.string.setting_theme),
                    subtitle = settings.themeMode.label(),
                    onClick = { dialog = SettingsDialog.THEME },
                )
                // Contrast/pure dark/absolute dark only affect dark theme — hidden entirely when the
                // effective theme (accounting for "System default" resolving to light) is light.
                if (isDarkTheme) {
                    RowDivider()
                    NavigationRow(
                        icon = Icons.Rounded.Contrast,
                        title = stringResource(R.string.setting_theme_contrast),
                        subtitle = settings.themeContrast.label(),
                        onClick = { dialog = SettingsDialog.CONTRAST },
                    )
                    RowDivider()
                    ToggleRow(
                        icon = Icons.Rounded.Brightness6,
                        title = stringResource(R.string.setting_pure_dark),
                        subtitle = stringResource(R.string.setting_pure_dark_subtitle),
                        checked = settings.pureDark,
                        onCheckedChange = viewModel::setPureDark,
                    )
                    RowDivider()
                    ToggleRow(
                        icon = Icons.Rounded.NightsStay,
                        title = stringResource(R.string.setting_absolute_dark),
                        subtitle = stringResource(R.string.setting_absolute_dark_subtitle),
                        checked = settings.absoluteDark,
                        onCheckedChange = viewModel::setAbsoluteDark,
                    )
                }
            }

            SectionLabel(stringResource(R.string.section_about))
            SettingsGroupCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_app_icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp)),
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        text = stringResource(R.string.about_version, versionName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RowDivider()
                Text(
                    text = stringResource(R.string.privacy_statement_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
                RowDivider()
                NavigationRow(
                    icon = Icons.Rounded.Shield,
                    title = stringResource(R.string.section_privacy_policy),
                    subtitle = null,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = { openUrl(context, PRIVACY_POLICY_URL) },
                )
                RowDivider()
                NavigationRow(
                    icon = Icons.Rounded.Code,
                    title = stringResource(R.string.section_open_source_licenses),
                    subtitle = stringResource(R.string.settings_row_licenses_subtitle),
                    onClick = onNavigateToLicenses,
                )
                RowDivider()
                NavigationRow(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.setting_sensors_detected),
                    subtitle = listOfNotNull(
                        stringResource(R.string.sensor_accelerometer).takeIf { sensors.accelerometer },
                        stringResource(R.string.sensor_gyroscope).takeIf { sensors.gyroscope },
                    ).joinToString(" · ").ifEmpty { stringResource(R.string.sensor_none) },
                    onClick = { dialog = SettingsDialog.SENSOR_MODE },
                )
            }
        }
    }

    val dismiss = { dialog = null }
    when (dialog) {
        SettingsDialog.THEME -> SettingsPickerDialog(
            title = stringResource(R.string.setting_theme),
            options = ThemeMode.entries,
            selected = settings.themeMode,
            label = { it.label() },
            onSelect = viewModel::setThemeMode,
            onDismiss = dismiss,
        )
        SettingsDialog.COLOR_THEME -> ThemePickerDialog(
            selected = settings.colorTheme,
            onSelect = viewModel::setColorTheme,
            onDismiss = dismiss,
        )
        SettingsDialog.CONTRAST -> SettingsPickerDialog(
            title = stringResource(R.string.setting_theme_contrast),
            options = ThemeContrast.entries,
            selected = settings.themeContrast,
            label = { it.label() },
            onSelect = viewModel::setThemeContrast,
            onDismiss = dismiss,
        )
        SettingsDialog.CUE_STYLE -> SettingsPickerDialog(
            title = stringResource(R.string.setting_cue_style),
            options = CueStyle.entries,
            selected = cues.style,
            label = { it.label() },
            onSelect = viewModel::setCueStyle,
            onDismiss = dismiss,
        )
        SettingsDialog.CUE_COLOR -> CueColorPickerDialog(
            selected = cues.color,
            onSelect = viewModel::setCueColor,
            onDismiss = dismiss,
        )
        SettingsDialog.CUE_SIZE -> SettingsPickerDialog(
            title = stringResource(R.string.setting_cue_size),
            options = CueSize.entries,
            selected = cues.size,
            label = { it.label() },
            onSelect = viewModel::setCueSize,
            onDismiss = dismiss,
        )
        SettingsDialog.CUE_DENSITY -> SettingsPickerDialog(
            title = stringResource(R.string.setting_cue_density),
            options = CueDensity.entries,
            selected = cues.density,
            label = { it.label() },
            onSelect = viewModel::setCueDensity,
            onDismiss = dismiss,
        )
        SettingsDialog.SENSOR_MODE -> SettingsPickerDialog(
            title = stringResource(R.string.setting_sensor_mode),
            options = SensorMode.entries.filter { mode ->
                when (mode) {
                    SensorMode.AUTO -> true
                    SensorMode.ACCELEROMETER -> sensors.accelerometer
                    SensorMode.GYROSCOPE -> sensors.gyroscope
                }
            },
            selected = cues.sensorMode,
            label = { it.label() },
            onSelect = viewModel::setSensorMode,
            onDismiss = dismiss,
        )
        null -> Unit
    }
}

/** Android 13+ can prompt the user to add our Quick Settings tile directly. */
@SuppressLint("NewApi")
private fun requestAddTile(context: Context) {
    val statusBar = context.getSystemService(StatusBarManager::class.java) ?: return
    runCatching {
        statusBar.requestAddTileService(
            ComponentName(context, CuesTileService::class.java),
            context.getString(R.string.tile_label),
            Icon.createWithResource(context, R.drawable.ic_stat_cues),
            context.mainExecutor,
        ) { }
    }
}
