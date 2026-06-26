## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.
## 2024-05-18 - [Fail Securely: Returning Null for Cryptographic Failure]
**Vulnerability:** Crypto decryptions that failed would return empty strings `""`.
**Learning:** Returning empty strings on cryptographic failure introduces a risk of data loss or unintentional overwrites, specifically if code assumes an empty string means a legitimate empty value instead of a complete failure of the operation.
**Prevention:** Always ensure cryptographic operations fail securely by either throwing explicit exceptions or returning `null` to signal to the caller that the operation did not complete successfully, enabling safer fallback logic.
