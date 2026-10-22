-- ==============================================================================
-- DATABASE INITIALIZATION SCRIPT FOR POSTGRESQL 17
-- Dự án: Hệ thống Quản lý và Tự động Lập lịch Công việc Cá nhân (SmartTask AI)
-- Database: quanlycongviec_db
-- Quy chuẩn đặt tên:
--   - Tên bảng: Chữ thường (lowercase), không dùng dấu gạch dưới _
--   - Tên thuộc tính: Viết hoa các âm (PascalCase), không dùng dấu gạch dưới _
-- ==============================================================================

-- Drop tables if exists (theo thứ tự khóa ngoại)
DROP TABLE IF EXISTS thongbao CASCADE;
DROP TABLE IF EXISTS nhatkynangsuat CASCADE;
DROP TABLE IF EXISTS nhatkyai CASCADE;
DROP TABLE IF EXISTS nhacnho CASCADE;
DROP TABLE IF EXISTS phientaptrung CASCADE;
DROP TABLE IF EXISTS khoithoigian CASCADE;
DROP TABLE IF EXISTS congvieccon CASCADE;
DROP TABLE IF EXISTS congviecthe CASCADE;
DROP TABLE IF EXISTS congviec CASCADE;
DROP TABLE IF EXISTS the CASCADE;
DROP TABLE IF EXISTS danhmuc CASCADE;
DROP TABLE IF EXISTS khonggianlamviec CASCADE;
DROP TABLE IF EXISTS caidatnguoidung CASCADE;
DROP TABLE IF EXISTS vaitronguoidung CASCADE;
DROP TABLE IF EXISTS vaitro CASCADE;
DROP TABLE IF EXISTS nguoidung CASCADE;

