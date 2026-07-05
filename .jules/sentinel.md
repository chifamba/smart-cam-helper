## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2024-06-03 - Fix insecure encryption key size and decryption failure handling
**Vulnerability:** AES-GCM encryption did not explicitly enforce a 256-bit key size, and cryptographic operations handled failures insecurely by returning empty strings instead of null.
**Learning:** Implicit key sizes can lead to weaker encryption if defaults change, and returning empty strings on decryption failures can lead to logic bugs and data loss where an empty string is treated as valid data (instead of indicating failure).
**Prevention:** Always explicitly enforce intended encryption key sizes (e.g., 256 bits). Cryptographic functions should always fail securely by returning null or throwing exceptions instead of empty strings or default values.
