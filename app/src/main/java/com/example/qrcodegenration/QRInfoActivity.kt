package com.example.qrcodegenration

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class QRInfoActivity : ComponentActivity() {

    private val CALL_PERMISSION_CODE = 102
    private var phoneNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_info)

        val tvName = findViewById<TextView>(R.id.tvName)
        val tvCarName = findViewById<TextView>(R.id.tvCarName)
        val tvCarNumber = findViewById<TextView>(R.id.tvCarNumber)
        val btnCall = findViewById<Button>(R.id.btnCall)
        val tvOtherData = findViewById<TextView>(R.id.tvOtherData)
        val btnBack = findViewById<Button>(R.id.btnBack)

        // Get the QR data from intent
        val qrData = intent.getStringExtra("QR_DATA") ?: "No data found"

        // Parse and display the QR data
        parseAndDisplayQRData(qrData, tvName, tvCarName, tvCarNumber, btnCall, tvOtherData)

        // Call button click listener
        btnCall.setOnClickListener {
            phoneNumber?.let { number ->
                // Show confirmation dialog before calling
                showCallConfirmation(number)
            }
        }

        // Back button click listener
        btnBack.setOnClickListener {
            finish() // Close this activity and go back to previous
        }
    }

    private fun parseAndDisplayQRData(
        rawData: String,
        tvName: TextView,
        tvCarName: TextView,
        tvCarNumber: TextView,
        btnCall: Button,
        tvOtherData: TextView
    ) {
        if (rawData.contains("\n") && rawData.contains(":")) {
            // If it's formatted data from our QR generation
            val lines = rawData.split("\n")
            var hasPhone = false

            for (line in lines) {
                when {
                    line.startsWith("Name:") -> {
                        tvName.text = line
                        tvName.visibility = View.VISIBLE
                    }
                    line.startsWith("Phone:") -> {
                        // Extract phone number but don't display it
                        val phone = line.removePrefix("Phone:").trim()
                        if (phone.isNotEmpty()) {
                            // Clean and format the phone number
                            phoneNumber = formatPhoneNumber(phone)
                            hasPhone = true
                        }
                    }
                    line.startsWith("Car Name:") -> {
                        tvCarName.text = line
                        tvCarName.visibility = View.VISIBLE
                    }
                    line.startsWith("Car Number:") -> {
                        tvCarNumber.text = line
                        tvCarNumber.visibility = View.VISIBLE
                    }
                }
            }

            // Show call button if phone number is available
            if (hasPhone) {
                btnCall.visibility = View.VISIBLE
            }

        } else if (rawData.startsWith("tel:")) {
            // If it's a direct phone number QR
            val phone = rawData.removePrefix("tel:")
            phoneNumber = formatPhoneNumber(phone)
            btnCall.visibility = View.VISIBLE
            tvOtherData.text = "Phone number QR code scanned"
            tvOtherData.visibility = View.VISIBLE

        } else {
            // For any other type of QR data
            tvOtherData.text = "Scanned Data:\n$rawData"
            tvOtherData.visibility = View.VISIBLE
        }
    }

    private fun formatPhoneNumber(phone: String): String {
        // Remove all non-digit characters
        var cleaned = phone.replace("[^0-9]".toRegex(), "")

        // Handle Indian numbers specifically
        if (cleaned.length == 10) {
            // Add India country code if it's a 10-digit number
            cleaned = "+91$cleaned"
        } else if (cleaned.startsWith("91") && cleaned.length == 12) {
            // Add + prefix if it starts with 91 and is 12 digits
            cleaned = "+$cleaned"
        } else if (cleaned.length >= 10 && !cleaned.startsWith("+")) {
            // Add + prefix if it doesn't have one
            cleaned = "+$cleaned"
        }

        return cleaned
    }

    private fun showCallConfirmation(number: String) {
        // Show a toast with the formatted number (without revealing full number)
        val maskedNumber = number.takeLast(4).let { last4 ->
            "XXXXX$last4"
        }
        Toast.makeText(this, "Calling number ending with $maskedNumber", Toast.LENGTH_SHORT).show()

        // Check permission and make call
        checkCallPermission(number)
    }

    private fun checkCallPermission(number: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                CALL_PERMISSION_CODE
            )
            // Store the phone number temporarily
            this.phoneNumber = number
        } else {
            makeCall(number)
        }
    }

    private fun makeCall(number: String) {
        try {
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.data = Uri.parse("tel:${Uri.encode(number)}")
            startActivity(callIntent)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Call permission denied", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to make call: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CALL_PERMISSION_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    phoneNumber?.let { makeCall(it) }
                } else {
                    Toast.makeText(this, "Call permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}