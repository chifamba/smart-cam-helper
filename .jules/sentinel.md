## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-08 - Explicit AES-256 key size and bounds check
**Vulnerability:** Key generation did not enforce 256-bit size, risking downgrade, and decryption lacked bounds checks and error handling, risking crashes.
**Learning:** Default key sizes in Android Keystore might change or downgrade depending on the provider. It's safer to explicitly enforce the key size. Unvalidated payload sizes can crash the app when reading IV.
**Prevention:** Always use `.setKeySize(256)` on `KeyGenParameterSpec.Builder` and handle index bounds securely in `decrypt`.
