## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.
## 2024-05-18 - Fix Fail-Open Cryptography & Set Key Size
**Vulnerability:** `decryptString` in Android keystore integrations caught exceptions and returned empty strings (`""`) by default.
**Learning:** Returning empty strings on crypto failures can silently overwrite valid saved preferences/data in the app, losing paired camera configurations rather than cleanly failing.
**Prevention:** Failing securely means throwing exceptions or returning nullable types (`null`) so callers handle state cleanly without data loss.
