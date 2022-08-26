package com.tech7fox.myoassistant

import android.app.Service
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
import com.tech7fox.myolink.tools.Logy

class MyosService : Service(), BaseMyo.ConnectionListener, ClassifierEventListener {

    private val binder = LocalBinder()
    public var fragment: FirstFragment? = null
    private val tag: String = "MyosService"
    private lateinit var myoConnector: MyoConnector
    public var myos: MutableList<String> = mutableListOf()
    public var savedMyos: HashMap<String, Myo?> = HashMap()
    public var classifierProcessors: HashMap<String, ClassifierProcessor?> = HashMap()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logy.w(tag, "Starting MyosService...")

        //test data
        savedMyos["E3:65:94:9C:0C:4F"] = null;

        myoConnector = MyoConnector(applicationContext)

        val scannerCallback = MyoConnector.ScannerCallback {
            //Logy.w(tag, "MYOS:" + it.size);

            val newMyos: MutableList<String> = mutableListOf()

            for (myo in it) {

                if (savedMyos.contains(myo.deviceAddress)) { // for saved myos
                    if (myo.connectionState == BaseMyo.ConnectionState.DISCONNECTED) { // connect if not
                        Logy.w(tag, "connecting to ${myo.deviceAddress}")
                        myo.addConnectionListener(this)

                        savedMyos[myo.deviceAddress] = myo;

                        myo.connect()

                        // setup myo
//                        classifierProcessors[myo.deviceAddress] = ClassifierProcessor()
//                        classifierProcessors[myo.deviceAddress]?.addListener(this)
//                        myo.addProcessor(classifierProcessors[myo.deviceAddress])
                    } else if (myo.connectionState == BaseMyo.ConnectionState.CONNECTED) { // else update myo
                        //myo.readBatteryLevel(null)
                        //Logy.w(tag, "Updating ${myo.deviceAddress}")
                    }
                } else {
                    newMyos.add(myo.deviceAddress) // add to all list
                }
            }
            myos = newMyos
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
        fragment = null;
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
                        connectionSpeed = BaseMyo.ConnectionSpeed.HIGH
                        writeSleepMode(MyoCmds.SleepMode.NORMAL, null)
                        writeMode(
                            MyoCmds.EmgMode.FILTERED,
                            MyoCmds.ImuMode.RAW,
                            MyoCmds.ClassifierMode.ENABLED,
                            null
                        )
                        writeUnlock(MyoCmds.UnlockType.HOLD, null)
                    }
                    fragment?.addMyo(myo)
                }
            }, 10000)
        }
//        if (p1 == BaseMyo.ConnectionState.CONNECTED) {
//            val myo = savedMyos[baseMyo?.deviceAddress]
//            if (myo !== null) fragment?.addMyo(myo)
//        }
        else if (p1 == BaseMyo.ConnectionState.DISCONNECTED) {
            val myo = savedMyos[baseMyo?.deviceAddress]
            if (myo !== null) fragment?.removeMyo(myo)
        }
    }

    override fun onClassifierEvent(p0: ClassifierEvent?) {
        Logy.w("ClassifierEvent service","got event!")
        Logy.w("ClassifierEvent Service", p0?.type.toString())
    }
}