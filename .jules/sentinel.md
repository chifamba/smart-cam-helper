## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.

## 2024-06-09 - Fix insecure cryptographic failures and explicit key size definition
**Vulnerability:** Application failed insecurely by not catching exceptions during cryptographic operations which could lead to crashes and information leaks. AES key generation size was implicit.
**Learning:** Implicit key sizes and missing exception handling in cryptographic procedures like Android KeyStore operations expose the app to potential denial of service or information leakage.
**Prevention:** Always wrap cryptographic operations in try/catch blocks that fail securely by returning empty strings/defaults without leaking exception details. Explicitly define key sizes like .setKeySize(256) for AES key generation.
