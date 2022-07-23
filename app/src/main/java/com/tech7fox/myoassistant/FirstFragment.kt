package com.tech7fox.myoassistant

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tech7fox.myoassistant.databinding.FragmentFirstBinding
import com.tech7fox.myolink.BaseMyo
import com.tech7fox.myolink.BaseMyo.ConnectionListener
import com.tech7fox.myolink.MyoCmds
import com.tech7fox.myolink.MyoConnector
import com.tech7fox.myolink.tools.Logy
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class FirstFragment : Fragment(), ConnectionListener {

    private var _binding: FragmentFirstBinding? = null
    private var scanning = false
    private var myos: MutableList<String> = mutableListOf()
    private var myoConnector: MyoConnector? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Logy.sLoglevel = Logy.NORMAL;

        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            1
        )

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        myoConnector = MyoConnector(requireContext())

        myoConnector?.scan(5000, MyoConnector.ScannerCallback {
            Logy.w("scannerCallback", "MYOS:" + it.size);
            for (myo in it) {
                Logy.w("Myo handler", "deviceName is ${myo.deviceAddress}")
                if (!myos.contains(myo.deviceAddress)) {
                    val myoview: MyoView = LayoutInflater.from(requireActivity()).inflate(R.layout.view_myo, binding.listMyos, false) as MyoView
                    myo.addConnectionListener(this)
                    myo.connect()
                    myo.connectionSpeed = BaseMyo.ConnectionSpeed.HIGH
                    myo.writeSleepMode(MyoCmds.SleepMode.NEVER, null)
                    myo.writeMode(
                        MyoCmds.EmgMode.FILTERED,
                        MyoCmds.ImuMode.RAW,
                        MyoCmds.ClassifierMode.DISABLED,
                        null
                    )
                    myo.writeUnlock(MyoCmds.UnlockType.HOLD, null)
                    val tekst = TextView(requireContext())
                    tekst.text = "myo: ${myo.deviceAddress}"

                    lifecycleScope.launch {
                        Logy.w("runnable", "Adding myoview to list!");
                        myoview.setMyo(myo);
                        binding.listMyos.addView(myoview)
                        Logy.w("runnable", "Adding textview to list!");
                        binding.listMyos.addView(tekst)
                        Logy.w("runnable", "done Adding views to list!");
                    }

                    myos.add(myo.deviceAddress)
                }
            }
        })

        super.onViewCreated(view, savedInstanceState)


        //binding.buttonSecond.setOnClickListener {
            //myMyo?.writeVibrate(MyoCmds.VibrateType.SHORT, null)
            //myMyo?.writeLEDs(Color.valueOf(0f, 0f, 255f), Color.valueOf(0f, 0f, 255f), null)
        //}
    }

    override fun onResume() {
        Logy.w("onResume", "triggered!");
       //val mMyoConnector = MyoConnector(requireContext());
       //mMyoConnector.scan(5000, MyoConnector.ScannerCallback() {
       //    Logy.d("scannerCallback", "MYOS:" + it.size);

       //    lifecycleScope.launch {
       //        Logy.d("runnable", "Adding textview to list!");
       //        var tekst: TextView = TextView(requireContext())
       //        tekst.text = "testing123!";
       //        var list: LinearLayout = binding.listMyos;
       //        list.addView(tekst);
       //    }

            //var myo: Myo = it[0];
            //myMyo = myo;
//
            //myo.addConnectionListener(this);
            //myo.connect();
            //myo.writeUnlock(
            //    MyoCmds.UnlockType.HOLD
            //) {
            //    myo, _ -> myo.writeVibrate(MyoCmds.VibrateType.LONG, null)
//
            //    myo.writeLEDs(
            //        Color.valueOf(0f, 0f, 255f),
            //        Color.valueOf(0f, 255f, 0f),
            //        null
            //    );
            //}
        //});
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onConnectionStateChanged(myo: BaseMyo, state: BaseMyo.ConnectionState) {
        Logy.d("ConnectionStateChanged", "Connection changed to: $state");
        if (view == null) return

        //if (state == BaseMyo.ConnectionState.DISCONNECTED) {
        //    requireView().post { mContainer.removeView(mMyoViewMap.get(myo)) }
        //}
    }
}