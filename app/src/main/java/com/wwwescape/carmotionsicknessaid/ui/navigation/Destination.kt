package com.wwwescape.carmotionsicknessaid.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.wwwescape.carmotionsicknessaid.R

/** Every screen reachable from the app's own top-level navigation. */
enum class Destination(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector,
) {
    Home("home", R.string.app_name, Icons.Rounded.Home),
    Settings("settings", R.string.title_settings, Icons.Rounded.Settings);

    companion object {
        fun fromRoute(route: String?): Destination = entries.find { it.route == route } ?: Home
    }
}
