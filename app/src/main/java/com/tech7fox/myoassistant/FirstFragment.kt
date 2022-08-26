package com.tech7fox.myoassistant

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tech7fox.myoassistant.databinding.FragmentFirstBinding
import com.tech7fox.myolink.BaseMyo
import com.tech7fox.myolink.BaseMyo.ConnectionListener
import com.tech7fox.myolink.Myo
import com.tech7fox.myolink.MyoCmds
import com.tech7fox.myolink.MyoConnector
import com.tech7fox.myolink.tools.Logy
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private var scanning = false

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var mService: MyosService
    private var mBound: Boolean = false

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as MyosService.LocalBinder
            mService = binder.getService()
            mService.fragment = this@FirstFragment;
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

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



//        lifecycleScope.launch {
//            Logy.w("runnable", "Adding myoview to list!");
//            Handler().postDelayed({
//                myoview.setMyo(myo);
//                binding.listMyos.addView(myoview)
//            }, 5000)
//        }

        super.onViewCreated(view, savedInstanceState)


        //binding.buttonSecond.setOnClickListener {
            //myMyo?.writeVibrate(MyoCmds.VibrateType.SHORT, null)
            //myMyo?.writeLEDs(Color.valueOf(0f, 0f, 255f), Color.valueOf(0f, 0f, 255f), null)
        //}
    }

    override fun onStart() {
        super.onStart()

        Intent(context, MyosService::class.java).also { intent ->
            requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    public fun removeMyo(myo: Myo) {
        for (view in binding.listMyos.children) {
             if ((view as MyoView)._myo.deviceAddress == myo.deviceAddress) {
                 lifecycleScope.launch {
                     Logy.w("removeMyo", "Removing myo ${myo.deviceAddress}!");
                     binding.listMyos.removeView(view);
                 }
             }
        }
    }

    public fun addMyo(myo: Myo) {
        val myoView: MyoView = LayoutInflater.from(requireActivity()).inflate(R.layout.view_myo, binding.listMyos, false) as MyoView
        lifecycleScope.launch {
            Logy.w("runnable", "Adding myoview to list!");
            Handler().postDelayed({
                myoView.setMyo(myo);
                binding.listMyos.addView(myoView)
            }, 10000)
        }
    }

    public fun updateMyos() {
        Logy.w(tag, "Update myos")

        for (myo in mService.myos) {
            val textView = TextView(context)
            textView.text = myo;
            binding.listMyos.addView(textView)
        }

        for (myo in mService.savedMyos) {
            myo.value?.let {
                val myoView: MyoView = LayoutInflater.from(requireActivity()).inflate(R.layout.view_myo, binding.listMyos, false) as MyoView
                lifecycleScope.launch {
                    Logy.w("runnable", "Adding myoview to list!");
                    Handler().postDelayed({
                        myoView.setMyo(it);
                        binding.listMyos.addView(myoView)
                    }, 7000)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        requireContext().unbindService(connection)
        mBound = false
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
}