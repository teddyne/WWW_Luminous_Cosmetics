
CREATE TABLE IF NOT EXISTS FlashSale (
                                         flashSaleID INT AUTO_INCREMENT PRIMARY KEY,
                                         name VARCHAR(200) NOT NULL,
                                         bannerUrl VARCHAR(500) NULL,
                                         description TEXT NULL,
                                         startAt DATETIME NOT NULL,
                                         endAt DATETIME NOT NULL,
                                         status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                                         createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         updatedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                         version BIGINT NOT NULL DEFAULT 0,
                                         CONSTRAINT chk_flash_sale_time CHECK (endAt > startAt)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS FlashSaleItem (
                                             flashSaleItemID INT AUTO_INCREMENT PRIMARY KEY,
                                             flashSaleID INT NOT NULL,
                                             variantID INT NOT NULL,
                                             discountPercent INT NOT NULL,
                                             promoQuantity INT NOT NULL,
                                             maxPerCustomer INT NULL,
                                             CONSTRAINT fk_flash_item_sale FOREIGN KEY (flashSaleID) REFERENCES FlashSale (flashSaleID),
                                             CONSTRAINT fk_flash_item_variant FOREIGN KEY (variantID) REFERENCES ProductVariant (variantID),
                                             CONSTRAINT uq_flash_item UNIQUE (flashSaleID, variantID),
                                             CONSTRAINT chk_flash_item_percent CHECK (discountPercent BETWEEN 1 AND 99),
                                             CONSTRAINT chk_flash_item_qty CHECK (promoQuantity >= 1),
                                             CONSTRAINT chk_flash_item_max CHECK (maxPerCustomer IS NULL OR maxPerCustomer >= 1)
) ENGINE=InnoDB;
