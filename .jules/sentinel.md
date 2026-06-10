## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.
## 2024-05-24 - [Unhandled Keystore Exceptions Cause Crash Loops]
**Vulnerability:** The Android KeyStore encryption and decryption methods in `CryptoManager.kt` lacked exception handling. If a Keystore key is invalidated (e.g., user changes lock screen settings) or an encrypted payload is corrupted, it throws exceptions like `KeyPermanentlyInvalidatedException` or `AEADBadTagException`, causing the app to crash unrecoverably on startup until app data is wiped.
**Learning:** Android Keystore operations are highly susceptible to external state changes and payload corruption, making unhandled exceptions a critical availability/DoS risk for the application.
**Prevention:** Always wrap KeyStore cryptographic operations in `try-catch` blocks and fail securely (e.g., return empty strings or null) to handle invalidated keys or bad data gracefully without crashing the app.
