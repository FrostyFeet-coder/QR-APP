package com.example.qrcodegenration.utils

import android.content.Context
import android.util.Log
import com.example.qrcodegenration.BuildConfig
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig

object VoIPManager {
    private var rtcEngine: RtcEngine? = null
    private val APP_ID = BuildConfig.AGORA_APP_ID 

    fun initialize(context: Context, eventHandler: IRtcEngineEventHandler): Boolean {
        // Always destroy and recreate to ensure fresh event handler
        if (rtcEngine != null) {
            Log.d("VoIPManager", "Destroying existing RtcEngine to create fresh instance")
            try {
                rtcEngine?.leaveChannel()
                RtcEngine.destroy()
            } catch (e: Exception) {
                Log.w("VoIPManager", "Error destroying old engine: ${e.message}")
            }
            rtcEngine = null
        }
        
        try {
            val config = RtcEngineConfig()
            config.mContext = context.applicationContext
            config.mAppId = APP_ID
            config.mEventHandler = eventHandler

            rtcEngine = RtcEngine.create(config)
            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            
            // Set audio profile for voice communication - AEC is built-in for COMMUNICATION profile
            rtcEngine?.setAudioProfile(
                Constants.AUDIO_PROFILE_SPEECH_STANDARD,
                Constants.AUDIO_SCENARIO_DEFAULT
            )
            
            // Enable audio subsystem
            rtcEngine?.enableAudio()
            rtcEngine?.enableLocalAudio(true)
            rtcEngine?.muteLocalAudioStream(false)
            rtcEngine?.setDefaultAudioRoutetoSpeakerphone(false) // Earpiece to avoid echo
            
            Log.d("VoIPManager", "RtcEngine initialized - audio enabled, local audio enabled, unmuted")
            return true
        } catch (e: Exception) {
            Log.e("VoIPManager", "Error initializing Agora RtcEngine: ${e.message}", e)
            return false
        }
    }

    fun joinChannel(channelId: String, token: String? = null) {
        Log.d("VoIPManager", "Joining channel: '$channelId'")
        val result = rtcEngine?.joinChannel(token, channelId, "Extra Optional Data", 0)
        Log.d("VoIPManager", "joinChannel result: $result (0 = Success)")
        
        // Ensure audio is unmuted after joining
        rtcEngine?.muteLocalAudioStream(false)
        rtcEngine?.enableLocalAudio(true)
    }

    fun leaveChannel() {
        Log.d("VoIPManager", "Leaving channel")
        rtcEngine?.leaveChannel()
    }

    fun destroy() {
        try {
            rtcEngine?.leaveChannel()
            RtcEngine.destroy()
        } catch (e: Exception) {
            Log.w("VoIPManager", "Error during destroy: ${e.message}")
        }
        rtcEngine = null
    }

    fun muteLocalAudioStream(muted: Boolean) {
        rtcEngine?.muteLocalAudioStream(muted)
    }

    fun setEnableSpeakerphone(enabled: Boolean) {
        rtcEngine?.setEnableSpeakerphone(enabled)
    }
}
