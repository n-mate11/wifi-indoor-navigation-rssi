package com.example.thesisandroidapp

import android.content.*
import android.graphics.*
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date


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

    private var startTimeStamp: Long = 0
    private var endTimeStamp: Long = 0
    private var scanCounter: Int = 0

    private var addresses: List<MAC> = listOf()

    private val wifiManager: WifiManager by lazy {
        requireActivity().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private var startScanButton: FloatingActionButton? = null
    private var stopScanButton: FloatingActionButton? = null

    private var startTimeTextView: TextView? = null
    private var endTimeTextView: TextView? = null

    private var scanCounterTextView: TextView? = null

    private var scanResultList = mutableListOf<List<ScanResult>>()

    private val fileName = "scan1.csv"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_movescan, container, false)
        val macAddresses = (activity as MainActivity).getJsonDataFromRaw()
        val gson = Gson()
        val listMACType = object : TypeToken<List<MAC>>() {}.type
        addresses = gson.fromJson(macAddresses, listMACType)

        startScanButton = view.findViewById(R.id.startScanButton)
        stopScanButton = view.findViewById(R.id.stopScanButton)
        startTimeTextView = view.findViewById(R.id.startTimeTextView)
        endTimeTextView = view.findViewById(R.id.stopTimeTextView)
        scanCounterTextView = view.findViewById(R.id.scanCountTextView)

        scanCounterTextView?.text = "Scan counter: $scanCounter"

        stopScanButton?.isEnabled = false
        stopScanButton?.isClickable = false

        startScanButton?.setOnClickListener {
            startTimeStamp = System.currentTimeMillis()
            val date = Date(startTimeStamp)
            startTimeTextView?.text = "Start time: $date"
            startScanButton?.isEnabled = false
            startScanButton?.isClickable = false
            stopScanButton?.isEnabled = true
            stopScanButton?.isClickable = true
            scanWifiSignals()
        }

        stopScanButton?.setOnClickListener {
            context?.unregisterReceiver(wifiScanReceiver)
            endTimeStamp = System.currentTimeMillis()
            val date = Date(endTimeStamp)
            endTimeTextView?.text = "End time: $date"
            startScanButton?.isEnabled = true
            startScanButton?.isClickable = true
            stopScanButton?.isEnabled = false
            stopScanButton?.isClickable = false
            writeToCsvFile()
        }

        return view
    }

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                val wifiList = wifiManager.scanResults
                scanResultList.add(wifiList)
                scanCounter++
                scanCounterTextView?.text = "Scan counter: $scanCounter"

                for (scanResult in wifiList) {
                    Log.d(
                        "WifiScan",
                        "SSID: ${scanResult.SSID}, BSSID: ${scanResult.BSSID}, RSSI: ${scanResult.level}, Frequency: ${scanResult.frequency}, Timestamp: ${scanResult.timestamp}"
                    )
                }

                wifiManager.startScan()
            } else {
                Log.e("WifiScan", "Error scanning wifi")
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeToCsvFile() {
        val content = StringBuilder()
        for (address in addresses) {
            content.append("${address.name},${address.name}_timestamp,")
        }
        content.append("startTimestamp, stopTimestamp\n")
        val row = StringBuilder()
        for (scanResult in scanResultList) {
            for (address in addresses) {
                val scanResultForAddress =
                    scanResult.find { scan -> scan.BSSID == address.mac }
                if (scanResultForAddress != null) {
                    row.append("${scanResultForAddress.level},${scanResultForAddress.timestamp},")
                } else {
                    row.append("0,0,")
                }
            }
            row.append("${startTimeStamp},${endTimeStamp}\n")
            content.append(row.toString())
        }

        Log.d("content", content.toString())

        val resolver = context?.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/csv")
        }

        val uri = resolver?.insert(
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            contentValues
        )
        uri?.let {
            resolver.openOutputStream(it, "wa")?.use { outputStream ->
                outputStream.write(content.toString().toByteArray())
                outputStream.flush()
            }
        }
    }

    private fun scanWifiSignals() {
        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
        }

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

        val locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        if (!locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context?.registerReceiver(wifiScanReceiver, intentFilter)

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