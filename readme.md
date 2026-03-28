# 🚀 FiloraFS Lite – Spring Boot File Upload API Boilerplate

A **ready-to-use Spring Boot starter** for uploading, serving, deleting, and managing files via REST APIs.

👉 Full details: [https://buildbasekit.com/boilerplates/filora-fs-lite](https://buildbasekit.com/boilerplates/filora-fs-lite)

---

## ⚡ What is this?

FiloraFS Lite is a **minimal, production-ready file handling backend** you can plug into any application.

Perfect for:
- 📂 Document storage systems  
- 🎬 Media servers  
- 🎓 Student projects & demos  
- 💼 Freelance / client projects  

---

## ✨ Features

- Upload files via REST API  
- Stream/download files with correct MIME type  
- Delete files by name  
- List all uploaded files  
- Retrieve file metadata (size, type, last modified)  
- Includes Postman collection for quick testing  

---

## 🛠 Tech Stack

- Java 21  
- Spring Boot 3  
- Maven  

---

## 📡 API Overview
```

| Method | Endpoint                | Description          |
| ------ | ----------------------- | -------------------- |
| POST   | `/file`                 | Upload file          |
| GET    | `/file/{filename}`      | Download/stream file |
| DELETE | `/file/{filename}`      | Delete file          |
| GET    | `/file/list`            | List all files       |
| GET    | `/file/info/{filename}` | File metadata        |
```

---

## 🔥 Want the Full Version?

👉 **FiloraFS Pro** includes:

* Authentication & authorization
* Role-based access
* AWS S3 integration
* Production-ready architecture

Get it here:
👉 [https://buildbasekit.gumroad.com/l/filorafs-pro-self-hosted-file-storage](https://buildbasekit.gumroad.com/l/filorafs-pro-self-hosted-file-storage)

---

## 🎯 Who is this for?

* Spring Boot developers
* Students building real-world projects
* Freelancers who need a quick backend

---

## ⭐ Support

If this helped you:

* Give it a ⭐ on GitHub
* Share with other devs

---

## 🔗 About BuildBaseKit

We build **production-ready starter kits** so you can skip setup and ship faster.

🌐 [https://buildbasekit.com](https://buildbasekit.com)

For special requirements:
📩 **[hello@buildbasekit.com](mailto:hello@buildbasekit.com)**
