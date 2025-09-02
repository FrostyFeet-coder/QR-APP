package com.example.qrcodegenration

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity

class NavigationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navData = intent.getStringExtra("NAV_DATA")

        if (navData != null) {
            val uri = Uri.parse(navData)

            val lat = uri.getQueryParameter("lat")
            val lng = uri.getQueryParameter("lng")

            if (lat != null && lng != null) {
                val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Parked Car)")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

                try {
                    startActivity(mapIntent) // opens in any available maps app
                } catch (e: Exception) {
                    Toast.makeText(this, "No Maps app found!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No location data in QR", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No navigation data found!", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}
