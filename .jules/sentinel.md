## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.
## 2026-06-12 - [Android KeyStore Default Key Size Vulnerability]
**Vulnerability:** Android KeyStore generated AES keys without explicitly defining the key size, falling back to potential system defaults which may be weaker than the required AES-256.
**Learning:** In Android cryptography, system defaults should not be trusted. The size must be explicitly declared during key generation.
**Prevention:** Always use `.setKeySize(256)` in `KeyGenParameterSpec.Builder` when generating AES keys for Android KeyStore.
