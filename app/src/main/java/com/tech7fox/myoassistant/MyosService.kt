package com.tech7fox.myoassistant

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.preference.PreferenceManager
import com.tech7fox.myolink.BaseMyo
import com.tech7fox.myolink.Myo
import com.tech7fox.myolink.MyoCmds
import com.tech7fox.myolink.MyoConnector
import com.tech7fox.myolink.processor.classifier.ClassifierEvent
import com.tech7fox.myolink.processor.classifier.ClassifierProcessor
import com.tech7fox.myolink.processor.classifier.ClassifierProcessor.ClassifierEventListener
import com.tech7fox.myolink.processor.classifier.PoseClassifierEvent
import com.tech7fox.myolink.processor.imu.ImuProcessor
import com.tech7fox.myolink.processor.imu.ImuData
import com.tech7fox.myolink.processor.imu.ImuProcessor.ImuDataListener
import com.tech7fox.myolink.tools.Logy
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.URL
import org.json.JSONObject
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.core.content.edit

class MyosService : Service(), BaseMyo.ConnectionListener, ClassifierEventListener, ImuDataListener {

    private val binder = LocalBinder()
    public var firstFragment: FirstFragment? = null
    public var scanFragment: ScanFragment? = null
    private val tag: String = "MyosService"
    private lateinit var myoConnector: MyoConnector
    //public var myos: MutableList<String> = mutableListOf()
    public var savedMyos: HashMap<String, Myo?> = HashMap()
    private var classifierProcessors: HashMap<String, ClassifierProcessor?> = HashMap()
    private var ImuProcessors: HashMap<String, ImuProcessor?> = HashMap()
    private var lastImuUpdate: Long = 0
    private var headingOffsets: HashMap<String, Double> = HashMap()
    private var latestYaw: HashMap<String, Double> = HashMap()
    
    // Complementary filter coefficient (0.0 = all accelerometer, 1.0 = all gyro/quaternion)
    // 0.98 means 98% gyro and 2% accelerometer - good balance for drift correction
    private val COMPLEMENTARY_FILTER_ALPHA = 0.98
    
    // Store filtered roll and pitch for each device
    private var filteredRoll: HashMap<String, Double> = HashMap()
    private var filteredPitch: HashMap<String, Double> = HashMap()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logy.w(tag, "Starting MyosService...")

        val savedMyosString: String =
            getSharedPreferences("myo_assistant", Context.MODE_PRIVATE).getString("myos", "").toString()

        Logy.w("OnStartCommand", savedMyosString)

        for (myo in savedMyosString.split(" ")) {
            if (myo.isNotEmpty()) savedMyos[myo] = null
        }

        myoConnector = MyoConnector(applicationContext)

