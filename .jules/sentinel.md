## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2024-06-03 - Fix insecure encryption fallback overwrite vulnerability
**Vulnerability:** The application was failing open by returning an empty string when cryptographic decryption failed, potentially leading to persistent state being overwritten with empty values.
**Learning:** Returning default empty values (like "") during decryption failures can lead to unexpected state overwrite bugs that corrupt stored preferences or data.
**Prevention:** Always fail securely by returning nullable types (like `String?`) and explicitly returning `null` or throwing exceptions to indicate failure in cryptographic operations.
