package com.amrul.mymqttapps

interface OnSettingsSaveListener {
    fun onSettingsSave(
        serverUrl: String,
        clientId: String,
        publishTopic: String,
        busBodyNo: String,
        busDeviceId: String,
        source: String
    )
}