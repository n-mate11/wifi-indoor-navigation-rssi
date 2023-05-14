package com.example.thesisandroidapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileWriter
import java.io.IOException

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ScanFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var startScan: FloatingActionButton? = null
    private val arrayList: ArrayList<String> = ArrayList()
    private var adapter: ArrayAdapter<*>? = null
    private var listView: ListView? = null

    private var x = 0
    private var y = 0
    private var z = 5

    private var counter = 0

    private val fileName = "floor5v8.csv"
    private val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
    private var writer: FileWriter? = null

    private var addresses: List<MAC> = listOf()

    private fun getJsonDataFromRaw(): String? {
        val jsonString: String
        try {
            jsonString = resources.openRawResource(R.raw.mac_addresses).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return null
        }
        return jsonString
    }

    private fun checkIfHasPermission(permission: String): Boolean {
        val res: Int? = activity?.applicationContext?.checkCallingOrSelfPermission(permission)
        return res == PackageManager.PERMISSION_GRANTED
    }

    private fun writeDataToFile(aps: List<ScanResult>) {
        // if fileWrite is not initialized
        if (writer == null) {
            writer = FileWriter(file, true)
            Log.i("file", "File writer was null, initialized")
            val header = StringBuilder()
            for (address in addresses) {
                header.append("${address.name},")
            }
            header.append("x,y,z\n")
            writer?.write(header.toString())
        }

        // Append new data point, row to file
        try {
            val row = StringBuilder()
            var match = false

            for (address in addresses) {
                for (ap in aps) {
                    if (address.mac == ap.BSSID) {
                        match = true
                        row.append("${ap.level},")
                        Log.i("MATCH", "${ap.level}, ${ap.BSSID}")
                    }
                }
                if (!match) {
                    row.append("1,")
                }
                match = false
            }

            row.append("$x,$y,$z\n")
            writer?.write(row.toString())
            Log.i("file", "Wrote to file.")
            Log.i("file", writer.toString())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private val wifiScanReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.Q)
        override fun onReceive(context: Context?, intent: Intent?) {
            arrayList.clear()
            val wifiMan = context!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val scanResults = wifiMan.scanResults
            Log.i("scan", scanResults.toString())

            val aps = scanResults.filter {(it.SSID == "VU-Campusnet")} // && it.frequency < 5000
            Log.i("APs", aps.toString())

            // List APs on screen
            var resultText: String
            for (ap in aps) {
                resultText = "SSID: " + ap.SSID + "\n" +
                        "BSSID: " + ap.BSSID + "\n" +
                        "frequency: " + ap.frequency + "\n" +
                        "level: " + ap.level + "\n" +
                        "Coordinates: $x, $y, $z"

                arrayList.add(resultText)
                adapter!!.notifyDataSetChanged()
            }

            writeDataToFile(aps)

            // toast - we're done scanning and writing to file
            Toast.makeText(requireContext(), "Done scanning!", Toast.LENGTH_SHORT).show()
            startScan?.isEnabled = true
        }
    }

    private fun doStartScan() {
        arrayList.clear()
        activity?.applicationContext?.registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        val wifiMan = activity?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val success = wifiMan.startScan()
        Log.i("scan","Waiting for scanning results")
        if (success) {
            Log.i("scan", "scan successful")
        } else {
            Log.i("scan", "scan failed")
        }
    }

    private fun onStartScan() {
        val wifiMan = activity?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiMan.isScanAlwaysAvailable && !wifiMan.isWifiEnabled) {
            Log.i("wifi", "wifi is not enabled")
            wifiMan.isWifiEnabled = true
            return
        }

        Log.i("wifi","Start network scanning")

        // Network scanning requires access to device location, but we first need to check
        // whether the user has given its permission to use it.
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                1)
        } else if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1)
        } else {
            val coarseLocation = checkIfHasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            val fineLocation = checkIfHasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            val accessWifi = checkIfHasPermission(Manifest.permission.ACCESS_WIFI_STATE)
            val changeWifi = checkIfHasPermission(Manifest.permission.CHANGE_WIFI_STATE)
            val accessNetwork = checkIfHasPermission(Manifest.permission.ACCESS_NETWORK_STATE)
            Log.i("scan coarseLocation",  coarseLocation.toString())
            Log.i("scan fineLocation",  fineLocation.toString())
            Log.i("scan accessWifi",  accessWifi.toString())
            Log.i("scan changeWifi",  changeWifi.toString())
            Log.i("scan accessNetwork",  accessNetwork.toString())

            doStartScan()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Permission granted", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStop() {
        super.onStop()
        // Make sure we unregister our receiver to avoid memory leaks.
        try {
            activity?.applicationContext?.unregisterReceiver(wifiScanReceiver)
            writer?.flush()
            writer?.close()
        } catch (e: Exception) {
            Log.i("scan","Wi-Fi scan results receiver already unregistered")
        }
    }

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
        val view = inflater.inflate(R.layout.fragment_scan, container, false)
        // Inflate the layout for this fragment
        startScan = view.findViewById(R.id.scanButton)

        val counterTextView = view.findViewById<TextView>(R.id.counter)

        val incX = view.findViewById<ExtendedFloatingActionButton>(R.id.plusXButton)
        val decX = view.findViewById<ExtendedFloatingActionButton>(R.id.minusXButton)
        val incY = view.findViewById<ExtendedFloatingActionButton>(R.id.plusYButton)
        val decY = view.findViewById<ExtendedFloatingActionButton>(R.id.minusYButton)

        val xText = view.findViewById<TextView>(R.id.XCoord)
        val yText = view.findViewById<TextView>(R.id.YCoord)
        val zText = view.findViewById<TextView>(R.id.ZCoord)

        val jsonString = getJsonDataFromRaw()
        val gson = Gson()
        val listMACType = object : TypeToken<List<MAC>>() {}.type
        addresses = gson.fromJson(jsonString, listMACType)

        xText.text = "X $x"
        yText.text = "Y $y"
        zText.text = "Z $z"

        listView = view.findViewById(R.id.wifiList)

        adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, arrayList)
        listView!!.adapter = adapter

        incX.setOnClickListener {
            x += 1
            xText.text = "X $x"
        }

        decX.setOnClickListener {
            x -= 1
            xText.text = "X $x"
        }

        incY.setOnClickListener {
            y += 1
            yText.text = "Y $y"
        }

        decY.setOnClickListener {
            y -= 1
            yText.text = "Y $y"
        }
        startScan?.setOnClickListener {
            startScan?.isEnabled = false
            if (counter > 14) {
                counter = 1
            } else {
              counter++
            }
            counterTextView.text = counter.toString()
            onStartScan()
        }

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ScanFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ScanFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}