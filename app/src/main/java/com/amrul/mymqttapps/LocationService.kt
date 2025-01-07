package com.amrul.mymqttapps

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class LocationService : Service() {

    private lateinit var locationManager: LocationManager

    override fun onCreate() {
        super.onCreate()

        // Buat channel notifikasi untuk Foreground Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        startForegroundService()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Mulai pembaruan lokasi
        requestLocationUpdates()
    }

    private fun requestLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_UPDATE_INTERVAL,
                LOCATION_UPDATE_DISTANCE,
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
        Log.d("LocationService", "Latitude: $latitude, Longitude: $longitude")
    }

    // Membuat notifikasi untuk Foreground Service dengan tombol "Stop"
    private fun startForegroundService() {
        val stopIntent = Intent(this, LocationService::class.java).apply {
            action = "STOP_LOCATION_SERVICE"
        }

        // Menambahkan FLAG_IMMUTABLE atau FLAG_MUTABLE pada PendingIntent
        val stopPendingIntent: PendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE // Menggunakan FLAG_IMMUTABLE jika tidak perlu dimodifikasi
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Service")
            .setContentText("Aplikasi sedang melacak lokasi Anda.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Sesuaikan dengan ikon aplikasi Anda
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent) // Tambahkan tombol "Stop"
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Tidak digunakan, karena service ini berjalan di foreground
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hentikan pembaruan lokasi saat service dihentikan
        locationManager.removeUpdates(locationListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_LOCATION_SERVICE") {
            stopSelf() // Hentikan service ketika tombol Stop ditekan
            Log.d("LocationService", "Service dihentikan")
        }
        return START_STICKY
    }

    companion object {
        private const val CHANNEL_ID = "LocationServiceChannel"
        private const val LOCATION_UPDATE_INTERVAL: Long = 5000 // 5 detik
        private const val LOCATION_UPDATE_DISTANCE: Float = 0f // Tidak ada jarak minimal
    }
}