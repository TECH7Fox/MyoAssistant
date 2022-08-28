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
    BatteryCallback,
    ReadDeviceNameCallback, ClassifierProcessor.ClassifierEventListener,
    EmgProcessor.EmgDataListener, ImuProcessor.ImuDataListener {

    //private val binding = ViewMyoBinding.inflate(LayoutInflater.from(context))
    public lateinit var _myo: Myo

    private lateinit var tv_name: TextView
    private lateinit var tv_battery: TextView
    private lateinit var tv_firmware: TextView
    private lateinit var tv_address: TextView

    private lateinit var mClassifierProcessor: ClassifierProcessor
    //private lateinit var mEmgProcessor: EmgProcessor

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

//        mClassifierProcessor = ClassifierProcessor()
//        mClassifierProcessor.addListener(this)
//        myo.addProcessor(mClassifierProcessor)

//        mEmgProcessor = EmgProcessor()
//        mEmgProcessor.addListener(this)
//        myo.addProcessor(mEmgProcessor)
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

    override fun onClassifierEvent(p0: ClassifierEvent?) {
        Logy.w("ClassifierEvent MyoView", p0?.type.toString())

        if (p0?.type == ClassifierEvent.Type.POSE) {
            Logy.w("POSE", (p0 as PoseClassifierEvent).pose.toString())
        }
    }

    override fun onNewEmgData(p0: EmgData?) {
        Logy.w("onNewEmgData MyoView", p0?.toString())
    }

    override fun onNewImuData(p0: ImuData?) {
        TODO("Not yet implemented")
    }
}