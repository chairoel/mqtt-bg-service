package com.amrul.mymqttapps.mqtt

import android.content.Context
import android.util.Log
import info.mqtt.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.lang.ref.WeakReference

class MQTTClientNew private constructor(
    context: Context,
    private val brokerUrl: String,
    private val clientId: String
) {
    // WeakReference untuk menghindari memory leak
    private val contextRef = WeakReference(context.applicationContext)

    private var mqttAndroidClient: MqttAndroidClient? = null
    private var isConnected = false

    companion object {
        @Volatile
        private var instance: MQTTClientNew? = null

        fun getInstance(context: Context, brokerUrl: String, clientId: String): MQTTClientNew {
            return synchronized(this) {
                instance?.let {
                    if (it.contextRef != context.applicationContext) {
                        it.release()
                        instance = null
                    }
                }
                instance ?: MQTTClientNew(context.applicationContext, brokerUrl, clientId).also {
                    instance = it
                }
            }
        }
    }

    fun connect(onConnected: () -> Unit, onConnectionFailed: (Throwable) -> Unit) {
        if (isConnected) {
            Log.d("MQTTClient", "Already connected")
            return
        }

        val context = contextRef.get() ?: run {
            Log.e("MQTTClient", "Context is null. Re-initializing client.")
            onConnectionFailed(IllegalStateException("Context is null"))
            return
        }

        mqttAndroidClient = MqttAndroidClient(context, brokerUrl, clientId).apply {
            setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    this@MQTTClientNew.isConnected = false
                    Log.d("MQTTClient", "Connection lost: ${cause?.message}")
                    reconnect()
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val payload = message?.toString() ?: "No message"
                    Log.d("MQTTClient", "Message arrived on topic $topic: $payload")
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d("MQTTClient", "Delivery complete: $token")
                }
            })
        }

        try {
            val token = mqttAndroidClient!!.connect()
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    isConnected = true
                    Log.d("MQTTClient", "Connected to broker")
                    onConnected()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    isConnected = false
                    Log.d("MQTTClient", "Failed to connect to broker: ${exception.message}")
                    onConnectionFailed(exception)
                }
            }
        } catch (e: MqttException) {
            isConnected = false
            onConnectionFailed(e)
        }
    }

    fun disconnect(onResult: (Boolean) -> Unit) {
        if (!isConnected || mqttAndroidClient == null) {
            Log.d("MQTTClient", "Already disconnected or client is null")
            onResult(false)
            return
        }

        try {
            mqttAndroidClient?.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    isConnected = false
                    Log.d("MQTTClient", "Disconnected from broker")
                    release()
                    onResult(true)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d("MQTTClient", "Failed to disconnect from broker")
                    onResult(false)
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
            onResult(false)
        }
    }

    private fun reconnect() {
        if (mqttAndroidClient == null) {
            Log.d("MQTTClient", "Re-initializing MQTT client before reconnecting")
            val context = contextRef.get() ?: return
            mqttAndroidClient = MqttAndroidClient(context, brokerUrl, clientId)
        }

        try {
            val token = mqttAndroidClient!!.connect()
            token.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    isConnected = true
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

    fun release() {
        Log.d("MQTTClient", "Releasing MQTT client resources")
        mqttAndroidClient?.apply {
            try {
                disconnect()
                close()
                Log.d("MQTTClient", "MQTT client closed successfully")
            } catch (e: Exception) {
                Log.e("MQTTClient", "Failed to close MQTT client: ${e.message}")
            }
        }
        mqttAndroidClient = null
        instance = null
    }

    private var messageListener: ((topic: String?, message: String) -> Unit)? = null

    fun setMessageListener(listener: (topic: String?, message: String) -> Unit) {
        messageListener = listener
    }

    fun subscribe(topic: String, qos: Int = 1) {
        try {
            mqttAndroidClient?.subscribe(topic, qos, null, object : IMqttActionListener {
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
            mqttAndroidClient?.unsubscribe(topic, null, object : IMqttActionListener {
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
            mqttAndroidClient?.publish(topic, mqttMessage, null, object : IMqttActionListener {
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