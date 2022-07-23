package com.tech7fox.myoassistant

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.tech7fox.myolink.Myo
import com.tech7fox.myolink.MyoCmds
import com.tech7fox.myolink.msgs.MyoMsg
import com.tech7fox.myolink.tools.Logy
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.logging.Handler

class MyoView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attributeSet, defStyleAttr), Myo.FirmwareCallback, Myo.BatteryCallback, Myo.ReadDeviceNameCallback {

    //private val binding = ViewMyoBinding.inflate(LayoutInflater.from(context))
    private lateinit var _myo: Myo

    private lateinit var tv_name: TextView
    private lateinit var tv_battery: TextView
    private lateinit var tv_firmware: TextView
    private lateinit var tv_serialnumber: TextView
    private lateinit var tv_address: TextView

    init {
        Logy.w("MyoView init", "kotlin init block called.")
        //inflate(context, R.layout.view_myo, this)
        Logy.w("MyoView init", "inflation started.")
    }

    public fun setMyo(myo: Myo) {
        Logy.w("MYO SET!", myo.deviceAddress)
        _myo = myo;

        android.os.Handler().postDelayed(Runnable {
            _myo.readDeviceName(this)
            _myo.readFirmware(this)
            _myo.readBatteryLevel(this)
        }, 5000);

    }

    override fun onFinishInflate() {
        Logy.w("onFinishInflate", "onFinishInflate() called.")
        tv_name = findViewById<TextView>(R.id.tv_title)
        tv_battery = findViewById<TextView>(R.id.tv_batterylevel)
        tv_firmware = findViewById<TextView>(R.id.tv_firmware)
        tv_serialnumber = findViewById<TextView>(R.id.tv_serialnumber)
        tv_address = findViewById<TextView>(R.id.tv_address)
        super.onFinishInflate()
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        Logy.w("long click", "Long click!")
        _myo.writeVibrate(MyoCmds.VibrateType.LONG, null)
        super.setOnLongClickListener(l)
    }

    override fun onFirmwareRead(p0: Myo?, p1: MyoMsg?, p2: String?) {
        Logy.w("firmware", p2)
        handler.post(Runnable {
            tv_firmware.text = p2;
        })
    }

    override fun onBatteryLevelRead(p0: Myo?, p1: MyoMsg?, p2: Int) {
        Logy.w("battery", p2.toString())
        handler.post(Runnable {
            tv_battery.text = "Battery: $p2%";
        })
    }

    override fun onDeviceNameRead(p0: Myo?, p1: MyoMsg?, p2: String?) {
        Logy.w("name", p2)
        handler.post(Runnable {
            tv_name.text = p2
        })
    }
}