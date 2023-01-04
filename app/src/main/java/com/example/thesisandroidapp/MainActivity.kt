package com.example.thesisandroidapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.FileWriter
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private val arrayList: ArrayList<String> = ArrayList()
    private var adapter: ArrayAdapter<*>? = null
    private var listView: ListView? = null
    private val numMeasurements = 10
    private var x = 0
    private var y = 0

    private val AP04333 = ""
    private val AP04096 = "5c:5a:c7:85:5c:2f"
    private val AP04097 = "5c:5a:c7:66:46:8f" // not sure yet freq
    private val AP04098 = ""
    private val AP04099 = "5c:5a:c7:85:69:00" // not sure yet freq
    private val AP04100 = "5c:5a:c7:72:a9:0f" // not sure yet freq

    var writer: FileWriter? = null

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startScan = findViewById<FloatingActionButton>(R.id.scanButton)

        val incX = findViewById<ExtendedFloatingActionButton>(R.id.plusXButton)
        val decX = findViewById<ExtendedFloatingActionButton>(R.id.minusXButton)
        val incY = findViewById<ExtendedFloatingActionButton>(R.id.plusYButton)
        val decY = findViewById<ExtendedFloatingActionButton>(R.id.minusYButton)

        val xText = findViewById<TextView>(R.id.XCoord)
        val yText = findViewById<TextView>(R.id.YCoord)

        xText.text = "X $x"
        yText.text = "Y $y"

        listView = findViewById(R.id.wifiList)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayList)
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

        startScan.setOnClickListener {
            onStartScan()
        }
    }

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val wifiMan = context!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val scanResults = wifiMan.scanResults
            var resultText: String
            Log.i("scan", scanResults.toString())

            Log.d("File", "Writing to " + getStorageDir())
            try {
                writer = FileWriter(File(getStorageDir(),"scan.csv"), true
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }

            for (scanResult in scanResults) {
                //if (scanResult.SSID == "VU-Campusnet" && (scanResult.BSSID == AP04096 || scanResult.BSSID == AP04097 || scanResult.BSSID == AP04099)) {
                if (scanResult.SSID == "Mezga5" || scanResult.SSID == "Mezga1" || scanResult.SSID == "Geza") {
                    resultText = "SSID: " + scanResult.SSID + "\n" +
                            "BSSID: " + scanResult.BSSID + "\n" +
                            "frequency: " + scanResult.frequency + "\n" +
                            "level: " + scanResult.level + "\n" +
                            "Coordinates: $x, $y"

                    arrayList.add(resultText)
                    adapter!!.notifyDataSetChanged()

                    val data = "${scanResult.level},$x,$y,0\n"
                    writer?.write(data)

                }
            }
        }
    }

    private fun checkIfHasPermission(permission: String): Boolean {
        val res: Int = applicationContext.checkCallingOrSelfPermission(permission)
        return res == PackageManager.PERMISSION_GRANTED
    }

    private fun onStartScan() {
        val wifiMan = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiMan.isScanAlwaysAvailable && !wifiMan.isWifiEnabled) {
            Log.i("wifi", "wifi is not enabled")
            wifiMan.isWifiEnabled = true
            return
        }

        Log.i("wifi","Start network scanning")

        // Network scanning requires access to device location, but we first need to check
        // whether the user has given its permission to use it.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                1)
        } else if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
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

    private fun doStartScan() {
        arrayList.clear()
        applicationContext.registerReceiver(wifiScanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

        val wifiMan = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val success = wifiMan.startScan()
        Log.i("scan","Waiting for scanning results")
        if (success) {
            Log.i("scan", "scan successful")
        } else {
            Log.i("scan", "scan failed")
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStop() {
        super.onStop()
        // Make sure we unregister our receiver to avoid memory leaks.
        try {
            applicationContext.unregisterReceiver(wifiScanReceiver)
            writer?.close()
        } catch (e: Exception) {
            Log.i("scan","Wi-Fi scan results receiver already unregistered")
        }
    }

    private fun getStorageDir(): String? {
        return getExternalFilesDir(null)!!.absolutePath
    }
}