package com.tech7fox.myoassistant

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.util.AttributeSet
import android.view.View.OnClickListener
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.tech7fox.myolink.Myo
import com.tech7fox.myolink.MyoCmds
import com.tech7fox.myolink.MyoInfo
import com.tech7fox.myolink.msgs.MyoMsg
import com.tech7fox.myolink.tools.Logy

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
    private lateinit var tv_address: TextView

    init {
        Logy.w("MyoView init", "kotlin init block called.")
        //inflate(context, R.layout.view_myo, this)
        Logy.w("MyoView init", "inflation started.")
    }

    public fun setMyo(myo: Myo) {
        Logy.w("MYO SET!", myo.deviceAddress)
        _myo = myo;

        Handler().post {
            tv_address.text = _myo.deviceAddress;
        }

        _myo.readDeviceName(this)
        _myo.readFirmware(this)
        _myo.readBatteryLevel(this)
    }

    override fun onFinishInflate() {
        Logy.w("onFinishInflate", "onFinishInflate() called.")
        tv_name = findViewById<TextView>(R.id.tv_title)
        tv_battery = findViewById<TextView>(R.id.tv_batterylevel)
        tv_firmware = findViewById<TextView>(R.id.tv_firmware)
        tv_address = findViewById<TextView>(R.id.tv_address)
        super.onFinishInflate()
    }

    override fun setOnClickListener(l: OnClickListener?) {
        Logy.w("myo", "click detected!")
        super.setOnClickListener(l)
    }

    override fun onAttachedToWindow() {

        this.setOnClickListener {
            _myo.writeVibrate(MyoCmds.VibrateType.LONG, null)
            _myo.writeLEDs(Color.valueOf(0f, 100f, 185f), Color.valueOf(0f, 185f, 0f), null)
        }

        tv_name.setOnClickListener {
            val dialogBuilder = AlertDialog.Builder(
                context
            )
                .setTitle("Rename")
            val dialogContext = dialogBuilder.context
            val editText = EditText(dialogContext)
            editText.setText(_myo.deviceName)
            dialogBuilder.setPositiveButton(
                "Change"
            ) { _, _ -> _myo.writeDeviceName(editText.text.toString(), null) }
            dialogBuilder.setView(editText).show()
        }

        super.onAttachedToWindow()
    }

    override fun callOnClick(): Boolean {
        Logy.w("myo", "click detected!")
        return super.callOnClick()
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        Logy.w("long click", "Long click!")
        _myo.writeVibrate(MyoCmds.VibrateType.LONG, null)
        super.setOnLongClickListener(l)
    }

    override fun onFirmwareRead(p0: Myo?, p1: MyoMsg?, p2: String?) {
        handler.post {
            tv_firmware.text = p2;
        }
    }

    override fun onBatteryLevelRead(p0: Myo?, p1: MyoMsg?, p2: Int) {
        handler.post {
            tv_battery.text = "Battery: $p2%";
        }
    }

    override fun onDeviceNameRead(p0: Myo?, p1: MyoMsg?, p2: String?) {
        handler.post {
            tv_name.text = p2
        }
    }
}