package com.amrul.mymqttapps

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager
    private val PERMISSION_REQUEST_CODE: Int
        get() = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Meminta izin lokasi jika belum diberikan
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun startLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L, // Interval 5 detik
                0f,   // Jarak minimal 10 meter
                locationListener
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // LocationListener untuk menerima pembaruan lokasi
    private val locationListener = LocationListener { location ->
        val latitude = location.latitude
        val longitude = location.longitude
        Log.d("Location", "Latitude: $latitude, Longitude: $longitude")
    }

    // Menangani hasil permintaan izin
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Izin diberikan, mulai pembaruan lokasi
                startLocationUpdates()
            } else {
                // Izin ditolak
                Log.d("Location", "Izin lokasi ditolak")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Hentikan pembaruan lokasi saat activity tidak aktif
        locationManager.removeUpdates(locationListener)
    }
}