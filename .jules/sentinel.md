## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2024-06-03 - Enforce explicit key size in Android Keystore
**Vulnerability:** Relying on default parameters for KeyGenParameterSpec in Android Keystore can sometimes result in weaker key sizes (e.g., 128-bit) instead of the intended stronger sizes like 256-bit, failing to meet the specified security constraints.
**Learning:** Security parameters, especially cryptographic key sizes, should not be implicitly trusted to default to the most secure option.
**Prevention:** Always explicitly define security parameters such as key sizes (e.g., `.setKeySize(256)`) when generating cryptographic keys to ensure compliance with intended security standards.
