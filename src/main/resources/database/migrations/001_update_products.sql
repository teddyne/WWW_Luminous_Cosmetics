USE LUMINOUS_DB;

ALTER TABLE Category
    ADD COLUMN createdAt DATETIME NULL AFTER status;

ALTER TABLE Voucher
    ADD COLUMN scopeType VARCHAR(20) NOT NULL DEFAULT 'ALL' AFTER applicableScope,
    ADD CONSTRAINT chk_voucher_scope_type CHECK (scopeType IN ('ALL', 'CATEGORY', 'PRODUCT'));

CREATE TABLE IF NOT EXISTS VoucherCategory (
                                               voucherCategoryID INT AUTO_INCREMENT PRIMARY KEY,
                                               voucherID INT NOT NULL,
                                               categoryID INT NOT NULL,
                                               CONSTRAINT uq_voucher_category UNIQUE (voucherID, categoryID),
                                               CONSTRAINT fk_vc_voucher FOREIGN KEY (voucherID) REFERENCES Voucher(voucherID) ON DELETE CASCADE ON UPDATE CASCADE,
                                               CONSTRAINT fk_vc_category FOREIGN KEY (categoryID) REFERENCES Category(categoryID) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS VoucherProduct (
                                              voucherProductID INT AUTO_INCREMENT PRIMARY KEY,
                                              voucherID INT NOT NULL,
                                              productID INT NOT NULL,
                                              CONSTRAINT uq_voucher_product UNIQUE (voucherID, productID),
                                              CONSTRAINT fk_vp_voucher FOREIGN KEY (voucherID) REFERENCES Voucher(voucherID) ON DELETE CASCADE ON UPDATE CASCADE,
                                              CONSTRAINT fk_vp_product FOREIGN KEY (productID) REFERENCES Product(productID) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;


ALTER TABLE Orders
    ADD COLUMN IF NOT EXISTS cancelReason VARCHAR(500) NULL AFTER note;

ALTER TABLE Orders ADD COLUMN IF NOT EXISTS confirmedAt DATETIME NULL;
ALTER TABLE Orders ADD COLUMN IF NOT EXISTS shippedAt DATETIME NULL;
ALTER TABLE Orders ADD COLUMN IF NOT EXISTS deliveredAt DATETIME NULL;
ALTER TABLE Orders ADD COLUMN IF NOT EXISTS completedAt DATETIME NULL;
ALTER TABLE Orders ADD COLUMN IF NOT EXISTS cancelledAt DATETIME NULL;
ALTER TABLE Orders ADD COLUMN IF NOT EXISTS salesChannel VARCHAR(20) NOT NULL DEFAULT 'ONLINE';

ALTER TABLE Orders ADD COLUMN IF NOT EXISTS createdBy INT NULL;
ALTER TABLE Payment ADD COLUMN IF NOT EXISTS cashGiven DECIMAL(12,2) NULL;
ALTER TABLE Payment ADD COLUMN IF NOT EXISTS changeAmount DECIMAL(12,2) NULL;

ALTER TABLE Orders ADD COLUMN IF NOT EXISTS createdBy INT NULL;
ALTER TABLE Payment ADD COLUMN IF NOT EXISTS cashGiven DECIMAL(12,2) NULL;
ALTER TABLE Payment ADD COLUMN IF NOT EXISTS changeAmount DECIMAL(12,2) NULL;