package com.lumina.manualcamera.advanced

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Lắng nghe con quay hồi chuyển đo độ rung của máy ảnh
 */
class GyroShakeDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    var currentAngularSpeed: Float = 0.0f
        private set

    fun start() {
        gyroSensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_GYROSCOPE) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        // Bộ lọc thông thấp (EMA) làm mượt giá trị đo
        currentAngularSpeed = currentAngularSpeed * 0.7f + magnitude * 0.3f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
