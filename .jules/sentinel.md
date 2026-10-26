## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.
\n## 2026-10-26 - Fail securely on decryption error and enforce AES 256-bit key
**Vulnerability:** The application returned an empty string upon decryption failure instead of failing securely, potentially leading to data loss/overwrite bugs. The AES key generation also didn't explicitly enforce a 256-bit key size.
**Learning:** Returning a valid type (like an empty string) on decryption failure is dangerous. Instead, cryptographic wrappers should fail securely by returning a nullable type or throwing an exception. Security constraints like key size must be explicitly enforced.
**Prevention:** Implement explicit failure states (like `null`) and enforce explicit security constraints during key generation.
