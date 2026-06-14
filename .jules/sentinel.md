## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.
## 2024-05-30 - Fix insecure encryption exception handling and key size
**Vulnerability:** Weak AES key size and application crashes on encryption/decryption exceptions, exposing stack traces.
**Learning:** The application used a default 128-bit key size for AES-GCM and did not catch exceptions like `IndexOutOfBoundsException` during decryption, potentially causing crashes and leaking sensitive info.
**Prevention:** Explicitly specified a 256-bit key size and wrapped cryptographic operations in `try-catch` blocks to fail securely by returning an empty string.
