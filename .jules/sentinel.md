## 2024-05-30 - Fix insecure encryption fallback and cleartext traffic
**Vulnerability:** Weak XOR encryption fallback and cleartext HTTP traffic allowed.
**Learning:** The application had an insecure fallback to XOR encryption if AES-GCM failed, and permitted cleartext HTTP traffic which is risky.
**Prevention:** Removed the XOR fallback to ensure failures fail securely and disabled cleartext traffic in AndroidManifest.xml.
## 2024-05-24 - SSRF Protection on Camera IP Validation
**Vulnerability:** The application blindly parsed and executed HTTP GET requests to URLs returned in a JSON payload from the Sony Camera over an unencrypted Wi-Fi link. An attacker performing a MITM attack could inject arbitrary URLs pointing to local network devices, causing the app to perform unauthorized requests (SSRF).
**Learning:** Even when connected to a "trusted" or isolated Wi-Fi AP (like a camera's hotspot), URLs retrieved from external inputs must be validated before creating connections.
**Prevention:** Added an SSRF protection check that ensures `url.host` exactly matches the expected Camera IP address (retrieved via DHCP gateway) before opening an `HttpURLConnection`.
