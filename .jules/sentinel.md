## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.
## 2024-05-18 - [Secure Encyption configuration and Data overwrite protection]
**Vulnerability:** Android KeyStore AES GCM was missing explicit key size enforcement, and failure of decryption returned an empty string which could lead to accidental overwrite of user data and bypassing secure handling.
**Learning:** Returning default or empty values from critical security operations like decryption can lead to implicit data overwrite when the system interprets the empty response as legitimate lack of data rather than an error condition. Explicit key size (e.g. 256 bits) should always be specified.
**Prevention:** Fail securely in cryptographic operations by propagating errors (e.g., throwing exceptions) or using nullable return types (`String?`) and returning `null`. Always enforce AES key size to 256-bit rather than relying on default behaviors.
