package com.example.qrcodegenration

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

class GenerateParkingQRActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var etVehicleNumber: EditText
    private lateinit var etFacility: EditText
    private lateinit var etZone: EditText
    private lateinit var etLevel: EditText
    private lateinit var etSpot: EditText
    private lateinit var etLat: EditText
    private lateinit var etLng: EditText
    private lateinit var ivQRCode: ImageView
    private lateinit var btnGenerate: Button
    private lateinit var btnSave: Button

    private var generatedBitmap: Bitmap? = null
    private var vehicleNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_parking_qr)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        etVehicleNumber = findViewById(R.id.etVehicleNumber)
        etFacility = findViewById(R.id.etParkingFacility)
        etZone = findViewById(R.id.etZone)
        etLevel = findViewById(R.id.etLevel)
        etSpot = findViewById(R.id.etSpot)
        etLat = findViewById(R.id.etLatitude)
        etLng = findViewById(R.id.etLongitude)
        ivQRCode = findViewById(R.id.ivQRCode)
        btnGenerate = findViewById(R.id.btnGenerate)
        btnSave = findViewById(R.id.btnSave)

        // Assign the vehicle number from intent to class property
        vehicleNumber = intent.getStringExtra("VEHICLE_NUMBER") ?: ""
        etVehicleNumber.setText(vehicleNumber)

        fetchLocation()

        btnGenerate.setOnClickListener {
            val facility = etFacility.text.toString().trim()
            val zone = etZone.text.toString().trim()
            val level = etLevel.text.toString().trim()
            val spot = etSpot.text.toString().trim()
            val lat = etLat.text.toString().trim()
            val lng = etLng.text.toString().trim()

            if (facility.isNotEmpty() && zone.isNotEmpty() && level.isNotEmpty() && spot.isNotEmpty() && lat.isNotEmpty() && lng.isNotEmpty()) {
                val qrData = "parkingnav://$facility/$zone/$level/$spot?lat=$lat&lng=$lng"
                generateQRCode(qrData)
            } else {
                Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show()
            }
        }

        btnSave.setOnClickListener {
            generatedBitmap?.let {
                saveToGallery(it)
            } ?: Toast.makeText(this, "Generate QR first!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    etLat.setText(location.latitude.toString())
                    etLng.setText(location.longitude.toString())
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        }
    }

    private fun generateQRCode(data: String) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) ContextCompat.getColor(this, android.R.color.black) else ContextCompat.getColor(this, android.R.color.white))
                }
            }

            ivQRCode.setImageBitmap(bitmap)
            ivQRCode.visibility = ImageView.VISIBLE
            btnSave.visibility = Button.VISIBLE
            generatedBitmap = bitmap
            Toast.makeText(this, "QR Generated!", Toast.LENGTH_SHORT).show()
        } catch (e: WriterException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to generate QR!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveToGallery(bitmap: Bitmap) {
        Thread {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "QR_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/QR Codes")
                }

                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                }

                runOnUiThread {
                    Toast.makeText(this, "QR saved to gallery!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Failed to save QR: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
