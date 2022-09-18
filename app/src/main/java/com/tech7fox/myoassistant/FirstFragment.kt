package com.tech7fox.myoassistant

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.tech7fox.myoassistant.databinding.FragmentFirstBinding
import com.tech7fox.myolink.Myo
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
            mService.firstFragment = this@FirstFragment;
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

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_scanFragment)
        }

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

    override fun onStop() {
        super.onStop()
        requireContext().unbindService(connection)
        mBound = false
    }

    override fun onResume() {
        Intent(context, MyosService::class.java).also { intent ->
            requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}