package com.tech7fox.myoassistant

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import com.tech7fox.myolink.tools.Logy
import org.json.JSONArray
import org.json.JSONStringer

class MyoDeviceView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0
) : RelativeLayout(context, attributeSet, defStyle) {

    private lateinit var deviceAddress: String
    private var saved: Boolean = false
    private var state: String = ""

    public fun setStats(deviceAddress: String, saved: Boolean) {
        this.deviceAddress = deviceAddress
        this.saved = saved

        findViewById<TextView>(R.id.device_address).text = deviceAddress
    }

    public fun setState(state: String) {
        this.state = state

        //findViewById<TextView>()
    }
}