package com.tech7fox.myoassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class Autostart : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        //val restartServiceIntent = Intent(context, this.javaClass)
        //restartServiceIntent.setPackage(context.packageName)
        //context.startService(restartServiceIntent)
    }
}