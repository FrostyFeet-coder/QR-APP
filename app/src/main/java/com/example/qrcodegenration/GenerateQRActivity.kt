package com.example.qrcodegenration

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.content.Intent
import android.net.Uri
import java.io.OutputStream
import java.io.File
import java.io.FileOutputStream
import android.os.Environment
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.provider.MediaStore
import android.content.ContentValues
import android.graphics.Bitmap.CompressFormat

class GenerateQRActivity : ComponentActivity() {

    private var currentQRBitmap: Bitmap? = null
    private var currentQRFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_qr)

        val nameInput = findViewById<EditText>(R.id.nameInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val carNameInput = findViewById<EditText>(R.id.carNameInput)
        val carNumberInput = findViewById<EditText>(R.id.carNumberInput)
        val generateButton = findViewById<Button>(R.id.generateButton)
        val shareButton = findViewById<Button>(R.id.shareButton)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val buttonContainer = findViewById<android.widget.LinearLayout>(R.id.buttonContainer)
        val imageView = findViewById<ImageView>(R.id.qrImage)

        // Initially hide the buttons
        buttonContainer.isVisible = false

        generateButton.setOnClickListener {
            val name = nameInput.text.toString()
            var phone = phoneInput.text.toString()
            val carName = carNameInput.text.toString()
            val carNumber = carNumberInput.text.toString()

            if (name.isNotEmpty() && phone.isNotEmpty() && carName.isNotEmpty() && carNumber.isNotEmpty()) {
                // Format the phone number before generating QR
                phone = formatPhoneNumberForQR(phone).toString()
                val qrData = "Name: $name\nPhone: $phone\nCar Name: $carName\nCar Number: $carNumber"
                val bitmap = generateQRBitmap(qrData)
                currentQRBitmap = bitmap
                imageView.setImageBitmap(bitmap)
                imageView.isVisible = true
                buttonContainer.isVisible = true

                // Save the QR code temporarily for sharing
                currentQRFile = saveQRCodeTemp(bitmap)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        shareButton.setOnClickListener {
            currentQRFile?.let { file ->
                shareQRCode(file)
            } ?: run {
                Toast.makeText(this, "Generate a QR code first", Toast.LENGTH_SHORT).show()
            }
        }

        saveButton.setOnClickListener {
            currentQRBitmap?.let { bitmap ->
                saveToGallery(bitmap)
            } ?: run {
                Toast.makeText(this, "Generate a QR code first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateQRBitmap(text: String): Bitmap {
        val size = 512
        val bits = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (bits.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        return bmp
    }

    private fun formatPhoneNumberForQR(phone: String): String? {
        // Remove all non-digit except +
        var cleaned = phone.replace("[^0-9+]".toRegex(), "")

        // Agar input khali ho toh invalid
        if (cleaned.isEmpty()) return null

        // If it already has + at start
        if (cleaned.startsWith("+")) {
            // Check if valid international format (min 8, max 15 digits after +)
            val digits = cleaned.substring(1)
            if (digits.length in 8..15) {
                return cleaned
            } else {
                return null
            }
        }

        // Indian numbers
        if (cleaned.length == 10) {
            // Valid 10-digit mobile number (not starting with 0/1)
            if (cleaned[0] in '6'..'9') {
                return "+91$cleaned"
            } else {
                return null
            }
        } else if (cleaned.startsWith("91") && cleaned.length == 12) {
            val rest = cleaned.substring(2)
            if (rest[0] in '6'..'9') {
                return "+$cleaned"
            } else {
                return null
            }
        }

        // For other numbers (international format without +)
        if (cleaned.length in 8..15) {
            return "+$cleaned"
        }

        // Invalid case
        return null
    }


    private fun saveQRCodeTemp(bitmap: Bitmap): File {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "QRCode_$timeStamp.png"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val imageFile = File(storageDir, fileName)

            val outputStream: OutputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            return imageFile
        } catch (e: Exception) {
            throw e
        }
    }

    private fun shareQRCode(imageFile: File) {
        try {
            // Get the URI using FileProvider
            val imageUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                imageFile
            )

            // Create a share intent
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, imageUri)
                type = "image/png"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Create a chooser intent
            val chooserIntent = Intent.createChooser(shareIntent, "Share QR Code")

            // Start the activity
            startActivity(chooserIntent)

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to share QR code: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveToGallery(bitmap: Bitmap) {
        Thread {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "QRCode_${System.currentTimeMillis()}.png")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }

                    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        contentResolver.openOutputStream(it)?.use { outputStream ->
                            bitmap.compress(CompressFormat.PNG, 100, outputStream)
                        }
                    }
                } else {
                    val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val imageFile = File(imagesDir, "QRCode_${System.currentTimeMillis()}.png")
                    FileOutputStream(imageFile).use { outputStream ->
                        bitmap.compress(CompressFormat.PNG, 100, outputStream)
                        MediaStore.Images.Media.insertImage(contentResolver, imageFile.absolutePath, imageFile.name, "QR Code")
                    }
                }

                runOnUiThread {
                    Toast.makeText(this, "QR code saved to gallery", Toast.LENGTH_SHORT).show()
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