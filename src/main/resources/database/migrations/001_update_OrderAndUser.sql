use LUMINOUS_DB;
ALTER TABLE Orders DROP CONSTRAINT chk_orders_status;
ALTER TABLE Orders ADD CONSTRAINT chk_orders_status
    CHECK (status IN ('PENDING','CONFIRMED','SHIPPING','DELIVERED','COMPLETED','CANCELLED','RETURNED'));

ALTER TABLE `User`
    ADD COLUMN IF NOT EXISTS provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL' AFTER gender,
    ADD COLUMN IF NOT EXISTS providerId VARCHAR(255) NULL AFTER provider;
ALTER TABLE Orders
    ADD COLUMN IF NOT EXISTS returnedAt DATETIME NULL AFTER completedAt,
    ADD COLUMN IF NOT EXISTS returnReason VARCHAR(500) NULL AFTER cancelReason;
ALTER TABLE Orders ADD COLUMN IF NOT EXISTS adminHidden BOOLEAN NOT NULL DEFAULT FALSE;

USE LUMINOUS_DB;

ALTER TABLE Product
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE ProductVariant
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE Orders
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 2. Bang khoa chong trung don hang (IdempotencyKey).
-- Cot khoa dat ten idempotencyKey vi "key" la tu khoa du tru cua MariaDB.
CREATE TABLE IF NOT EXISTS IdempotencyKey (
                                              idempotencyKey VARCHAR(64) NOT NULL PRIMARY KEY,
                                              userID INT NOT NULL,
                                              orderCode VARCHAR(50) NOT NULL UNIQUE,
                                              createdAt DATETIME NOT NULL,
                                              INDEX idx_idem_user (userID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE ReviewMedia
    ADD COLUMN IF NOT EXISTS createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE Voucher ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE Category ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE `User` ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;