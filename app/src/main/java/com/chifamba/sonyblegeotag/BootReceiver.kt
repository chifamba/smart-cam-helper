package com.chifamba.sonyblegeotag

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val sharedPrefs = context.getSharedPreferences("sony_ble_prefs", Context.MODE_PRIVATE)
            val saved = sharedPrefs.getString("last_device_address", null)
            val savedAddress = if (!saved.isNullOrEmpty()) decryptString(saved) else null
            if (!savedAddress.isNullOrEmpty()) {
                val serviceIntent = Intent(context, SonyGpsService::class.java).apply {
                    action = SonyGpsService.ACTION_START
                    putExtra(SonyGpsService.EXTRA_DEVICE_ADDRESS, savedAddress)
                }
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    android.util.Log.e("SonyBootReceiver", "Failed to auto-restart geotag service on boot", e)
                }
            }
        }
    }

    private fun decryptString(value: String): String {
        return try {
            CryptoManager.decrypt(value)
        } catch (e: Exception) {
            android.util.Log.e("SonyBootReceiver", "Error decrypting string", e)
            ""
        }
    }
}
