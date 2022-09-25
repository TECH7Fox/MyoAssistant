package com.tech7fox.myoassistant

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View.OnClickListener
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.tech7fox.myolink.BaseMyo
import com.tech7fox.myolink.Myo
import com.tech7fox.myolink.Myo.*
import com.tech7fox.myolink.MyoCmds
import com.tech7fox.myolink.MyoInfo
import com.tech7fox.myolink.msgs.MyoMsg
import com.tech7fox.myolink.processor.classifier.ClassifierEvent
import com.tech7fox.myolink.processor.classifier.ClassifierProcessor
import com.tech7fox.myolink.processor.classifier.PoseClassifierEvent
import com.tech7fox.myolink.processor.emg.EmgData
import com.tech7fox.myolink.processor.emg.EmgProcessor
import com.tech7fox.myolink.processor.imu.ImuData
import com.tech7fox.myolink.processor.imu.ImuProcessor
import com.tech7fox.myolink.tools.Logy

class MyoView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attributeSet, defStyleAttr),
    FirmwareCallback,
    BatteryCallback {

    //private val binding = ViewMyoBinding.inflate(LayoutInflater.from(context))
    var _myo: Myo? = null

    lateinit var address: String

    fun setMyo(myo: Myo) {
        Logy.w("MYO SET!", myo.deviceAddress)
        _myo = myo

        Handler().post {
            findViewById<TextView>(R.id.tv_address).text = _myo!!.deviceAddress
            findViewById<TextView>(R.id.tv_title).text = _myo!!.deviceName
        }

        _myo!!.readFirmware(this)
        _myo!!.readBatteryLevel(this)

        setState("Connected")
    }

    override fun onAttachedToWindow() {

        this.setOnClickListener {
            if (_myo?.connectionState != BaseMyo.ConnectionState.CONNECTED) {
                Toast.makeText(context, "Myo not connected.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            _myo!!.writeVibrate(MyoCmds.VibrateType.LONG, null)
            _myo!!.writeLEDs(Color.valueOf(0f, 100f, 185f), Color.valueOf(0f, 185f, 0f), null)
        }

        findViewById<TextView>(R.id.tv_title).setOnClickListener {
            if (_myo?.connectionState != BaseMyo.ConnectionState.CONNECTED) {
                Toast.makeText(context, "Myo not connected.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dialogBuilder = AlertDialog.Builder(
                context
            )
                .setTitle("Rename")
            val dialogContext = dialogBuilder.context
            val editText = EditText(dialogContext)
            editText.setText(_myo!!.deviceName)
            dialogBuilder.setPositiveButton(
                "Change"
            ) { _, _ -> _myo!!.writeDeviceName(editText.text.toString(), null) }
            dialogBuilder.setView(editText).show()
        }

        super.onAttachedToWindow()
    }

    fun setState(state: String) {
        if (Looper.myLooper() == null) Looper.prepare()
        Handler(Looper.getMainLooper()).post {
            findViewById<TextView>(R.id.tv_state).text = state
        }
    }

    fun setDeviceAddress(address: String) {
        this.address = address
        if (Looper.myLooper() == null) Looper.prepare()
        Handler(Looper.getMainLooper()).post {
            findViewById<TextView>(R.id.tv_address).text = address
        }
    }

    override fun onFirmwareRead(p0: Myo?, p1: MyoMsg?, p2: String?) {
        handler.post {
            findViewById<TextView>(R.id.tv_firmware).text = p2
        }
    }

    override fun onBatteryLevelRead(p0: Myo?, p1: MyoMsg?, p2: Int) {
        handler.post {
            findViewById<TextView>(R.id.tv_batterylevel).text = "Battery: $p2%"
        }
    }
}