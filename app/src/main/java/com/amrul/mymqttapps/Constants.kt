package com.amrul.mymqttapps

object Constants {
    const val MQTT_CONFIG = "MQTTConfig"

    const val SERVER_URL = "SERVER URL"
    const val CLIENT_ID = "CLIENT ID"
    const val PUBLISH_TOPIC = "PUBLISH TOPIC"

    // MQTT CONFIG DEFAULT
    const val SERVER_URL_DEFAULT = "tcp://track.transjakarta.co.id:1883" // Server hostname dan port
    const val CLIENT_ID_DEFAULT = "001" // Ganti dengan ID unik
    const val PUBLISH_TOPIC_DEFAULT = "/tracking/obu/TEST"
}