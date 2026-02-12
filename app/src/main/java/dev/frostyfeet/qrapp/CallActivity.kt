package dev.frostyfeet.qrapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.frostyfeet.qrapp.utils.VoIPManager
import io.agora.rtc2.IRtcEngineEventHandler

class CallActivity : ComponentActivity() {

    private var channelId: String = ""
    private var isCaller: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        channelId = intent.getStringExtra("CHANNEL_ID") ?: ""
        isCaller = intent.getBooleanExtra("IS_CALLER", false)

        if (channelId.isEmpty()) {
            Toast.makeText(this, "Invalid Call Info", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            CallScreen(channelId = channelId, isCaller = isCaller, onEndCall = {
                finish()
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        VoIPManager.leaveChannel()
        // In a real app we might not want to destroy the engine every time if we expect frequent calls
        // But for this simple flow, it's safer. 
        // Ideally, VoIPManager lifecycle should be tied to Application or a Service.
        VoIPManager.destroy() 
    }

    @Composable
    fun CallScreen(channelId: String, isCaller: Boolean, onEndCall: () -> Unit) {
        val context = LocalContext.current
        var isMuted by remember { mutableStateOf(false) }
        var isSpeakerOn by remember { mutableStateOf(false) }
        var remoteUserJoined by remember { mutableStateOf(false) }
        var permissionGranted by remember { mutableStateOf(false) }

        fun initializeAgora() {
            val eventHandler = object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    runOnUiThread { Toast.makeText(context, "Joined Channel", Toast.LENGTH_SHORT).show() }
                }

                override fun onUserJoined(uid: Int, elapsed: Int) {
                    runOnUiThread { 
                        remoteUserJoined = true 
                        Toast.makeText(context, "User Joined", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    runOnUiThread { 
                        remoteUserJoined = false
                        Toast.makeText(context, "User Offline", Toast.LENGTH_SHORT).show()
                        onEndCall()  // End call if remote leaves (optional policy)
                    }
                }

                override fun onError(err: Int) {
                    runOnUiThread {
                        Toast.makeText(context, "Agora Error: $err", Toast.LENGTH_LONG).show()
                    }
                }
            }

            if (VoIPManager.initialize(context, eventHandler)) {
                VoIPManager.joinChannel(channelId)
            }
        }

        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = { perms ->
                val allGranted = perms.values.all { it }
                permissionGranted = allGranted
                if (allGranted) {
                    initializeAgora()
                } else {
                    Toast.makeText(context, "Permissions needed for call", Toast.LENGTH_SHORT).show()
                    onEndCall()
                }
            }
        )

        LaunchedEffect(Unit) {
            val missingPermissions = permissions.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            
            if (missingPermissions.isEmpty()) {
                permissionGranted = true
                initializeAgora()
            } else {
                launcher.launch(permissions)
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (remoteUserJoined) "Connected" else (if (isCaller) "Calling..." else "Waiting for caller..."),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Secure Channel",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute Button
                IconButton(
                    onClick = { 
                        isMuted = !isMuted
                        VoIPManager.muteLocalAudioStream(isMuted)
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (isMuted) Color.White else Color.Gray, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = if (isMuted) Color.Black else Color.White
                    )
                }

                // End Call Button
                IconButton(
                    onClick = { onEndCall() },
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.Red, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White
                    )
                }

                // Speaker Button
                IconButton(
                    onClick = { 
                        isSpeakerOn = !isSpeakerOn 
                        VoIPManager.setEnableSpeakerphone(isSpeakerOn)
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (isSpeakerOn) Color.White else Color.Gray, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speaker",
                        tint = if (isSpeakerOn) Color.Black else Color.White
                    )
                }
            }
        }
    }
}
