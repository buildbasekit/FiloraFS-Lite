# 🔐 Security Policy

## 📌 Supported Versions

This project is actively maintained as part of **BuildBaseKit**.  
Only the latest version is supported with security updates.

---

## 🚨 Reporting a Vulnerability

If you discover a security vulnerability, please report it responsibly.

**Do NOT open a public issue.**

Instead, contact:

📧 [hello@buildbasekit.com](mailto:hello@buildbasekit.com)

---

## 🛡️ What to Include

Please provide:

* Description of the vulnerability  
* Steps to reproduce  
* Potential impact  
* Suggested fix (if available)  

---

## ⏱️ Response Time

* Initial response: within 48 hours  
* Resolution timeline depends on severity  

---

## 🔒 Security Best Practices

This project provides a file management system. It follows basic secure practices like:

* Controlled file upload handling  
* UUID-based file naming  
* API key-based access control  

However, you should always:

* Validate file types (not just MIME type)  
* Restrict file size limits  
* Sanitize filenames to prevent path traversal  
* Store files outside public directories when needed  
* Protect API keys using environment variables  
* Use HTTPS in production  
* Regularly update dependencies  

---

## ⚠️ Disclaimer

This project is a boilerplate intended for learning and rapid development.

It is your responsibility to:

* Review file handling security  
* Configure access control properly  
* Adapt storage strategy for production (e.g., cloud storage)  

---

## 🌐 BuildBaseKit

This project is part of **BuildBaseKit**  
👉 https://buildbasekit.com  

---

Security is a shared responsibility. Report issues responsibly.