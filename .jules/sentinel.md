## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2024-06-21 - Exception leakage and explicit key size during local data encryption
**Vulnerability:** Weak default key sizing configurations or crashes leading to application-level denial of service via uncaught encryption and decryption exceptions.
**Learning:** `CryptoManager.kt` relied on default sizes and could throw unhandled exceptions if the AndroidKeyStore encountered unexpected states, crashing the process.
**Prevention:** Explicitly enforce 256-bit KeyGenParameterSpec configurations. Always wrap Keystore cryptographic operations in try-catch blocks and handle failure states by returning an empty string. Validate that ciphertext is at least 28 bytes (12-byte IV + 16-byte GCM authentication tag) before attempting decryption. Log errors without including the exception stacktrace to avoid leaking implementation details.
