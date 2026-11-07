# DACN_QUANLYCONGVIECCANHAN

## Chạy backend an toàn

Backend không còn chứa mật khẩu cơ sở dữ liệu hoặc khóa JWT mặc định. Trước khi chạy, cấu hình các biến môi trường sau trong PowerShell:

```powershell
$env:DB_USERNAME = "smarttask_app"
$env:DB_PASSWORD = "<mật-khẩu-riêng-của-database>"
$env:JWT_SECRET = "<chuỗi-ngẫu-nhiên-tối-thiểu-32-byte>"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173"
$env:GEMINI_API_KEY = "<tùy-chọn>"
```

Có thể tạo khóa JWT ngẫu nhiên bằng `openssl rand -hex 32`. Tài khoản PostgreSQL nên là role riêng chỉ có quyền trên database ứng dụng, không dùng `postgres`/superuser. Các giá trị bí mật trước đây từng nằm trong cấu hình đã bị gỡ khỏi working tree; hãy thay mật khẩu database và tạo `JWT_SECRET` mới trước khi triển khai.

Sau đó chạy backend bằng `tools/apache-maven-3.9.6/bin/mvn.cmd -f backend/pom.xml spring-boot:run` và frontend từ `frontend` bằng `npm run dev`.
