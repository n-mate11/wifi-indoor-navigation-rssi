package com.example.thesisandroidapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private var wifiList: ListView? = null
    private var buttonScan: Button? = null
    private var results: List<ScanResult>? = null
    private var arrayList: ArrayList<String> = ArrayList()
    private var adapter: ArrayAdapter<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        buttonScan = findViewById(R.id.scanButton)
        buttonScan!!.setOnClickListener {
            scanWifi()
        }

        wifiList = findViewById(R.id.wifiList)
        wifiManager = (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?)!!

        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show()
            wifiManager.setWifiEnabled(true)
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayList)
        wifiList!!.setAdapter(adapter)
        scanWifi()
        Log.i("results", "done with scanning function")
    }

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i("results", "should ge results")
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            Log.i("results", success.toString())
            results = wifiManager.getScanResults()
            unregisterReceiver(this)

            Log.i("results", results.toString())
            for (scanResult in (results as MutableList<ScanResult>?)!!) {
                arrayList.add(scanResult.SSID + " - " + scanResult.capabilities)
                adapter!!.notifyDataSetChanged()
            }
        }
    }

    private fun scanWifi() {
        arrayList.clear()
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        applicationContext.registerReceiver(wifiReceiver, intentFilter)
        wifiManager.startScan()
        Log.i("results", "started scanning")
        Toast.makeText(this, "Scanning WiFi ...", Toast.LENGTH_SHORT).show()
    }
}