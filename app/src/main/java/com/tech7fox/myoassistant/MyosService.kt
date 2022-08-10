package com.tech7fox.myoassistant

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.os.HandlerCompat.postDelayed
import androidx.fragment.app.Fragment
import com.tech7fox.myolink.BaseMyo
import com.tech7fox.myolink.Myo
import com.tech7fox.myolink.MyoConnector
import com.tech7fox.myolink.tools.Logy
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MyosService : Service(), BaseMyo.ConnectionListener {

    private val binder = LocalBinder()
    public var fragment: FirstFragment? = null
    private val tag: String = "MyosService"
    private lateinit var myoConnector: MyoConnector
    public var myos: MutableList<String> = mutableListOf()
    public var savedMyos: HashMap<String, Myo?> = HashMap()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logy.w(tag, "Starting MyosService...")

        //test data
        savedMyos["E3:65:94:9C:0C:4F"] = null;

        myoConnector = MyoConnector(applicationContext)

        val scannerCallback = MyoConnector.ScannerCallback {
            Logy.w(tag, "MYOS:" + it.size);

            val newMyos: MutableList<String> = mutableListOf()

            for (myo in it) {

                if (savedMyos.contains(myo.deviceAddress)) { // for saved myos
                    if (myo.connectionState == BaseMyo.ConnectionState.DISCONNECTED) { // connect if not
                        Logy.w(tag, "connecting to ${myo.deviceAddress}")
                        myo.addConnectionListener(this)
                        myo.connect()
                        // setup myo
                        savedMyos[myo.deviceAddress] = myo;
                    } else if (myo.connectionState == BaseMyo.ConnectionState.CONNECTED) { // else update myo
                        myo.readBatteryLevel(null)
                        Logy.w(tag, "Updating ${myo.deviceAddress}")
                    }
                } else {
                    newMyos.add(myo.deviceAddress) // add to all list
                }
            }
            myos = newMyos
            fragment?.updateMyos()
        }

        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                Logy.w(tag, "Scan for myos...")
                myoConnector.scan(30000, scannerCallback)
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

    override fun onConnectionStateChanged(p0: BaseMyo?, p1: BaseMyo.ConnectionState?) {
        Logy.w(tag, "Connection updated for ${p0!!.deviceAddress} to $p1");
    }
}