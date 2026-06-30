## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2024-06-02 - Fix empty string returns on decryption failure
**Vulnerability:** Cryptographic wrapper functions (like `decryptString`) were returning an empty string on failure instead of propagating the error or returning a null value.
**Learning:** Failing open by returning an empty string from a crypto operation can lead to data loss and overwrite bugs where valid data gets overwritten by empty strings during state persistence.
**Prevention:** Always use nullable return types (`String?`) and return `null` or throw explicit exceptions when cryptographic operations fail to ensure the caller can handle the error securely.
