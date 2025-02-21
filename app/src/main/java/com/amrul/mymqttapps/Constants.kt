package com.amrul.mymqttapps

object Constants {
    const val MQTT_CONFIG = "MQTTConfig"

    const val SERVER_URL = "SERVER URL"
    const val CLIENT_ID = "CLIENT ID"
    const val PUBLISH_TOPIC = "PUBLISH TOPIC"

    const val BUS_BODY_NO = "BUS BODY NUMBER"
    const val BUS_DEVICE_ID = "BUS DEVICE ID"
    const val SOURCE = "SOURCE"

    // MQTT CONFIG DEFAULT
    const val SERVER_URL_DEFAULT = "tcp://track.transjakarta.co.id:1883" // Server hostname dan port
    const val CLIENT_ID_DEFAULT = "001" // Ganti dengan ID unik
    const val PUBLISH_TOPIC_DEFAULT = "/tracking/obu/TEST"

    // BUS CONFIG DEFAULT
    const val BUS_BODY_NO_DEFAULT = "OBU002"
    const val BUS_DEVICE_ID_DEFAULT = "001"
    const val SOURCE_DEFAULT = "Android-BG-Service"
}