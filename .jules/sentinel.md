## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.
## 2024-06-12 - [AES Key Size and Error Handling Enhancement]
**Vulnerability:** Default AES key generation didn't explicitly enforce 256-bit keys, and unhandled encryption/decryption exceptions could leak sensitive information via stack traces or crash the app.
**Learning:** Android Keystore defaults to 128-bit AES keys if not explicitly defined. Unhandled exceptions in crypto operations can lead to information disclosure or DoS via app crash.
**Prevention:** Always explicitly define `.setKeySize(256)` during key generation and wrap crypto operations in try-catch blocks to fail securely.
