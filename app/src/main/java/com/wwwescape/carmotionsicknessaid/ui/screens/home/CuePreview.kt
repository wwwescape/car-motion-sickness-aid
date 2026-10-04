package com.wwwescape.carmotionsicknessaid.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.wwwescape.carmotionsicknessaid.data.settings.CueSettings
import com.wwwescape.carmotionsicknessaid.motion.CueAnimator
import com.wwwescape.carmotionsicknessaid.motion.CueRenderer
import com.wwwescape.carmotionsicknessaid.motion.MotionEngine
import com.wwwescape.carmotionsicknessaid.motion.VehicleAccel

/**
 * A live, in-app rendering of the cues over mock "content" lines, driven by the real sensors so
 * moving or tilting the device shows how the overlay will respond. Sensors only run while the
 * screen is resumed.
 */
@Composable
fun CuePreview(
    settings: CueSettings,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onAccel: (VehicleAccel) -> Unit = {},
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current.density
    val engine = remember { MotionEngine(context.applicationContext) { view.display?.rotation ?: 0 } }
    val animator = remember { CueAnimator() }
    val renderer = remember(density) { CueRenderer(density) }
    var frame by remember { mutableLongStateOf(0L) }
    val currentSettings by rememberUpdatedState(settings)
    val currentOnAccel by rememberUpdatedState(onAccel)

    LifecycleResumeEffect(engine) {
        engine.start()
        onPauseOrDispose { engine.stop() }
    }
    LaunchedEffect(settings.sensorMode) { engine.mode = settings.sensorMode }
    LaunchedEffect(engine) {
        var last = 0L
        var reported = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 1f / 60f else (now - last) / 1e9f
                last = now
                animator.step(dt, engine.current, currentSettings)
                if (now - reported > 250_000_000L) {
                    reported = now
                    currentOnAccel(engine.current)
                }
                frame = now
            }
        }
    }

    Canvas(modifier = modifier) {
        if (frame < 0L) return@Canvas // reading the frame state redraws this every frame
        // Placeholder "text" lines stand in for whatever app the overlay will sit on top of.
        val lineHeight = 10.dp(density)
        val gap = 14.dp(density)
        var y = 28.dp(density)
        var i = 0
        while (y < size.height - 20.dp(density)) {
            val widthFraction = if (i % 4 == 3) 0.6f else 1f
            drawRoundRect(
                color = contentColor.copy(alpha = 0.18f),
                topLeft = Offset(size.width * 0.1f, y),
                size = Size(size.width * 0.8f * widthFraction, lineHeight),
                cornerRadius = CornerRadius(lineHeight / 2),
            )
            y += lineHeight + gap
            i++
        }
        drawIntoCanvas { canvas ->
            renderer.draw(canvas.nativeCanvas, size.width, size.height, animator, settings, alpha = settings.opacity)
        }
    }
}

private fun Int.dp(density: Float) = this * density
