package com.wwwescape.carmotionsicknessaid.motion

import com.wwwescape.carmotionsicknessaid.data.settings.CueSettings
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.tanh

/**
 * Per-frame cue state: turns the engine's vehicle acceleration into a smoothed on-screen
 * displacement. [offsetX]/[offsetY] are normalized to -1..1 (1 = the renderer's maximum shift),
 * in screen coordinates — +x right, +y down.
 *
 * Cues behave like objects that keep their inertia: speeding up pushes them back (down the
 * screen), braking throws them forward (up), and a left turn slides them right — the same
 * direction the passenger's body is pushed, so what the eyes see agrees with the inner ear.
 */
class CueAnimator {

    var offsetX = 0f
        private set
    var offsetY = 0f
        private set

    /** 0..1 — how visible the cues should be; only drops below 1 with "hide when still". */
    var visibility = 1f
        private set

    private var motionLevel = 0f

    fun step(dtSeconds: Float, accel: VehicleAccel, settings: CueSettings) {
        val dt = dtSeconds.coerceIn(0f, 0.1f)
        val gain = 0.25f + settings.sensitivity.coerceIn(0f, 1f) * 2.25f
        val tau = 0.05f + settings.smoothing.coerceIn(0f, 1f) * 0.45f

        // tanh keeps a hard pothole from flinging the cues off screen while staying linear for
        // everyday driving.
        val targetX = tanh(-accel.lateral * gain / FULL_SCALE_MPS2)
        val targetY = tanh(accel.longitudinal * gain / FULL_SCALE_MPS2)
        offsetX = MotionMath.lowPass(offsetX, targetX, dt, tau)
        offsetY = MotionMath.lowPass(offsetY, targetY, dt, tau)

        val magnitude = hypot(targetX, targetY)
        // Rises quickly, decays slowly, so cues don't flicker between bursts of motion.
        motionLevel = MotionMath.lowPass(motionLevel, magnitude, dt, if (magnitude > motionLevel) 0.15f else 1.5f)
        val targetVisibility = if (settings.hideWhenStill) smoothStep(STILL_LEVEL, MOVING_LEVEL, motionLevel) else 1f
        visibility = MotionMath.lowPass(visibility, targetVisibility, dt, 0.3f)
    }

    /** True if this frame would draw noticeably differently from [previousX]/[previousY]. */
    fun movedSince(previousX: Float, previousY: Float, previousVisibility: Float): Boolean =
        abs(offsetX - previousX) > 0.002f || abs(offsetY - previousY) > 0.002f ||
            abs(visibility - previousVisibility) > 0.01f

    fun reset() {
        offsetX = 0f
        offsetY = 0f
        motionLevel = 0f
    }

    private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    companion object {
        /** Reference acceleration (m/s², about firm braking); at default sensitivity it maps to
         * roughly 90% of the maximum shift. */
        const val FULL_SCALE_MPS2 = 3f

        private const val STILL_LEVEL = 0.04f
        private const val MOVING_LEVEL = 0.15f
    }
}
