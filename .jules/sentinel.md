## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.
## 2024-06-03 - Incomplete Error Handling Causes State Overwrite\n**Vulnerability:** `decryptString` failing with empty strings incorrectly populated uninitialized variables and silently replaced persisted state.\n**Learning:** Nullability must be used as an explicit signal for failed decryptions rather than default values like empty strings, preventing accidental state changes.\n**Prevention:** Return nullable types (e.g., `String?`) for cryptographic failures and appropriately update calling sites to safely handle potential nulls.
