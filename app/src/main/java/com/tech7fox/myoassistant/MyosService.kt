package com.tech7fox.myoassistant

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.tech7fox.myolink.BaseMyo
import com.tech7fox.myolink.Myo
import com.tech7fox.myolink.MyoCmds
import com.tech7fox.myolink.MyoConnector
import com.tech7fox.myolink.processor.classifier.ClassifierEvent
import com.tech7fox.myolink.processor.classifier.ClassifierProcessor
import com.tech7fox.myolink.processor.classifier.ClassifierProcessor.ClassifierEventListener
import com.tech7fox.myolink.processor.classifier.PoseClassifierEvent
import com.tech7fox.myolink.tools.Logy

class MyosService : Service(), BaseMyo.ConnectionListener, ClassifierEventListener {

    private val binder = LocalBinder()
    public var firstFragment: FirstFragment? = null
    public var scanFragment: ScanFragment? = null
    private val tag: String = "MyosService"
    private lateinit var myoConnector: MyoConnector
    //public var myos: MutableList<String> = mutableListOf()
    public var savedMyos: HashMap<String, Myo?> = HashMap()
    private var classifierProcessors: HashMap<String, ClassifierProcessor?> = HashMap()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logy.w(tag, "Starting MyosService...")

        val savedMyosString: String =
            getSharedPreferences("myo_assistant", Context.MODE_PRIVATE).getString("myos", "").toString()

        Logy.w("OnStartCommand", savedMyosString)

        for (myo in savedMyosString.split(" ")) {
            if (myo.isNotEmpty()) savedMyos[myo] = null
        }

        //test data
        //savedMyos["E3:65:94:9C:0C:4F"] = null;

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

                        // setup myo
                        classifierProcessors[myo.deviceAddress] = ClassifierProcessor()
                        classifierProcessors[myo.deviceAddress]?.addListener(this)
                        myo.addProcessor(classifierProcessors[myo.deviceAddress])
                    } else if (myo.connectionState == BaseMyo.ConnectionState.CONNECTED) { // else update myo
                        myo.readBatteryLevel(null)
                        //Logy.w(tag, "Updating ${myo.deviceAddress}")
                    }
                } else {
                    newMyos[myo.deviceAddress] = myo.deviceName // add to all list
                }
            }
            //myos = newMyos
            scanFragment?.showMyos(newMyos)
        }

        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                //Logy.w(tag, "Scan for myos...")
                myoConnector.scan(5000, scannerCallback)
                mainHandler.postDelayed(this, 30000)
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

    override fun onConnectionStateChanged(baseMyo: BaseMyo?, p1: BaseMyo.ConnectionState?) {
        Logy.w(tag, "Connection updated for ${baseMyo?.deviceAddress} to $p1");
        Logy.w(tag, "myo object for ${baseMyo?.deviceAddress} is ${baseMyo?.isRunning}");
        if (p1 == BaseMyo.ConnectionState.CONNECTED) { // && baseMyo?.connectionSpeed !== BaseMyo.ConnectionSpeed.HIGH) {
            Logy.w(tag, "Starting delay!")
            if (Looper.myLooper() == null) {
                Looper.prepare()
            }

            val mainHandler = Handler(Looper.getMainLooper())

            mainHandler.postDelayed({
                Logy.w(tag, "Started! Getting myo...")
                val myo = savedMyos[baseMyo?.deviceAddress]
                if (myo !== null) {
                    Logy.w(tag, "Setting speed and options!")
                    with(myo) {
                        connectionSpeed = BaseMyo.ConnectionSpeed.BALANCED
                        writeSleepMode(MyoCmds.SleepMode.NORMAL, null)
                        writeMode(
                            MyoCmds.EmgMode.NONE,
                            MyoCmds.ImuMode.NONE,
                            MyoCmds.ClassifierMode.ENABLED,
                            null
                        )
                        writeUnlock(MyoCmds.UnlockType.HOLD, null)
                    }
                    firstFragment?.addMyo(myo)
                }
            }, 10000)
        }
//        if (p1 == BaseMyo.ConnectionState.CONNECTED) {
//            val myo = savedMyos[baseMyo?.deviceAddress]
//            if (myo !== null) fragment?.addMyo(myo)
//        }
        else if (p1 == BaseMyo.ConnectionState.DISCONNECTED) {
            val myo = savedMyos[baseMyo?.deviceAddress]
            if (myo !== null) firstFragment?.removeMyo(myo)
        }
    }

    override fun onClassifierEvent(p0: ClassifierEvent?) {
        Logy.w("ClassifierEvent Service", p0?.type.toString())

        if (p0?.type == ClassifierEvent.Type.POSE) {
            val pose = (p0 as PoseClassifierEvent).pose.toString()
            Logy.w("POSE", pose)
        }

        // send to Home Assistant
//        val url = URL("http://homeassistant.local:8123/api/states/sensor.myo1")
//        val postData = "{\"state\": \"${p0?.type.toString()}\"}"
//
//        val conn = url.openConnection()
//        conn.doOutput = true
//        conn.setRequestProperty("Content-Type", "application/json")
//
//        DataOutputStream(conn.getOutputStream()).use { it.writeBytes(postData) }
//        Logy.w("url", conn.getOutputStream().toString())
    }
}