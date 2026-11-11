# SMARTTASK - QUY TẮC DỰ ÁN & BỘ NHỚ HỆ THỐNG DÀI HẠN (SYSTEM BRAIN)

> **Tài liệu này là "Bộ nhớ dài hạn tối thượng (Single Source of Truth)"** của toàn bộ hệ thống **SmartTask AI (Hệ thống Quản lý và Tự động Lập lịch Công việc Cá nhân)**.  
> Mọi Agent, Subagent và mọi phiên chat mới (**New Chat**) **BẮT BUỘC** đọc và tuân thủ 100% các quy chuẩn kiến trúc, cơ sở dữ liệu, bảo mật, nghiệp vụ và quy trình kiểm thử dưới đây để tuyệt đối không làm gãy vỡ chức năng cũ hoặc gây lỗi hồi quy (regression).

---

## 1. TỔNG QUAN KIẾN TRÚC & CÔNG NGHỆ CHÍNH

### 1.1. Công nghệ & Cổng kết nối (Ports & Tech Stack)
- **Frontend**:
  - React 18 / TypeScript / Vite.
  - Cổng chạy: `http://localhost:5173`.
  - Styling: **100% Vanilla CSS** kết hợp CSS Custom Properties (biến CSS), hỗ trợ Dark/Light mode cao cấp, Typography Google Font `Inter`. **Tuyệt đối KHÔNG tự ý cài đặt hay dùng TailwindCSS** khi người dùng chưa yêu cầu.
  - HTTP Client: Axios (`frontend/src/services/api.ts`) với Request Interceptor tự động đính kèm `Authorization: Bearer <token>` và Response Interceptor bắt mã lỗi `401` để phát sự kiện `smarttask:session-expired`.
- **Backend**:
  - Spring Boot 4.1.2 (Spring Framework 7) / Java 21 LTS.
  - Cổng chạy: `http://localhost:8085`, `server.servlet.context-path=/api`.
  - Quản lý build: Apache Maven 3.9+ (`backend/pom.xml`).
- **Database**:
  - PostgreSQL 17.11 chạy cổng `5432` (database name: `quanlycongviec_db`).
  - Múi giờ chuẩn hệ thống: **`Asia/Ho_Chi_Minh` (GMT+7)** xuyên suốt từ Database, JPA/Jackson (`spring.jackson.time-zone=Asia/Ho_Chi_Minh`) đến giao diện người dùng.
- **Git Workflow**:
  - Nhánh làm việc chính: **`dev`**. Mọi commit và push đều hướng về nhánh `dev`. Nhánh `main` chỉ dùng khi bàn giao chính thức.

---

## 2. QUY CHUẨN CƠ SỞ DỮ LIỆU & ÁNH XẠ JPA (DATABASE & ENTITIES)

