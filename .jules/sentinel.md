## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2024-06-05 - Fail Securely on Cryptographic Errors
**Vulnerability:** The application was catching exceptions during decryption and returning an empty string `""` instead of failing securely, which could lead to data loss or overwrite bugs if a valid encrypted payload failed to decrypt.
**Learning:** Cryptographic functions should not hide failures by returning valid but empty data types. This can cause the application state to become inconsistent or overwrite valid stored data with empty strings.
**Prevention:** Always fail securely by returning nullable types (e.g., `null`) or propagating the exception when cryptographic operations fail, and ensure calling code explicitly handles the failure state.
