package com.wwwescape.carmotionsicknessaid.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wwwescape.carmotionsicknessaid.R
import com.wwwescape.carmotionsicknessaid.data.settings.ColorTheme
import com.wwwescape.carmotionsicknessaid.data.settings.CueColor
import com.wwwescape.carmotionsicknessaid.data.settings.CueDensity
import com.wwwescape.carmotionsicknessaid.data.settings.CueSize
import com.wwwescape.carmotionsicknessaid.data.settings.CueStyle
import com.wwwescape.carmotionsicknessaid.data.settings.SensorMode
import com.wwwescape.carmotionsicknessaid.data.settings.ThemeContrast
import com.wwwescape.carmotionsicknessaid.data.settings.ThemeMode

@Composable
fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.LIGHT -> R.string.theme_mode_light
        ThemeMode.DARK -> R.string.theme_mode_dark
        ThemeMode.SYSTEM -> R.string.theme_mode_system
    },
)

@Composable
fun ColorTheme.label(): String = stringResource(
    when (this) {
        ColorTheme.DEFAULT -> R.string.color_theme_default
        ColorTheme.OCEAN -> R.string.color_theme_ocean
        ColorTheme.FOREST -> R.string.color_theme_forest
        ColorTheme.SUNSET -> R.string.color_theme_sunset
        ColorTheme.GRAPE -> R.string.color_theme_grape
        ColorTheme.ROSE -> R.string.color_theme_rose
        ColorTheme.SLATE -> R.string.color_theme_slate
        ColorTheme.GOLD -> R.string.color_theme_gold
        ColorTheme.TEAL -> R.string.color_theme_teal
        ColorTheme.PLUM -> R.string.color_theme_plum
        ColorTheme.MOSS -> R.string.color_theme_moss
        ColorTheme.CORAL -> R.string.color_theme_coral
        ColorTheme.INDIGO -> R.string.color_theme_indigo
        ColorTheme.MUSTARD -> R.string.color_theme_mustard
        ColorTheme.CRIMSON -> R.string.color_theme_crimson
        ColorTheme.MINT -> R.string.color_theme_mint
        ColorTheme.PERIWINKLE -> R.string.color_theme_periwinkle
    },
)

@Composable
fun ThemeContrast.label(): String = stringResource(
    when (this) {
        ThemeContrast.STANDARD -> R.string.contrast_standard
        ThemeContrast.MEDIUM -> R.string.contrast_medium
        ThemeContrast.HIGH -> R.string.contrast_high
    },
)

@Composable
fun CueStyle.label(): String = stringResource(
    when (this) {
        CueStyle.EDGE_DOTS -> R.string.cue_style_edge_dots
        CueStyle.FRAME_DOTS -> R.string.cue_style_frame_dots
        CueStyle.DOT_GRID -> R.string.cue_style_dot_grid
        CueStyle.HORIZON -> R.string.cue_style_horizon
    },
)

@Composable
fun CueColor.label(): String = stringResource(
    when (this) {
        CueColor.ADAPTIVE -> R.string.cue_color_adaptive
        CueColor.BLUE -> R.string.cue_color_blue
        CueColor.WHITE -> R.string.cue_color_white
        CueColor.BLACK -> R.string.cue_color_black
        CueColor.GREY -> R.string.cue_color_grey
        CueColor.RED -> R.string.cue_color_red
        CueColor.ORANGE -> R.string.cue_color_orange
        CueColor.YELLOW -> R.string.cue_color_yellow
        CueColor.GREEN -> R.string.cue_color_green
        CueColor.PURPLE -> R.string.cue_color_purple
    },
)

@Composable
fun CueSize.label(): String = stringResource(
    when (this) {
        CueSize.SMALL -> R.string.size_small
        CueSize.MEDIUM -> R.string.size_medium
        CueSize.LARGE -> R.string.size_large
        CueSize.EXTRA_LARGE -> R.string.size_extra_large
    },
)

@Composable
fun CueDensity.label(): String = stringResource(
    when (this) {
        CueDensity.LOW -> R.string.density_low
        CueDensity.MEDIUM -> R.string.density_medium
        CueDensity.HIGH -> R.string.density_high
    },
)

@Composable
fun SensorMode.label(): String = stringResource(
    when (this) {
        SensorMode.AUTO -> R.string.sensor_mode_auto
        SensorMode.ACCELEROMETER -> R.string.sensor_mode_accelerometer
        SensorMode.GYROSCOPE -> R.string.sensor_mode_gyroscope
    },
)
