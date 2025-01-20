package com.amrul.mymqttapps.views

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.amrul.mymqttapps.MQTTClient
import com.amrul.mymqttapps.databinding.ActivityMqttBinding

class MqttActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMqttBinding
    private lateinit var mqttClient: MQTTClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMqttBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

            btnDisconnect.setOnClickListener {
                mqttClient.disconnect { isSuccess ->
                    if (isSuccess) {
                        Log.d(TAG, "Disconnected successfully")
                        runOnUiThread {
                            "Disconnect".also { tvStatusConnect.text = it }
                            binding.tvData.text = ""
                        }
                    } else {
                        Log.e(TAG, "Failed to disconnect")
                        runOnUiThread {
                            "Disconnect Failed".also { tvStatusConnect.text = it }
                        }
                    }
                }
            }
        }

    }

    companion object {
        val TAG: String = MqttActivity::class.java.simpleName
        const val SERVER_URL = "tcp://track.transjakarta.co.id:1883" // Server hostname dan port
        const val CLIENT_ID = "001" // Ganti dengan ID unik
        const val SUBSCRIBE_TOPIC = "/bus/MYS-17029" // Topic yang akan disubscribe
    }
}