package com.wwwescape.carmotionsicknessaid.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.Choreographer
import android.view.View
import com.wwwescape.carmotionsicknessaid.data.settings.CueSettings
import com.wwwescape.carmotionsicknessaid.motion.CueAnimator
import com.wwwescape.carmotionsicknessaid.motion.CueRenderer
import com.wwwescape.carmotionsicknessaid.motion.MotionEngine
import com.wwwescape.carmotionsicknessaid.motion.VehicleAccel

/** The full-screen, non-touchable overlay surface. Advances the [CueAnimator] once per display
 * frame and only invalidates when the cues have visibly moved, so a parked car costs almost
 * nothing to draw. */
@SuppressLint("ViewConstructor")
class CueOverlayView(
    context: Context,
    private val engine: MotionEngine,
) : View(context), Choreographer.FrameCallback {

    var settings: CueSettings = CueSettings()

    private val animator = CueAnimator()
    private val renderer = CueRenderer(resources.displayMetrics.density)
    private var lastFrameNs = 0L
    private var drawnX = Float.NaN
    private var drawnY = Float.NaN
    private var drawnVisibility = Float.NaN
    private var ticking = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startTicking()
    }

    override fun onDetachedFromWindow() {
        stopTicking()
        super.onDetachedFromWindow()
    }

    fun startTicking() {
        if (ticking) return
        ticking = true
        lastFrameNs = 0L
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun stopTicking() {
        ticking = false
        Choreographer.getInstance().removeFrameCallback(this)
        animator.reset()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!ticking) return
        val dt = if (lastFrameNs == 0L) 1f / 60f else (frameTimeNanos - lastFrameNs) / 1e9f
        lastFrameNs = frameTimeNanos
        animator.step(dt, engine.current.takeIf { visibility == VISIBLE } ?: VehicleAccel.ZERO, settings)
        if (drawnX.isNaN() || animator.movedSince(drawnX, drawnY, drawnVisibility)) invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    /** Forces a redraw after a settings change, even if the cues haven't moved. */
    fun refresh() {
        drawnX = Float.NaN
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas, width.toFloat(), height.toFloat(), animator, settings)
        drawnX = animator.offsetX
        drawnY = animator.offsetY
        drawnVisibility = animator.visibility
    }
}
