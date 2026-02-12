package dev.frostyfeet.qrapp.utils

import android.content.Context
import android.util.Log
import dev.frostyfeet.qrapp.BuildConfig
import io.agora.rtm.*

object SignalingManager {
    private const val TAG = "SignalingManager"
    private val APP_ID = BuildConfig.AGORA_APP_ID

    private var rtmClient: RtmClient? = null
    private var isInitialized = false
    private var isLoggedIn = false
    private var currentUserId: String? = null

    interface SignalingListener {
        fun onInvitationReceived(callerId: String, channelName: String)
        fun onLoginSuccess()
        fun onLoginError(errorCode: Int)
    }

    private var listener: SignalingListener? = null

    /**
     * Sanitize a string to be a valid Agora RTM User ID / channel name.
     * Replace Base64 special characters.
     */
    fun sanitizeUserId(raw: String): String {
        return raw.replace("/", "_").replace("+", "-").replace("=", "")
    }

    private fun createEventListener(): RtmEventListener {
        return object : RtmEventListener {
            override fun onMessageEvent(event: MessageEvent?) {
                if (event == null) return
                val data = event.getMessage()?.getData()
                val messageText = when (data) {
                    is ByteArray -> String(data, Charsets.UTF_8)
                    is String -> data
                    else -> data?.toString() ?: ""
                }
                val publisher = event.getPublisherId() ?: "unknown"
                Log.d(TAG, "Message from $publisher on channel ${event.getChannelName()}: $messageText")

                if (messageText.startsWith("INVITE:")) {
                    val channelName = messageText.substringAfter("INVITE:")
                    listener?.onInvitationReceived(publisher, channelName)
                }
            }

            override fun onPresenceEvent(event: PresenceEvent?) {}
            override fun onTopicEvent(event: TopicEvent?) {}
            override fun onLockEvent(event: LockEvent?) {}
            override fun onStorageEvent(event: StorageEvent?) {}
            override fun onConnectionStateChanged(channelName: String?, state: RtmConstants.RtmConnectionState?, reason: RtmConstants.RtmConnectionChangeReason?) {
                Log.d(TAG, "Connection state changed: channel=$channelName state=$state reason=$reason")
                
                // Track disconnection
                if (state == RtmConstants.RtmConnectionState.DISCONNECTED) {
                    isLoggedIn = false
                } else if (state == RtmConstants.RtmConnectionState.CONNECTED) {
                    isLoggedIn = true
                }
            }
            override fun onTokenPrivilegeWillExpire(channelName: String?) {}
        }
    }

    fun initialize(context: Context, signalingListener: SignalingListener) {
        listener = signalingListener
        Log.d(TAG, "SignalingManager initialized (listener set)")
    }

    /**
     * Check if currently logged in and connected.
     */
    fun isConnected(): Boolean {
        return isInitialized && isLoggedIn && rtmClient != null
    }

    fun login(userId: String) {
        val safeId = sanitizeUserId(userId)
        
        // If already logged in with any ID, just notify success and return
        if (isConnected()) {
            Log.d(TAG, "Already connected as ${currentUserId}, skipping re-login")
            listener?.onLoginSuccess()
            return
        }
        
        currentUserId = safeId

        // If initialized but not logged in, destroy and start fresh
        if (isInitialized) {
            destroy()
        }

        try {
            val config = RtmConfig.Builder(APP_ID, safeId)
                .eventListener(createEventListener())
                .build()

            rtmClient = RtmClient.create(config)
            isInitialized = true
            Log.d(TAG, "RTM 2.x client created for userId: $safeId")
        } catch (e: Exception) {
            Log.e(TAG, "RTM 2.x client creation failed: ${e.message}", e)
            listener?.onLoginError(-1)
            return
        }

        Log.d(TAG, "Logging in with userId: $safeId")
        rtmClient?.login("", object : ResultCallback<Void> {
            override fun onSuccess(responseInfo: Void?) {
                Log.d(TAG, "RTM 2.x Login Success for $safeId")
                isLoggedIn = true

                // Subscribe to our own "inbox" channel to receive messages
                subscribeToInbox(safeId)

                listener?.onLoginSuccess()
            }

            override fun onFailure(errorInfo: ErrorInfo?) {
                Log.e(TAG, "RTM 2.x Login Failure: code=${errorInfo?.errorCode}, reason=${errorInfo?.errorReason}")
                listener?.onLoginError(errorInfo?.errorCode?.ordinal ?: -1)
            }
        })
    }

    private fun subscribeToInbox(userId: String) {
        val inboxChannel = "inbox_$userId"
        Log.d(TAG, "Subscribing to inbox channel: $inboxChannel")

        val options = SubscribeOptions()
        rtmClient?.subscribe(inboxChannel, options, object : ResultCallback<Void> {
            override fun onSuccess(responseInfo: Void?) {
                Log.d(TAG, "Subscribed to inbox: $inboxChannel")
            }

            override fun onFailure(errorInfo: ErrorInfo?) {
                Log.e(TAG, "Failed to subscribe to inbox: ${errorInfo?.errorReason}")
            }
        })
    }

    fun logout() {
        rtmClient?.logout(object : ResultCallback<Void> {
            override fun onSuccess(responseInfo: Void?) {
                Log.d(TAG, "RTM Logout success")
                isLoggedIn = false
            }

            override fun onFailure(errorInfo: ErrorInfo?) {
                Log.e(TAG, "RTM Logout failure: ${errorInfo?.errorReason}")
            }
        })
    }

    fun sendInvite(peerId: String, channelName: String, callback: (Boolean) -> Unit) {
        val safePeerId = sanitizeUserId(peerId)
        val inboxChannel = "inbox_$safePeerId"
        val messageText = "INVITE:$channelName"

        Log.d(TAG, "Publishing invite to channel: $inboxChannel, message: $messageText, isConnected: ${isConnected()}")

        if (!isConnected()) {
            Log.e(TAG, "Cannot send invite - not connected!")
            callback(false)
            return
        }

        val options = PublishOptions()
        rtmClient?.publish(inboxChannel, messageText, options, object : ResultCallback<Void> {
            override fun onSuccess(responseInfo: Void?) {
                Log.d(TAG, "Invite published successfully to $inboxChannel")
                callback(true)
            }

            override fun onFailure(errorInfo: ErrorInfo?) {
                Log.e(TAG, "Invite publish failed: ${errorInfo?.errorReason}")
                callback(false)
            }
        })
    }

    fun destroy() {
        try {
            if (isInitialized) {
                RtmClient.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error during destroy: ${e.message}")
        }
        rtmClient = null
        isInitialized = false
        isLoggedIn = false
        currentUserId = null
    }
}
