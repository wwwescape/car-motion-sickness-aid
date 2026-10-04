package com.wwwescape.carmotionsicknessaid.data.settings

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** [seedHue] is a 0-359 HSV hue driving [com.wwwescape.carmotionsicknessaid.ui.theme.generateColorScheme].
 * DEFAULT (null) keeps the app's own hand-authored static palette (see Color.kt) instead of a
 * generated one. Ignored entirely when dynamic color is on, which takes precedence. */
enum class ColorTheme(val seedHue: Float?) {
    DEFAULT(null),
    OCEAN(205f),
    FOREST(140f),
    SUNSET(25f),
    GRAPE(280f),
    ROSE(340f),
    SLATE(220f),
    GOLD(45f),
    TEAL(175f),
    PLUM(300f),
    MOSS(95f),
    CORAL(12f),
    INDIGO(245f),
    MUSTARD(55f),
    CRIMSON(355f),
    MINT(160f),
    PERIWINKLE(230f),
}

/** Only affects (or only makes sense for) dark theme. */
enum class ThemeContrast { STANDARD, MEDIUM, HIGH }

/** How the motion cues are drawn over the screen. */
enum class CueStyle {
    /** Columns of dots along the left and right edges — keeps the middle of the screen clear. */
    EDGE_DOTS,

    /** Dots along all four edges. */
    FRAME_DOTS,

    /** An even grid of dots covering the whole screen. */
    DOT_GRID,

    /** A single horizon line that tilts with turns and rises/falls with braking/accelerating. */
    HORIZON,
}

/** ARGB colors for the cues. [ADAPTIVE] picks black or white per frame depending on the theme. */
enum class CueColor(val argb: Long?) {
    ADAPTIVE(null),
    BLUE(0xFF2A7FFF),
    WHITE(0xFFFFFFFF),
    BLACK(0xFF000000),
    GREY(0xFF8A8A8A),
    RED(0xFFE53935),
    ORANGE(0xFFFB8C00),
    YELLOW(0xFFFDD835),
    GREEN(0xFF43A047),
    PURPLE(0xFF8E24AA),
}

/** Dot radius in dp. */
enum class CueSize(val radiusDp: Float) {
    SMALL(4f),
    MEDIUM(6f),
    LARGE(9f),
    EXTRA_LARGE(13f),
}

/** How many dots are drawn — per edge for the edge styles, grid spacing for [CueStyle.DOT_GRID]. */
enum class CueDensity(val dotsPerEdge: Int, val gridSpacingDp: Float) {
    LOW(5, 120f),
    MEDIUM(8, 88f),
    HIGH(12, 64f),
}

/** Which sensors feed the cues. [AUTO] fuses accelerometer and gyroscope when both exist. */
enum class SensorMode { AUTO, ACCELEROMETER, GYROSCOPE }

data class CueSettings(
    val style: CueStyle = CueStyle.EDGE_DOTS,
    val color: CueColor = CueColor.ADAPTIVE,
    val size: CueSize = CueSize.MEDIUM,
    val density: CueDensity = CueDensity.MEDIUM,
    /** 0.2..0.8 — capped at 0.8 so touches still pass through the overlay on Android 12+. */
    val opacity: Float = 0.6f,
    /** 0..1 slider, mapped to a 0.25x..2.5x gain in [com.wwwescape.carmotionsicknessaid.motion.CueAnimator]. */
    val sensitivity: Float = 0.5f,
    /** 0..1 slider — higher is smoother but lags a little more. */
    val smoothing: Float = 0.5f,
    /** Fade the cues out while the vehicle isn't accelerating, braking or turning. */
    val hideWhenStill: Boolean = false,
    val sensorMode: SensorMode = SensorMode.AUTO,
)

data class ActivationSettings(
    /** Start the overlay automatically when Activity Recognition reports the user is in a vehicle,
     * and stop it again when they leave it. */
    val autoStartInVehicle: Boolean = false,
    /** Stop listening to the sensors while the screen is off. */
    val pauseWhenScreenOff: Boolean = true,
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val colorTheme: ColorTheme = ColorTheme.DEFAULT,
    val themeContrast: ThemeContrast = ThemeContrast.STANDARD,
    val pureDark: Boolean = false,
    val absoluteDark: Boolean = false,
    val cues: CueSettings = CueSettings(),
    val activation: ActivationSettings = ActivationSettings(),
)
