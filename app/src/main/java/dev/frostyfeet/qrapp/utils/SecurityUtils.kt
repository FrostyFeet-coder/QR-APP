package dev.frostyfeet.qrapp.utils

import android.util.Base64

object SecurityUtils {
    private const val SECRET_KEY = 42 // Simple key for XOR demonstration

    fun maskData(data: String): String {
        val inputBytes = data.toByteArray(Charsets.UTF_8)
        val maskedBytes = ByteArray(inputBytes.size)

        for (i in inputBytes.indices) {
            maskedBytes[i] = (inputBytes[i].toInt() xor SECRET_KEY).toByte()
        }

        // Encode to Base64 to make it QR-friendly and printable
        return Base64.encodeToString(maskedBytes, Base64.NO_WRAP)
    }

    fun unmaskData(maskedData: String): String {
        return try {
            val decodedBytes = Base64.decode(maskedData, Base64.NO_WRAP)
            val unmaskedBytes = ByteArray(decodedBytes.size)

            for (i in decodedBytes.indices) {
                unmaskedBytes[i] = (decodedBytes[i].toInt() xor SECRET_KEY).toByte()
            }
            String(unmaskedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
