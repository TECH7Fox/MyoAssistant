package com.tech7fox.myoassistant

import android.Manifest
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.tech7fox.myoassistant.databinding.FragmentFirstBinding
import com.tech7fox.myolink.BaseMyo
import com.tech7fox.myolink.BaseMyo.ConnectionListener
import com.tech7fox.myolink.Myo
import com.tech7fox.myolink.MyoCmds
import com.tech7fox.myolink.MyoConnector
import com.tech7fox.myolink.tools.Logy


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class FirstFragment : Fragment(), ConnectionListener {

    private var _binding: FragmentFirstBinding? = null
    private var mScanning = false
    private var myMyo: Myo? = null;
    //private var mScannerCallback: MyoConnector.ScannerCallback = MyoConnector.ScannerCallback {
    //    Logy.d("log", "test");
    //}

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Logy.sLoglevel = Logy.VERBOSE;

        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            1
        )

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            myMyo?.writeVibrate(MyoCmds.VibrateType.SHORT, null)
            myMyo?.writeLEDs(Color.valueOf(0f, 0f, 255f), Color.valueOf(0f, 0f, 255f), null)
        }
    }

    override fun onResume() {
        Logy.d("onResume", "triggered!");
        val mMyoConnector = MyoConnector(requireContext());
        mMyoConnector.scan(5000, MyoConnector.ScannerCallback() {
            Logy.d("scannerCallback", "MYOS:" + it.size);
            var myo: Myo = it[0];
            myMyo = myo;

            myo.addConnectionListener(this);
            myo.connect();
            myo.writeUnlock(
                MyoCmds.UnlockType.HOLD
            ) {
                myo, _ -> myo.writeVibrate(MyoCmds.VibrateType.LONG, null)

                myo.writeLEDs(
                    Color.valueOf(0f, 0f, 255f),
                    Color.valueOf(0f, 255f, 0f),
                    null
                );
            }
        });
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