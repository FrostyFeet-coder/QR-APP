package com.example.qrcodegenration

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.LuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class FindParkingQRActivity : ComponentActivity() {

    private val REQUEST_CODE_GALLERY = 1002

    private lateinit var etVehicleNumber: EditText
    private lateinit var btnFindQR: Button
    private lateinit var btnGenerateParkingQR: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_parking_qr)

        etVehicleNumber = findViewById(R.id.etVehicleNumber)
        btnFindQR = findViewById(R.id.btnFindQR)
        btnGenerateParkingQR = findViewById(R.id.btnGenerateParkingQR)

        btnFindQR.setOnClickListener {
            val vehicleNumber = etVehicleNumber.text.toString().trim()
            if (vehicleNumber.isNotEmpty()) {
                showScanOptions()
            } else {
                Toast.makeText(this, "Please enter vehicle number", Toast.LENGTH_SHORT).show()
            }
        }

        btnGenerateParkingQR.setOnClickListener {
            val vehicleNumber = etVehicleNumber.text.toString().trim()
            if (vehicleNumber.isNotEmpty()) {
                generateParkingQR(vehicleNumber)
            } else {
                Toast.makeText(this, "Please enter vehicle number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showScanOptions() {
        val options = arrayOf("Scan using Camera", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Select Option")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openScanner()
                    1 -> openGallery()
                }
            }
            .show()
    }

    // Camera scanner using ZXing Embedded
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            openNavigation(result.contents)
        } else {
            Toast.makeText(this, "No QR code found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan Parking QR")
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        barcodeLauncher.launch(options)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_GALLERY)
    }

    private fun generateParkingQR(vehicleNumber: String) {
        val intent = Intent(this, GenerateParkingQRActivity::class.java)
        intent.putExtra("VEHICLE_NUMBER", vehicleNumber)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_GALLERY && resultCode == RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            if (selectedImageUri != null) {
                val decodedText = decodeFromGallery(selectedImageUri)
                if (!decodedText.isNullOrEmpty()) {
                    openNavigation(decodedText)
                } else {
                    Toast.makeText(this, "No QR code found in image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun decodeFromGallery(uri: Uri): String? {
        return try {
            val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val intArray = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val source: LuminanceSource = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = MultiFormatReader().decode(binaryBitmap)
            result.text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun openNavigation(scannedData: String) {
        // Ensure the scannedData has ?lat= and ?lng=, else show error
        if (scannedData.contains("lat=") && scannedData.contains("lng=")) {
            val intent = Intent(this, NavigationActivity::class.java)
            intent.putExtra("NAV_DATA", scannedData)
            startActivity(intent)
        } else {
            Toast.makeText(this, "QR code does not contain valid navigation info!", Toast.LENGTH_SHORT).show()
        }
    }
}
