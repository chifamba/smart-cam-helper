## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2026-06-15 - Secure Key Generation and Cryptographic Exception Handling
**Vulnerability:** Weak default key generation and unhandled cryptographic exceptions leading to potential crashes or data leakage.
**Learning:** The application generated AES-GCM keys without explicitly enforcing strong key sizes (e.g., 256-bit). Furthermore, the `CryptoManager.kt` lacked proper error handling, risking application crashes or potentially exposing sensitive stack traces/data when handling corrupted or malformed encrypted data (like `IndexOutOfBoundsException`).
**Prevention:** explicitly enforcing a 256-bit key size during AES key generation and implementing `try-catch` blocks within encryption/decryption routines that securely fail by returning empty strings (`""`).
