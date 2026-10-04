package com.wwwescape.carmotionsicknessaid.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.wwwescape.carmotionsicknessaid.data.settings.SensorMode

/** Which motion sensors this device actually has. */
data class SensorAvailability(
    val accelerometer: Boolean,
    val gyroscope: Boolean,
    val linearAcceleration: Boolean,
    val gravity: Boolean,
) {
    val canRun: Boolean get() = accelerometer || gyroscope

    companion object {
        fun of(context: Context): SensorAvailability {
            val sm = context.getSystemService(SensorManager::class.java)
            fun has(type: Int) = sm?.getDefaultSensor(type) != null
            return SensorAvailability(
                accelerometer = has(Sensor.TYPE_ACCELEROMETER),
                gyroscope = has(Sensor.TYPE_GYROSCOPE),
                linearAcceleration = has(Sensor.TYPE_LINEAR_ACCELERATION),
                gravity = has(Sensor.TYPE_GRAVITY),
            )
        }
    }
}

/**
 * Listens to the accelerometer and/or gyroscope and keeps a running estimate of the vehicle's
 * horizontal acceleration ([current]), in m/s², in the screen's current orientation.
 *
 * Uses the platform's fused linear-acceleration and gravity sensors when present; otherwise
 * separates gravity from the raw accelerometer with a low-pass filter. A very slow high-pass
 * removes sensor bias and any constant offset left by an imperfect gravity estimate, so the cues
 * settle back to center when the car is cruising.
 *
 * [rotationProvider] returns the display's `Surface.ROTATION_*` so cues stay correct after the
 * screen rotates. Not thread-safe beyond what's needed: sensor callbacks land on the main thread
 * and [current] is a single volatile read.
 */
class MotionEngine(
    context: Context,
    private val rotationProvider: () -> Int,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    val availability = SensorAvailability.of(context)

    @Volatile
    var current: VehicleAccel = VehicleAccel.ZERO
        private set

    var mode: SensorMode = SensorMode.AUTO
        set(value) {
            if (field == value) return
            field = value
            if (running) {
                stop()
                start()
            }
        }

    private var running = false

    // Device-frame state.
    private var gravity = Vec3(0f, 0f, SensorManager.GRAVITY_EARTH)
    private var rawAccel = Vec3.ZERO
    private var linear = Vec3.ZERO
    private var gyro = Vec3.ZERO
    private var gotGravitySensor = false

    // Vehicle-frame state.
    private var accelEstimate = VehicleAccel.ZERO
    private var gyroEstimate = VehicleAccel.ZERO
    private var biasLat = 0f
    private var biasLong = 0f
    private var lastTimestampNs = 0L
    private var lastAccelTimestampNs = 0L

    private val useAccel get() = mode != SensorMode.GYROSCOPE && availability.accelerometer
    private val useGyro get() = mode != SensorMode.ACCELEROMETER && availability.gyroscope

    fun start() {
        val sm = sensorManager ?: return
        if (running) return
        running = true
        lastTimestampNs = 0L
        lastAccelTimestampNs = 0L
        val rate = SensorManager.SENSOR_DELAY_GAME
        // Gravity is needed in every mode to find the horizontal plane.
        val gravitySensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY)
        gotGravitySensor = gravitySensor != null
        gravitySensor?.let { sm.registerListener(this, it, rate) }
        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sm.registerListener(this, it, rate) }
        if (useAccel) {
            sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.let { sm.registerListener(this, it, rate) }
        }
        if (useGyro) {
            sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let { sm.registerListener(this, it, rate) }
        }
    }

    fun stop() {
        if (!running) return
        running = false
        sensorManager?.unregisterListener(this)
        accelEstimate = VehicleAccel.ZERO
        gyroEstimate = VehicleAccel.ZERO
        current = VehicleAccel.ZERO
    }

    override fun onSensorChanged(event: SensorEvent) {
        val v = Vec3(event.values[0], event.values[1], event.values[2])
        val dt = if (lastTimestampNs == 0L) 0.02f else ((event.timestamp - lastTimestampNs) / 1e9f).coerceIn(0.001f, 0.2f)
        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> gravity = v
            Sensor.TYPE_ACCELEROMETER -> {
                rawAccel = v
                val accelDt = if (lastAccelTimestampNs == 0L) 0.02f else ((event.timestamp - lastAccelTimestampNs) / 1e9f).coerceIn(0.001f, 0.2f)
                lastAccelTimestampNs = event.timestamp
                if (!gotGravitySensor) {
                    gravity = Vec3(
                        MotionMath.lowPass(gravity.x, v.x, accelDt, GRAVITY_TAU),
                        MotionMath.lowPass(gravity.y, v.y, accelDt, GRAVITY_TAU),
                        MotionMath.lowPass(gravity.z, v.z, accelDt, GRAVITY_TAU),
                    )
                }
                if (!availability.linearAcceleration && useAccel) {
                    linear = rawAccel - gravity
                    updateAccelEstimate()
                }
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                linear = v
                updateAccelEstimate()
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyro = v
                val (right, _) = MotionMath.screenAxes(rotationProvider())
                gyroEstimate = MotionMath.gyroAccel(gyro, gravity, right)
            }
            else -> return
        }
        lastTimestampNs = event.timestamp
        publish(dt)
    }

    private fun updateAccelEstimate() {
        val (right, _) = MotionMath.screenAxes(rotationProvider())
        accelEstimate = MotionMath.vehicleAccel(linear, gravity, right)
    }

    private fun publish(dt: Float) {
        val raw = when {
            useAccel && useGyro -> MotionMath.fuse(accelEstimate, gyroEstimate)
            useGyro -> gyroEstimate
            else -> accelEstimate
        }
        biasLat = MotionMath.lowPass(biasLat, raw.lateral, dt, BIAS_TAU)
        biasLong = MotionMath.lowPass(biasLong, raw.longitudinal, dt, BIAS_TAU)
        current = VehicleAccel(raw.lateral - biasLat, raw.longitudinal - biasLong)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        /** Gravity separation for devices without a fused gravity sensor. */
        const val GRAVITY_TAU = 0.8f

        /** Bias removal — long enough that a normal turn or braking isn't cancelled out. */
        const val BIAS_TAU = 20f
    }
}
