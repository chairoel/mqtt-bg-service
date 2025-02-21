package com.amrul.mymqttapps.mqtt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.amrul.mymqttapps.Constants
import com.amrul.mymqttapps.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MQTTService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var mqttClient: MQTTClientNew
    private lateinit var locationManager: LocationManager
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var publishTopic: String

    override fun onCreate() {
        super.onCreate()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTTService::WakeLock")
        wakeLock.acquire(10 * 60 * 1000L /* 10 menit */)


        val (serverUrl, clientId, topic) = loadConfig()
        publishTopic = topic
        mqttClient = MQTTClientNew.getInstance(this, serverUrl, clientId)

        // Inisialisasi MQTTClient
//        mqttClient = MQTTClientNew.getInstance(this, SERVER_URL_DEFAULT, CLIENT_ID_DEFAULT)

        // Inisialisasi LocationManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        connectToMQTT()
//        startLocationUpdates()
    }

    private fun loadConfig(): Triple<String, String, String> {
        val pref = getSharedPreferences(Constants.MQTT_CONFIG, Context.MODE_PRIVATE)
        val serverUrl = pref.getString(Constants.SERVER_URL, Constants.SERVER_URL_DEFAULT)
        val clientId = pref.getString(Constants.CLIENT_ID, Constants.CLIENT_ID_DEFAULT)
        val publishTopic = pref.getString(Constants.PUBLISH_TOPIC, Constants.PUBLISH_TOPIC_DEFAULT)

        return Triple(serverUrl ?: "", clientId ?: "", publishTopic ?: "")
    }

    private fun connectToMQTT() {
        mqttClient.connect(
            onConnected = {
                Log.d("MQTTService", "Connected to MQTT broker")
                sendConnectionStatusBroadcast(true)

                // Mulai pembaruan lokasi setelah koneksi berhasil
                serviceScope.launch {
                    startLocationUpdates()
                }
            },
            onConnectionFailed = { exception ->
                Log.e("MQTTService", "Failed to connect to MQTT broker: ${exception.message}")
                sendConnectionStatusBroadcast(false)
            }
        )
    }

    private fun startLocationUpdates() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("MQTTService", "GPS permission not granted")
            return
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_UPDATE_INTERVAL,
                LOCATION_UPDATE_DISTANCE,
                locationListener
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            Log.e("MQTTService", "GPS permission not granted: ${e.message}")
        }
    }

    private val locationListener = LocationListener { location ->
        publishLocation(location)
    }

    private fun sendConnectionStatusBroadcast(isConnected: Boolean) {
        val intent = Intent(ACTION_CONNECTION_STATUS).apply {
            putExtra(EXTRA_CONNECTION_STATUS, isConnected)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun publishLocation(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        val payload = """{"latitude":$latitude,"longitude":$longitude}"""

        mqttClient.publish(publishTopic, payload, qos = 1, retained = false)
        Log.d("MQTTService", "Published location: $payload")

        // Kirim broadcast lokal
        val intent = Intent(ACTION_PUBLISH_DATA).apply {
            putExtra(EXTRA_DATA, payload)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()

        serviceScope.cancel()

        // Hentikan foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            stopForeground(STOP_FOREGROUND_REMOVE) // API 33+
        } else {
            @Suppress("DEPRECATION") // Tambahkan anotasi ini untuk menekan peringatan deprecasi
            stopForeground(true) // Kompatibilitas untuk API level lama
        }

        if (::mqttClient.isInitialized) { // Periksa apakah mqttClient diinisialisasi
            mqttClient.disconnect { isSuccess ->
                if (isSuccess) {
                    Log.d("MQTTService", "Disconnected from MQTT broker")
                }
            }
            mqttClient.release()
        }

        if (::locationManager.isInitialized) { // Periksa apakah locationManager diinisialisasi
            locationManager.removeUpdates(locationListener)
        }

        if (wakeLock.isHeld) {
            Log.d("MQTTService", "Releasing WakeLock")
            wakeLock.release()
        }

        Log.d("MQTTService", "Service destroyed")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Periksa izin foreground service di Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(
                "MQTTService",
                "Izin FOREGROUND_SERVICE_LOCATION tidak diberikan. Service dihentikan."
            )
            stopSelf()
            return START_NOT_STICKY
        }

        // **Pastikan Notifikasi Dibuat Sebelum startForeground**
        val notification =
            createNotification("MQTT Service Running", "Sending location to MQTT broker...")
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(title: String, content: String): Notification {
        val channelId = "mqtt_service_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "MQTT Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, MQTTService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Stop",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val LOCATION_UPDATE_INTERVAL: Long = 5000 // 5 detik
        private const val LOCATION_UPDATE_DISTANCE: Float = 0f // Tidak ada jarak minimal
        private const val ACTION_STOP_SERVICE = "STOP_MQTT_SERVICE"

//        const val SERVER_URL_DEFAULT = "tcp://track.transjakarta.co.id:1883" // Server hostname dan port
//        const val CLIENT_ID_DEFAULT = "001" // Ganti dengan ID unik
//        const val PUBLISH_TOPIC_DEFAULT = "/tracking/obu/TEST"

        const val ACTION_CONNECTION_STATUS = "ACTION_CONNECTION_STATUS"
        const val EXTRA_CONNECTION_STATUS = "EXTRA_CONNECTION_STATUS"

        const val ACTION_PUBLISH_DATA = "PUBLISH_DATA"
        const val EXTRA_DATA = "EXTRA_DATA"

        private const val NOTIFICATION_ID = 1
    }
}