# FiloraFS-Lite 📁

Spring Boot file upload API boilerplate to handle upload, storage, and file management in minutes.

👉 Get full details & documentation: https://buildbasekit.com/boilerplates/filora-fs-lite  
⭐ Star this repo if it saves you time

---

## ❌ The Problem

Handling file uploads in Spring Boot usually means:
- writing upload/download logic from scratch  
- dealing with file storage and MIME types  
- repeating the same setup in every project  

---

## ✅ The Solution

FiloraFS-Lite gives you a ready-to-use file handling backend with upload, streaming, and management APIs so you can focus on building features.

---

## ⚡ Quick Start

### 1. Clone the repository
Download or clone the project from GitHub.

```bash
git clone https://github.com/buildbasekit/FiloraFS-Lite
cd FiloraFS-Lite
```

### 2. Run the application
Start the Spring Boot app instantly with zero configuration required.

```bash
./mvnw spring-boot:run
```

### 3. Test APIs
Open the built-in API Test UI in your browser:
**[http://localhost:8000/api-test](http://localhost:8000/api-test)**

Or use the **provided Postman collection**.

### 4. Configure for Production
Production overrides are managed via an optional `.env` file or directly in `application.properties`. See `.env.example` for details on securing the `X-API-KEY` and overriding the storage path.

---

## 🚀 Features

* Upload files via REST API
* API key-based authentication to secure all file endpoints
* Built-in dependency-free `/api-test` browser UI
* Stream/download files with correct MIME type
* Delete files by name
* List all uploaded files
* Retrieve file metadata
* Modern Java NIO file handling
* Secure against path traversal
* Clean and minimal backend structure (Spring Boot 4.1.1, Java 25)

---

## 🎯 Use Cases

* Document storage systems
* Media/file servers
* Backend starter projects
* Client/freelance projects

---

## 🚀 Need a Production-Ready Backend?

Get a complete backend with:

* Authentication
* File storage (S3)
* Clean architecture

👉 FiloraFS-Pro
[https://buildbasekit.gumroad.com/l/filorafs-pro-self-hosted-file-storage](https://buildbasekit.gumroad.com/l/filorafs-pro-self-hosted-file-storage)

---

Built by BuildBaseKit
[https://buildbasekit.com](https://buildbasekit.com)
