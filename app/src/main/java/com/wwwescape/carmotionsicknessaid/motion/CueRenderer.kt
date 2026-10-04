package com.wwwescape.carmotionsicknessaid.motion

import android.graphics.Canvas
import android.graphics.Paint
import com.wwwescape.carmotionsicknessaid.data.settings.CueColor
import com.wwwescape.carmotionsicknessaid.data.settings.CueSettings
import com.wwwescape.carmotionsicknessaid.data.settings.CueStyle
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Draws the motion cues onto a plain [Canvas], so the system overlay (a View) and the in-app
 * preview (Compose, via `nativeCanvas`) render identically.
 */
class CueRenderer(private val density: Float) {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    /**
     * @param alpha overall opacity multiplier (0..1) on top of [CueAnimator.visibility].
     */
    fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        animator: CueAnimator,
        settings: CueSettings,
        alpha: Float = 1f,
    ) {
        val a = (alpha * animator.visibility).coerceIn(0f, 1f)
        if (a <= 0.01f || width <= 0f || height <= 0f) return

        val radius = settings.size.radiusDp * density
        val maxShift = min(width, height) * MAX_SHIFT_FRACTION
        val dx = animator.offsetX * maxShift
        val dy = animator.offsetY * maxShift
        preparePaints(settings.color, radius, a)

        when (settings.style) {
            CueStyle.EDGE_DOTS -> drawEdgeDots(canvas, width, height, dx, dy, radius, settings.density.dotsPerEdge, allEdges = false)
            CueStyle.FRAME_DOTS -> drawEdgeDots(canvas, width, height, dx, dy, radius, settings.density.dotsPerEdge, allEdges = true)
            CueStyle.DOT_GRID -> drawGrid(canvas, width, height, dx, dy, radius, settings.density.gridSpacingDp * density)
            CueStyle.HORIZON -> drawHorizon(canvas, width, height, animator.offsetX, dy, radius)
        }
    }

    private fun preparePaints(color: CueColor, radius: Float, alpha: Float) {
        val alphaInt = (alpha * 255).roundToInt()
        val argb = color.argb?.toInt() ?: ADAPTIVE_FILL
        fill.color = argb
        fill.alpha = alphaInt
        line.color = argb
        line.alpha = alphaInt
        line.strokeWidth = radius * 1.2f
        // The adaptive color is white with a dark ring, so it reads over light *and* dark content.
        halo.color = ADAPTIVE_HALO
        halo.alpha = if (color == CueColor.ADAPTIVE) (alphaInt * 0.7f).roundToInt() else 0
        halo.strokeWidth = max(1f, radius * 0.3f)
    }

    private fun dot(canvas: Canvas, x: Float, y: Float, r: Float) {
        canvas.drawCircle(x, y, r, fill)
        if (halo.alpha > 0) canvas.drawCircle(x, y, r, halo)
    }

    private fun drawEdgeDots(
        canvas: Canvas,
        w: Float,
        h: Float,
        dx: Float,
        dy: Float,
        r: Float,
        perLongEdge: Int,
        allEdges: Boolean,
    ) {
        val inset = max(EDGE_INSET_DP * density, r * 2.5f)
        val spacing = max(w, h) / perLongEdge

        // Left and right columns.
        val rows = max(2, (h / spacing).roundToInt())
        val rowStep = h / rows
        for (i in 0 until rows) {
            val y = wrap((i + 0.5f) * rowStep + dy, h)
            dot(canvas, inset + dx, y, r)
            dot(canvas, w - inset + dx, y, r)
        }
        if (!allEdges) return

        // Top and bottom rows, skipping the corners the columns already cover.
        val cols = max(2, (w / spacing).roundToInt())
        val colStep = (w - 2 * inset) / cols
        for (i in 1 until cols) {
            val x = wrap(inset + i * colStep + dx, w)
            dot(canvas, x, inset + dy, r)
            dot(canvas, x, h - inset + dy, r)
        }
    }

    private fun drawGrid(canvas: Canvas, w: Float, h: Float, dx: Float, dy: Float, r: Float, spacing: Float) {
        val ox = ((dx % spacing) + spacing) % spacing
        val oy = ((dy % spacing) + spacing) % spacing
        var y = oy - spacing + spacing / 2
        while (y < h + spacing) {
            var x = ox - spacing + spacing / 2
            while (x < w + spacing) {
                dot(canvas, x, y, r)
                x += spacing
            }
            y += spacing
        }
    }

    private fun drawHorizon(canvas: Canvas, w: Float, h: Float, tilt: Float, dy: Float, r: Float) {
        // A left turn pushes cues right (positive tilt), so the line banks the way the body leans.
        val angle = Math.toRadians((tilt * MAX_TILT_DEG).toDouble())
        val cx = w / 2
        val cy = h / 2 + dy
        val half = w * 0.42f
        val ex = (cos(angle) * half).toFloat()
        val ey = (sin(angle) * half).toFloat()
        canvas.drawLine(cx - ex, cy - ey, cx + ex, cy + ey, line)
        // End markers and a center mark make the tilt readable even at low opacity.
        dot(canvas, cx - ex, cy - ey, r * 1.4f)
        dot(canvas, cx + ex, cy + ey, r * 1.4f)
        dot(canvas, cx, cy, r)
    }

    /** Keeps dots that drift past an edge re-entering from the opposite edge. */
    private fun wrap(v: Float, size: Float): Float = ((v % size) + size) % size

    companion object {
        /** Largest cue shift, as a fraction of the screen's shorter side. */
        const val MAX_SHIFT_FRACTION = 0.12f
        const val MAX_TILT_DEG = 22f
        const val EDGE_INSET_DP = 18f
        private const val ADAPTIVE_FILL = 0xFFFFFFFF.toInt()
        private const val ADAPTIVE_HALO = 0xFF202124.toInt()
    }
}
