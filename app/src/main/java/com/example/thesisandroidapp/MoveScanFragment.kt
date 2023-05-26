package com.example.thesisandroidapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MoveScanFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MoveScanFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var actionButton: FloatingActionButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_movescan, container, false)

        actionButton = view.findViewById(R.id.floatingActionButton)

        actionButton?.setOnClickListener {
            // scan wifi rssi signals
            scanWifiSignals()
        }

        return view
    }

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiList = wifiManager.scanResults

            for (scanResult in wifiList) {
                Log.d(
                    "WifiScan",
                    "SSID: ${scanResult.SSID}, BSSID: ${scanResult.BSSID}, RSSI: ${scanResult.level}, Frequency: ${scanResult.frequency}, Timestamp: ${scanResult.timestamp}"
                )
            }
        }
    }

    private fun scanWifiSignals() {
        // Check if wifi is enabled and enable it if not
        val wifiManager = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
        }

        // check for permissions: ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, CHANGE_WIFI_STATE
        // Request permissions if not granted
        if (requireActivity().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requireActivity().requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
        if (requireActivity().checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requireActivity().requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
                1
            )
        }
        if (requireActivity().checkSelfPermission(android.Manifest.permission.CHANGE_WIFI_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requireActivity().requestPermissions(
                arrayOf(android.Manifest.permission.CHANGE_WIFI_STATE),
                1
            )
        }

        // check if Location services are enabled and enable them if not
        val locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }

        // Register a broadcast listener for SCAN_RESULTS_AVAILABLE_ACTION
        context?.registerReceiver(
            wifiScanReceiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )
        val scan = wifiManager.startScan()
        if (scan) {
            Log.d("WifiScan", "Scanning wifi")
        } else {
            Log.e("WifiScan", "Error scanning wifi")
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MoveScanFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MoveScanFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}