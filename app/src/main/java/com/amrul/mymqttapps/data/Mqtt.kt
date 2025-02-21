package com.amrul.mymqttapps.data

data class Mqtt(
	val busBodyNo: String,
	val busDeviceId: String,
	val latitude: Double,
	val longitude: Double,
	val speed: Float,
	val bearing: Float,
	val totalDistance: Double,
	val source: String,
	val deviceTimestamp: Long,
)
