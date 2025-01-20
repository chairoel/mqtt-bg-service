package com.amrul.mymqttapps.views

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.amrul.mymqttapps.databinding.ActivityMqttBackgroundBinding
import com.amrul.mymqttapps.mqtt.MQTTService

class MqttBackgroundActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMqttBackgroundBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMqttBackgroundBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            tvServer.text = SERVER_URL

            btnConnect.setOnClickListener {
                startMQTTService()
            }

            // Tombol Disconnect
            btnDisconnect.setOnClickListener {
                stopMQTTService()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(MQTTService.ACTION_PUBLISH_DATA)
        LocalBroadcastManager.getInstance(this).registerReceiver(dataReceiver, intentFilter)
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dataReceiver)
    }

    private val dataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MQTTService.ACTION_PUBLISH_DATA) {
                val data = intent.getStringExtra(MQTTService.EXTRA_DATA)
                updatePublishedData(data)
            }
        }
    }

    private fun updatePublishedData(data: String?) {
        val formattedData = data
            ?.replace(",", ",\n")
            ?.replace("{", "{\n")
            ?.replace("}", "\n}") ?: "No data available"
        binding.tvData.text = formattedData
    }

    private fun startMQTTService() {
        val serviceIntent = Intent(this, MQTTService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        updateUI("Service Started (MQTT + GPS)", true)
        Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show()
    }

    private fun stopMQTTService() {
        val serviceIntent = Intent(this, MQTTService::class.java)
        stopService(serviceIntent)
        updateUI("Service Stopped", false)
        updatePublishedData("-")
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

    override fun onDestroy() {
        super.onDestroy()
//        stopMQTTService() // Hentikan service jika Activity dihancurkan
    }

    companion object {
        const val SERVER_URL = "tcp://track.transjakarta.co.id:1883" // Server MQTT
        const val CLIENT_ID = "unique_client_id" // ID unik untuk client
    }
}