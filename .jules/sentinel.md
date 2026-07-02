## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.
## 2026-07-02 - Enforce 256-bit key size for AES in CryptoManager
**Vulnerability:** The AES KeyGenerator was not explicitly configured with a 256-bit key size.
**Learning:** Depending on the Android version and KeyStore provider,  defaults might change or use weaker sizes (like 128-bit) if not explicitly set.
**Prevention:** Always enforce strong key lengths (e.g., 256 bits) using  on the  when generating cryptographic keys.
## 2024-06-03 - Enforce 256-bit key size for AES in CryptoManager
**Vulnerability:** The AES KeyGenerator was not explicitly configured with a 256-bit key size.
**Learning:** Depending on the Android version and KeyStore provider, KeyGenerator.getInstance defaults might change or use weaker sizes (like 128-bit) if not explicitly set.
**Prevention:** Always enforce strong key lengths (e.g., 256 bits) using .setKeySize(256) on the KeyGenParameterSpec.Builder when generating cryptographic keys.
