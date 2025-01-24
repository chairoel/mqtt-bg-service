package com.amrul.mymqttapps

interface OnSettingsSaveListener {
    fun onSettingsSave(serverUrl: String, clientId: String, publishTopic: String)
}