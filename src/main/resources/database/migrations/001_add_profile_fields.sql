-- ============================================================
-- Migration 001 - chay SAU khi import luminous_db.sql
-- Ly do: form Dang ky theo Figma co Ngay sinh + Gioi tinh,
-- nhung file luminous_db.sql goc KHONG co 2 cot nay trong bang `User`.
-- 2 cot them vao dang NULLABLE nen khong anh huong du lieu mau da co.
-- Neu khong chay file nay, app se bao loi validate schema khi khoi dong
-- (do entity User map them 2 cot nay, xem model/User.java).
-- ============================================================
USE LUMINOUS_DB;

-- Chi chay 1 lan (chay lan 2 se bao cot da ton tai, khong sao).
ALTER TABLE `User`
    ADD COLUMN dateOfBirth DATE NULL AFTER phone,
    ADD COLUMN gender VARCHAR(10) NULL AFTER dateOfBirth;
