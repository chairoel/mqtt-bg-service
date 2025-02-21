package com.amrul.mymqttapps.views

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.amrul.mymqttapps.Constants
import com.amrul.mymqttapps.OnSettingsSaveListener
import com.amrul.mymqttapps.databinding.ActivityMqttBackgroundBinding
import com.amrul.mymqttapps.mqtt.MQTTService

class MqttBackgroundActivity : AppCompatActivity(), OnSettingsSaveListener {

    private lateinit var binding: ActivityMqttBackgroundBinding

    private val requestForegroundServicePermissionLauncher =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                startMQTTService() // Jalankan service setelah semua izin diberikan
            } else {
                Toast.makeText(this, "Izin lokasi diperlukan agar service berjalan!", Toast.LENGTH_SHORT).show()
            }
        }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                checkAndRequestForegroundServicePermission() // Setelah notifikasi, lanjut cek izin lokasi
            } else {
                Toast.makeText(this, "Izin notifikasi diperlukan agar service berjalan!", Toast.LENGTH_SHORT).show()
            }
        }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMqttBackgroundBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val (serverUrl, clientId, topic) = loadConfig(this@MqttBackgroundActivity)

        binding.apply {
            tvServer.text = getServerUrl(this@MqttBackgroundActivity)

            btnConnect.setOnClickListener {
                startMQTTService()
            }

            // Tombol Disconnect
            btnDisconnect.setOnClickListener {
                stopMQTTService()
            }

            btnSetting.setOnClickListener {
                val (serverUrl, clientId, topic) = loadConfig(this@MqttBackgroundActivity)
                val bottomSheetFragment =
                    SettingsBottomSheet.newInstance(serverUrl, clientId, topic)
                bottomSheetFragment.onSettingsSaveListener = this@MqttBackgroundActivity
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // Register receiver untuk status koneksi MQTT
        val connectionStatusFilter = IntentFilter(MQTTService.ACTION_CONNECTION_STATUS)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(connectionStatusReceiver, connectionStatusFilter)

        val intentFilter = IntentFilter(MQTTService.ACTION_PUBLISH_DATA)
        LocalBroadcastManager.getInstance(this).registerReceiver(dataReceiver, intentFilter)
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(connectionStatusReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dataReceiver)
    }

    private val connectionStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MQTTService.ACTION_CONNECTION_STATUS) {
                val isConnected = intent.getBooleanExtra(MQTTService.EXTRA_CONNECTION_STATUS, false)
                updateConnectionStatusUI(isConnected)
            }
        }
    }

    private val dataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MQTTService.ACTION_PUBLISH_DATA) {
                val data = intent.getStringExtra(MQTTService.EXTRA_DATA)
                updatePublishedDataUI(data)
            }
        }
    }

    private fun updateConnectionStatusUI(isConnected: Boolean) {
        val statusText =
            if (isConnected) "Connected to MQTT broker" else "Failed to connect to MQTT broker"
        binding.tvStatusConnect.text = statusText
        binding.tvStatusConnect.setTextColor(
            ContextCompat.getColor(
                this,
                if (isConnected) android.R.color.holo_green_dark else android.R.color.holo_red_dark
            )
        )
        Toast.makeText(this, statusText, Toast.LENGTH_SHORT).show()
    }

    private fun updatePublishedDataUI(data: String?) {
        if (data?.isNotEmpty() == true) {
            val formattedData = data
                .replace(",", ",\n")
                .replace("{", "")
                .replace("}", "")
            binding.tvBracketOpen.visibility = View.VISIBLE
            binding.tvBracketClose.visibility = View.VISIBLE
            binding.tvData.text = formattedData
        } else {
            binding.tvBracketOpen.visibility = View.GONE
            binding.tvBracketClose.visibility = View.GONE
            binding.tvData.text = "-"
        }
    }

    private fun startMQTTService() {
        // Cek izin notifikasi di Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        // Cek izin foreground service location di Android 14+ (API 34+)
        checkAndRequestForegroundServicePermission()
    }

    private fun checkAndRequestForegroundServicePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestForegroundServicePermissionLauncher.launch(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            return
        }

        // Jika semua izin sudah diberikan, jalankan service
        val serviceIntent = Intent(this, MQTTService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }



    /* private fun startMQTTService() {
         val serviceIntent = Intent(this, MQTTService::class.java)
         ContextCompat.startForegroundService(this, serviceIntent)
 //        updateUI("Service Started (MQTT + GPS)", true)
 //        Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show()
     }*/

    private fun stopMQTTService() {
        val serviceIntent = Intent(this, MQTTService::class.java)
        stopService(serviceIntent)
        updateUI("Service Stopped", false)
        updatePublishedDataUI("")
        Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show()
    }

    private fun updateUI(status: String, isConnected: Boolean) {
        binding.apply {
            tvStatusConnect.text = status
            tvStatusConnect.setTextColor(
                ContextCompat.getColor(
                    this@MqttBackgroundActivity,
                    if (isConnected) android.R.color.holo_green_dark else android.R.color.holo_red_dark
                )
            )
        }
    }

    private fun getServerUrl(context: Context): String {
        val pref = context.getSharedPreferences(Constants.MQTT_CONFIG, Context.MODE_PRIVATE)
        return pref.getString(Constants.SERVER_URL, Constants.SERVER_URL_DEFAULT) ?: ""
    }

    private fun loadConfig(context: Context): Triple<String, String, String> {
        val pref = context.getSharedPreferences(Constants.MQTT_CONFIG, Context.MODE_PRIVATE)
        val serverUrl = pref.getString(Constants.SERVER_URL, Constants.SERVER_URL_DEFAULT)
        val clientId = pref.getString(Constants.CLIENT_ID, Constants.CLIENT_ID_DEFAULT)
        val publishTopic = pref.getString(Constants.PUBLISH_TOPIC, Constants.PUBLISH_TOPIC_DEFAULT)

        return Triple(serverUrl ?: "", clientId ?: "", publishTopic ?: "")
    }

    private fun saveMQTTConfig(
        context: Context,
        serverUrl: String,
        clientId: String,
        publishTopic: String
    ) {
        val sharedPreferences =
            context.getSharedPreferences(Constants.MQTT_CONFIG, Context.MODE_PRIVATE)

        val editor = sharedPreferences.edit()
        editor.putString(Constants.SERVER_URL, serverUrl)
        editor.putString(Constants.CLIENT_ID, clientId)
        editor.putString(Constants.PUBLISH_TOPIC, publishTopic)
        editor.apply()
    }

    override fun onSettingsSave(serverUrl: String, clientId: String, publishTopic: String) {
        Log.d("SettingsSave", "Server URL: $serverUrl")
        Log.d("SettingsSave", "Client ID: $clientId")
        Log.d("SettingsSave", "Publish Topic: $publishTopic")

        binding.tvServer.text = serverUrl

        saveMQTTConfig(
            context = this,
            serverUrl = serverUrl,
            clientId = clientId,
            publishTopic = publishTopic
        )
    }

    override fun onDestroy() {
        super.onDestroy()
//        stopMQTTService() // Hentikan service jika Activity dihancurkan
    }

    companion object {
        const val SERVER_URL = "tcp://track.transjakarta.co.id:1883" // Server MQTT
        const val CLIENT_ID = "unique_client_id" // ID unik untuk client
    }
}