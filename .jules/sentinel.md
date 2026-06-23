## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2024-06-23 - Explicit Cryptography Key Size & Secure Failure

**Vulnerability:** The application was using `KeyGenerator` without explicitly specifying the key size. This leaves the system reliant on defaults, which may be weaker (e.g., 128-bit). Additionally, on decryption failure, the system previously fell back to an empty string `""` instead of failing securely, which could lead to data loss or unintended logic bugs due to empty string validations passing.
**Learning:** Always explicitly enforce key strength (e.g. 256-bit for AES) during key generation rather than relying on dynamic defaults. In cryptographic operations, never swallow exceptions to return empty strings; failures must return `null` or throw to trigger proper fallback logic and avoid corrupting state.
**Prevention:** Hardcode `.setKeySize(256)` in AES key builder configurations. Enforce `null` return types on failed decrypt operations across all components.