        val scannerCallback = MyoConnector.ScannerCallback {
            Logy.w(tag, "MYOS:" + it.size);

            val newMyos: HashMap<String, String> = HashMap()

            for (myo in it) {

                if (savedMyos.contains(myo.deviceAddress)) { // for saved myos
                    if (myo.connectionState == BaseMyo.ConnectionState.DISCONNECTED) { // connect if not
                        Logy.w(tag, "connecting to ${myo.deviceAddress}")
                        myo.addConnectionListener(this)

                        savedMyos[myo.deviceAddress] = myo

                        myo.connect()

                        // setup classifier
                        classifierProcessors[myo.deviceAddress] = ClassifierProcessor()
                        classifierProcessors[myo.deviceAddress]?.addListener(this)
                        myo.addProcessor(classifierProcessors[myo.deviceAddress])

                        // setup IMU processor
                        ImuProcessors[myo.deviceAddress] = ImuProcessor()
                        ImuProcessors[myo.deviceAddress]?.addListener(this)
                        myo.addProcessor(ImuProcessors[myo.deviceAddress])

                    } else if (myo.connectionState == BaseMyo.ConnectionState.CONNECTED) { // else update myo
                        myo.readBatteryLevel(null)
                    }
                } else {
                    newMyos[myo.deviceAddress] = myo.deviceName // add to all list
                }
            }
            scanFragment?.showMyos(newMyos)
        }

        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                myoConnector.scan(5000, scannerCallback)
                mainHandler.postDelayed(this, 6000)
            }
        })

        return START_STICKY
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): MyosService = this@MyosService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        firstFragment = null;
        return super.onUnbind(intent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Logy.w(tag, "Restart MyosService")
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }

    fun saveMyos() {
        val editor: SharedPreferences.Editor = getSharedPreferences("myo_assistant", Context.MODE_PRIVATE).edit()
        editor.putString("myos", savedMyos.keys.joinToString(" "))
        editor.apply()
    }

    override fun onConnectionStateChanged(baseMyo: BaseMyo?, p1: BaseMyo.ConnectionState?) {
        Logy.w(tag, "Connection updated for ${baseMyo?.deviceAddress} to $p1");
        Logy.w(tag, "myo object for ${baseMyo?.deviceAddress} is ${baseMyo?.isRunning}");
        if (p1 == BaseMyo.ConnectionState.CONNECTED) { // && baseMyo?.connectionSpeed !== BaseMyo.ConnectionSpeed.HIGH) {
            Logy.w(tag, "Starting delay!")

            if (Looper.myLooper() == null) Looper.prepare()
            Handler(Looper.getMainLooper()).postDelayed({
                Logy.w(tag, "Started! Getting myo...")
                val myo = savedMyos[baseMyo?.deviceAddress]
                if (myo !== null) {
                    Logy.w(tag, "Setting speed and options!")
                    with(myo) {
                        connectionSpeed = BaseMyo.ConnectionSpeed.BALANCED
                        writeSleepMode(MyoCmds.SleepMode.NORMAL, null)
                        writeMode(
                            MyoCmds.EmgMode.NONE,
                            MyoCmds.ImuMode.DATA,
                            MyoCmds.ClassifierMode.ENABLED,
                            null
                        )
                        writeUnlock(MyoCmds.UnlockType.HOLD, null)
                    }
                    firstFragment?.setMyo(myo)
                }
            }, 10000)
        }
//        if (p1 == BaseMyo.ConnectionState.CONNECTED) {
//            val myo = savedMyos[baseMyo?.deviceAddress]
//            if (myo !== null) fragment?.addMyo(myo)
//        }
        else if (p1 == BaseMyo.ConnectionState.DISCONNECTED) {
            val myo = savedMyos[baseMyo?.deviceAddress]
            if (myo !== null) firstFragment?.setState("Disconnected", myo.deviceAddress)
        }
    }

    override fun onClassifierEvent(p0: ClassifierEvent?) {
        Logy.w("ClassifierEvent Service", p0?.type. toString())

        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        var state = p0?.type. toString()

        if (p0?.type == ClassifierEvent. Type.POSE) {
            state = (p0 as PoseClassifierEvent).pose.toString()
            Logy.w("POSE", state)
        }

        // send to Home Assistant
        sendToHomeAssistant("sensor.myo1", state, null)
    }

    override fun onNewImuData(imuData:  ImuData) {
        // Throttle updates to avoid overwhelming Home Assistant (update every 500ms)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastImuUpdate < 500) {
            return
        }
        lastImuUpdate = currentTime

        val orientation = imuData. orientationData
        val accelerometer = imuData.accelerometerData
        val gyro = imuData.gyroData

        // Convert quaternion to Euler angles (roll, pitch, yaw)
        val w = orientation[0]
        val x = orientation[1]
        val y = orientation[2]
        val z = orientation[3]

        // Calculate Euler angles from quaternion in radians
        val roll = atan2(2.0 * (w * x + y * z), 1.0 - 2.0 * (x * x + y * y))
        val pitch = asin(2.0 * (w * y - z * x))
        val yaw = atan2(2.0 * (w * z + x * y), 1.0 - 2.0 * (y * y + z * z))

        // Convert to degrees
        val rollDeg = Math.toDegrees(roll)
        val pitchDeg = Math.toDegrees(pitch)
        val yawDeg = Math.toDegrees(yaw)
        
        // DRIFT CORRECTION: Use accelerometer to calculate roll and pitch from gravity
        val accelX = accelerometer[0]
        val accelY = accelerometer[1]
        val accelZ = accelerometer[2]
        
        // Calculate roll and pitch from accelerometer (gravity vector)
        // These are drift-free because they're based on gravity
        val accelRoll = Math.toDegrees(atan2(accelY, accelZ))
        val accelPitch = Math.toDegrees(atan2(-accelX, sqrt(accelY * accelY + accelZ * accelZ)))
        
        // Get device address for per-device filtering
        val deviceAddress = imuData.deviceAddress
        
        // Initialize filtered values if first time for this device
        if (!filteredRoll.containsKey(deviceAddress)) {
            filteredRoll[deviceAddress] = rollDeg
            filteredPitch[deviceAddress] = pitchDeg
        }
        
        // Apply complementary filter to fuse gyro-based (quaternion) and accelerometer data
        // This reduces drift while maintaining responsiveness
        // Blend current quaternion-based angles with accelerometer angles
        val correctedRoll = COMPLEMENTARY_FILTER_ALPHA * rollDeg + (1 - COMPLEMENTARY_FILTER_ALPHA) * accelRoll
        val correctedPitch = COMPLEMENTARY_FILTER_ALPHA * pitchDeg + (1 - COMPLEMENTARY_FILTER_ALPHA) * accelPitch
        
        // Store filtered values for next iteration (for future enhancements)
        filteredRoll[deviceAddress] = correctedRoll
        filteredPitch[deviceAddress] = correctedPitch

        // Store latest yaw for this device
        latestYaw[deviceAddress] = yawDeg

        // Get heading offset for this device (default to 0)
        val offset = headingOffsets[deviceAddress] ?: 0.0

        // Calculate tilt-compensated heading using corrected roll and pitch
        // This accounts for device tilt when calculating magnetic/gyro heading
        val rollRad = Math.toRadians(correctedRoll)
        val pitchRad = Math.toRadians(correctedPitch)
        val yawRad = Math.toRadians(yawDeg)
        
        // Tilt compensation formulas
        val cosRoll = cos(rollRad)
        val sinRoll = sin(rollRad)
        val cosPitch = cos(pitchRad)
        val sinPitch = sin(pitchRad)
        
        // Calculate tilt-compensated heading
        val Xh = cos(yawRad) * cosPitch + sin(yawRad) * sinPitch * sinRoll
        val Yh = sin(yawRad) * cosRoll
        val tiltCompensatedYaw = Math.toDegrees(atan2(Yh, Xh))

        // Apply heading offset for calibration
        var heading = tiltCompensatedYaw - offset

        // Normalize to 0-360 degrees
        while (heading < 0) heading += 360
        while (heading >= 360) heading -= 360

        // Create attributes object with IMU data
        val attributes = JSONObject().apply {
            // Raw quaternion
            put("orientation_w", "%.3f".format(w))
            put("orientation_x", "%.3f".format(x))
            put("orientation_y", "%.3f".format(y))
            put("orientation_z", "%.3f".format(z))

            // Euler angles in degrees (drift-corrected)
            put("roll", "%.1f".format(correctedRoll))
            put("pitch", "%.1f".format(correctedPitch))
            put("yaw", "%.1f".format(yawDeg))
            put("heading", "%.1f".format(heading))
            put("heading_calibrated", offset != 0.0)
            put("drift_corrected", true)

            // Accelerometer
            put("accel_x", "%.3f".format(accelerometer[0]))
            put("accel_y", "%.3f".format(accelerometer[1]))
            put("accel_z", "%.3f".format(accelerometer[2]))

            // Gyroscope
            put("gyro_x", "%.3f".format(gyro[0]))
            put("gyro_y", "%.3f".format(gyro[1]))
            put("gyro_z", "%.3f".format(gyro[2]))
        }

        // Send IMU data to Home Assistant
        sendToHomeAssistant("sensor.myo1_imu", "active", attributes)

        Logy.v(tag, "Sent IMU data: Roll=%.1f° Pitch=%.1f° Yaw=%.1f° Heading=%.1f° (corrected)".format(
            correctedRoll, correctedPitch, yawDeg, heading
        ))
    }

    fun calibrateHeading(deviceAddress: String) {
        val currentYaw = latestYaw[deviceAddress]
        if (currentYaw != null) {
            headingOffsets[deviceAddress] = currentYaw
            Logy.w(tag, "Heading calibrated for $deviceAddress with offset: $currentYaw°")

            // Save to preferences
            val preferences = getSharedPreferences("myo_assistant", Context.MODE_PRIVATE)
            preferences.edit { putFloat("heading_offset_$deviceAddress", currentYaw.toFloat()) }

            // Notify user via toast on main thread
            Handler(Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    applicationContext,
                    "Heading calibrated! ",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Logy.e(tag, "Cannot calibrate - no IMU data received yet for $deviceAddress")
            Handler(Looper.getMainLooper()).post {
                android. widget.Toast.makeText(
                    applicationContext,
                    "Wait for IMU data before calibrating",
                    android. widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun sendToHomeAssistant(entityId: String, state: String, attributes: JSONObject?) {
        Thread {
            try {
                val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val url = URL(preferences.getString("ha_ip", "").toString() + "/api/states/$entityId")

                val postData = JSONObject().apply {
                    put("state", state)
                    if (attributes != null) {
                        put("attributes", attributes)
                    }
                }. toString()

                val conn = url.openConnection()
                conn. doOutput = true
                conn.setRequestProperty("Authorization", "Bearer " + preferences.getString("ha_bearer", "").toString())
                conn.setRequestProperty("Content-Type", "application/json")

                DataOutputStream(conn.outputStream).use { it.writeBytes(postData) }

                BufferedReader(InputStreamReader(conn.inputStream)).use { bf ->
                    Logy.v(tag, "HA API response for $entityId: ${bf.readText()}")
                }
            } catch (error: Exception) {
                Logy.e(tag, "HA API error: $error")
            }
        }. start()
    }
}