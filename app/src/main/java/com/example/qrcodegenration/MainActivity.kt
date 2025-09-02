package com.example.qrcodegenration

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : ComponentActivity() {

    private val CAMERA_PERMISSION_CODE = 100
    private val CALL_PERMISSION_CODE = 101

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            handleQR(result.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Button to start QR scanning
        findViewById<Button>(R.id.scanButton).setOnClickListener {
            checkCameraPermission()
        }

        // Button to open QR generation screen
        findViewById<Button>(R.id.generateQRButton).setOnClickListener {
            startActivity(Intent(this, GenerateQRActivity::class.java))
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            startQRScan()
        }
    }

    private fun startQRScan() {
        val options = ScanOptions()
        options.setPrompt("Scan a QR code")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        options.setCaptureActivity(CustomScannerActivity::class.java) // Optional: Custom scanner UI
        barcodeLauncher.launch(options)
    }

    private fun handleQR(data: String) {
        if (data.startsWith("tel:")) {
            // For phone QR codes, go directly to QRInfoActivity
            val intent = Intent(this, QRInfoActivity::class.java)
            intent.putExtra("QR_DATA", data)
            startActivity(intent)
        } else {
            // For other QR codes, launch QRInfoActivity to show the scanned data
            val intent = Intent(this, QRInfoActivity::class.java)
            intent.putExtra("QR_DATA", data)
            startActivity(intent)
        }
    }
    private fun makeCall(phoneNumber: String) {
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:$phoneNumber")
        startActivity(callIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startQRScan()
                } else {
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                }
            }
            CALL_PERMISSION_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "Permission granted. Scan QR again to call.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Call permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}