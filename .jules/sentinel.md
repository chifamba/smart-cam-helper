## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2024-06-03 - Secure Failures for Cryptographic Decryption
**Vulnerability:** `decryptString` wrapper functions returned empty strings (`""`) upon decryption exceptions (e.g., incorrect keys, corrupted data), which could silently overwrite legitimate data leading to data loss or subtle bugs.
**Learning:** Cryptographic functions should fail securely. Returning empty strings masks errors and creates vulnerabilities where corrupted/invalid data silently replaces valid data.
**Prevention:** Always return nullable types (e.g., `String?`) and `null` on failure, or throw explicit exceptions when decryption fails, allowing callers to handle the error properly rather than propagating bad state.
