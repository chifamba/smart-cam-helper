package com.chifamba.sonyblegeotag

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SonyMainActivity"
        private const val SCAN_PERIOD_MS = 15000L
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isScanning = false
    private var discoveredDeviceAddress: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var statusTextView: TextView
    private lateinit var scanButton: Button
    private lateinit var startServiceButton: Button
    private lateinit var stopServiceButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Layout structure instantiated directly
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
            filterTouchesWhenObscured = true // Mitigate tapjacking overlay attacks
        }

        val titleView = TextView(this).apply {
            text = "Sony Camera BLE Geotagger"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 40)
        }
        container.addView(titleView)

        statusTextView = TextView(this).apply {
            text = "Status: Permissions pending..."
            textSize = 16f
            setPadding(0, 0, 0, 60)
        }
        container.addView(statusTextView)

        scanButton = Button(this).apply {
            text = "Scan for Sony Camera"
            isEnabled = false
            setOnClickListener { startBleScan() }
        }
        container.addView(scanButton)

        startServiceButton = Button(this).apply {
            text = "Start Geotag Background Service"
            isEnabled = false
            setOnClickListener { toggleService(true) }
        }
        container.addView(startServiceButton)

        stopServiceButton = Button(this).apply {
            text = "Stop Background Service"
            isEnabled = false
            setOnClickListener { toggleService(false) }
        }
        container.addView(stopServiceButton)

        setContentView(container)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        } else {
            onForegroundPermissionsGranted()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val btConnectGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            results[Manifest.permission.BLUETOOTH_CONNECT] ?: false
        } else true

        if (fineGranted && btConnectGranted) {
            onForegroundPermissionsGranted()
        } else {
            statusTextView.text = "Error: Bluetooth or GPS Permissions Denied."
            Toast.makeText(this, "Location and Bluetooth permissions are mandatory.", Toast.LENGTH_LONG).show()
        }
    }

    private fun onForegroundPermissionsGranted() {
        // Request Background Location separately as mandated by Google policies
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                showBackgroundLocationDisclosure()
                return
            }
        }
        onAllPermissionsGranted()
    }

    private fun showBackgroundLocationDisclosure() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Background Location Access Required")
            .setMessage("This app collects location data to enable automatic photo geotagging on your Sony camera, even when the app is closed or not in use. This allows you to lock your phone and keep it in your pocket while shooting. Please select 'Allow all the time' in the next system prompt.")
            .setPositiveButton("Continue") { dialog, _ ->
                dialog.dismiss()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                statusTextView.text = "Warning: Background location denied. Screen-off sync will be degraded."
                onAllPermissionsGranted()
            }
            .setCancelable(false)
            .show()
    }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onAllPermissionsGranted()
        } else {
            statusTextView.text = "Warning: Background location denied. Screen-off sync will be degraded."
            onAllPermissionsGranted() 
        }
    }

    private fun onAllPermissionsGranted() {
        val sharedPrefs = getSharedPreferences("sony_ble_prefs", Context.MODE_PRIVATE)
        val saved = sharedPrefs.getString("last_device_address", null)
        val savedAddress = if (!saved.isNullOrEmpty()) decryptString(saved) else null
        if (!savedAddress.isNullOrEmpty()) {
            discoveredDeviceAddress = savedAddress
            statusTextView.text = "Status: Service Active.\nConnected camera: $savedAddress"
            startServiceButton.isEnabled = false
            stopServiceButton.isEnabled = true
            scanButton.isEnabled = true
        } else {
            statusTextView.text = "Status: Ready to Scan."
            scanButton.isEnabled = true
            startServiceButton.isEnabled = false
            stopServiceButton.isEnabled = false
        }
    }

    private fun startBleScan() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return
        }

        val scanner = adapter.bluetoothLeScanner ?: return
        if (isScanning) return

        statusTextView.text = "Status: Scanning for Sony Camera..."
        discoveredDeviceAddress = null
        startServiceButton.isEnabled = false
        stopServiceButton.isEnabled = false
        isScanning = true

        // Filter for Sony's Company ID (0x012D) or location linkage services
        val scanFilters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(SonyGpsService.LOCATION_SERVICE_UUID))
                .build(),
            ScanFilter.Builder()
                .setManufacturerData(0x012D, byteArrayOf())
                .build()
        )

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        mainHandler.postDelayed({
            if (isScanning) {
                scanner.stopScan(scanCallback)
                isScanning = false
                if (discoveredDeviceAddress == null) {
                    statusTextView.text = "Status: Scan finished. No camera found."
                }
            }
        }, SCAN_PERIOD_MS)

        scanner.startScan(scanFilters, settings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return
            }

            val deviceName = device.name ?: "Unknown Sony Camera"
            discoveredDeviceAddress = device.address
            statusTextView.text = "Status: Discovered Camera!\nName: $deviceName\nAddress: ${device.address}"
            
            if (isScanning) {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(this)
                isScanning = false
            }

            startServiceButton.isEnabled = true
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            isScanning = false
            statusTextView.text = "Status: BLE Scan failed (Error: $errorCode)"
        }
    }

    private fun toggleService(start: Boolean) {
        val address = discoveredDeviceAddress ?: return
        val serviceIntent = Intent(this, SonyGpsService::class.java)

        if (start) {
            serviceIntent.action = SonyGpsService.ACTION_START
            serviceIntent.putExtra(SonyGpsService.EXTRA_DEVICE_ADDRESS, address)
            ContextCompat.startForegroundService(this, serviceIntent)
            
            statusTextView.text = "Status: Service Active.\nConnected camera: $address"
            startServiceButton.isEnabled = false
            stopServiceButton.isEnabled = true
        } else {
            serviceIntent.action = SonyGpsService.ACTION_STOP
            startService(serviceIntent)
            
            statusTextView.text = "Status: Service Inactive."
            startServiceButton.isEnabled = true
            stopServiceButton.isEnabled = false
        }
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
}
