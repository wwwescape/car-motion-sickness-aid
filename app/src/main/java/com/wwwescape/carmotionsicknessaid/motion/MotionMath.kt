package com.wwwescape.carmotionsicknessaid.motion

import kotlin.math.sqrt

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun length() = sqrt(this dot this)
    fun normalized(): Vec3? = length().let { if (it < 1e-4f) null else this * (1f / it) }

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
    }
}

/** Acceleration felt by the passenger in the vehicle's horizontal plane, in m/s².
 * [lateral] is positive to the right, [longitudinal] positive forward (speeding up). */
data class VehicleAccel(val lateral: Float, val longitudinal: Float) {
    companion object {
        val ZERO = VehicleAccel(0f, 0f)
    }
}

/**
 * Pure geometry for turning raw device-frame sensor readings into vehicle-frame motion, kept free
 * of Android types so it can be unit tested on the JVM.
 *
 * Device axes follow Android's sensor convention: x right, y up (toward the top of the screen in
 * the device's natural orientation), z out of the screen toward the viewer. The gravity sensor
 * reports a vector pointing *up*.
 */
object MotionMath {

    /** Assumed vehicle speed used to convert a yaw rate (rad/s) into lateral acceleration. */
    const val ASSUMED_SPEED_MPS = 8f

    /** Scale for turning a pitch rate (rad/s) into a longitudinal cue in gyroscope-only mode. */
    const val PITCH_RATE_GAIN = 4f

    /** Screen right and screen up expressed in device axes for a `Surface.ROTATION_*` value
     * (0, 1, 2, 3 for 0°, 90°, 180°, 270°). Matches `SensorManager.remapCoordinateSystem`. */
    fun screenAxes(rotation: Int): Pair<Vec3, Vec3> = when (rotation and 3) {
        1 -> Vec3(0f, 1f, 0f) to Vec3(-1f, 0f, 0f)
        2 -> Vec3(-1f, 0f, 0f) to Vec3(0f, -1f, 0f)
        3 -> Vec3(0f, -1f, 0f) to Vec3(1f, 0f, 0f)
        else -> Vec3(1f, 0f, 0f) to Vec3(0f, 1f, 0f)
    }

    /**
     * Horizontal "right" and "forward" unit vectors in device axes. Right is screen-right flattened
     * onto the horizontal plane; forward is perpendicular to it, pointing away from the viewer —
     * so it's screen-up when the device lies flat and out of the back when it's held upright.
     * Returns null when screen-right is (nearly) vertical and no sensible frame exists.
     */
    fun horizontalFrame(gravityUp: Vec3, screenRight: Vec3): Pair<Vec3, Vec3>? {
        val up = gravityUp.normalized() ?: return null
        val rightFlat = (screenRight - up * (screenRight dot up))
        if (rightFlat.length() < 0.25f) return null
        val right = rightFlat.normalized() ?: return null
        val forward = (up cross right).normalized() ?: return null
        return right to forward
    }

    /** Projects gravity-free (linear) acceleration onto the vehicle's horizontal plane. */
    fun vehicleAccel(linear: Vec3, gravityUp: Vec3, screenRight: Vec3): VehicleAccel {
        val (right, forward) = horizontalFrame(gravityUp, screenRight) ?: return VehicleAccel.ZERO
        return VehicleAccel(lateral = linear dot right, longitudinal = linear dot forward)
    }

    /**
     * Gyroscope-derived equivalent of [vehicleAccel]: yaw rate about the vertical axis, times an
     * assumed speed, gives the centripetal (lateral) acceleration of a turn; a turn to the left
     * (positive yaw) pushes the passenger right. Pitch rate stands in, weakly, for longitudinal
     * motion — a braking car noses down (negative pitch).
     */
    fun gyroAccel(gyro: Vec3, gravityUp: Vec3, screenRight: Vec3): VehicleAccel {
        val up = gravityUp.normalized() ?: return VehicleAccel.ZERO
        val (right, _) = horizontalFrame(gravityUp, screenRight) ?: return VehicleAccel.ZERO
        val yawRate = gyro dot up
        val pitchRate = gyro dot right
        return VehicleAccel(
            lateral = -yawRate * ASSUMED_SPEED_MPS,
            longitudinal = pitchRate * PITCH_RATE_GAIN,
        )
    }

    /** Blends accelerometer and gyroscope estimates. The accelerometer is authoritative for
     * longitudinal motion; laterally the gyroscope is less noisy, so both contribute. */
    fun fuse(accel: VehicleAccel, gyro: VehicleAccel): VehicleAccel = VehicleAccel(
        lateral = accel.lateral * 0.6f + gyro.lateral * 0.4f,
        longitudinal = accel.longitudinal,
    )

    /** One step of an exponential low-pass filter with time constant [tau] seconds. */
    fun lowPass(current: Float, target: Float, dt: Float, tau: Float): Float {
        if (tau <= 0f) return target
        val k = 1f - kotlin.math.exp(-dt / tau)
        return current + (target - current) * k
    }
}
