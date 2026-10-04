package com.wwwescape.carmotionsicknessaid.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wwwescape.carmotionsicknessaid.R
import com.wwwescape.carmotionsicknessaid.motion.SensorAvailability
import com.wwwescape.carmotionsicknessaid.motion.VehicleAccel
import com.wwwescape.carmotionsicknessaid.overlay.CueOverlayService
import com.wwwescape.carmotionsicknessaid.overlay.CueStatus
import com.wwwescape.carmotionsicknessaid.ui.components.EmptyStateNotice
import com.wwwescape.carmotionsicknessaid.ui.components.RowDivider
import com.wwwescape.carmotionsicknessaid.ui.components.SectionLabel
import com.wwwescape.carmotionsicknessaid.ui.components.SettingsGroupCard
import com.wwwescape.carmotionsicknessaid.ui.theme.PillShape

/** Width at which Home switches to a side-by-side preview + controls layout (tablets, foldables
 * and landscape). */
private val TwoPaneMinWidth = 720.dp

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Permissions are granted in system screens, so re-check whenever we come back.
    var canDrawOverlays by remember { mutableStateOf(CueOverlayService.canDrawOverlays(context)) }
    var notificationsAllowed by remember { mutableStateOf(notificationsAllowed(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        canDrawOverlays = CueOverlayService.canDrawOverlays(context)
        notificationsAllowed = notificationsAllowed(context)
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsAllowed = it
    }
    var accel by remember { mutableStateOf(VehicleAccel.ZERO) }

    val preview: @Composable (Modifier) -> Unit = { previewModifier ->
        PreviewCard(
            modifier = previewModifier,
            content = {
                CuePreview(
                    settings = settings.cues,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onAccel = { accel = it },
                    modifier = Modifier.fillMaxSize(),
                )
            },
            accel = accel,
        )
    }
    val controls: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!viewModel.sensors.canRun) {
                EmptyStateNotice(
                    title = stringResource(R.string.home_no_sensors_title),
                    body = stringResource(R.string.home_no_sensors_body),
                    icon = Icons.Rounded.RemoveCircleOutline,
                )
            }
            if (!canDrawOverlays) {
                PermissionCard(
                    icon = Icons.Rounded.Layers,
                    title = stringResource(R.string.permission_overlay_title),
                    body = stringResource(R.string.permission_overlay_body),
                    action = stringResource(R.string.action_grant),
                    onClick = { openOverlaySettings(context) },
                )
            }
            if (!notificationsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionCard(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.permission_notifications_title),
                    body = stringResource(R.string.permission_notifications_body),
                    action = stringResource(R.string.action_allow),
                    onClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                )
            }

            StatusLine(status)

            if (status == CueStatus.STOPPED) {
                Button(
                    onClick = viewModel::start,
                    enabled = canDrawOverlays && viewModel.sensors.canRun,
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Text(text = stringResource(R.string.action_start_cues), modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                Button(
                    onClick = viewModel::stop,
                    shape = PillShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Icon(Icons.Rounded.Stop, contentDescription = null)
                    Text(text = stringResource(R.string.action_stop_cues), modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(
                    onClick = viewModel::togglePause,
                    shape = PillShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    val paused = status == CueStatus.PAUSED
                    Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null)
                    Text(
                        text = stringResource(if (paused) R.string.action_resume else R.string.action_pause),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            SectionLabel(stringResource(R.string.section_sensors))
            SensorsCard(viewModel.sensors)

            SectionLabel(stringResource(R.string.section_how_it_works))
            SettingsGroupCard {
                Text(
                    text = stringResource(R.string.how_it_works_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= TwoPaneMinWidth) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                preview(Modifier.weight(1.2f).fillMaxSize())
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Tagline()
                    controls()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Tagline()
                preview(Modifier.fillMaxWidth().aspectRatio(4f / 3f))
                controls()
            }
        }
    }
}

@Composable
private fun Tagline() {
    Text(
        text = stringResource(R.string.home_hero_tagline),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PreviewCard(modifier: Modifier, content: @Composable () -> Unit, accel: VehicleAccel) {
    val locale = LocalConfiguration.current.locales[0]
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge),
            ) { content() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.home_preview_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.home_preview_readout,
                        String.format(locale, "%+.1f", accel.lateral),
                        String.format(locale, "%+.1f", accel.longitudinal),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusLine(status: CueStatus) {
    Text(
        text = stringResource(
            when (status) {
                CueStatus.STOPPED -> R.string.status_stopped
                CueStatus.RUNNING -> R.string.status_running
                CueStatus.PAUSED -> R.string.status_paused
            },
        ),
        style = MaterialTheme.typography.titleMedium,
        color = if (status == CueStatus.RUNNING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun PermissionCard(icon: ImageVector, title: String, body: String, action: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 8.dp),
            )
            FilledTonalButton(
                onClick = onClick,
                shape = PillShape,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 12.dp),
            ) { Text(action) }
        }
    }
}

@Composable
private fun SensorsCard(sensors: SensorAvailability) {
    SettingsGroupCard {
        val rows = listOf(
            R.string.sensor_accelerometer to sensors.accelerometer,
            R.string.sensor_gyroscope to sensors.gyroscope,
            R.string.sensor_linear_acceleration to sensors.linearAcceleration,
            R.string.sensor_gravity to sensors.gravity,
        )
        rows.forEachIndexed { index, (label, present) ->
            if (index > 0) RowDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (present) Icons.Rounded.CheckCircle else Icons.Rounded.RemoveCircleOutline,
                    contentDescription = stringResource(if (present) R.string.sensor_available else R.string.sensor_missing),
                    tint = if (present) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun notificationsAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun openOverlaySettings(context: Context) {
    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
    runCatching { context.startActivity(intent) }
}
