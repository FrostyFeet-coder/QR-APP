package dev.frostyfeet.qrapp

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
import dev.frostyfeet.qrapp.utils.SignalingManager

class QRInfoActivity : ComponentActivity() {

    private val CALL_PERMISSION_CODE = 102
    private var phoneNumber: String? = null
    private var isSecureCall = false
    private var secureChannelId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_info)

        val tvName = findViewById<TextView>(R.id.tvName)
        val tvCarName = findViewById<TextView>(R.id.tvCarName)
        val tvCarNumber = findViewById<TextView>(R.id.tvCarNumber)
        val btnCall = findViewById<Button>(R.id.btnCall)
        val tvOtherData = findViewById<TextView>(R.id.tvOtherData)
        val btnBack = findViewById<Button>(R.id.btnBack)

        val qrData = intent.getStringExtra("QR_DATA") ?: "No data found"
        parseAndDisplayQRData(qrData, tvName, tvCarName, tvCarNumber, btnCall, tvOtherData)

        // Initialize Signaling Manager with login state tracking
        SignalingManager.initialize(this, object : SignalingManager.SignalingListener {
            override fun onInvitationReceived(callerId: String, channelName: String) {}
            override fun onLoginSuccess() {
                runOnUiThread {
                    if (isSecureCall) {
                        btnCall.text = "Secure In-App Call"
                        btnCall.isEnabled = true
                    }
                    Toast.makeText(this@QRInfoActivity, "Connected to signaling", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onLoginError(errorCode: Int) {
                runOnUiThread {
                    Toast.makeText(this@QRInfoActivity, "Signaling connection failed ($errorCode)", Toast.LENGTH_SHORT).show()
                }
            }
        })

        // Login as caller with a stable ID (not timestamp-based)
        val callerId = "Caller_" + android.os.Build.SERIAL.hashCode()
        SignalingManager.login(callerId)

        // Disable button initially for secure calls until RTM is ready
        if (isSecureCall) {
            btnCall.text = "Connecting..."
            btnCall.isEnabled = false
        }

        btnCall.setOnClickListener {
            if (isSecureCall && secureChannelId != null) {
                if (!SignalingManager.isConnected()) {
                    Toast.makeText(this, "Still connecting... please wait", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                // Send RTM Invite
                Toast.makeText(this, "Calling...", Toast.LENGTH_SHORT).show()
                btnCall.isEnabled = false
                val channelName = "SecureChannel_" + System.currentTimeMillis()
                SignalingManager.sendInvite(secureChannelId!!, channelName) { success ->
                    runOnUiThread {
                        btnCall.isEnabled = true
                        if (success) {
                            Toast.makeText(this, "Ringing...", Toast.LENGTH_LONG).show()
                            val intent = Intent(this, CallActivity::class.java)
                            intent.putExtra("CHANNEL_ID", channelName)
                            intent.putExtra("IS_CALLER", true)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Failed to connect. User might be offline.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                phoneNumber?.let { number ->
                    showCallConfirmation(number)
                }
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
    
    override fun onResume() {
        super.onResume()
        val btnCall = findViewById<Button>(R.id.btnCall)
        if (isSecureCall) {
            if (SignalingManager.isConnected()) {
                // Already connected, enable button
                btnCall.text = "Secure In-App Call"
                btnCall.isEnabled = true
            } else {
                // Need to login
                btnCall.text = "Reconnecting..."
                btnCall.isEnabled = false
                val callerId = "Caller_" + android.os.Build.SERIAL.hashCode()
                SignalingManager.login(callerId)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't destroy SignalingManager here - keep the connection alive for re-calls
    }

    private fun parseAndDisplayQRData(
        rawData: String,
        tvName: TextView,
        tvCarName: TextView,
        tvCarNumber: TextView,
        btnCall: Button,
        tvOtherData: TextView
    ) {
        if (rawData.startsWith("SECURE_CALL:")) {
            isSecureCall = true
            try {
                val parts = rawData.split("|")
                secureChannelId = parts[0].substringAfter("SECURE_CALL:")
                
                val name = parts.find { it.startsWith("Name:") }?.substringAfter("Name:") ?: "Unknown"
                val car = parts.find { it.startsWith("Car:") }?.substringAfter("Car:") ?: ""
                val reg = parts.find { it.startsWith("Reg:") }?.substringAfter("Reg:") ?: ""

                tvName.text = "Name: $name"
                tvName.visibility = View.VISIBLE
                
                if (car.isNotEmpty()) {
                    tvCarName.text = "Car: $car"
                    tvCarName.visibility = View.VISIBLE
                }
                if (reg.isNotEmpty()) {
                    tvCarNumber.text = "Reg: $reg"
                    tvCarNumber.visibility = View.VISIBLE
                }

                btnCall.text = "Secure In-App Call"
                btnCall.visibility = View.VISIBLE
                tvOtherData.visibility = View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
                tvOtherData.text = "Invalid Secure QR Data"
                tvOtherData.visibility = View.VISIBLE
            }

        } else if (rawData.contains("\n") && rawData.contains(":")) {
            val lines = rawData.split("\n")
            var hasPhone = false

            for (line in lines) {
                when {
                    line.startsWith("Name:") -> {
                        tvName.text = line
                        tvName.visibility = View.VISIBLE
                    }
                    line.startsWith("Phone:") -> {
                        val phone = line.removePrefix("Phone:").trim()
                        if (phone.isNotEmpty()) {
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

            if (hasPhone) {
                btnCall.visibility = View.VISIBLE
            }

        } else if (rawData.startsWith("tel:")) {
            val phone = rawData.removePrefix("tel:")
            phoneNumber = formatPhoneNumber(phone)
            btnCall.visibility = View.VISIBLE
            tvOtherData.text = "Phone number QR code scanned"
            tvOtherData.visibility = View.VISIBLE

        } else {
            tvOtherData.text = "Scanned Data:\n$rawData"
            tvOtherData.visibility = View.VISIBLE
        }
    }

    private fun formatPhoneNumber(phone: String): String {
        var cleaned = phone.replace("[^0-9]".toRegex(), "")
        if (cleaned.length == 10) cleaned = "+91$cleaned"
        else if (cleaned.startsWith("91") && cleaned.length == 12) cleaned = "+$cleaned"
        else if (cleaned.length >= 10 && !cleaned.startsWith("+")) cleaned = "+$cleaned"
        return cleaned
    }

    private fun showCallConfirmation(number: String) {
        val maskedNumber = number.takeLast(4).let { "XXXXX$it" }
        Toast.makeText(this, "Calling number ending with $maskedNumber", Toast.LENGTH_SHORT).show()
        checkCallPermission(number)
    }

    private fun checkCallPermission(number: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), CALL_PERMISSION_CODE)
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
        permissions: Array<String>,
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