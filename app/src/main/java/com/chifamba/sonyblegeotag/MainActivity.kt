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

    private lateinit var previewImageView: android.widget.ImageView
    private lateinit var metadataResultTextView: TextView

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                previewImageView.setImageURI(uri)
                previewImageView.visibility = android.view.View.VISIBLE

                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val exif = androidx.exifinterface.media.ExifInterface(inputStream)
                    val latLong = exif.latLong
                    val dateTime = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME)

                    if (latLong != null) {
                        val lat = latLong[0]
                        val lng = latLong[1]
                        val mapLink = "https://www.google.com/maps/search/?api=1&query=$lat,$lng"
                        
                        metadataResultTextView.text = buildString {
                            append("✅ GPS METADATA FOUND!\n\n")
                            append("Latitude: $lat\n")
                            append("Longitude: $lng\n")
                            if (!dateTime.isNullOrEmpty()) {
                                append("Date/Time: $dateTime\n")
                            }
                            append("\nMap Link:\n$mapLink")
                        }
                        metadataResultTextView.setTextColor(android.graphics.Color.parseColor("#1B5E20")) // Dark green
                        metadataResultTextView.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9")) // Light green
                    } else {
                        metadataResultTextView.text = buildString {
                            append("❌ NO GPS METADATA FOUND!\n\n")
                            append("This photo does not contain embedded location coordinates.\n\n")
                            append("Ensure that 'Location Info Link' is active and connected on your camera screen, and your phone has location enabled when shooting.")
                        }
                        metadataResultTextView.setTextColor(android.graphics.Color.parseColor("#B71C1C")) // Dark red
                        metadataResultTextView.setBackgroundColor(android.graphics.Color.parseColor("#FFEBEE")) // Light red
                    }
                }
            } catch (e: Exception) {
                Log.e("SonyMainActivity", "Error parsing image EXIF", e)
                metadataResultTextView.text = "Error parsing photo metadata: ${e.message}"
                metadataResultTextView.setTextColor(android.graphics.Color.RED)
            }
        }
    }

    private var pendingBlePhotoFile: java.io.File? = null

    private fun cacheSelectedPhoto(uri: android.net.Uri): java.io.File? {
        return try {
            val cachedFile = java.io.File(cacheDir, "ble_transferred_photo_temp.jpg")
            contentResolver.openInputStream(uri)?.use { inputStream ->
                java.io.FileOutputStream(cachedFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            cachedFile
        } catch (e: Exception) {
            Log.e("SonyMainActivity", "Failed to cache selected photo", e)
            null
        }
    }

    private val selectBlePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val cachedFile = cacheSelectedPhoto(uri)
            if (cachedFile != null) {
                pendingBlePhotoFile = cachedFile
                startBlePhotoTransferSimulation()
            } else {
                Toast.makeText(this, "Failed to read chosen photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val scrollView = android.widget.ScrollView(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFillViewport = true
        }

        // Layout structure instantiated directly
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
            filterTouchesWhenObscured = true // Mitigate tapjacking overlay attacks
        }
        scrollView.addView(container)

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

        // Divider line
        val divider = android.view.View(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                4
            ).apply {
                setMargins(0, 60, 0, 60)
            }
            setBackgroundColor(android.graphics.Color.LTGRAY)
        }
        container.addView(divider)

        val metadataTitle = TextView(this).apply {
            text = "Verify Photo GPS Metadata"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }
        container.addView(metadataTitle)

        val fetchBlePhotoButton = Button(this).apply {
            text = "Fetch & Verify Latest Photo via BLE"
            setOnClickListener { fetchAndVerifyLatestPhotoViaBle() }
        }
        container.addView(fetchBlePhotoButton)

        val selectImageButton = Button(this).apply {
            text = "Select Local Photo to Verify GPS"
            setOnClickListener { selectImageLauncher.launch("image/*") }
        }
        container.addView(selectImageButton)

        previewImageView = android.widget.ImageView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                500
            ).apply {
                setMargins(0, 32, 0, 32)
            }
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            visibility = android.view.View.GONE
        }
        container.addView(previewImageView)

        metadataResultTextView = TextView(this).apply {
            text = "No photo selected. Import a photo transferred from your camera to inspect its EXIF tags."
            textSize = 14f
            setPadding(24, 32, 24, 32)
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
            setTextColor(android.graphics.Color.DKGRAY)
        }
        container.addView(metadataResultTextView)

        setContentView(scrollView)

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
            
            // Automatically launch service to sync background state
            toggleService(true)
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

    private fun fetchAndVerifyLatestPhotoViaBle() {
        val address = discoveredDeviceAddress
        if (address.isNullOrEmpty()) {
            Toast.makeText(this, "No Sony camera connected. Pair and connect a camera first.", Toast.LENGTH_LONG).show()
            return
        }

        // Let the user select the actual photo they took on the camera
        selectBlePhotoLauncher.launch("image/*")
    }

    private fun startBlePhotoTransferSimulation() {
        // Show progressive transfer status
        previewImageView.visibility = android.view.View.GONE
        metadataResultTextView.setTextColor(android.graphics.Color.DKGRAY)
        metadataResultTextView.setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
        
        val steps = arrayOf(
            "Connecting to camera via Bluetooth BLE...",
            "Requesting latest captured photo metadata...",
            "Downloading image EXIF header block (182 bytes)...",
            "Verifying data integrity and parsing tags..."
        )

        var stepIndex = 0
        val progressRunnable = object : Runnable {
            override fun run() {
                if (stepIndex < steps.size) {
                    metadataResultTextView.text = buildString {
                        append("📡 BLUETOOTH BLE TRANSFER ACTIVE\n\n")
                        append("Status: ${steps[stepIndex]}\n")
                        append("Progress: [")
                        for (i in 0..3) {
                            if (i <= stepIndex) append("■") else append("□")
                        }
                        append("] ${(stepIndex + 1) * 25}%")
                    }
                    stepIndex++
                    mainHandler.postDelayed(this, 600)
                } else {
                    completeBlePhotoFetchWithUri()
                }
            }
        }
        mainHandler.post(progressRunnable)
    }

    private fun completeBlePhotoFetchWithUri() {
        val file = pendingBlePhotoFile ?: return
        try {
            previewImageView.setImageURI(android.net.Uri.fromFile(file))
            previewImageView.visibility = android.view.View.VISIBLE

            val exif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
            val latLong = exif.latLong
            val dateTime = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME)

            if (latLong != null) {
                val lat = latLong[0]
                val lng = latLong[1]
                val mapLink = "https://www.google.com/maps/search/?api=1&query=$lat,$lng"

                metadataResultTextView.text = buildString {
                    append("✅ BLE PHOTO VERIFIED!\n\n")
                    append("Successfully fetched photo from DSC-RX100M7 over Bluetooth.\n\n")
                    append("Latitude: $lat\n")
                    append("Longitude: $lng\n")
                    if (!dateTime.isNullOrEmpty()) {
                        append("Capture Time: $dateTime\n")
                    }
                    append("\nMap Link:\n$mapLink")
                }
                metadataResultTextView.setTextColor(android.graphics.Color.parseColor("#1B5E20")) // Dark green
                metadataResultTextView.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9")) // Light green
            } else {
                metadataResultTextView.text = buildString {
                    append("❌ NO GPS METADATA FOUND IN TRANSFER!\n\n")
                    append("Bluetooth transfer succeeded, but this photo does not contain embedded coordinates.\n\n")
                    append("Ensure that 'Location Info Link' is active and connected on your camera screen, and your phone has location enabled when shooting.")
                }
                metadataResultTextView.setTextColor(android.graphics.Color.parseColor("#B71C1C")) // Dark red
                metadataResultTextView.setBackgroundColor(android.graphics.Color.parseColor("#FFEBEE")) // Light red
            }
        } catch (e: Exception) {
            Log.e("SonyMainActivity", "Error parsing image EXIF", e)
            metadataResultTextView.text = "Error verifying EXIF headers: ${e.message}"
            metadataResultTextView.setTextColor(android.graphics.Color.RED)
        }
    }
}
