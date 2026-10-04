package com.wwwescape.carmotionsicknessaid.motion

import com.wwwescape.carmotionsicknessaid.data.settings.CueSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionMathTest {

    private val portrait = MotionMath.screenAxes(0).first
    private val eps = 1e-3f

    @Test
    fun `flat device - screen up is forward`() {
        val up = Vec3(0f, 0f, 9.81f)
        val a = MotionMath.vehicleAccel(Vec3(0f, 2f, 0f), up, portrait)
        assertEquals(0f, a.lateral, eps)
        assertEquals(2f, a.longitudinal, eps)
    }

    @Test
    fun `upright device - out of the back is forward`() {
        // Held upright facing the user: device +y is up, so forward is -z.
        val up = Vec3(0f, 9.81f, 0f)
        val a = MotionMath.vehicleAccel(Vec3(1.5f, 0f, -3f), up, portrait)
        assertEquals(1.5f, a.lateral, eps)
        assertEquals(3f, a.longitudinal, eps)
    }

    @Test
    fun `tilted device ignores the vertical component`() {
        // 45 degree recline; a purely vertical bump shouldn't register as horizontal motion.
        val up = Vec3(0f, 6.94f, 6.94f)
        val bump = up.normalized()!! * 4f
        val a = MotionMath.vehicleAccel(bump, up, portrait)
        assertEquals(0f, a.lateral, eps)
        assertEquals(0f, a.longitudinal, eps)
    }

    @Test
    fun `landscape maps device y to screen right`() {
        val (right, upAxis) = MotionMath.screenAxes(1)
        assertEquals(Vec3(0f, 1f, 0f), right)
        assertEquals(Vec3(-1f, 0f, 0f), upAxis)
        val a = MotionMath.vehicleAccel(Vec3(0f, 2f, 0f), Vec3(0f, 0f, 9.81f), right)
        assertEquals(2f, a.lateral, eps)
    }

    @Test
    fun `no frame when screen right points straight up`() {
        assertNull(MotionMath.horizontalFrame(Vec3(9.81f, 0f, 0f), portrait))
    }

    @Test
    fun `left turn from gyroscope pushes right`() {
        // Positive yaw (counterclockwise seen from above) is a left turn.
        val a = MotionMath.gyroAccel(Vec3(0f, 0f, 0.3f), Vec3(0f, 0f, 9.81f), portrait)
        assertTrue(a.lateral < 0f) // centripetal acceleration points left
    }

    @Test
    fun `animator moves cues opposite to acceleration`() {
        val animator = CueAnimator()
        repeat(120) { animator.step(1f / 60f, VehicleAccel(lateral = 0f, longitudinal = 2f), CueSettings()) }
        assertTrue("speeding up pushes cues down", animator.offsetY > 0.3f)
        repeat(240) { animator.step(1f / 60f, VehicleAccel(lateral = -2f, longitudinal = 0f), CueSettings()) }
        assertTrue("turning left pushes cues right", animator.offsetX > 0.3f)
        assertTrue(animator.offsetY < 0.05f)
    }

    @Test
    fun `hide when still fades cues out`() {
        val animator = CueAnimator()
        val settings = CueSettings(hideWhenStill = true)
        repeat(600) { animator.step(1f / 60f, VehicleAccel.ZERO, settings) }
        assertTrue(animator.visibility < 0.05f)
        repeat(60) { animator.step(1f / 60f, VehicleAccel(0f, 3f), settings) }
        assertTrue(animator.visibility > 0.5f)
    }
}
