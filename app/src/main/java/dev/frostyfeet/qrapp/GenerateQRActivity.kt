package dev.frostyfeet.qrapp

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import dev.frostyfeet.qrapp.utils.SecurityUtils
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import android.os.Environment
import android.content.ContentValues
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.*
import dev.frostyfeet.qrapp.service.CallService

class GenerateQRActivity : ComponentActivity() {

    private var currentQRBitmap: Bitmap? = null
    private var currentQRFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GenerateQRScreen()
        }
    }

    @Composable
    fun GenerateQRScreen() {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var carName by remember { mutableStateOf("") }
        var carNumber by remember { mutableStateOf("") }
        var isSecureCallEnabled by remember { mutableStateOf(true) }
        var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
        val scrollState = rememberScrollState()
        
        // Service State
        var isOnline by remember { mutableStateOf(false) }

        val context = LocalContext.current

        // App Background Gradient: #BBDEFB (Top) -> #64B5F6 (Bottom)
        val bgBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFFBBDEFB), Color(0xFF64B5F6))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Generate QR Code",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )

                Text(
                    text = "Enter details below to create your parking QR",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF444444)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                StyledTextField(value = name, onValueChange = { name = it }, label = "Name")
                StyledTextField(value = phone, onValueChange = { phone = it }, label = "Phone")
                StyledTextField(value = carName, onValueChange = { carName = it }, label = "Car Name")
                StyledTextField(value = carNumber, onValueChange = { carNumber = it }, label = "Car Number")

                Spacer(modifier = Modifier.height(10.dp))

                PrimaryButton(text = "Generate QR Code") {
                    if (name.isNotEmpty() && phone.isNotEmpty() && carName.isNotEmpty() && carNumber.isNotEmpty()) {
                        val formattedPhone = phone
                        
                        val qrData = if (isSecureCallEnabled) {
                            val maskedPhone = SecurityUtils.maskData(formattedPhone)
                            "SECURE_CALL:$maskedPhone|Name:$name|Car:$carName|Reg:$carNumber"
                        } else {
                            "Name: $name\nPhone: $formattedPhone\nCar Name: $carName\nCar Number: $carNumber"
                        }

                        qrBitmap = generateQRBitmap(qrData)
                        currentQRBitmap = qrBitmap
                        currentQRFile = saveQRCodeTemp(qrBitmap!!)
                    } else {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    }
                }

                qrBitmap?.let { bitmap ->
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(220.dp).padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SecondaryButton(text = "Share", modifier = Modifier.weight(1f)) {
                            currentQRFile?.let { shareQRCode(it) }
                        }
                        SecondaryButton(text = "Save", modifier = Modifier.weight(1f)) {
                            currentQRBitmap?.let { saveToGallery(it) }
                        }
                    }

                    if (isSecureCallEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Secure Call Status",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                if (isOnline) {
                                    Text("You are ONLINE. You will receive a notification when someone scans your QR.",
                                        color = Color(0xFF4CAF50),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            isOnline = false
                                            val intent = Intent(context, CallService::class.java)
                                            context.stopService(intent)
                                            Toast.makeText(context, "You are now Offline", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Text("Go Offline")
                                    }
                                } else {
                                    Text("Go Online to receive calls via notification without sharing your number.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            val maskedPhone = SecurityUtils.maskData(phone)
                                            // Optimistic update, but actual status depends on Service
                                            isOnline = true 
                                            val intent = Intent(context, CallService::class.java)
                                            intent.putExtra("USER_ID", maskedPhone)
                                            context.startForegroundService(intent) // Updated to startForegroundService
                                            Toast.makeText(context, "Starting Service...", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Text("Go Online")
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    @Composable
    fun StyledTextField(value: String, onValueChange: (String) -> Unit, label: String) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.8f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                focusedBorderColor = Color(0xFF1565C0),
                unfocusedBorderColor = Color.Gray
            ),
            singleLine = true
        )
    }

    @Composable
    fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(55.dp),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF1565C0)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF1565C0)
            )
        ) {
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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

    private fun saveQRCodeTemp(bitmap: Bitmap): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "QRCode_$timeStamp.png"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File(storageDir, fileName)
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return imageFile
    }

    private fun shareQRCode(file: File) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share QR Code"))
    }

    private fun saveToGallery(bitmap: Bitmap) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "QRCode_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            runOnUiThread { Toast.makeText(this, "Saved to Gallery", Toast.LENGTH_SHORT).show() }
        }
    }
}