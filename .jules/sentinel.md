## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.
## 2024-05-15 - Enforce secure failure handling and strict AES-256 key size
**Vulnerability:** The application was failing insecurely by returning empty strings (`""`) upon decryption errors in `decryptString`. Empty strings could mistakenly be processed as valid states. In addition, the KeyGenerator was relying on provider default sizes instead of explicitly enforcing a 256-bit key size.
**Learning:** Defensive programming in Kotlin should leverage nullability (`String?`) to represent failure states securely and explicitly. Cryptographic parameters (such as `setKeySize(256)`) must be hardcoded rather than relying on underlying system defaults which might be weaker.
**Prevention:** Always use nullable return types for operations that can fail securely (e.g. cryptography, networking) instead of empty strings. Always explicitly define security parameters like key sizes and iterations when utilizing the Android Keystore.
