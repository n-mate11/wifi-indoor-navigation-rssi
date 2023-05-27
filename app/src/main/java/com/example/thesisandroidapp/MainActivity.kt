package com.example.thesisandroidapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.IOException

class MainActivity : AppCompatActivity() {

    fun getJsonDataFromRaw(): String? {
        val jsonString: String
        try {
            jsonString = resources.openRawResource(R.raw.mac_addresses).bufferedReader()
                .use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return null
        }
        return jsonString
    }

    private fun setCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment,fragment)
            commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        val moveScanFragment = MoveScanFragment()
        val scanFragment = ScanFragment()

        setCurrentFragment(scanFragment)

        bottomNavigationView.setOnItemSelectedListener {
            when(it.itemId) {
                R.id.scan -> setCurrentFragment(scanFragment)
                R.id.moveScan -> setCurrentFragment(moveScanFragment)
            }
            true
        }
    }
}