## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2024-06-03 - Enforce 256-bit AES key size and secure crypto failures
**Vulnerability:** AES key generation lacked explicit key size enforcement, and decryption failures returned empty strings instead of null or exceptions, risking data overwrite bugs.
**Learning:** Default key generation without explicit size might lead to weaker encryption depending on provider defaults, and masking crypto failures with empty strings makes applications fail insecurely.
**Prevention:** Always explicitly set key size during generation (e.g., `setKeySize(256)`) and ensure crypto wrapper functions fail securely by returning nullable types (`String?`) or throwing exceptions.
