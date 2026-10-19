## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2024-06-03 - Enforce 256-bit AES Key Size and Secure Failure in Cryptography
**Vulnerability:** The application did not explicitly enforce a 256-bit AES key size during generation, and the  wrapper functions returned empty strings on failure, leading to potential data loss and overwrite bugs.
**Learning:** Cryptographic key generation should explicitly specify key sizes to prevent unintended weaker defaults, and cryptographic failures must be handled securely (e.g., returning null or throwing exceptions) instead of returning default/empty values that can corrupt application state.
**Prevention:** Explicitly use  during AES key generation and ensure all decryption wrappers return nullable types () to properly propagate failures.

## 2024-06-03 - Enforce 256-bit AES Key Size and Secure Failure in Cryptography
**Vulnerability:** The application did not explicitly enforce a 256-bit AES key size during generation, and the `decryptString` wrapper functions returned empty strings on failure, leading to potential data loss and overwrite bugs.
**Learning:** Cryptographic key generation should explicitly specify key sizes to prevent unintended weaker defaults, and cryptographic failures must be handled securely (e.g., returning null or throwing exceptions) instead of returning default/empty values that can corrupt application state.
**Prevention:** Explicitly use `.setKeySize(256)` during AES key generation and ensure all decryption wrappers return nullable types (`String?`) to properly propagate failures.
