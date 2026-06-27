## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-01 - Fix SSRF Vulnerability in Wi-Fi Photo Download
**Vulnerability:** The application parsed photo URLs from a JSON response over a local HTTP connection without validating the protocol or host, allowing potential Server-Side Request Forgery (SSRF) if a malicious hotspot supplied arbitrary URLs.
**Learning:** Blindly trusting URLs provided by an external device or network service, even on a local connection, can lead to SSRF vulnerabilities.
**Prevention:** Always validate the protocol and host of URLs received from external sources against an expected allowlist before initiating requests.

## 2024-06-02 - Fix Data Loss Due to Empty String Cryptographic Return Values
**Vulnerability:** Cryptographic functions `encryptString` and `decryptString` failed by returning empty strings (`""`) rather than `null`. This empty string could be interpreted as a valid saved value, resulting in silent data corruption or overwrite bugs, instead of triggering proper fallback mechanisms.
**Learning:** Cryptographic functions should always fail securely by returning a distinct null value (or throwing an exception) to indicate failure. This is especially true when an empty string could be mistakenly parsed as empty data.
**Prevention:** Ensure cryptographic operations use nullable return types (`String?`) when catching exceptions, returning `null` to explicitly denote a failure state.
