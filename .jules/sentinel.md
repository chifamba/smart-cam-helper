## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2026-10-21 - Fix Fail-Open Empty String on Crypto Failure & Enforce AES-256 Key Size
**Vulnerability:** The `decryptString` function failed-open by returning an empty string (`""`) on decryption failure, leading to potential data corruption or overwrites. Furthermore, `CryptoManager.kt` was missing explicit enforcement of the 256-bit key size for AES-GCM.
**Learning:** Exception handling in cryptographic wrapper functions must explicitly return `null` or throw to indicate failure, avoiding ambiguous empty strings that hide errors. Explicit security property enforcement (e.g., `.setKeySize(256)`) is crucial to meet expected cryptographic standards.
**Prevention:** Cryptographic wrappers must use nullable return types (`String?`) and handle failures securely. Always explicitly specify key sizes during KeyGenParameterSpec building.
