-- Migration: Thêm GoogleId vào nguoidung và tạo bảng xacthucotp
ALTER TABLE nguoidung ADD COLUMN IF NOT EXISTS "GoogleId" VARCHAR(100) UNIQUE;
ALTER TABLE nguoidung ALTER COLUMN "MatKhau" DROP NOT NULL;

CREATE TABLE IF NOT EXISTS xacthucotp (
    "id" BIGSERIAL PRIMARY KEY,
    "Email" VARCHAR(120) NOT NULL,
    "MaOtp" VARCHAR(10) NOT NULL,
    "LoaiOtp" VARCHAR(30) DEFAULT 'FORGOT_PASSWORD', -- FORGOT_PASSWORD, VERIFY_EMAIL
    "HetHanLuc" TIMESTAMPTZ NOT NULL,
    "DaSuDung" BOOLEAN DEFAULT FALSE,
    "SoLanThu" INT DEFAULT 0,
    "NgayTao" TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_xacthucotp_email_loai ON xacthucotp("Email", "LoaiOtp");
