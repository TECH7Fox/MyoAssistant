package com.tech7fox.myoassistant

import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.tech7fox.myoassistant.databinding.FragmentScanBinding
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null

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
            mService.scanFragment = this@ScanFragment
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
        (requireActivity() as MainActivity).setMenuVisibility(false)
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as MainActivity).setMenuVisibility(true)
        _binding = null
    }

    override fun onPause() {
        mService.scanFragment = null
        super.onPause()
    }

    override fun onResume() {
        Intent(context, MyosService::class.java).also { intent ->
            requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        super.onResume()
    }

    fun showMyos(myos: MutableList<String>) {
        lifecycleScope.launch {
            binding.myos.removeAllViews()
            for (myo in myos) {
                val deviceView: MyoDeviceView = LayoutInflater.from(requireActivity()).inflate(R.layout.view_myodevice, binding.myos, false) as MyoDeviceView
                deviceView.setStats(myo, false)
                deviceView.setOnClickListener() {
                    if (!mService.savedMyos.containsKey(myo)) {
                        mService.savedMyos[myo] = null
                    }
                    savePreferences(mService.savedMyos.keys.joinToString(" "))
                }
                binding.myos.addView(deviceView)
            }
        }
    }

    private fun savePreferences(myos: String) {
        val editor: SharedPreferences.Editor = requireContext().getSharedPreferences("myo_assistant", Context.MODE_PRIVATE).edit()
        editor.putString("myos", myos)
        editor.apply()
    }
}