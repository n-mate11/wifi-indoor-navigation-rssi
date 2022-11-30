package com.example.thesisandroidapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlin.collections.ArrayList
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private val arrayList: ArrayList<String> = ArrayList()
    private var adapter: ArrayAdapter<*>? = null
    private var listView: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startScan = findViewById<Button>(R.id.scanButton)
        listView = findViewById(R.id.wifiList)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayList)
        listView!!.setAdapter(adapter)

        startScan.setOnClickListener {
            onStartScan()
        }
    }

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val wifiMan = context!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val scanResults = wifiMan.scanResults
            var resultText = ""
            Log.i("scan", scanResults.toString())
            for (scanResult in scanResults) {
                if (scanResult.SSID == "VU-Campusnet") {
                    resultText = "SSID: " + scanResult.SSID + "\n" +
                                 "BSSID: " + scanResult.BSSID + "\n" +
                                 "frequency: " + scanResult.frequency + "\n" +
                                 "level: " + scanResult.level + "\n" +
                                 "distance: " + calculateDistanceRSSI(scanResult.level) + "\n"
                    arrayList.add(resultText)
                    adapter!!.notifyDataSetChanged()
                }
            }
        }
    }

    private fun calculateDistanceRSSI(RSSILevel: Int): Double {
        val n = 2.7
        val distance = 10.0.pow(((53 - RSSILevel) / 10 * n))
        return distance
    }

    private fun checkIfHasPermission(permission: String): Boolean {
        val res: Int = applicationContext.checkCallingOrSelfPermission(permission)
        return res == PackageManager.PERMISSION_GRANTED
    }

    private fun onStartScan() {
        val wifiMan = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiMan.isScanAlwaysAvailable && !wifiMan.isWifiEnabled) {
            Log.i("wifi", "wifi is not enabled")
            wifiMan.setWifiEnabled(true)
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
        } catch (e: Exception) {
            Log.i("scan","Wi-Fi scan results receiver already unregistered")
        }
    }

}