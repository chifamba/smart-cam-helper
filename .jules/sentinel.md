## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2024-10-27 - Fix Insecure Default AES Key Size
**Vulnerability:** The application used Android KeyStore with AES for local data encryption without explicitly specifying a key size, falling back to potential weak defaults (like 128-bit) instead of enforcing strong 256-bit encryption.
**Learning:** Default cryptographic parameters can be weak or become outdated. Explicitly specifying strong key sizes is critical for robust security posture and compliance with best practices.
**Prevention:** Always explicitly define cryptographic parameters like key size (e.g., `setKeySize(256)`) when generating keys using KeyGenerator.
