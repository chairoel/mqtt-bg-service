package com.amrul.mymqttapps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.amrul.mymqttapps.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mqttClient: MQTTClient

    // Mendeklarasikan launcher untuk permintaan izin lokasi (ACCESS_FINE_LOCATION dan ACCESS_COARSE_LOCATION)
    private val requestLocationPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationPermissionGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (locationPermissionGranted) {
            // Izin lokasi diberikan, lanjutkan untuk meminta izin background location
            requestBackgroundLocationPermission()
        } else {
            // Izin lokasi ditolak
            Log.d("MainActivity", "Izin lokasi tidak diberikan")
            Toast.makeText(this, "Izin lokasi diperlukan", Toast.LENGTH_SHORT).show()
        }
    }

    // Mendeklarasikan launcher untuk permintaan izin background location (ACCESS_BACKGROUND_LOCATION)
    private val requestBackgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Izin ACCESS_BACKGROUND_LOCATION diberikan, lanjutkan ke permintaan izin notifikasi jika diperlukan
            requestNotificationPermission()
        } else {
            // Izin ACCESS_BACKGROUND_LOCATION ditolak
            Log.d("MainActivity", "Izin ACCESS_BACKGROUND_LOCATION tidak diberikan")
            Toast.makeText(this, "Izin lokasi latar belakang diperlukan", Toast.LENGTH_SHORT).show()
        }
    }

    // Mendeklarasikan launcher untuk permintaan izin notifikasi (POST_NOTIFICATIONS)
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Izin notifikasi diberikan, mulai service lokasi
            startLocationService()
        } else {
            // Izin notifikasi ditolak
            Log.d("MainActivity", "Izin notifikasi tidak diberikan")
            Toast.makeText(this, "Izin notifikasi diperlukan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        cekPermission()
        mqttClient = MQTTClient(this, SERVER_URL, CLIENT_ID)

        binding.apply {
            SERVER_URL.also { tvServer.text = it }
            btnConnect.setOnClickListener {
                mqttClient.connect(
                    onConnected = {
                        runOnUiThread {
                            "Connected".also { binding.tvStatusConnect.text = it }
                        }
                        mqttClient.subscribe(SUBSCRIBE_TOPIC)
                    },
                    onConnectionFailed = {
                        runOnUiThread {
                            "Connection failed".also { tvStatusConnect.text = it }
                        }
                    }
                )

                mqttClient.setMessageListener { topic, message ->
                    runOnUiThread {
                        binding.tvData.append("Message from $topic: $message\n")
                    }
                }
            }
        }

//        val _btn2 = findViewById<Button>(R.id.streaming_publishtext)
//        var _input = findViewById<EditText>(R.id.streaming_textinput)
//        _btn2.setOnClickListener {
//            publish("galihashari", _input.text.toString())
//        }
    }

    private fun cekPermission() {
        // Memeriksa apakah izin ACCESS_FINE_LOCATION sudah diberikan
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Izin lokasi sudah diberikan, lanjutkan ke permintaan ACCESS_BACKGROUND_LOCATION jika perangkat memenuhi syarat
            requestBackgroundLocationPermission()
        } else {
            // Minta izin lokasi
            requestLocationPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun requestBackgroundLocationPermission() {
        // Pengecekan API level terlebih dahulu sebelum meminta izin background location
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Jika SDK >= 29 (Android 10), minta izin ACCESS_BACKGROUND_LOCATION
            requestBackgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            // Jika SDK < 29 (Android 10), lanjutkan tanpa meminta izin background location
            requestNotificationPermission()
        }
    }

    private fun requestNotificationPermission() {
        // Pengecekan API level untuk izin POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Jika SDK >= 33 (Android 13), minta izin POST_NOTIFICATIONS
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Jika SDK < 33, langsung mulai service lokasi tanpa izin notifikasi
            startLocationService()
        }
    }

    private fun startLocationService() {
        // Memulai service lokasi
        val serviceIntent = Intent(this, LocationService::class.java)
        startService(serviceIntent)
    }

    companion object {
        const val SERVER_URL = "tcp://track.transjakarta.co.id:1883" // Server hostname dan port
        const val CLIENT_ID = "001" // Ganti dengan ID unik
        const val SUBSCRIBE_TOPIC = "/bus/MYS-17029" // Topic yang akan disubscribe
    }
}
