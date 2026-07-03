## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2026-07-03 - Enforce Key Size and Secure Failure in Cryptography
**Vulnerability:** The AES-GCM key generation did not explicitly enforce a 256-bit key size, and cryptographic wrapper functions returned empty strings on failure instead of failing securely (e.g. returning null).
**Learning:** Returning empty strings on crypto failure can lead to data loss or overwrite bugs. In addition, default key sizes may vary by platform, so explicit key size enforcement is necessary for consistent security.
**Prevention:** Always explicitly set key sizes when generating keys (e.g., `setKeySize(256)`) and ensure that cryptographic operations fail securely by returning nullable types (like `String?`) and returning `null` or throwing exceptions on failure instead of default/empty values.
