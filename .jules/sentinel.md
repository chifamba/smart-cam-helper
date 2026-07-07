## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2024-06-02 - Fix Insecure Cryptographic Failures and Enforce AES-256
**Vulnerability:** The application handled Android KeyStore decryption failures by returning an empty string `""`. In features such as Bluetooth address persistence, this resulted in silent overwrites, where corrupted or compromised local data would effectively nullify settings without indication. Additionally, AES keys were generated without strictly enforcing the 256-bit key size.
**Learning:** Cryptographic wrappers should never mask decryption errors with empty string default values. Cryptographic functions should employ nullable return types (or throw exceptions) so that the application layer is aware of the failure and can halt insecure states (failing securely).
**Prevention:** Always implement `null` return types on cryptography utilities (`String?` in Kotlin) and assert the correct key length during generation via `setKeySize(256)` in `KeyGenParameterSpec`.
