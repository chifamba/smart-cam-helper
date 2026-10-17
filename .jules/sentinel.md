## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2024-06-02 - Fix insecure error handling in decryption fallback
**Vulnerability:** The `decryptString` wrapper functions returned an empty string (`""`) on error, creating a potential failure condition where an empty string could be interpreted as valid data, causing data loss or overwrite bugs.
**Learning:** Cryptographic functions and their wrappers should fail securely by returning a null or explicitly throwing an error, rather than falling back to an empty string which might bypass `isNullOrEmpty()` checks or corrupt persistent storage.
**Prevention:** Use nullable return types (`String?`) for cryptographic operations to ensure failures are explicitly handled as `null` by the calling code.
