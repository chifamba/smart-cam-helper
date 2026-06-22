## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2024-05-18 - Missing Explicit AES Key Size Specification
**Vulnerability:** The Android Keystore AES (GCM) key generation in `CryptoManager.kt` was missing an explicit key size configuration, potentially falling back to weaker defaults.
**Learning:** Security configurations relying on Android Keystore defaults may result in inconsistent cryptographic strengths across different API levels. Also, catching exceptions and returning empty strings during decryption is a dangerous pattern that can lead to data loss.
**Prevention:** Always explicitly define critical cryptographic parameters (like `.setKeySize(256)`). For cryptographic failures, throw exceptions or return null to indicate failure clearly rather than returning empty strings which could be interpreted as valid empty data and overwrite existing values.
