package com.example.thesisandroidapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import java.lang.StringBuilder

class WifiReciever(var wifiManager: WifiManager, wifiDeviceList: ListView): BroadcastReceiver() {

    val TAG = "reciever"
    var sb : StringBuilder? = null
    var wifiDeviceList : ListView
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "on Receive")
        val action = intent!!.action
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION == action) {
            Log.i(TAG, "scan results available")
            var wifiList: List<ScanResult> = wifiManager.scanResults
            var deviceList: ArrayList<String> = ArrayList()
            for (scanResults in wifiList) {
                Log.i("scanResults",scanResults.toString())
                Log.i("deviceList", deviceList.toString())
            }
            val arrayAdapter: ArrayAdapter<*> =
                ArrayAdapter(context!!.applicationContext,
                             android.R.layout.simple_list_item_1,
                             deviceList.toArray())
            wifiDeviceList.adapter = arrayAdapter

            Log.i(TAG, "adapter done")
        }
    }
    init {
        this.wifiDeviceList = wifiDeviceList
    }
}