package com.tech7fox.myoassistant

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.widget.Toast
import androidx.core.os.HandlerCompat.postDelayed
import com.tech7fox.myolink.BaseMyo
import com.tech7fox.myolink.Myo
import com.tech7fox.myolink.MyoConnector
import com.tech7fox.myolink.tools.Logy
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MyosService : Service(), BaseMyo.ConnectionListener {

    private val tag: String = "MyosService"
    private lateinit var myoConnector: MyoConnector
    public var myos: MutableList<String> = mutableListOf()
    public var savedMyos: HashMap<String, Myo?> = HashMap<String, Myo?>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        myoConnector = MyoConnector(applicationContext)

        val scannerCallback = MyoConnector.ScannerCallback {
            Logy.w(tag, "MYOS:" + it.size);

            val newMyos: MutableList<String> = mutableListOf()

            for (myo in it) {
                newMyos.add(myo.deviceAddress) // add to all list

                if (savedMyos.contains(myo.deviceAddress)) {
                    if (myo.connectionState == BaseMyo.ConnectionState.DISCONNECTED) {
                        Logy.w(tag, "connecting to ${myo.deviceAddress}")
                        myo.connect()
                        // setup myo
                        savedMyos[myo.deviceAddress] = myo;
                    } else if (myo.connectionState == BaseMyo.ConnectionState.CONNECTED) {
                        // update
                        Logy.w(tag, "Updating ${myo.deviceAddress}")
                    }
                }
            }
            myos = newMyos;
        }

        myoConnector.scan(60000, scannerCallback)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Logy.w(tag, "Restart MyosService")
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }

    override fun onConnectionStateChanged(p0: BaseMyo?, p1: BaseMyo.ConnectionState?) {
        Logy.w(tag, "Connected updated for ${p0!!.deviceAddress} to $p1");
    }
}