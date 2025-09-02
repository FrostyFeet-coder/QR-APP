package com.example.qrcodegenration

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.RGBLuminanceSource
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : ComponentActivity() {

    private val CAMERA_PERMISSION_CODE = 100
    private val CALL_PERMISSION_CODE = 101

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) handleQR(result.contents)
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { decodeQRFromGallery(it)?.let { handleQR(it) } ?: Toast.makeText(this, "No QR found", Toast.LENGTH_SHORT).show() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.scanButton).setOnClickListener {
            showScanOptions()
        }

        findViewById<Button>(R.id.generateQRButton).setOnClickListener {
            startActivity(Intent(this, GenerateQRActivity::class.java))
        }

        findViewById<Button>(R.id.findMyQRButton).setOnClickListener {
            startActivity(Intent(this, FindParkingQRActivity::class.java))
        }
    }

    private fun showScanOptions() {
        val options = arrayOf("Scan using Camera", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Select Option")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> pickFromGallery()
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            startQRScan()
        }
    }

    private fun startQRScan() {
        val options = ScanOptions()
        options.setPrompt("Scan a QR code")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        barcodeLauncher.launch(options)
    }

    private fun pickFromGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun decodeQRFromGallery(uri: Uri): String? {
        return try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val intArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            MultiFormatReader().decode(binaryBitmap).text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun handleQR(data: String) {
        val intent = Intent(this, QRInfoActivity::class.java)
        intent.putExtra("QR_DATA", data)
        startActivity(intent)
    }

    private fun makeCall(phoneNumber: String) {
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:$phoneNumber")
        startActivity(callIntent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) startQRScan()
                else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
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
