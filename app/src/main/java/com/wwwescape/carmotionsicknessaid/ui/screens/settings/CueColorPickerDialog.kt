package com.wwwescape.carmotionsicknessaid.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wwwescape.carmotionsicknessaid.R
import com.wwwescape.carmotionsicknessaid.data.settings.CueColor

/** Swatch grid for the cue color, mirroring [ThemePickerDialog]. Every swatch gets an outline so
 * white and black stay visible on either theme. */
@Composable
fun CueColorPickerDialog(selected: CueColor, onSelect: (CueColor) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.setting_cue_color)) },
        text = {
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(240.dp)) {
                items(CueColor.entries) { color ->
                    val swatch = color.argb?.let { Color(it) } ?: Color.White
                    Column(
                        modifier = Modifier
                            .padding(6.dp)
                            .clickable { onSelect(color); onDismiss() },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(swatch, CircleShape)
                                .border(
                                    width = if (color == CueColor.ADAPTIVE) 4.dp else 1.dp,
                                    color = if (color == CueColor.ADAPTIVE) Color(0xFF202124) else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (color == selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = if (swatch.luminance() > 0.5f) Color.Black else Color.White,
                                )
                            }
                        }
                        Text(
                            text = color.label(),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
}
