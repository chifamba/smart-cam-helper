## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2024-10-18 - Fix insecure cryptographic failure handling and enforce 256-bit AES
**Vulnerability:** The application fell back to returning empty strings `""` when decryption failed (e.g. `decryptString`), and AES keys were generated using default sizes rather than explicitly enforcing a strong 256-bit key size.
**Learning:** Returning empty strings upon cryptographic failure is an anti-pattern that can cause application state to become overwritten with invalid empty data. It masks exceptions and prevents failing securely. Moreover, depending on default parameter sizes for crypto generation is risky as defaults vary by platform API level.
**Prevention:** Always ensure cryptographic operations fail securely by returning null types (or throwing exceptions), and always explicitly specify strong key sizes (e.g. `setKeySize(256)`) when generating new keys.
