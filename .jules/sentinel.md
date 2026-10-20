## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2026-10-20 - Prevent Data Overwrites on Crypto Failure
**Vulnerability:** Cryptographic wrapper functions (`decryptString`) returned an empty string `""` upon failure (e.g., due to key loss or corruption). This caused calling functions to interpret the failure as a valid but empty configuration value, potentially overwriting valid persisted data with empty strings.
**Learning:** Security wrappers must fail securely and unambiguously. Returning an empty string masks the error and introduces logic bugs.
**Prevention:** Always use nullable return types (e.g., `String?`) for cryptographic operations that can fail, and return `null` or throw exceptions instead of returning empty fallback strings.