-- 1. Bảng NGUOIDUNG (Người dùng & Tài khoản)
CREATE TABLE nguoidung (
    "id" BIGSERIAL PRIMARY KEY,
    "Email" VARCHAR(120) NOT NULL UNIQUE,
    "MatKhau" VARCHAR(255) NOT NULL,
    "HoVaTen" VARCHAR(100) NOT NULL,
    "SoDienThoai" VARCHAR(20),
    "AnhDaiDien" VARCHAR(500),
    "NguonXacThuc" VARCHAR(20) DEFAULT 'LOCAL', -- LOCAL, GOOGLE
    "KichHoat" BOOLEAN DEFAULT TRUE,
    "XacThucEmail" BOOLEAN DEFAULT FALSE,
    "NgayTao" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    "NgayCapNhat" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng VAITRO & VAITRONGUOIDUNG (Phân quyền bảo mật RBAC)
CREATE TABLE vaitro (
    "id" SERIAL PRIMARY KEY,
    "TenVaiTro" VARCHAR(50) NOT NULL UNIQUE, -- ROLE_USER, ROLE_ADMIN
    "MoTa" VARCHAR(255)
);

CREATE TABLE vaitronguoidung (
    "NguoiDungId" BIGINT NOT NULL REFERENCES nguoidung("id") ON DELETE CASCADE,
    "VaiTroId" INT NOT NULL REFERENCES vaitro("id") ON DELETE CASCADE,
    PRIMARY KEY ("NguoiDungId", "VaiTroId")
);

-- 3. Bảng CAIDATNGUOIDUNG (Cấu hình thói quen làm việc & AI)
CREATE TABLE caidatnguoidung (
    "id" BIGSERIAL PRIMARY KEY,
    "NguoiDungId" BIGINT NOT NULL UNIQUE REFERENCES nguoidung("id") ON DELETE CASCADE,
    "GioBatDauLamViec" TIME DEFAULT '08:00:00',
    "GioKetThucLamViec" TIME DEFAULT '18:00:00',
    "NgayLamViec" VARCHAR(50) DEFAULT 'MON,TUE,WED,THU,FRI',
    "ThoiGianDemPhut" INT DEFAULT 15,            -- Thời gian đệm giữa các việc
    "ThoiGianHieuSuatCao" VARCHAR(20) DEFAULT 'MORNING', -- MORNING, AFTERNOON, EVENING
    "ThoiGianPomodoroMacDinh" INT DEFAULT 25,
    "NghiNganMacDinh" INT DEFAULT 5,
    "NghiDaiMacDinh" INT DEFAULT 15,
    "TuDongDoiLich" BOOLEAN DEFAULT TRUE,        -- Tự động dời lịch khi trễ việc
    "MuiGio" VARCHAR(50) DEFAULT 'Asia/Ho_Chi_Minh',
    "GiaoDien" VARCHAR(20) DEFAULT 'LIGHT',       -- LIGHT, DARK, SYSTEM
    "NgayTao" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    "NgayCapNhat" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 4. Bảng KHONGGIANLAMVIEC (Không gian làm việc độc lập)
CREATE TABLE khonggianlamviec (
    "id" BIGSERIAL PRIMARY KEY,
    "NguoiDungId" BIGINT NOT NULL REFERENCES nguoidung("id") ON DELETE CASCADE,
    "TenKhongGian" VARCHAR(100) NOT NULL,
    "TenBieuTuong" VARCHAR(50) DEFAULT 'Briefcase',
    "MacDinh" BOOLEAN DEFAULT FALSE,
    "NgayTao" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    "NgayCapNhat" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 5. Bảng DANHMUC (Danh mục công việc)
CREATE TABLE danhmuc (
    "id" BIGSERIAL PRIMARY KEY,
    "NguoiDungId" BIGINT NOT NULL REFERENCES nguoidung("id") ON DELETE CASCADE,
    "KhongGianId" BIGINT REFERENCES khonggianlamviec("id") ON DELETE CASCADE,
    "TenDanhMuc" VARCHAR(100) NOT NULL,
    "MaMau" VARCHAR(20) DEFAULT '#4F46E5',
    "TenBieuTuong" VARCHAR(50) DEFAULT 'Folder',
    "MacDinh" BOOLEAN DEFAULT FALSE,
    "NgayTao" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    "NgayCapNhat" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 6. Bảng THE (Thẻ phân loại nhãn)
CREATE TABLE the (
    "id" BIGSERIAL PRIMARY KEY,
    "NguoiDungId" BIGINT NOT NULL REFERENCES nguoidung("id") ON DELETE CASCADE,
    "TenThe" VARCHAR(50) NOT NULL,
    "MaMau" VARCHAR(20) DEFAULT '#64748B',
    "NgayTao" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 7. Bảng CONGVIEC (Công việc / Nhiệm vụ)
CREATE TABLE congviec (
    "id" BIGSERIAL PRIMARY KEY,
    "NguoiDungId" BIGINT NOT NULL REFERENCES nguoidung("id") ON DELETE CASCADE,
    "KhongGianId" BIGINT NOT NULL REFERENCES khonggianlamviec("id") ON DELETE CASCADE,
    "DanhMucId" BIGINT REFERENCES danhmuc("id") ON DELETE RESTRICT,
    "TieuDe" VARCHAR(255) NOT NULL,
    "MoTa" TEXT,
    "MucDoUuTien" VARCHAR(20) DEFAULT 'MEDIUM',           -- LOW, MEDIUM, HIGH, URGENT
    "GocPhanTu" VARCHAR(20) DEFAULT 'SCHEDULE',          -- DO_FIRST, SCHEDULE, DELEGATE, DONT_DO
    "TrangThai" VARCHAR(20) DEFAULT 'TODO',               -- TODO, IN_PROGRESS, COMPLETED, CANCELLED, OVERDUE
    "HanChot" TIMESTAMPTZ,                                -- Deadline hoàn thành
    "ThoiLuongUocTinh" INT DEFAULT 30,                    -- Thời lượng dự kiến (phút)
    "ThoiLuongThucTe" INT DEFAULT 0,                      -- Thời gian thực tế đã làm
    "LichCoDinh" BOOLEAN DEFAULT FALSE,                   -- Lịch cứng không thể tự động dời
    "BatDauCoDinh" TIMESTAMPTZ,
    "KetThucCoDinh" TIMESTAMPTZ,
    "LapLai" BOOLEAN DEFAULT FALSE,                       -- Việc lặp lại
    "QuyTacLapLai" VARCHAR(100),                         -- VD: DAILY, WEEKLY_MON_WED_FRI, MONTHLY
    "CongViecChaId" BIGINT REFERENCES congviec("id") ON DELETE CASCADE, -- Quan hệ cha-con
    "HoanThanhLuc" TIMESTAMPTZ,
    "NgayTao" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    "NgayCapNhat" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 8. Bảng CONGVIECTHE (Quan hệ nhiều - nhiều giữa Công việc và Thẻ)
CREATE TABLE congviecthe (
    "CongViecId" BIGINT NOT NULL REFERENCES congviec("id") ON DELETE CASCADE,
    "TheId" BIGINT NOT NULL REFERENCES the("id") ON DELETE CASCADE,
    PRIMARY KEY ("CongViecId", "TheId")
);

-- 9. Bảng CONGVIECCON (Checklist / Công việc con)
CREATE TABLE congvieccon (
    "id" BIGSERIAL PRIMARY KEY,
    "CongViecId" BIGINT NOT NULL REFERENCES congviec("id") ON DELETE CASCADE,
    "TieuDe" VARCHAR(255) NOT NULL,
    "HoanThanh" BOOLEAN DEFAULT FALSE,
    "ThoiLuongPhut" INT DEFAULT 15,
    "ThuTu" INT DEFAULT 0,
    "NgayTao" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 10. Bảng KHOITHOIGIAN (Khối thời gian trên lịch trình cá nhân)
CREATE TABLE khoithoigian (
    "id" BIGSERIAL PRIMARY KEY,
    "NguoiDungId" BIGINT NOT NULL REFERENCES nguoidung("id") ON DELETE CASCADE,
    "CongViecId" BIGINT REFERENCES congviec("id") ON DELETE SET NULL,
    "TieuDe" VARCHAR(255) NOT NULL,
    "ThoiGianBatDau" TIMESTAMPTZ NOT NULL,
    "ThoiGianKetThuc" TIMESTAMPTZ NOT NULL,
    "TuDongXepLich" BOOLEAN DEFAULT FALSE,         -- Được AI xếp lịch tự động
    "TrangThai" VARCHAR(20) DEFAULT 'SCHEDULED',   -- SCHEDULED, IN_PROGRESS, COMPLETED, SKIPPED, CANCELLED
    "MaMau" VARCHAR(20),
    "GhiChu" TEXT,
    "NgayTao" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    "NgayCapNhat" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 11. Bảng PHIENTAPTRUNG (Phiên tập trung Pomodoro)
CREATE TABLE phientaptrung (
    "id" BIGSERIAL PRIMARY KEY,
    "NguoiDungId" BIGINT NOT NULL REFERENCES nguoidung("id") ON DELETE CASCADE,
    "CongViecId" BIGINT REFERENCES congviec("id") ON DELETE SET NULL,
    "ThoiGianBatDau" TIMESTAMPTZ NOT NULL,
    "ThoiGianKetThuc" TIMESTAMPTZ,
    "ThoiLuongPhut" INT NOT NULL DEFAULT 25,
    "LoaiPhien" VARCHAR(20) DEFAULT 'WORK',        -- WORK, SHORT_BREAK, LONG_BREAK
    "HoanThanh" BOOLEAN DEFAULT TRUE,
    "GhiChu" TEXT,
    "NgayTao" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 12. Bảng NHACNHO (Nhắc nhở)
CREATE TABLE nhacnho (
    "id" BIGSERIAL PRIMARY KEY,
    "NguoiDungId" BIGINT NOT NULL REFERENCES nguoidung("id") ON DELETE CASCADE,
    "CongViecId" BIGINT NOT NULL REFERENCES congviec("id") ON DELETE CASCADE,
    "ThoiGianNhac" TIMESTAMPTZ NOT NULL,
    "KenhNhac" VARCHAR(20) DEFAULT 'PUSH',         -- PUSH, EMAIL, IN_APP
    "DaGui" BOOLEAN DEFAULT FALSE,
    "GuiLuc" TIMESTAMPTZ,
    "NgayTao" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 13. Bảng THONGBAO (Thông báo hệ thống & Lưu trạng thái đã đọc)
CREATE TABLE thongbao (
    "id" BIGSERIAL PRIMARY KEY,
    "NguoiDungId" BIGINT NOT NULL REFERENCES nguoidung("id") ON DELETE CASCADE,
    "CongViecId" BIGINT REFERENCES congviec("id") ON DELETE SET NULL,
    "LoaiThongBao" VARCHAR(50) DEFAULT 'reminder', -- reminder, ai, alert, success
    "TieuDe" VARCHAR(255) NOT NULL,
    "NoiDung" TEXT NOT NULL,
    "DaDoc" BOOLEAN DEFAULT FALSE,
    "NgayTao" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    "NgayCapNhat" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 14. Bảng NHATKYAI (Nhật ký xử lý tự động của Gemini API)
CREATE TABLE nhatkyai (
    "id" BIGSERIAL PRIMARY KEY,
    "NguoiDungId" BIGINT NOT NULL REFERENCES nguoidung("id") ON DELETE CASCADE,
    "LoaiHanhDong" VARCHAR(50) NOT NULL,           -- AUTO_SCHEDULE, TASK_BREAKDOWN, NATURAL_PARSING, RESCHEDULE_OVERDUE
    "CauLenhDauVao" TEXT NOT NULL,
    "PhanHoiAI" JSONB,
    "TrangThai" VARCHAR(20) DEFAULT 'SUCCESS',     -- SUCCESS, FAILED, PARTIALLY_APPLIED
    "SoToken" INT DEFAULT 0,
    "ThoiGianXuLyMs" BIGINT DEFAULT 0,
    "NgayTao" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 15. Bảng NHATKYNANGSUAT (Báo cáo & Thống kê năng suất hằng ngày)
CREATE TABLE nhatkynangsuat (
    "id" BIGSERIAL PRIMARY KEY,
    "NguoiDungId" BIGINT NOT NULL REFERENCES nguoidung("id") ON DELETE CASCADE,
    "NgayBaoCao" DATE NOT NULL,
    "TongCongViecHoanThanh" INT DEFAULT 0,
    "TongPhutTapTrung" INT DEFAULT 0,
    "TyLeDungHan" NUMERIC(5,2) DEFAULT 0.00,
    "SoCongViecQuaHan" INT DEFAULT 0,
    "NhanXetAI" TEXT,                              -- Đánh giá nhận xét tự động từ Gemini
    "NgayTao" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_nhatkynangsuat_ngaybaocao UNIQUE ("NguoiDungId", "NgayBaoCao")
);

-- ==============================================================================
-- INDEXES TỐI ƯU HÓA HIỆU NĂNG TRUY VẤN
-- ==============================================================================
CREATE INDEX idx_congviec_nguoidung_trangthai ON congviec("NguoiDungId", "TrangThai");
CREATE INDEX idx_congviec_nguoidung_hanchot ON congviec("NguoiDungId", "HanChot");
CREATE INDEX idx_congviec_danhmuc ON congviec("DanhMucId");
CREATE INDEX idx_khoithoigian_nguoidung_thoigian ON khoithoigian("NguoiDungId", "ThoiGianBatDau", "ThoiGianKetThuc");
CREATE INDEX idx_phientaptrung_nguoidung_batdau ON phientaptrung("NguoiDungId", "ThoiGianBatDau");
CREATE INDEX idx_nhacnho_dagui ON nhacnho("ThoiGianNhac") WHERE "DaGui" = FALSE;
CREATE INDEX idx_thongbao_nguoidung_dadoc ON thongbao("NguoiDungId", "DaDoc");

-- ==============================================================================
-- DỮ LIỆU MẪU BAN ĐẦU (SEED DATA)
-- ==============================================================================
INSERT INTO vaitro ("id", "TenVaiTro", "MoTa") VALUES 
(1, 'ROLE_USER', 'Người dùng cá nhân tiêu chuẩn'),
(2, 'ROLE_ADMIN', 'Quản trị viên hệ thống')
ON CONFLICT ("id") DO NOTHING;

-- Mật khẩu mặc định: '1' cho admin@gmail.com và 'password123' cho demo
INSERT INTO nguoidung ("id", "Email", "MatKhau", "HoVaTen", "NguonXacThuc", "KichHoat", "XacThucEmail") VALUES 
(1, 'admin@gmail.com', '$2a$10$WzW1762U0K/uV5FmIomQge4qU27eP6X00D7qB9kC7q0cK8p9z0S2e', 'Minh Anh', 'LOCAL', TRUE, TRUE),
(2, 'demo@smarttask.ai', '$2a$10$7R3vP1X75lKqM8fF85F80eYlS9sXoUjH7tCq6rV4v9Q8k2p8z0S2e', 'Người Dùng Demo', 'LOCAL', TRUE, TRUE)
ON CONFLICT ("id") DO NOTHING;

INSERT INTO vaitronguoidung ("NguoiDungId", "VaiTroId") VALUES 
(1, 2),
(2, 1)
ON CONFLICT DO NOTHING;

INSERT INTO caidatnguoidung ("id", "NguoiDungId", "GioBatDauLamViec", "GioKetThucLamViec", "NgayLamViec", "ThoiGianDemPhut", "ThoiGianHieuSuatCao") VALUES 
(1, 1, '08:00:00', '18:00:00', 'MON,TUE,WED,THU,FRI', 15, 'MORNING')
ON CONFLICT ("id") DO NOTHING;

-- Khởi tạo không gian làm việc
INSERT INTO khonggianlamviec ("id", "NguoiDungId", "TenKhongGian", "TenBieuTuong", "MacDinh") VALUES
(1, 1, 'Không gian cá nhân', 'User', TRUE),
(2, 1, 'Đồ án Tốt nghiệp SmartTask', 'Briefcase', FALSE),
(3, 1, 'Học tập & Nghiên cứu', 'GraduationCap', FALSE)
ON CONFLICT ("id") DO NOTHING;

-- Khởi tạo danh mục thuộc workspace 1 (Không gian cá nhân)
INSERT INTO danhmuc ("id", "NguoiDungId", "KhongGianId", "TenDanhMuc", "MaMau", "TenBieuTuong", "MacDinh") VALUES 
(1, 1, 1, 'Học tập', '#3B82F6', 'GraduationCap', TRUE),
(2, 1, 1, 'Công việc', '#8B5CF6', 'Briefcase', FALSE),
(3, 1, 1, 'Sức khỏe', '#10B981', 'Heart', FALSE),
(4, 1, 1, 'Giải trí', '#F59E0B', 'Smile', FALSE)
ON CONFLICT ("id") DO NOTHING;

-- Thông báo được tự động tạo và lưu vào bảng thongbao bởi Thuật toán Quyết định Thông minh (NotificationDecisionService)
