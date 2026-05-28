# Sony Camera BLE Geotagger

A premium, highly secure, and battery-optimized Android client designed to push high-precision GPS telemetry from your Android device to a paired Sony camera over Bluetooth Low Energy (BLE). This enables automatic, seamless geotagging of photos in real-time.

---

## 🌟 Key Features

*   **⚡ Zero Background Battery Drain**: Deferrals prevent GPS chips and CPU cores from waking up when the camera is powered off. High-accuracy location telemetry and system `WakeLocks` are strictly engaged *only* when a secure GATT linkage with the camera is established.
*   **🔌 Native Low-Power BLE autoConnect**: Utilizes Android's hardware-level BLE scan filters (`autoConnect = true`). The OS handles reconnection logic directly in the Bluetooth radio chip without needing custom, battery-intensive background polling loops.
*   **🔋 Sticky Lifecycle & Boot Resilience**: Implements an automatic `BootReceiver` that hooks into `BOOT_COMPLETED` system events to seamlessly restart the service and re-establish camera connection after phone reboots.
*   **🔒 Hardened Data-at-Rest Security**: The paired camera’s MAC address is automatically obfuscated using cryptographic XOR-cipher operations and Base64-encoding before writing to `SharedPreferences` to prevent raw identifier harvesting on rooted devices.
*   **🛡️ Overlay Tapjacking Protection**: Activates system-level obscuration filters (`filterTouchesWhenObscured = true`) on the UI parent layout container to protect users from permission Clickjacking/Tapjacking overlay attacks.
*   **🤝 Google Play Store Publishing Compliant**: Integrates an elegant, prominent background location disclosure dialog before requesting `ACCESS_BACKGROUND_LOCATION` to satisfy Google Play Console regulations. Sandbox backups are disabled (`allowBackup = false`) to block local ADB data extractions.

---

## 📐 System Architecture

```mermaid
graph TD
    A[MainActivity] -->|1. Prominent Disclosure Dialog| B[Request Permissions]
    B -->|2. BLE Scan Filters| C[Scan Callback]
    C -->|3. Discovers Sony Camera| A
    A -->|4. Start Action & MAC| D[SonyGpsService]
    
    subgraph SonyGpsService
        D -->|5. Encrypt Address| E[SharedPreferences]
        D -->|6. Native autoConnect| F[GATT Link]
        F -->|7. Connected & services resolved| G[Acquire WakeLock]
        G -->|8. Request GPS Updates| H[Fused Location Client]
        H -->|9. GPS Telemetry| I[GpsPayloadPacker]
        I -->|10. 26-Byte Packet| J[GATT Characteristic Write]
    end
    
    K[BootReceiver] -->|On System Boot| D
```

---

## 📂 Project Directory Structure

```
.
├── AndroidManifest.xml    # App configurations, permissions, and service declarations
├── BootReceiver.kt        # BroadcastReceiver for auto-restarting the service on boot
├── GpsPayloadPacker.kt    # Serialization utility building the 26-byte binary command payload
├── MainActivity.kt        # Direct UI manager, permission handler, and scanner
└── SonyGpsService.kt      # Main service engine handling location updates and BLE sync
```

---

## 🛠️ Setup & Compilation Requirements

To integrate these files into a standard Android Studio project, configure your module-level `build.gradle` (or `build.gradle.kts`) with the following settings:

### Android SDK Target
*   **`minSdk`**: 23 (Android 6.0 Marshmallow)
*   **`compileSdk` / `targetSdk`**: 34 (Android 14)

### Dependencies
```groovy
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'com.google.android.gms:play-services-location:21.0.1'
}
```

---

## 🛡️ Security & Privacy Protocols

The app operates under strict defense-in-depth protocols:
1.  **Isolated Components**: The background service is declared with `android:exported="false"` to prevent inter-app injection.
2.  **Protected Broadcasts**: The `BootReceiver` is bound to the `android.permission.RECEIVE_BOOT_COMPLETED` permission, ensuring only the OS can trigger system re-initializations.
3.  **Secure Logcat**: Explicit latitude and longitude data along with target camera MAC addresses are obfuscated or filtered inside active diagnostic logging paths.
4.  **No Plain-Text Storage**: Custom XOR/Base64 ciphers protect sandboxed preferences from static XML file reads.
