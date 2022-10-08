package com.tech7fox.myoassistant

import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.tech7fox.myoassistant.databinding.FragmentScanBinding
import com.tech7fox.myolink.Myo
import com.tech7fox.myolink.processor.classifier.ClassifierProcessor
import com.tech7fox.myolink.tools.Logy
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

    fun showMyos(myos: HashMap<String, String>) {
        lifecycleScope.launch {
            binding.newMyos.removeAllViews()
            for (myo in myos) {
                binding.newMyos.addView(createDeviceView(myo.value, myo.key))
            }
        }
    }

    private fun createDeviceView(name: String, address: String) : MyoDeviceView {
        val deviceView: MyoDeviceView = LayoutInflater.from(requireActivity()).inflate(R.layout.view_myodevice, binding.newMyos, false) as MyoDeviceView
        deviceView.setStats(name, address)

        val listener: View.OnClickListener = View.OnClickListener() {
            if (!mService.savedMyos.containsKey(address)) mService.savedMyos[address] = null
            Logy.w("added myo", address)
            mService.saveMyos()
            Toast.makeText(context, "${(it as MyoDeviceView).deviceName} Added.", Toast.LENGTH_SHORT).show()
            (it.parent as LinearLayout).removeView(it)
        }

        deviceView.setOnClickListener(listener)
        return deviceView
    }
}