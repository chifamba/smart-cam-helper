## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.
## 2026-06-03 - [Added Input Length Limit to Prevent DoS via Storage Exhaustion]
**Vulnerability:** The application was downloading photo files directly from the camera over Wi-Fi without any maximum file size limits, risking device storage exhaustion and potential Denial of Service (DoS) attacks.
**Learning:** Network streams reading potentially large files should always enforce bounds. Unlimited stream reads can be exploited (or inadvertently fail) if the payload is disproportionately large, causing Out-of-Memory (OOM) errors or filling up local storage.
**Prevention:** Always implement maximum byte limits when reading from  InputStreams before writing them to local files.
## 2026-06-03 - [Added Input Length Limit to Prevent DoS via Storage Exhaustion]
**Vulnerability:** The application was downloading photo files directly from the camera over Wi-Fi without any maximum file size limits, risking device storage exhaustion and potential Denial of Service (DoS) attacks.
**Learning:** Network streams reading potentially large files should always enforce bounds. Unlimited stream reads can be exploited (or inadvertently fail) if the payload is disproportionately large, causing Out-of-Memory (OOM) errors or filling up local storage.
**Prevention:** Always implement maximum byte limits when reading from `HttpURLConnection` InputStreams before writing them to local files.
