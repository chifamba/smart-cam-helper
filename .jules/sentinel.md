## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.
## 2024-06-11 - [CryptoManager] Enforce 256-bit AES Key and Catch Exceptions
**Vulnerability:** CryptoManager used AES-GCM for local encryption without explicitly setting the key size to 256 bits, potentially defaulting to a lower bound. In addition, it failed to handle bad decryption inputs properly which could crash the app (like IndexOutOfBoundsException when decryption data length < 12).
**Learning:** For `AndroidKeyStore`, explicitly specify `.setKeySize(256)` when using AES/GCM to guarantee security strength. Exception handling should cover bounds exceptions securely (by returning default/empty string values instead of leaking exception details or causing crashes).
**Prevention:** In the future, always default AES encryption keys explicitly to 256 bits, wrap decryption operations to gracefully catch runtime exceptions, and provide safe fallback paths.
