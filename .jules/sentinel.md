## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2026-10-16 - [Fix insecure cryptography fallback and key size]
**Vulnerability:** CryptoManager was missing explicit key size enforcement for AES key generation. The `decryptString` wrapper functions fell back to empty strings on decryption failures, which could result in data overwrites with empty values.
**Learning:** Returning default blank strings on decryption failures mask security failures and can lead to silent corruption. Crypto primitives must be properly configured and explicitly sized according to the requested algorithm strength (256-bit AES in this case).
**Prevention:** Explicitly enforce cryptographic parameters such as key sizes in security code. Use nullable types (`String?`) or exceptions rather than default blank strings to propagate decryption failures securely to call sites.