Cơ sở dữ liệu được định nghĩa chuẩn hóa 3NF tại [CSDL_CHI_TIET_TRUONG_DU_LIEU.md](file:///d:/DACN_QUANLYCONGVIECCANHAN/CSDL_CHI_TIET_TRUONG_DU_LIEU.md) và kịch bản [database/init_db_quanlycongviec.sql](file:///d:/DACN_QUANLYCONGVIECCANHAN/database/init_db_quanlycongviec.sql).

### 2.1. Quy ước đặt tên bất biến (Naming Conventions):
- **Tên bảng**: 100% chữ thường (lowercase), **tuyệt đối không dùng dấu gạch dưới `_`**.  
  *Danh sách bảng chuẩn: `nguoidung`, `congviec`, `khoithoigian`, `danhmuc`, `khonggianlamviec`, `the`, `congvieccon`, `caidatnguoidung`, `thongbao`, `nhatkynangsuat`, `nhatkythoiquen`, `phientaptrung`, `nhacnho`, `nhatkyai`, `vaitro`, `vaitronguoidung`.*
- **Tên thuộc tính (cột)**: Viết hoa từng từ (PascalCase), **tuyệt đối không dùng dấu gạch dưới `_`**.  
  *Ví dụ: `HoVaTen`, `NgayTao`, `HanChot`, `TieuDe`, `MaMau`, `TrangThai`, `MucDoUuTien`, `GocPhanTu`, `KhoaRiengTu`, `ThoiLuongUocTinh`, `LapLai`...*
- **Cấu hình JPA**: Bắt buộc sử dụng `PhysicalNamingStrategyStandardImpl` trong `application.properties` để Hibernate giữ nguyên tên cột PascalCase khi sinh câu truy vấn:
  ```properties
  spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
  ```

### 2.2. Danh sách Entity cốt lõi (`backend/src/main/java/com/taskmanager/entity/`):
*Toàn bộ tên file Entity và class Java đã được chuẩn hóa đồng bộ 100% sang tiếng Việt tương ứng với tên bảng PostgreSQL:*
1. **`NguoiDung` (Bảng `nguoidung`, file `NguoiDung.java`)**: Quản lý tài khoản, email duy nhất, `MatKhau` (BCrypt), `HoVaTen`, `SoDienThoai`, `AnhDaiDien`, `NguonXacThuc` (`LOCAL`/`GOOGLE`), `KichHoat` (`isActive`), liên kết Many-to-Many với `VaiTro` qua bảng `vaitronguoidung`.
2. **`CongViec` (Bảng `congviec`, file `CongViec.java`)**:
   - `TieuDe`, `MoTa`, `GhiChu`.
   - `MucDoUuTien`: `LOW`, `MEDIUM`, `HIGH`, `URGENT`.
   - `GocPhanTu` (Eisenhower): `DO_FIRST` (Q1), `SCHEDULE` (Q2), `DELEGATE` (Q3), `DONT_DO` (Q4).
   - `TrangThai`: `TODO`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `OVERDUE`.
   - `HanChot`: `OffsetDateTime`.
   - `ThoiLuongUocTinh`, `ThoiLuongThucTe`.
   - `KhoaRiengTu`: `Boolean isLocked` (đánh dấu task riêng tư cần bảo vệ mật khẩu).
   - `LapLai` (`isRecurring`), `QuyTacLapLai` (`recurrenceRule`: `DAILY`, `WEEKDAYS`, `WEEKLY`, `MONTHLY`).
   - `NguoiDungId`, `KhongGianId`, `DanhMucId`.
3. **`KhoiThoiGian` (Bảng `khoithoigian`, file `KhoiThoiGian.java`)**:
   - Lịch trình theo khung giờ cụ thể: `ThoiGianBatDau`, `ThoiGianKetThuc` (`OffsetDateTime`).
   - `CongViecId` (liên kết với Task hoặc sự kiện độc lập).
   - `TuDongXepLich` (`Boolean isAutoScheduled`): Đánh dấu khối do AI tự phân bổ.
   - `TrangThai`: `SCHEDULED`, `COMPLETED`, `CANCELLED`.
   - `MaMau` (`colorCode`), `GhiChu` (`note`), `KhoaRiengTu` (`isLocked`).
4. **`KhongGianLamViec` (Bảng `khonggianlamviec`, file `KhongGianLamViec.java`)**:
   - `TenKhongGian`, `TenBieuTuong`, `MacDinh` (`isDefault`).
   - Phân tách độc lập dữ liệu công việc (Cá nhân, Công việc, Học tập...).
5. **`DanhMuc` (Bảng `danhmuc`, file `DanhMuc.java`)**:
   - `TenDanhMuc`, `MaMau`, `TenBieuTuong`, `MacDinh` (`isDefault`), thuộc `NguoiDungId` và `KhongGianId`.
6. **`CongViecCon` (Bảng `congvieccon`, file `CongViecCon.java`)**:
   - `CongViecId`, `TieuDe`, `HoanThanh` (`isCompleted`), `ThoiLuongPhut`, `ThuTu` (`orderIndex`).
7. **`NhatKyThoiQuen` (Bảng `nhatkythoiquen`, file `NhatKyThoiQuen.java`)**:
   - Theo dõi thói quen hàng ngày: `CongViecId`, `NgayGhiNhan` (`LocalDate`), `TrangThai` (`COMPLETED`/`SKIPPED`), `GhiChu`.
   - Unique constraint: `("CongViecId", "NgayGhiNhan")`.
8. **`NhatKyNangSuat` (Bảng `nhatkynangsuat`, file `NhatKyNangSuat.java`)**:
   - `NgayBaoCao` (`LocalDate`), `TongCongViecHoanThanh`, `TongPhutTapTrung`, `TyLeDungHan`, `SoCongViecQuaHan`, `NhanXetAI`.
   - Unique constraint: `("NguoiDungId", "NgayBaoCao")`.
9. **`ThongBao` (Bảng `thongbao`, file `ThongBao.java`)**:
   - `LoaiThongBao` (`reminder`, `ai`, `alert`, `success`), `TieuDe`, `NoiDung`, `DaDoc` (`isRead`), `DaXoa` (`isDeleted`).
10. **`CaiDatNguoiDung` (Bảng `caidatnguoidung`, file `CaiDatNguoiDung.java`)**:
    - `GioBatDauLamViec`, `GioKetThucLamViec`, `NgayLamViec` (`MON,TUE...`), `ThoiGianDemPhut` (`bufferMinutes`), `ThoiGianHieuSuatCao` (`peakProductivityTime`), `TuDongDoiLich`, `MuiGio`, `GiaoDien` (`LIGHT`/`DARK`), `HinhNenUrl`, `LoaiHinhNen` (`NONE`/`IMAGE`/`VIDEO`), `DoPhuToi` (`overlayDarkness` 0-100%).
11. **`NhatKyAi` (Bảng `nhatkyai`, file `NhatKyAi.java`)**:
    - `LoaiHanhDong` (`AUTO_SCHEDULE`, `TASK_BREAKDOWN`, `NATURAL_PARSING`, `RESCHEDULE_OVERDUE`), `CauLenhDauVao`, `PhanHoiAI` (jsonb), `TrangThai`, `SoToken`, `ThoiGianXuLyMs`.
12. **`VaiTro` (Bảng `vaitro`, file `VaiTro.java`)**:
    - Quản lý vai trò / phân quyền người dùng (`ROLE_USER`, `ROLE_ADMIN`).
13. **`LichSuMedia` (Bảng `lich_su_media`, file `LichSuMedia.java`)**:
    - Lưu trữ lịch sử tải lên hình nền / video cá nhân hóa của người dùng.

---

## 3. BẢO MẬT & XÁC THỰC (AUTHENTICATION & SECURITY)

### 3.1. Cơ chế JWT (HMAC-SHA256):
- Xử lý tại [JwtTokenService.java](file:///d:/DACN_QUANLYCONGVIECCANHAN/backend/src/main/java/com/taskmanager/security/JwtTokenService.java). Khóa bí mật (`app.jwt.secret`) **bắt buộc phải có độ dài từ 32 bytes UTF-8 trở lên** để thỏa mãn thuật toán HMAC-SHA256 của thư viện `jjwt 0.12.x`.
- Khi người dùng đăng nhập thành công qua `POST /api/auth/login`, Backend cấp Bearer token có chứa `sub` là `userId` và claim `email`.
- Bộ lọc [JwtAuthenticationFilter.java](file:///d:/DACN_QUANLYCONGVIECCANHAN/backend/src/main/java/com/taskmanager/security/JwtAuthenticationFilter.java):
  - Chặn mọi request, trích xuất token từ header `Authorization: Bearer <token>`.
  - Giải mã `userId`, kiểm tra người dùng tồn tại và `isActive = true`.
  - Nạp danh tính vào `SecurityContextHolder`:
    ```java
    UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userId, null, authorities);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    ```
- **Tuyệt đối không sử dụng header giả mạo như `X-User-Email`** để xác thực người dùng.
- Mọi Controller/Service lấy ID người dùng hiện tại thông qua [UserResolver.java](file:///d:/DACN_QUANLYCONGVIECCANHAN/backend/src/main/java/com/taskmanager/util/UserResolver.java) bằng `userResolver.resolveUserId(request)`.

### 3.2. Cấu hình Properties & Fallback an toàn (Tránh lỗi `PlaceholderResolutionException`):
File [application.properties](file:///d:/DACN_QUANLYCONGVIECCANHAN/backend/src/main/resources/application.properties) **bắt buộc luôn có giá trị mặc định dự phòng (fallback)** sau dấu `:` cho tất cả các biến môi trường để khi chạy ở môi trường máy cá nhân (Local) không bị sập ứng dụng:
```properties
# Spring Datasource (có fallback mặc định an toàn cho local)
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/quanlycongviec_db}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:q1a2z3x4}
spring.datasource.driver-class-name=org.postgresql.Driver

# JWT Secret (fallback chuỗi hex 64 ký tự = 64 bytes)
app.jwt.secret=${JWT_SECRET:9a4f2c8d3b7a1e6f4c5d2e8a7b9c1d3e5f7a9b1c3d5e7f9a1b3c5d7e9f1a3b5c}
app.jwt.expiration-ms=86400000

# Google Gemini API
gemini.api.key=${GEMINI_API_KEY:}
gemini.api.model=${GEMINI_API_MODEL:gemini-1.5-flash}
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models

# CORS Origins
app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173}
```

### 3.3. Quy chuẩn Mật khẩu & Đăng ký:
- Mã hóa bằng BCrypt (`PasswordEncoder` độ mạnh `12`).
- Khi đăng ký / đổi mật khẩu: Mật khẩu phải từ **8 đến 50 ký tự**, không được vượt quá **72 bytes** (giới hạn an toàn của thuật toán băm BCrypt).
- Khi sai thông tin đăng nhập hoặc tài khoản không tồn tại, Backend luôn trả về thông báo lỗi chung *"Email hoặc mật khẩu không chính xác."* để phòng chống tấn công dò quét tài khoản (Username Enumeration).

---

## 4. NGHIỆP VỤ & TÍNH NĂNG CỐT LÕI (CORE BUSINESS RULES)

### 4.1. Tính năng Khóa riêng tư (Privacy Lock):
- Cho phép người dùng đánh dấu các Task hoặc TimeBlock là **riêng tư** (`isLocked = true` / `KhoaRiengTu`) và bảo vệ bằng mật khẩu riêng tư.
- Khi chưa mở khóa: Thẻ hiển thị dạng bị mờ (blurred / masked), tiêu đề ẩn đi thành dạng bảo mật và có biểu tượng ổ khóa đóng.
- Modal & Popover mở khóa: [PrivacyPasswordModal.tsx](file:///d:/DACN_QUANLYCONGVIECCANHAN/frontend/src/components/PrivacyPasswordModal.tsx) & [PrivacyCalloutPopover.tsx](file:///d:/DACN_QUANLYCONGVIECCANHAN/frontend/src/components/PrivacyCalloutPopover.tsx).
- **Quy tắc VÀNG trên Calendar Card ([WeeklyCalendar.tsx](file:///d:/DACN_QUANLYCONGVIECCANHAN/frontend/src/components/WeeklyCalendar.tsx) & [MonthCalendar.tsx](file:///d:/DACN_QUANLYCONGVIECCANHAN/frontend/src/components/MonthCalendar.tsx))**:
  - Khi thẻ đang bị khóa: Phải có **nút mở khóa** (sử dụng icon [EyeLockIcon.tsx](file:///d:/DACN_QUANLYCONGVIECCANHAN/frontend/src/components/EyeLockIcon.tsx) `locked={true}`) để bấm mở nhanh qua popover.
  - Khi thẻ đã được mở khóa: **PHẢI CÓ nút khóa lại** (`event-btn-unlock` với `locked={false}`) đặt ngay cạnh nút hoàn thành để người dùng có thể khóa lại bất cứ lúc nào.
  - **TUYỆT ĐỐI KHÔNG ĐƯỢC XÓA NHẦM HOẶC LÀM MẤT 2 NÚT NÀY KHI SỬA GIAO DIỆN LỊCH.**

### 4.2. Lịch Tuần & Lịch Tháng (Weekly & Monthly Calendar):
- Hiển thị theo lưới **24 giờ liên tục** (00:00 - 23:59) chuẩn múi giờ GMT+7 (`Asia/Ho_Chi_Minh`).
- Hiển thị TimeBlocks theo màu sắc của danh mục tương ứng (hoặc mã màu override `colorOverride`).
- Hỗ trợ đầy đủ:
  - Kéo và thả (Drag & Drop) di chuyển khung giờ hoặc chuyển sang ngày khác.
  - Thu phóng (Resize) cạnh trên/dưới để tăng/giảm thời lượng công việc.
  - Xem trước ghi chú khi di chuột (Hover note preview): Di chuột vào thẻ sẽ hiển thị popover thông tin chi tiết mà không làm vỡ bố cục hay bị che khuất viền lịch.
  - Nhân bản nhanh công việc (Quick Duplicate flyout popover).
  - Đánh dấu hoàn thành trực tiếp bằng dấu tick xanh (`✓`).

### 4.3. Bộ 3 Tính năng Gemini AI Thông minh:
1. **Lập lịch tự động (Auto-Schedule)**:
   - Endpoint: `POST /api/ai/auto-schedule`.
   - Người dùng chọn ngày đích (`targetDate`), không gian làm việc (`workspaceId`), khung giờ nghỉ giải lao (`breakStartTime`, `breakEndTime`).
   - AI phân tích danh sách công việc chưa xong, độ ưu tiên, deadline, thời lượng ước tính và các khoảng trống (gaps) trong ngày.
   - Trả về bản xem trước (**Preview**). Người dùng bấm **Áp dụng** (`applyImmediately: true`) thì hệ thống mới lưu chính thức vào bảng `khoithoigian`.
2. **Phân rã công việc (Task Breakdown)**:
   - Endpoint: `POST /api/ai/tasks/{taskId}/breakdown` hoặc `POST /api/ai/breakdown`.
   - Tự động chia nhỏ một nhiệm vụ phức tạp thành danh sách các bước con cụ thể ([Subtask](file:///d:/DACN_QUANLYCONGVIECCANHAN/backend/src/main/java/com/taskmanager/entity/Subtask.java)) kèm thời lượng ước tính.
3. **Bóc tách câu lệnh tự nhiên (Natural Language Parsing)**:
   - Endpoint: `POST /api/ai/parse-task`.
   - Tiếp nhận câu nói/nhập liệu tự nhiên của người dùng (ví dụ: *"Mai 9h sáng nộp báo cáo đồ án khẩn cấp"*) để tự động bóc tách thành Tiêu đề, Hạn chót, Độ ưu tiên và Danh mục tương ứng.

### 4.4. Ma trận Eisenhower (Eisenhower Matrix):
- Phân chia thành 4 góc phần tư chuẩn:
  - **Q1 (DO_FIRST - Khẩn cấp & Quan trọng)**: Cần làm ngay.
  - **Q2 (SCHEDULE - Không khẩn cấp nhưng Quan trọng)**: Lên lịch thực hiện (trọng tâm năng suất).
  - **Q3 (DELEGATE - Khẩn cấp nhưng Không quan trọng)**: Ủy quyền / xử lý nhanh.
  - **Q4 (DONT_DO - Không khẩn cấp & Không quan trọng)**: Cắt giảm / loại bỏ.
- Hỗ trợ kéo thả trực tiếp giữa các góc phần tư để cập nhật nhanh trường `GocPhanTu` của Task.

### 4.5. Động cơ Quyết định Thông báo Nền (Notification Decision Engine):
- Class: [NotificationDecisionService.java](file:///d:/DACN_QUANLYCONGVIECCANHAN/backend/src/main/java/com/taskmanager/service/NotificationDecisionService.java).
- Chạy định kỳ nền bằng `@Scheduled(fixedDelay = 180000)` (mỗi 3 phút một lần).
- Tự động đánh giá CSDL theo các quy tắc nghiệp vụ:
  - Cảnh báo công việc sắp đến hạn trong vòng 24h và khẩn cấp trong vòng 1h.
  - Cảnh báo công việc đã quá hạn chót.
  - Lời nhắc lịch trình đầu ngày hoặc nhắc nhở tập trung.
  - Tích hợp cơ chế khử trùng lặp vĩnh viễn (de-duplication) theo `taskId` và khung 24h để không spam người dùng khi họ đã đọc hoặc đã xóa thông báo.

### 4.6. Thống kê Năng suất & Chuỗi Thói quen (Productivity & Habits):
- Bảng `nhatkythoiquen`: Ghi nhận check-in hàng ngày, tính toán streak ngày liên tiếp.
- Bảng `nhatkynangsuat`: Tổng hợp số việc hoàn thành, số phút tập trung, tỷ lệ đúng hạn theo ngày.
- Giao diện [ProductivityDashboard.tsx](file:///d:/DACN_QUANLYCONGVIECCANHAN/frontend/src/components/ProductivityDashboard.tsx) và [StatsBar.tsx](file:///d:/DACN_QUANLYCONGVIECCANHAN/frontend/src/components/StatsBar.tsx) hiển thị trực quan các biểu đồ năng suất, tỷ lệ hoàn thành và streak thói quen.

### 4.7. Hình nền Cá nhân hóa & Video nền (Personalized Background):
- Hỗ trợ người dùng tải lên ảnh hoặc video ngắn (`.mp4`, `.webm`) để làm hình nền làm việc cá nhân hóa thông qua [MediaController.java](file:///d:/DACN_QUANLYCONGVIECCANHAN/backend/src/main/java/com/taskmanager/controller/MediaController.java).
- Giới hạn dung lượng tối đa 50MB, kiểm tra mã MIME an toàn qua `UploadedMediaValidator.java`.
- Cung cấp thanh trượt tùy chỉnh độ phủ tối (`overlayDarkness` từ 0% đến 100%) và độ mờ (`backgroundBlur`) để đảm bảo văn bản luôn dễ đọc.

---

## 5. BẢNG TRA CỨU ENDPOINTS API ĐẦY ĐỦ

| Nhóm API | Method & Path | Mô tả nghiệp vụ |
| :--- | :--- | :--- |
| **Auth** | `POST /api/auth/login` | Đăng nhập nhận JWT Bearer token |
| | `POST /api/auth/register` | Đăng ký tài khoản mới (mật khẩu 8-50 ký tự) |
| | `POST /api/auth/verify-password` | Xác thực mật khẩu tài khoản / mật khẩu riêng tư |
| | `POST /api/auth/change-password` | Đổi mật khẩu đăng nhập |
| | `GET /api/auth/me` | Lấy thông tin tài khoản hiện tại |
| **Tasks** | `GET /api/tasks` | Lấy danh sách task (lọc theo workspace, status, priority...) |
| | `POST /api/tasks` | Tạo task mới |
| | `GET /api/tasks/{id}` | Lấy chi tiết task |
| | `PUT /api/tasks/{id}` | Cập nhật task |
| | `DELETE /api/tasks/{id}` | Xóa task |
| **Subtasks** | `GET /api/subtasks/task/{taskId}` | Lấy danh sách subtask của task |
| | `POST /api/subtasks` | Tạo subtask mới |
| | `PUT /api/subtasks/{id}` | Cập nhật trạng thái hoàn thành / tên subtask |
| | `DELETE /api/subtasks/{id}` | Xóa subtask |
| **Schedule** | `GET /api/timeblocks` | Lấy các khối thời gian theo khoảng ngày (cho lịch 24h) |
| | `POST /api/timeblocks` | Tạo khối thời gian mới |
| | `PUT /api/timeblocks/{id}` | Cập nhật giờ/ngày/kéo thả khối thời gian |
| | `DELETE /api/timeblocks/{id}` | Xóa khối thời gian |
| **AI** | `POST /api/ai/auto-schedule` | Tự động phân bổ lịch trình thông minh bằng Gemini AI |
| | `POST /api/ai/parse-task` | Bóc tách ngôn ngữ tự nhiên thành thuộc tính công việc |
| | `POST /api/ai/tasks/{taskId}/breakdown` | Phân rã task có sẵn thành các bước con |
| | `POST /api/ai/breakdown` | Phân rã công việc trực tiếp từ prompt |
| **Workspaces**| `GET /api/workspaces` | Lấy danh sách không gian làm việc của user |
| | `POST /api/workspaces` | Tạo không gian làm việc mới |
| | `PUT /api/workspaces/{id}` | Cập nhật tên/icon workspace |
| | `DELETE /api/workspaces/{id}` | Xóa workspace |
| **Categories**| `GET /api/categories` | Lấy danh sách danh mục (mã màu, icon) |
| | `POST /api/categories` | Tạo danh mục mới |
| | `PUT /api/categories/{id}` | Cập nhật danh mục |
| | `DELETE /api/categories/{id}` | Xóa danh mục |
| **Habits** | `GET /api/habit-logs` | Lấy lịch sử thói quen và streak |
| | `POST /api/habit-logs/check-in` | Check-in hoàn thành thói quen hôm nay |
| **Productivity**| `GET /api/productivity/daily-report`| Báo cáo chi tiết năng suất theo ngày |
| | `GET /api/productivity/stats` | Thống kê tỷ lệ hoàn thành tuần/tháng |
| **Notifications**| `GET /api/notifications` | Lấy danh sách thông báo của người dùng |
| | `PUT /api/notifications/{id}/read` | Đánh dấu thông báo đã đọc |
| | `DELETE /api/notifications/{id}` | Xóa thông báo |
| **Media** | `POST /api/media/upload` | Upload ảnh đại diện, ảnh/video nền (tối đa 50MB) |
| | `GET /api/uploads/{filename}` | Truy cập file media tĩnh (public) |
| **Settings** | `GET /api/user-preferences` | Lấy cài đặt giao diện, giờ làm việc, theme, hình nền |
| | `PUT /api/user-preferences` | Lưu cài đặt giao diện và hình nền |

---

## 6. QUY TẮC PHÁT TRIỂN & PHÒNG NGỪA LỖI HỒI QUY (DEV RULES)

1. **Trước khi sửa code**:
   - Luôn đọc kỹ cấu trúc của component và logic xung quanh để không làm mất các tính năng phụ trợ (như nút khóa/mở khóa, menu ba chấm, tooltip, âm thanh tick xanh, quick duplicate).
   - Kiểm tra xem file có liên quan đến các cấu hình bảo mật hay database hay không.
2. **Sau khi sửa code Backend**:
   - Bắt buộc chạy `mvn test-compile` hoặc `mvn test` tại thư mục `backend/` để đảm bảo code biên dịch sạch và vượt qua toàn bộ 19+ unit tests.
3. **Sau khi sửa code Frontend**:
   - Bắt buộc kiểm tra kiểu dữ liệu TypeScript (`npx tsc --noEmit` hoặc build check) để không có lỗi type.
4. **Quản lý dung lượng & Cache**:
   - Thư mục `C:\Users\Administrator\.gemini\antigravity-ide\browser_recordings` là cache video tạm thời của kiểm thử tự động, có thể xóa sạch định kỳ nếu ổ cứng bị đầy.
   - Tuyệt đối không xóa hay làm hỏng file `AGENTS.md` này.
5. **Cấm tuyệt đối**:
   - Không tự ý sửa cấu trúc bảng trong CSDL nếu chưa đọc kỹ [CSDL_CHI_TIET_TRUONG_DU_LIEU.md](file:///d:/DACN_QUANLYCONGVIECCANHAN/CSDL_CHI_TIET_TRUONG_DU_LIEU.md).
   - Không bỏ fallback mặc định sau dấu `:` trong `application.properties`.
   - Không dùng `X-User-Email` hay các header mock để bypass authentication.
