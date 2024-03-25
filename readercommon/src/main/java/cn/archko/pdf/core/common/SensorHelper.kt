package cn.archko.pdf.core.common

import android.app.Activity
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author: archko 2018/7/22 :13:03
 */
class SensorHelper(private val activity: ComponentActivity) {

    private val sensorEventListener: SensorEventListener = object : SensorEventListener {
        /**
         * Called when accuracy changes.
         * This method is empty, but it's required by relevant interface.
         */
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        override fun onSensorChanged(event: SensorEvent) {
            gravity[0] = 0.8f * gravity[0] + 0.2f * event.values[0]
            gravity[1] = 0.8f * gravity[1] + 0.2f * event.values[1]
            gravity[2] = 0.8f * gravity[2] + 0.2f * event.values[2]
            val sq0 = gravity[0] * gravity[0]
            val sq1 = gravity[1] * gravity[1]
            val sq2 = gravity[2] * gravity[2]
            gravityAge++
            if (gravityAge < 4) {
                // ignore initial hiccups
                return
            }
            if (sq1 > 3 * (sq0 + sq2)) {
                if (gravity[1] > 4) setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) else if (gravity[1] < -4 && Build.VERSION.SDK.toInt() >= 9) setOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                )
            } else if (sq0 > 3 * (sq1 + sq2)) {
                if (gravity[0] > 4) setOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) else if (gravity[0] < -4 && Build.VERSION.SDK.toInt() >= 9) setOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                )
            }
        }
    }
    private var sensorManager: SensorManager? = null
    private val gravity = floatArrayOf(0f, -9.81f, 0f)
    private var gravityAge: Long = 0
    private var prevOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    private fun setOrientation(orientation: Int) {
        if (orientation != prevOrientation) {
            activity.requestedOrientation = orientation
            prevOrientation = orientation
        }
    }

    fun onPause() {
        if (sensorManager != null) {
            sensorManager!!.unregisterListener(sensorEventListener)
            sensorManager = null
            MMKV.defaultMMKV().decodeString(PREF_PREV_ORIENTATION, prevOrientation.toString())
        }
    }

    fun onResume() {
        activity.lifecycleScope.launch {
            prevOrientation = withContext(Dispatchers.IO) {
                val str: String? = MMKV.defaultMMKV().decodeString(PREF_PREV_ORIENTATION)
                if (null != str) {
                    return@withContext str.toInt()
                } else {
                    return@withContext ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
            if (null == sensorManager) {
                sensorManager = activity.getSystemService(Activity.SENSOR_SERVICE) as SensorManager
            }

            val orientation = MMKV.defaultMMKV().decodeInt(PREF_ORIENTATION)

            if (setOrientation(activity, orientation, prevOrientation)) {
                if (sensorManager!!.getSensorList(Sensor.TYPE_ACCELEROMETER).size > 0) {
                    gravity[0] = 0f
                    gravity[1] = -9.81f
                    gravity[2] = 0f
                    gravityAge = 0
                    sensorManager!!.registerListener(
                        sensorEventListener,
                        sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                    activity.requestedOrientation = prevOrientation
                } else {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        }
    }

    companion object {
        const val PREF_ORIENTATION = "orientation"
        const val PREF_PREV_ORIENTATION = "prevOrientation"
        fun setOrientation(activity: Activity, orientation: Int, prev: Int): Boolean {
            when (orientation) {
                0 -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                1 -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                2 -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                3 -> {
                    //Logcat.v(TAG, "restoring orientation: " + prev);
                    activity.requestedOrientation = prev
                    return true
                }

                4 -> activity.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT

                5 -> activity.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE

                6 -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                7 -> activity.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

                else -> {
                }
            }
            return false
        }
    }
}