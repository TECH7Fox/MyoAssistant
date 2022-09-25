package com.tech7fox.myoassistant

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isGone
import com.tech7fox.myolink.tools.Logy
import org.json.JSONArray
import org.json.JSONStringer

class MyoDeviceView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0
) : RelativeLayout(context, attributeSet, defStyle) {

    public fun setStats(deviceName: String, deviceAddress: String) {
        findViewById<TextView>(R.id.device_name).text = deviceName
        findViewById<TextView>(R.id.device_address).text = deviceAddress
    }
}