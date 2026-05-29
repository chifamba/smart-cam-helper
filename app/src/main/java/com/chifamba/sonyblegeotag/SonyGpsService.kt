package com.chifamba.sonyblegeotag

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.UUID

class SonyGpsService : Service() {

    companion object {
        private const val TAG = "SonyGpsService"
        const val ACTION_START = "com.chifamba.sonyblegeotag.START"
        const val ACTION_STOP = "com.chifamba.sonyblegeotag.STOP"
        const val EXTRA_DEVICE_ADDRESS = "extra_device_address"

        private const val NOTIFICATION_CHANNEL_ID = "sony_gps_channel"
        private const val NOTIFICATION_ID = 8801

        // Sony Location Linkage Custom UUIDs
        val LOCATION_SERVICE_UUID: UUID = UUID.fromString("8000DD00-DD00-FFFF-FFFF-FFFFFFFFFFFF")
        val LOCATION_WRITE_CHAR_UUID: UUID = UUID.fromString("0000DD11-0000-1000-8000-00805F9B34FB")
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var targetDeviceAddress: String? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var lastKnownLocation: Location? = null

    private var isConnected = false
    private var characteristicWritePending = false
    private val mainHandler = Handler(Looper.getMainLooper())

    // Watchdog connection timer running at a low-frequency rate (60 seconds) to conserve battery
    private val reconnectRunnable = object : Runnable {
        override fun run() {
            if (!isConnected && targetDeviceAddress != null) {
                Log.d(TAG, "Watchdog checking connection to ${maskMacAddress(targetDeviceAddress)}...")
                connectToDevice(targetDeviceAddress!!)
            }
            mainHandler.postDelayed(this, 60000) 
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service Created")
        
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // WakeLock configuration - instantiated here but only acquired when camera is actively connected
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SonyGps::BackgroundWakelock")

        // Register dynamic receiver to listen for secure OS-level pairing and Bluetooth state events
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(
            this,
            systemEventsReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand: action=$action")

        val sharedPrefs = getSharedPreferences("sony_ble_prefs", Context.MODE_PRIVATE)

        if (action == ACTION_START) {
            targetDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
            if (!targetDeviceAddress.isNullOrEmpty()) {
                sharedPrefs.edit().putString("last_device_address", encryptString(targetDeviceAddress!!)).apply()
            }
        } else if (action == ACTION_STOP) {
            Log.d(TAG, "Stop requested")
            sharedPrefs.edit().remove("last_device_address").apply()
            closeGatt()
            stopSelf()
            return START_NOT_STICKY
        }

        // If system restarts service, targetDeviceAddress is null. Load persisted address.
        if (targetDeviceAddress.isNullOrEmpty()) {
            val saved = sharedPrefs.getString("last_device_address", null)
            targetDeviceAddress = if (!saved.isNullOrEmpty()) decryptString(saved) else null
        }

        if (!targetDeviceAddress.isNullOrEmpty()) {
            startServiceForeground()
            mainHandler.removeCallbacks(reconnectRunnable)
            mainHandler.post(reconnectRunnable)
        } else {
            Log.e(TAG, "No camera BLE address available. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    private fun startServiceForeground() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Sony GPS Geotagger Connection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Maintains constant BLE connection and GPS sync with Sony camera."
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Sony Geotagger Active")
            .setContentText("Transmitting high-precision GPS telemetry to camera...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            }
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startLocationUpdates() {
        if (locationCallback != null) return // Already tracking
        Log.d(TAG, "Starting high-accuracy GPS tracking (Camera Connected).")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions missing. Cannot start tracking.")
            return
        }

        // Target: High accuracy location queries synchronized with our transmission interval
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).apply {
            setMinUpdateIntervalMillis(3000L)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    lastKnownLocation = location
                    Log.d(TAG, "GPS Update received, preparing transmission.")
                    transmitGpsPayload()
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission has been revoked at runtime", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location updates", e)
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            Log.d(TAG, "Stopping high-accuracy GPS tracking to save battery.")
            try {
                fusedLocationClient.removeLocationUpdates(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping location updates", e)
            }
            locationCallback = null
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == false) {
            Log.d(TAG, "Acquiring WakeLock to hold transmission stream.")
            try {
                wakeLock?.acquire(24 * 60 * 60 * 1000L)
            } catch (e: Exception) {
                Log.e(TAG, "Error acquiring WakeLock", e)
            }
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            Log.d(TAG, "Releasing WakeLock to allow CPU deep sleep.")
            try {
                wakeLock?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing WakeLock", e)
            }
        }
    }

    private fun connectToDevice(address: String) {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            Log.e(TAG, "Bluetooth not enabled or unavailable")
            return
        }

        val device = adapter.getRemoteDevice(address)
        Log.d(TAG, "Connecting to device: ${device.name} (${maskMacAddress(address)})")

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.e(TAG, "BLUETOOTH_CONNECT permission missing")
                return
            }
        }

        closeGatt()

        try {
            // Force TRANSPORT_LE parameter to connect over Bluetooth Low Energy directly.
            // Using autoConnect = true allows the OS to run scanning and connecting in a highly power-optimized way.
            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(this, true, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(this, true, gattCallback)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth connect permission to initiate GATT connection", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate GATT connection", e)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (ActivityCompat.checkSelfPermission(this@SonyGpsService, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to GATT server. Discovering services...")
                    isConnected = true
                    
                    // Verify pairing/bonding state. Sony cameras require active bonding keys.
                    val bondState = gatt.device.bondState
                    if (bondState == BluetoothDevice.BOND_NONE) {
                        Log.d(TAG, "Device not bonded. Invoking pairing request...")
                        gatt.device.createBond()
                    }
                    
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from GATT server.")
                    isConnected = false
                    stopLocationUpdates()
                    releaseWakeLock()
                    closeGatt()
                }
            } else {
                Log.e(TAG, "GATT Connection error: status=$status. Re-connecting.")
                isConnected = false
                stopLocationUpdates()
                releaseWakeLock()
                closeGatt()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered successfully!")
                val service = gatt.getService(LOCATION_SERVICE_UUID)
                if (service != null) {
                    val writeChar = service.getCharacteristic(LOCATION_WRITE_CHAR_UUID)
                    if (writeChar != null) {
                        Log.d(TAG, "Ready to write coordinates to location characteristic 0xDD11")
                        acquireWakeLock()
                        startLocationUpdates()
                        transmitGpsPayload()
                    } else {
                        Log.e(TAG, "Could not find characteristic 0xDD11")
                    }
                } else {
                    Log.e(TAG, "Could not find Sony location service UUID")
                }
            } else {
                Log.e(TAG, "Service Discovery failed with status $status")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            characteristicWritePending = false
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Successfully wrote GPS coordinates to Sony camera!")
            } else if (status == 144) {
                Log.w(TAG, "GATT write failed with status=144 (0x90). This typically indicates a pairing/authentication issue, or that 'Location Info. Link' is turned OFF in your Sony Camera's Network settings.")
            } else {
                Log.e(TAG, "GATT write failed with status=$status")
            }
        }
    }

    @Synchronized
    private fun transmitGpsPayload() {
        val gatt = bluetoothGatt
        val location = lastKnownLocation

        if (!isConnected || gatt == null || location == null) {
            return
        }

        if (characteristicWritePending) {
            Log.d(TAG, "Write pending, skipping update.")
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return
        }

        // Double check bonding. If unbonded, writing might pop up system pairing dialog
        if (gatt.device.bondState != BluetoothDevice.BOND_BONDED) {
            Log.w(TAG, "Encryption/Bonding is not fully completed yet.")
        }

        val service = gatt.getService(LOCATION_SERVICE_UUID) ?: return
        val characteristic = service.getCharacteristic(LOCATION_WRITE_CHAR_UUID) ?: return

        // Generate packed 26 bytes
        val payload = GpsPayloadPacker.packPayload(
            latitude = location.latitude,
            longitude = location.longitude,
            timestampMs = location.time
        )

        val writeType = if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        }

        Log.d(TAG, "Writing GPS payload using writeType: $writeType (properties: ${characteristic.properties})")
        characteristicWritePending = true

        try {
            val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val status = gatt.writeCharacteristic(
                    characteristic,
                    payload,
                    writeType
                )
                status == android.bluetooth.BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = payload
                @Suppress("DEPRECATION")
                characteristic.writeType = writeType
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(characteristic)
            }

            if (!success) {
                Log.e(TAG, "GATT hardware write initiation failed")
                characteristicWritePending = false
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while writing characteristic", e)
            characteristicWritePending = false
        } catch (e: Exception) {
            Log.e(TAG, "Error writing characteristic", e)
            characteristicWritePending = false
        }
    }

    private val systemEventsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                if (device?.address == targetDeviceAddress) {
                    when (bondState) {
                        BluetoothDevice.BOND_BONDED -> {
                            Log.d(TAG, "Secure bonding exchange verified! Writing GPS coordinates...")
                            transmitGpsPayload()
                        }
                        BluetoothDevice.BOND_NONE -> {
                            Log.w(TAG, "Device became unbonded.")
                        }
                    }
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_ON) {
                    Log.d(TAG, "Bluetooth turned on. Re-initiating connection...")
                    targetDeviceAddress?.let { connectToDevice(it) }
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    Log.d(TAG, "Bluetooth turned off. Closing connection state.")
                    isConnected = false
                    stopLocationUpdates()
                    releaseWakeLock()
                    closeGatt()
                }
            }
        }
    }

    private fun closeGatt() {
        val gatt = bluetoothGatt ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Cannot invoke disconnect/close due to missing BLUETOOTH_CONNECT permission")
            } else {
                try {
                    gatt.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Error disconnecting GATT", e)
                }
                try {
                    gatt.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing GATT", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "General exception while closing GATT", e)
        } finally {
            bluetoothGatt = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Stopping services and cleaning up memory allocations")
        
        mainHandler.removeCallbacks(reconnectRunnable)
        
        stopLocationUpdates()
        closeGatt()

        try {
            unregisterReceiver(systemEventsReceiver)
        } catch (e: Exception) {
            // Ignored
        }

        releaseWakeLock()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved: App removed from recents list.")
        if (!isConnected) {
            Log.d(TAG, "Camera is not connected. Respecting user action and stopping service.")
            val sharedPrefs = getSharedPreferences("sony_ble_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().remove("last_device_address").apply()
            closeGatt()
            stopSelf()
        } else {
            Log.d(TAG, "Camera is actively connected. Keeping foreground service running to geotag photos.")
        }
    }

    private fun maskMacAddress(address: String?): String {
        if (address == null || address.length < 5) return "Unknown"
        return "XX:XX:XX:XX:" + address.substring(address.length - 5)
    }

    private fun encryptString(value: String): String {
        val key = 0x5F.toByte()
        val result = value.toByteArray().map { (it.toInt() xor key.toInt()).toByte() }.toByteArray()
        return android.util.Base64.encodeToString(result, android.util.Base64.NO_WRAP)
    }

    private fun decryptString(value: String): String {
        return try {
            val decoded = android.util.Base64.decode(value, android.util.Base64.NO_WRAP)
            val key = 0x5F.toByte()
            val decrypted = decoded.map { (it.toInt() xor key.toInt()).toByte() }.toByteArray()
            String(decrypted)
        } catch (e: Exception) {
            ""
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
