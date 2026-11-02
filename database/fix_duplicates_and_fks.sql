-- ==============================================================================
-- FIX DATABASE SCHEMA: DROP DUPLICATE SNAKE_CASE COLUMNS & ADD MISSING FOREIGN KEYS
-- Database: quanlycongviec_db
-- ==============================================================================

-- 1. Bảng NGUOIDUNG: Xóa các cột trùng lặp dạng snake_case
ALTER TABLE nguoidung
  DROP COLUMN IF EXISTS anh_dai_dien,
  DROP COLUMN IF EXISTS kich_hoat,
  DROP COLUMN IF EXISTS ngay_cap_nhat,
  DROP COLUMN IF EXISTS ngay_tao,
  DROP COLUMN IF EXISTS nguon_xac_thuc,
  DROP COLUMN IF EXISTS so_dien_thoai,
  DROP COLUMN IF EXISTS xac_thuc_email;

-- 2. Bảng CONGVIEC: Xóa các cột trùng lặp dạng snake_case
ALTER TABLE congviec
  DROP COLUMN IF EXISTS bat_dau_co_dinh,
  DROP COLUMN IF EXISTS cong_viec_cha_id,
  DROP COLUMN IF EXISTS danh_muc_id,
  DROP COLUMN IF EXISTS goc_phan_tu,
  DROP COLUMN IF EXISTS han_chot,
  DROP COLUMN IF EXISTS hoan_thanh_luc,
  DROP COLUMN IF EXISTS ket_thuc_co_dinh,
  DROP COLUMN IF EXISTS khong_gian_id,
  DROP COLUMN IF EXISTS lap_lai,
  DROP COLUMN IF EXISTS lich_co_dinh,
  DROP COLUMN IF EXISTS mo_ta,
  DROP COLUMN IF EXISTS muc_do_uu_tien,
  DROP COLUMN IF EXISTS ngay_cap_nhat,
  DROP COLUMN IF EXISTS ngay_tao,
  DROP COLUMN IF EXISTS quy_tac_lap_lai,
  DROP COLUMN IF EXISTS thoi_luong_thuc_te,
  DROP COLUMN IF EXISTS thoi_luong_uoc_tinh,
  DROP COLUMN IF EXISTS trang_thai;

-- 3. Bảng KHOITHOIGIAN: Xóa các cột trùng lặp dạng snake_case
ALTER TABLE khoithoigian
  DROP COLUMN IF EXISTS cong_viec_id,
  DROP COLUMN IF EXISTS ghi_chu,
  DROP COLUMN IF EXISTS ma_mau,
  DROP COLUMN IF EXISTS ngay_cap_nhat,
  DROP COLUMN IF EXISTS ngay_tao,
  DROP COLUMN IF EXISTS trang_thai,
  DROP COLUMN IF EXISTS tu_dong_xep_lich;

-- 4. Bảng DANHMUC: Xóa các cột trùng lặp dạng snake_case
ALTER TABLE danhmuc
  DROP COLUMN IF EXISTS khong_gian_id,
  DROP COLUMN IF EXISTS ma_mau,
  DROP COLUMN IF EXISTS mac_dinh,
  DROP COLUMN IF EXISTS ngay_cap_nhat,
  DROP COLUMN IF EXISTS ngay_tao,
  DROP COLUMN IF EXISTS ten_bieu_tuong;

-- 5. Bảng KHONGGIANLAMVIEC: Xóa các cột trùng lặp dạng snake_case
ALTER TABLE khonggianlamviec
  DROP COLUMN IF EXISTS mac_dinh,
  DROP COLUMN IF EXISTS ngay_cap_nhat,
  DROP COLUMN IF EXISTS ngay_tao,
  DROP COLUMN IF EXISTS ten_bieu_tuong;

-- 6. Bảng THONGBAO: Xóa các cột trùng lặp dạng snake_case
ALTER TABLE thongbao
  DROP COLUMN IF EXISTS cong_viec_id,
  DROP COLUMN IF EXISTS da_doc,
  DROP COLUMN IF EXISTS ngay_cap_nhat,
  DROP COLUMN IF EXISTS ngay_tao;

-- 7. Bảng VAITRO: Xóa cột trùng lặp dạng snake_case
ALTER TABLE vaitro
  DROP COLUMN IF EXISTS mo_ta;

-- 8. Bảng NHATKYTHOIQUEN: Bổ sung liên kết khóa ngoại (Foreign Keys) đến NGUOIDUNG và CONGVIEC
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_nhatkythoiquen_nguoidung' 
          AND table_name = 'nhatkythoiquen'
    ) THEN
        ALTER TABLE nhatkythoiquen
          ADD CONSTRAINT fk_nhatkythoiquen_nguoidung
          FOREIGN KEY ("NguoiDungId") REFERENCES nguoidung("id") ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_nhatkythoiquen_congviec' 
          AND table_name = 'nhatkythoiquen'
    ) THEN
        ALTER TABLE nhatkythoiquen
          ADD CONSTRAINT fk_nhatkythoiquen_congviec
          FOREIGN KEY ("CongViecId") REFERENCES congviec("id") ON DELETE CASCADE;
    END IF;
END $$;

-- 9. Tạo index tối ưu hiệu năng cho bảng NHATKYTHOIQUEN
CREATE INDEX IF NOT EXISTS idx_nhatkythoiquen_nguoidung ON nhatkythoiquen("NguoiDungId");
CREATE INDEX IF NOT EXISTS idx_nhatkythoiquen_congviec ON nhatkythoiquen("CongViecId");
