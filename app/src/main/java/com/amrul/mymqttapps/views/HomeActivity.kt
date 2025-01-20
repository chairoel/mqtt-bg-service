package com.amrul.mymqttapps.views

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.amrul.mymqttapps.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {

            btnMqtt.setOnClickListener {
                startActivity(Intent(this@HomeActivity, MqttActivity::class.java))
            }

            btnLocation.setOnClickListener {
                startActivity(Intent(this@HomeActivity, LocationBackgroundActivity::class.java))
            }

            btnMqttBackground.setOnClickListener {
                startActivity(Intent(this@HomeActivity, MqttBackgroundActivity::class.java))
            }

        }
    }
}