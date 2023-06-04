package com.example.thesisandroidapp

import android.annotation.SuppressLint
import android.content.*
import android.graphics.*
import android.net.Uri
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
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
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

    private lateinit var wifiManager: WifiManager

    private var startScanButton: FloatingActionButton? = null
    private var stopScanButton: FloatingActionButton? = null

    private var startTimeTextView: TextView? = null
    private var endTimeTextView: TextView? = null

    private var scanCounterTextView: TextView? = null

    private var scanResultList = mutableListOf<List<ScanResult>>()
    private var scanTimeList = mutableListOf<Long>()

    private val fileName = "scan.csv"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    @SuppressLint("SetTextI18n")
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

        wifiManager =
            context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager

        checkAndRequestPermissions()

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

            context?.registerReceiver(
                wifiScanReceiver,
                IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            )
            val success = wifiManager.startScan()
            if (!success) {
                // scan failure handling
                Toast.makeText(context, "First Scan failed", Toast.LENGTH_SHORT).show()
            }
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

            scanCounter = 0
            scanCounterTextView?.text = "Scan counter: $scanCounter"

            scanResultList.clear()
            scanTimeList.clear()

            startTimeStamp = 0
            endTimeStamp = 0

            startTimeTextView?.text = "Start time: "
            endTimeTextView?.text = "End time: "

            Toast.makeText(context, "Scan finished", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun checkAndRequestPermissions() {
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
    }

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                Log.d("ScanResult", "Scan updated successfully")
                val results = wifiManager.scanResults
                scanResultList.add(results)
                scanTimeList.add(System.currentTimeMillis())
                scanCounter++
                scanCounterTextView?.text = "Scan counter: $scanCounter"
            }
            wifiManager.startScan()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        context?.unregisterReceiver(wifiScanReceiver)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeToCsvFile() {
        val content = StringBuilder()
        val header = StringBuilder()
        for (address in addresses) {
            header.append("${address.name},${address.name}_timestamp,")
        }
        header.append("startTimestamp,stopTimestamp\n")

        val rows = StringBuilder()
        scanResultList.forEachIndexed { index, scanResults ->
            for (address in addresses) {
                val scan = scanResults.find { scan -> scan.BSSID == address.mac }
                if (scan != null) {
                    rows.append("${scan.level},${scan.timestamp},")
                } else {
                    rows.append("0,0,")
                }
            }
            rows.append("$startTimeStamp,$endTimeStamp,${scanTimeList[index]}\n")
        }

        val resolver = context?.contentResolver

        content.append(header)
        content.append(rows)

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