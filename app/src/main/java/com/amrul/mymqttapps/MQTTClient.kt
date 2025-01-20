package com.amrul.mymqttapps

import android.content.Context
import android.util.Log
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage

class MQTTClient(
    private val context: Context,
    private val brokerUrl: String,
    private val clientId: String
) {

    private lateinit var mqttAndroidClient: MqttAndroidClient
    private var isConnected = false // Menyimpan status koneksi

    fun connect(onConnected: () -> Unit, onConnectionFailed: (Throwable) -> Unit) {
        if (isConnected) {
            Log.d("MQTTClient", "Already connected")
            return
        }

        mqttAndroidClient = MqttAndroidClient(context, brokerUrl, clientId)

        mqttAndroidClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                isConnected = false
                Log.d("MQTTClient", "Connection lost: ${cause?.message}")
                reconnect()
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                val payload = message?.toString() ?: "No message"
                Log.d("MQTTClient", "Message arrived on topic $topic: $payload")
                messageListener?.invoke(topic, payload) // Panggil listener jika ada
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d("MQTTClient", "Delivery complete: $token")
            }
        })

        try {
            val token = mqttAndroidClient.connect()
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    isConnected = true // Tandai sebagai terhubung
                    Log.d("MQTTClient", "Connected to broker")
                    onConnected()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    isConnected = false // Tetap tidak terhubung
                    Log.d("MQTTClient", "Failed to connect to broker")
                    onConnectionFailed(exception)
                }
            }
        } catch (e: MqttException) {
            isConnected = false
            onConnectionFailed(e)
        }
    }

    fun disconnect() {
        if (!isConnected) {
            Log.d("MQTTClient", "Already disconnected")
            return
        }

        try {
            mqttAndroidClient.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    isConnected = false // Tandai sebagai tidak terhubung
                    Log.d("MQTTClient", "Disconnected from broker")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d("MQTTClient", "Failed to disconnect from broker")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }


    private fun reconnect() {
        try {
            val token = mqttAndroidClient.connect()
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d("MQTTClient", "Reconnected successfully")
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.e("MQTTClient", "Reconnect failed: ${exception.message}")
                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private var messageListener: ((topic: String?, message: String) -> Unit)? = null

    fun setMessageListener(listener: (topic: String?, message: String) -> Unit) {
        messageListener = listener
    }

    fun subscribe(topic: String, qos: Int = 1) {
        try {
            mqttAndroidClient.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTTClient", "Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d("MQTTClient", "Failed to subscribe to $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun unsubscribe(topic: String) {
        try {
            mqttAndroidClient.unsubscribe(topic, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTTClient", "Unsubscribed from $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d("MQTTClient", "Failed to unsubscribe from $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun publish(topic: String, message: String, qos: Int = 1, retained: Boolean = false) {
        try {
            val mqttMessage = MqttMessage(message.toByteArray()).apply {
                this.qos = qos
                this.isRetained = retained
            }
            mqttAndroidClient.publish(topic, mqttMessage, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("MQTTClient", "Message published to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d("MQTTClient", "Failed to publish message to $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
}