## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2024-06-05 - Fix insecure cryptographic operations
**Vulnerability:** Cryptographic operations returned empty strings on failure and AES key size was not explicitly enforced to 256 bits.
**Learning:** Cryptographic functions should fail securely by returning `null` (or throwing exceptions) rather than empty strings to avoid data loss and overwrite bugs. Additionally, Android Keystore keys should have explicitly set sizes to ensure the intended security level (e.g., 256-bit AES).
**Prevention:** Use nullable return types like `String?` for cryptographic wrappers to return `null` on exceptions. Explicitly call `.setKeySize(256)` when building `KeyGenParameterSpec` for AES keys.
