-- ============================================================
-- LUMINOUS_DB - PHIEN BAN HOAN CHINH (MariaDB 10.4+ / Spring Data JPA)
-- Mat khau tai khoan mau (admin, khach hang): Luminous@123
-- ============================================================
DROP DATABASE IF EXISTS LUMINOUS_DB;
CREATE DATABASE LUMINOUS_DB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE LUMINOUS_DB;
SET NAMES utf8mb4;

-- ============================================================
-- 1. ROLE
-- ============================================================
CREATE TABLE Role (
                      roleID INT AUTO_INCREMENT PRIMARY KEY,
                      roleName VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- ============================================================
-- 2. USER (Da tich hop san cac cot bo sung)
-- ============================================================
CREATE TABLE `User` (
                        userID INT AUTO_INCREMENT PRIMARY KEY,
                        roleID INT NOT NULL,
                        fullName VARCHAR(100) NOT NULL,
                        email VARCHAR(100) NOT NULL UNIQUE,
                        password VARCHAR(255) NOT NULL,
                        phone VARCHAR(15) UNIQUE,
                        dateOfBirth DATE NULL,
                        gender VARCHAR(10) NULL,
                        skinQuizAsked BOOLEAN NOT NULL DEFAULT FALSE,
                        avtUrl VARCHAR(500),
                        pointBalance INT NOT NULL DEFAULT 0,
                        isActive BOOLEAN NOT NULL DEFAULT TRUE,
                        createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updatedAt DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT chk_user_point CHECK (pointBalance >= 0),
                        CONSTRAINT fk_user_role FOREIGN KEY (roleID) REFERENCES Role(roleID) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 3. ADDRESS
-- ============================================================
CREATE TABLE Address (
                         addressID INT AUTO_INCREMENT PRIMARY KEY,
                         userID INT NOT NULL,
                         recipientName VARCHAR(100) NOT NULL,
                         phone VARCHAR(15) NOT NULL,
                         province VARCHAR(100) NOT NULL,
                         district VARCHAR(100) NOT NULL,
                         ward VARCHAR(100) NOT NULL,
                         detailAddress VARCHAR(255) NOT NULL,
                         isDefault BOOLEAN NOT NULL DEFAULT FALSE,
                         CONSTRAINT fk_address_user FOREIGN KEY (userID) REFERENCES `User`(userID) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 4. CATEGORY (7 Danh muc: 5 cu + Toner + Son moi)
-- ============================================================
CREATE TABLE Category (
                          categoryID INT AUTO_INCREMENT PRIMARY KEY,
                          categoryName VARCHAR(100) NOT NULL UNIQUE,
                          description TEXT,
                          status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                          CONSTRAINT chk_category_status CHECK (status IN ('ACTIVE','INACTIVE'))
) ENGINE=InnoDB;

-- ============================================================
-- 5. PRODUCT
-- ============================================================
CREATE TABLE Product (
                         productID INT AUTO_INCREMENT PRIMARY KEY,
                         categoryID INT NOT NULL,
                         name VARCHAR(200) NOT NULL,
                         brand VARCHAR(100),
                         description TEXT,
                         isActive BOOLEAN NOT NULL DEFAULT TRUE,
                         createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         INDEX idx_product_name (name),
                         INDEX idx_product_brand (brand),
                         CONSTRAINT fk_product_category FOREIGN KEY (categoryID) REFERENCES Category(categoryID) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 6. PRODUCTMEDIA
-- ============================================================
CREATE TABLE ProductMedia (
                              mediaID INT AUTO_INCREMENT PRIMARY KEY,
                              productID INT NOT NULL,
                              mediaURL VARCHAR(500) NOT NULL,
                              mediaType VARCHAR(20) NOT NULL DEFAULT 'IMAGE',
                              thumbnailUrl VARCHAR(500),
                              displayOrder INT NOT NULL DEFAULT 0,
                              altText VARCHAR(255),
                              CONSTRAINT chk_pmedia_type CHECK (mediaType IN ('IMAGE','VIDEO')),
                              CONSTRAINT fk_productmedia_product FOREIGN KEY (productID) REFERENCES Product(productID) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 7. PRODUCTVARIANT
-- ============================================================
CREATE TABLE ProductVariant (
                                variantID INT AUTO_INCREMENT PRIMARY KEY,
                                productID INT NOT NULL,
                                sku VARCHAR(50) NOT NULL UNIQUE,
                                barcode VARCHAR(50) UNIQUE,
                                size VARCHAR(50),
                                shadeName VARCHAR(100),
                                hexCode VARCHAR(10),
                                price DECIMAL(12,2) NOT NULL,
                                salePrice DECIMAL(12,2),
                                stockQuantity INT NOT NULL DEFAULT 0,
                                ingredients TEXT,
                                suitableSkinType VARCHAR(100),
                                imgUrl VARCHAR(500),
                                isActive BOOLEAN NOT NULL DEFAULT TRUE,
                                CONSTRAINT chk_variant_price CHECK (price > 0),
                                CONSTRAINT chk_variant_sale CHECK (salePrice IS NULL OR (salePrice > 0 AND salePrice <= price)),
                                CONSTRAINT chk_variant_stock CHECK (stockQuantity >= 0),
                                CONSTRAINT fk_variant_product FOREIGN KEY (productID) REFERENCES Product(productID) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 8. CART
-- ============================================================
CREATE TABLE Cart (
                      cartID INT AUTO_INCREMENT PRIMARY KEY,
                      userID INT NOT NULL UNIQUE,
                      updatedAt DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                      CONSTRAINT fk_cart_user FOREIGN KEY (userID) REFERENCES `User`(userID) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 9. CARTITEM
-- ============================================================
CREATE TABLE CartItem (
                          cartItemID INT AUTO_INCREMENT PRIMARY KEY,
                          cartID INT NOT NULL,
                          variantID INT NOT NULL,
                          quantity INT NOT NULL DEFAULT 1,
                          CONSTRAINT uq_cartitem UNIQUE (cartID, variantID),
                          CONSTRAINT chk_cartitem_qty CHECK (quantity > 0),
                          CONSTRAINT fk_cartitem_cart FOREIGN KEY (cartID) REFERENCES Cart(cartID) ON DELETE CASCADE ON UPDATE CASCADE,
                          CONSTRAINT fk_cartitem_variant FOREIGN KEY (variantID) REFERENCES ProductVariant(variantID) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 10. ORDERS
-- ============================================================
CREATE TABLE Orders (
                        orderID INT AUTO_INCREMENT PRIMARY KEY,
                        userID INT NOT NULL,
                        orderCode VARCHAR(50) NOT NULL UNIQUE,
                        orderDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        recipientName VARCHAR(100) NOT NULL,
                        recipientPhone VARCHAR(15) NOT NULL,
                        shippingAddress TEXT NOT NULL,
                        subtotal DECIMAL(12,2) NOT NULL,
                        shippingFee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                        discountAmount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                        usedPoints INT NOT NULL DEFAULT 0,
                        totalAmount DECIMAL(12,2) NOT NULL,
                        status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                        note TEXT,
                        updatedAt DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                        INDEX idx_orders_user_date (userID, orderDate),
                        INDEX idx_orders_status (status),
                        CONSTRAINT chk_orders_money CHECK (subtotal >= 0 AND shippingFee >= 0 AND discountAmount >= 0 AND totalAmount >= 0),
                        CONSTRAINT chk_orders_points CHECK (usedPoints >= 0),
                        CONSTRAINT chk_orders_status CHECK (status IN ('PENDING','CONFIRMED','SHIPPING','DELIVERED','COMPLETED','CANCELLED')),
                        CONSTRAINT fk_orders_user FOREIGN KEY (userID) REFERENCES `User`(userID) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 11. PAYMENT
-- ============================================================
CREATE TABLE Payment (
                         paymentID INT AUTO_INCREMENT PRIMARY KEY,
                         orderID INT NOT NULL,
                         paymentMethod VARCHAR(30) NOT NULL,
                         amount DECIMAL(12,2) NOT NULL,
                         paymentStatus VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                         transactionCode VARCHAR(100),
                         paidAt DATETIME,
                         INDEX idx_payment_order (orderID),
                         CONSTRAINT chk_payment_amount CHECK (amount >= 0),
                         CONSTRAINT chk_payment_method CHECK (paymentMethod IN ('COD','MOMO','VNPAY','BANK_TRANSFER')),
                         CONSTRAINT chk_payment_status CHECK (paymentStatus IN ('PENDING','PAID','FAILED','REFUNDED')),
                         CONSTRAINT fk_payment_order FOREIGN KEY (orderID) REFERENCES Orders(orderID) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 12. ORDERDETAIL
-- ============================================================
CREATE TABLE OrderDetail (
                             orderDetailID INT AUTO_INCREMENT PRIMARY KEY,
                             orderID INT NOT NULL,
                             variantID INT NOT NULL,
                             quantity INT NOT NULL,
                             unitPrice DECIMAL(12,2) NOT NULL,
                             totalPrice DECIMAL(12,2) NOT NULL,
                             CONSTRAINT chk_od_qty CHECK (quantity > 0),
                             CONSTRAINT chk_od_total CHECK (totalPrice = quantity * unitPrice),
                             CONSTRAINT fk_orderdetail_order FOREIGN KEY (orderID) REFERENCES Orders(orderID) ON DELETE CASCADE ON UPDATE CASCADE,
                             CONSTRAINT fk_orderdetail_variant FOREIGN KEY (variantID) REFERENCES ProductVariant(variantID) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 13. VOUCHERTYPE
-- ============================================================
CREATE TABLE VoucherType (
                             voucherTypeID INT AUTO_INCREMENT PRIMARY KEY,
                             typeName VARCHAR(50) NOT NULL,
                             description TEXT
) ENGINE=InnoDB;

-- ============================================================
-- 14. VOUCHER
-- ============================================================
CREATE TABLE Voucher (
                         voucherID INT AUTO_INCREMENT PRIMARY KEY,
                         voucherTypeID INT NOT NULL,
                         voucherCode VARCHAR(50) NOT NULL UNIQUE,
                         voucherName VARCHAR(100) NOT NULL,
                         description TEXT,
                         discountType VARCHAR(20) NOT NULL,
                         discountValue DECIMAL(12,2) NOT NULL,
                         maxDiscountAmount DECIMAL(12,2),
                         minOrderValue DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                         usageLimit INT NOT NULL,
                         usedCount INT NOT NULL DEFAULT 0,
                         startDate DATETIME NOT NULL,
                         endDate DATETIME NOT NULL,
                         applicableScope VARCHAR(50) NOT NULL DEFAULT 'ALL',
                         imgUrl VARCHAR(500),
                         isActive BOOLEAN NOT NULL DEFAULT TRUE,
                         INDEX idx_voucher_dates (startDate, endDate),
                         CONSTRAINT chk_voucher_type CHECK (discountType IN ('FIXED_AMOUNT','PERCENTAGE')),
                         CONSTRAINT chk_voucher_value CHECK (discountValue > 0 AND (discountType <> 'PERCENTAGE' OR discountValue <= 100)),
                         CONSTRAINT chk_voucher_date CHECK (endDate > startDate),
                         CONSTRAINT chk_voucher_usage CHECK (usageLimit > 0 AND usedCount >= 0 AND usedCount <= usageLimit),
                         CONSTRAINT fk_voucher_type FOREIGN KEY (voucherTypeID) REFERENCES VoucherType(voucherTypeID) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 15. ORDERVOUCHER
-- ============================================================
CREATE TABLE OrderVoucher (
                              orderVoucherID INT AUTO_INCREMENT PRIMARY KEY,
                              voucherID INT NOT NULL,
                              userID INT NOT NULL,
                              orderID INT NOT NULL,
                              discountAmount DECIMAL(12,2) NOT NULL,
                              appliedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT uq_ordervoucher UNIQUE (voucherID, orderID),
                              CONSTRAINT chk_ov_amount CHECK (discountAmount >= 0),
                              CONSTRAINT fk_ordervoucher_voucher FOREIGN KEY (voucherID) REFERENCES Voucher(voucherID) ON DELETE RESTRICT ON UPDATE CASCADE,
                              CONSTRAINT fk_ordervoucher_user FOREIGN KEY (userID) REFERENCES `User`(userID) ON DELETE RESTRICT ON UPDATE CASCADE,
                              CONSTRAINT fk_ordervoucher_order FOREIGN KEY (orderID) REFERENCES Orders(orderID) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 16. POINTTRANSACTION
-- ============================================================
CREATE TABLE PointTransaction (
                                  historyID INT AUTO_INCREMENT PRIMARY KEY,
                                  userID INT NOT NULL,
                                  orderID INT,
                                  type VARCHAR(20) NOT NULL,
                                  points INT NOT NULL,
                                  balanceAfter INT NOT NULL,
                                  description VARCHAR(255),
                                  createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  INDEX idx_point_user_date (userID, createdAt),
                                  CONSTRAINT chk_point_type CHECK (type IN ('EARN','SPEND','REFUND','ADJUST')),
                                  CONSTRAINT chk_point_sign CHECK (points <> 0
                                      AND (type NOT IN ('EARN','REFUND') OR points > 0)
                                      AND (type <> 'SPEND' OR points < 0)),
                                  CONSTRAINT chk_point_balance CHECK (balanceAfter >= 0),
                                  CONSTRAINT fk_point_user FOREIGN KEY (userID) REFERENCES `User`(userID) ON DELETE RESTRICT ON UPDATE CASCADE,
                                  CONSTRAINT fk_point_order FOREIGN KEY (orderID) REFERENCES Orders(orderID) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 17. REVIEW
-- ============================================================
CREATE TABLE Review (
                        reviewID INT AUTO_INCREMENT PRIMARY KEY,
                        userID INT NOT NULL,
                        orderDetailID INT NOT NULL UNIQUE,
                        rating TINYINT NOT NULL,
                        comment TEXT,
                        createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        isVisible BOOLEAN NOT NULL DEFAULT TRUE,
                        isAnonymous BOOLEAN NOT NULL DEFAULT FALSE,
                        CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
                        CONSTRAINT fk_review_user FOREIGN KEY (userID) REFERENCES `User`(userID) ON DELETE CASCADE ON UPDATE CASCADE,
                        CONSTRAINT fk_review_detail FOREIGN KEY (orderDetailID) REFERENCES OrderDetail(orderDetailID) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 18. REVIEWMEDIA
-- ============================================================
CREATE TABLE ReviewMedia (
                             mediaID INT AUTO_INCREMENT PRIMARY KEY,
                             reviewID INT NOT NULL,
                             mediaUrl VARCHAR(500) NOT NULL,
                             mediaType VARCHAR(20) NOT NULL,
                             CONSTRAINT chk_rmedia_type CHECK (mediaType IN ('IMAGE','VIDEO')),
                             CONSTRAINT fk_reviewmedia_review FOREIGN KEY (reviewID) REFERENCES Review(reviewID) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 19. RETURNREQUEST
-- ============================================================
CREATE TABLE ReturnRequest (
                               requestID INT AUTO_INCREMENT PRIMARY KEY,
                               userID INT NOT NULL,
                               orderID INT NOT NULL,
                               orderDetailID INT NOT NULL,
                               reason VARCHAR(255) NOT NULL,
                               description TEXT,
                               adminNote TEXT,
                               mediaUrl VARCHAR(500),
                               typeMedia VARCHAR(20),
                               quantity INT NOT NULL DEFAULT 1,
                               refundAmount DECIMAL(12,2) NOT NULL,
                               refundMethod VARCHAR(50),
                               status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                               createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updatedAt DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
                               INDEX idx_return_status (status),
                               CONSTRAINT chk_return_qty CHECK (quantity > 0),
                               CONSTRAINT chk_return_refund CHECK (refundAmount >= 0),
                               CONSTRAINT chk_return_status CHECK (status IN ('PENDING','APPROVED','REJECTED','COMPLETED')),
                               CONSTRAINT fk_return_user FOREIGN KEY (userID) REFERENCES `User`(userID) ON DELETE RESTRICT ON UPDATE CASCADE,
                               CONSTRAINT fk_return_order FOREIGN KEY (orderID) REFERENCES Orders(orderID) ON DELETE RESTRICT ON UPDATE CASCADE,
                               CONSTRAINT fk_return_detail FOREIGN KEY (orderDetailID) REFERENCES OrderDetail(orderDetailID) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 20. NOTIFICATION
-- ============================================================
CREATE TABLE Notification (
                              notificationID INT AUTO_INCREMENT PRIMARY KEY,
                              userID INT NOT NULL,
                              requestID INT,
                              orderID INT,
                              notificationType VARCHAR(50) NOT NULL,
                              title VARCHAR(200) NOT NULL,
                              content TEXT NOT NULL,
                              actionUrl VARCHAR(500),
                              isRead BOOLEAN NOT NULL DEFAULT FALSE,
                              createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              INDEX idx_notif_user_read (userID, isRead),
                              CONSTRAINT fk_notif_user FOREIGN KEY (userID) REFERENCES `User`(userID) ON DELETE CASCADE ON UPDATE CASCADE,
                              CONSTRAINT fk_notif_request FOREIGN KEY (requestID) REFERENCES ReturnRequest(requestID) ON DELETE SET NULL ON UPDATE CASCADE,
                              CONSTRAINT fk_notif_order FOREIGN KEY (orderID) REFERENCES Orders(orderID) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 21. SKINQUIZ & SKINQUIZRESULT & SKINROUTINE
-- ============================================================
CREATE TABLE SkinQuiz (
                          quizID INT AUTO_INCREMENT PRIMARY KEY,
                          title VARCHAR(200) NOT NULL,
                          description TEXT,
                          questions JSON NOT NULL,
                          isActive BOOLEAN NOT NULL DEFAULT TRUE,
                          createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE SkinQuizResult (
                                resultID INT AUTO_INCREMENT PRIMARY KEY,
                                userID INT NOT NULL,
                                quizID INT NOT NULL,
                                skinType VARCHAR(50) NOT NULL,
                                skincareAdvice TEXT,
                                suitableProducts JSON,
                                completedAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT chk_quizres_type CHECK (skinType IN ('OILY','DRY','COMBINATION','NORMAL','SENSITIVE')),
                                CONSTRAINT fk_quizres_user FOREIGN KEY (userID) REFERENCES `User`(userID) ON DELETE CASCADE ON UPDATE CASCADE,
                                CONSTRAINT fk_quizres_quiz FOREIGN KEY (quizID) REFERENCES SkinQuiz(quizID) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS SkinRoutine (
                                           routineID INT AUTO_INCREMENT PRIMARY KEY,
                                           userID INT NOT NULL UNIQUE,
                                           quizID INT NOT NULL,
                                           skinType VARCHAR(50) NOT NULL,
                                           goals VARCHAR(255),
                                           budget VARCHAR(20),
                                           routineJson TEXT,
                                           createdAt DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           updatedAt DATETIME NULL,
                                           CONSTRAINT chk_routine_type CHECK (skinType IN ('OILY','DRY','COMBINATION','NORMAL','SENSITIVE')),
                                           CONSTRAINT chk_routine_budget CHECK (budget IN ('TIET_KIEM','CAN_BANG','CAO_CAP','KHONG_GIOI_HAN')),
                                           CONSTRAINT fk_routine_user FOREIGN KEY (userID) REFERENCES `User`(userID) ON DELETE CASCADE ON UPDATE CASCADE,
                                           CONSTRAINT fk_routine_quiz FOREIGN KEY (quizID) REFERENCES SkinQuiz(quizID) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;


-- ============================================================
-- ============  DU LIEU MAU (INSERT 1 LAN DUY NHAT) ===========
-- ============================================================

-- 1. Role
INSERT INTO Role (roleID, roleName) VALUES
                                        (1, 'ROLE_ADMIN'),
                                        (2, 'ROLE_CUSTOMER');

-- 2. User
INSERT INTO `User` (userID, roleID, fullName, email, password, phone, avtUrl, pointBalance, isActive, createdAt) VALUES
                                                                                                                     (1, 1, 'Bùi Ngọc Hiền', 'admin@luminous.vn', '$2a$12$lPPieJ03aq3.9WN0UhR7HuUs4R2gLkJZcQCmayx7N9kMT0NAbQMle', '0972606925', 'https://example.com/avatars/admin.png', 0, TRUE, '2026-08-01 08:00:00'),
                                                                                                                     (2, 2, 'Ứng Thị Thanh Trúc', 'thanhtruc@gmail.com', '$2a$12$lPPieJ03aq3.9WN0UhR7HuUs4R2gLkJZcQCmayx7N9kMT0NAbQMle', '0972606924', 'https://example.com/avatars/truc.png', 11, TRUE, '2026-08-15 09:30:00'),
                                                                                                                     (3, 2, 'Nguyễn Trần Hải Đăng', 'haidang@gmail.com', '$2a$12$lPPieJ03aq3.9WN0UhR7HuUs4R2gLkJZcQCmayx7N9kMT0NAbQMle', '0952606925', 'https://example.com/avatars/dang.png', 664, TRUE, '2026-08-20 19:10:00'),
                                                                                                                     (4, 2, 'Trần Gia Hân', 'giahan@gmail.com', '$2a$12$lPPieJ03aq3.9WN0UhR7HuUs4R2gLkJZcQCmayx7N9kMT0NAbQMle', '0912345678', 'https://example.com/avatars/han.png', 100, TRUE, '2026-10-01 10:00:00');

-- 3. Address
INSERT INTO Address (addressID, userID, recipientName, phone, province, district, ward, detailAddress, isDefault) VALUES
                                                                                                                      (1, 2, 'Ứng Thị Thanh Trúc', '0972606924', 'Thành phố Hồ Chí Minh', 'Quận Gò Vấp', 'Phường 1', '12 Nguyễn Văn Bảo', TRUE),
                                                                                                                      (2, 2, 'Ứng Thị Thanh Trúc', '0972606924', 'Thành phố Hồ Chí Minh', 'Quận 12', 'Phường Tân Chánh Hiệp', '45 Tô Ký', FALSE),
                                                                                                                      (3, 3, 'Nguyễn Trần Hải Đăng', '0952606925', 'Thành phố Hồ Chí Minh', 'Quận 1', 'Phường Bến Nghé', '84/10/1 Lý Thường Kiệt', TRUE),
                                                                                                                      (4, 4, 'Trần Gia Hân', '0912345678', 'Thành phố Hồ Chí Minh', 'Thành phố Thủ Đức', 'Phường Linh Trung', '1 Võ Văn Ngân', TRUE);

-- 4. Category (7 danh mục đầy đủ)
INSERT INTO Category (categoryID, categoryName, description, status) VALUES
                                                                         (1, 'Tẩy trang', 'Các sản phẩm làm sạch sâu dịu nhẹ', 'ACTIVE'),
                                                                         (2, 'Sữa rửa mặt', 'Sữa rửa mặt cân bằng độ ẩm và kiềm dầu', 'ACTIVE'),
                                                                         (3, 'Serum & Dưỡng chất', 'Serum tinh chất phục hồi và dưỡng sáng da', 'ACTIVE'),
                                                                         (4, 'Kem chống nắng', 'Bảo vệ da khỏi tia UVA/UVB hằng ngày', 'ACTIVE'),
                                                                         (5, 'Kem dưỡng ẩm', 'Cấp ẩm và phục hồi hàng rào bảo vệ da', 'ACTIVE'),
                                                                         (6, 'Toner', 'Nước cân bằng da, cấp ẩm và làm sạch nhẹ sau bước rửa mặt.', 'ACTIVE'),
                                                                         (7, 'Son môi', 'Son lì, son tint và son kem cho trang điểm hằng ngày.', 'ACTIVE');

-- 5. Product (128 sản phẩm: 1-8 mẫu, 9-108 skincare, 109-128 toner & son)
INSERT INTO Product (productID, categoryID, name, brand, description, isActive, createdAt) VALUES
                                                                                               (1, 1, 'Nước tẩy trang Cocoon bí đao làm sạch dịu nhẹ', 'COCOON', 'Nước tẩy trang bí đao Cocoon giúp làm sạch bụi bẩn, giảm dầu thừa hiệu quả.', TRUE, '2026-08-01 09:00:00'),
                                                                                               (2, 2, 'Sữa Rửa Mặt CeraVe Sạch Sâu Dành Cho Da Dầu', 'CERAVE', 'Sữa rửa mặt CeraVe Foaming Cleanser làm sạch dịu nhẹ, cân bằng độ ẩm tự nhiên.', TRUE, '2026-08-01 09:05:00'),
                                                                                               (3, 2, 'Sữa Rửa Mặt Cetaphil Dịu Nhẹ Cho Da Nhạy Cảm', 'CETAPHIL', 'Sữa rửa mặt Cetaphil Gentle Skin Cleanser không xà phòng, không hương liệu, phù hợp da khô và nhạy cảm.', TRUE, '2026-08-01 09:10:00'),
                                                                                               (4, 3, 'Serum The Ordinary Niacinamide 10% + Zinc 1%', 'THE ORDINARY', 'Serum giúp giảm bóng dầu, thu nhỏ lỗ chân lông và mờ thâm mụn.', TRUE, '2026-08-02 09:00:00'),
                                                                                               (5, 3, 'Serum Klairs Freshly Juiced Vitamin Drop', 'KLAIRS', 'Serum vitamin C dịu nhẹ giúp sáng da, mờ vết thâm, phù hợp da nhạy cảm.', TRUE, '2026-08-02 09:05:00'),
                                                                                               (6, 4, 'Kem Chống Nắng La Roche-Posay Anthelios UVMune 400', 'LA ROCHE-POSAY', 'Kem chống nắng phổ rộng SPF50+, kết cấu mỏng nhẹ, không gây bết dính.', TRUE, '2026-08-03 09:00:00'),
                                                                                               (7, 4, 'Gel Chống Nắng Skin Aqua UV Super Moisture Gel', 'SKIN AQUA', 'Gel chống nắng SPF50+ PA++++ dưỡng ẩm, dùng được hằng ngày.', TRUE, '2026-08-03 09:05:00'),
                                                                                               (8, 5, 'Kem Dưỡng Ẩm CeraVe Moisturising Cream', 'CERAVE', 'Kem dưỡng ẩm chứa 3 loại ceramide thiết yếu, phục hồi hàng rào bảo vệ da.', TRUE, '2026-08-04 09:00:00'),
                                                                                               (9, 1, 'Nước tẩy trang Bioderma Sensibio H2O', 'BIODERMA', 'Nước tẩy trang micellar làm sạch dịu nhẹ, không cần rửa lại, cho da nhạy cảm.', TRUE, '2026-09-01 09:00:00'),
                                                                                               (10, 1, 'Nước tẩy trang Garnier Micellar hồng', 'GARNIER', 'Nước micellar làm sạch lớp trang điểm và bụi bẩn, không gây nhờn rít.', TRUE, '2026-09-02 09:00:00'),
                                                                                               (11, 1, 'Nước tẩy trang Garnier Micellar xanh', 'GARNIER', 'Nước micellar dành cho da dầu và hỗn hợp, giúp kiềm dầu nhẹ.', TRUE, '2026-09-03 09:00:00'),
                                                                                               (12, 1, 'Nước tẩy trang L''Oréal Paris Micellar 3-in-1', 'L''OREAL PARIS', 'Nước micellar 3 trong 1: tẩy trang, làm sạch và làm dịu da.', TRUE, '2026-09-04 09:00:00'),
                                                                                               (13, 1, 'Nước tẩy trang Cocoon hoa hồng', 'COCOON', 'Nước tẩy trang chiết xuất hoa hồng giúp làm sạch và cân bằng da.', TRUE, '2026-09-05 09:00:00'),
                                                                                               (14, 1, 'Dầu tẩy trang Cocoon hoa hồng', 'COCOON', 'Dầu tẩy trang nhũ hóa nhanh, làm sạch sâu lớp chống nắng và trang điểm.', TRUE, '2026-09-06 09:00:00'),
                                                                                               (15, 1, 'Nước tẩy trang Simple Kind to Skin Micellar', 'SIMPLE', 'Nước micellar không hương liệu, không cồn, dịu nhẹ cho da nhạy cảm.', TRUE, '2026-09-07 09:00:00'),
                                                                                               (16, 1, 'Nước tẩy trang Nivea MicellAir Skin Breathe', 'NIVEA', 'Nước micellar làm sạch và cấp ẩm nhẹ, cho cảm giác da thoáng.', TRUE, '2026-09-08 09:00:00'),
                                                                                               (17, 1, 'Nước tẩy trang La Roche-Posay Micellar Water Ultra', 'LA ROCHE-POSAY', 'Nước micellar cho da nhạy cảm, làm sạch mà không gây khô căng.', TRUE, '2026-09-09 09:00:00'),
                                                                                               (18, 1, 'Nước tẩy trang Eucerin DermatoClean Micellar Water', 'EUCERIN', 'Nước micellar làm sạch sâu, hỗ trợ da khô và nhạy cảm.', TRUE, '2026-09-10 09:00:00'),
                                                                                               (19, 1, 'Nước tẩy trang Cetaphil Gentle', 'CETAPHIL', 'Nước tẩy trang dịu nhẹ, không xà phòng, phù hợp da khô và nhạy cảm.', TRUE, '2026-09-11 09:00:00'),
                                                                                               (20, 1, 'Nước tẩy trang Neutrogena Hydro Boost Micellar', 'NEUTROGENA', 'Nước micellar cấp nước, làm sạch nhẹ nhàng cho da thường và khô.', TRUE, '2026-09-12 09:00:00'),
                                                                                               (21, 1, 'Dầu tẩy trang Hada Labo Gokujyun Cleansing Oil', 'HADA LABO', 'Dầu tẩy trang cấp ẩm với Hyaluronic Acid, nhũ hóa sạch, không nhờn.', TRUE, '2026-09-13 09:00:00'),
                                                                                               (22, 1, 'Dầu tẩy trang Biore Perfect Cleansing Oil', 'BIORE', 'Dầu tẩy trang làm sạch sâu lỗ chân lông, cho da dầu và hỗn hợp.', TRUE, '2026-09-14 09:00:00'),
                                                                                               (23, 1, 'Nước tẩy trang Innisfree Green Tea Cleansing Water', 'INNISFREE', 'Nước tẩy trang trà xanh giúp làm sạch và chống oxy hóa nhẹ.', TRUE, '2026-09-15 09:00:00'),
                                                                                               (24, 1, 'Sáp tẩy trang Heimish All Clean Balm', 'HEIMISH', 'Sáp tẩy trang tan nhanh, làm sạch trang điểm và kem chống nắng.', TRUE, '2026-09-16 09:00:00'),
                                                                                               (25, 1, 'Sáp tẩy trang Banila Co Clean It Zero', 'BANILA CO', 'Sáp tẩy trang nổi tiếng, làm sạch sâu mà vẫn mềm da.', TRUE, '2026-09-17 09:00:00'),
                                                                                               (26, 1, 'Dầu tẩy trang Manyo Pure Cleansing Oil', 'MANYO', 'Dầu tẩy trang thuần chay, làm sạch nhẹ nhàng cho da nhạy cảm.', TRUE, '2026-09-18 09:00:00'),
                                                                                               (27, 1, 'Dầu tẩy trang DHC Deep Cleansing Oil', 'DHC', 'Dầu tẩy trang ô liu làm sạch sâu, không gây bít tắc.', TRUE, '2026-09-19 09:00:00'),
                                                                                               (28, 1, 'Dầu tẩy trang Kose Softymo Deep Cleansing Oil', 'KOSE', 'Dầu tẩy trang làm sạch nhanh, giá tốt cho dùng hằng ngày.', TRUE, '2026-09-20 09:00:00'),
                                                                                               (29, 2, 'Sữa rửa mặt CeraVe SA Smoothing Cleanser', 'CERAVE', 'Sữa rửa mặt chứa Salicylic Acid giúp làm mịn bề mặt da, cho da sần sùi.', TRUE, '2026-09-01 10:00:00'),
                                                                                               (30, 2, 'Sữa rửa mặt Cetaphil Oily Skin Cleanser', 'CETAPHIL', 'Sữa rửa mặt cho da dầu, làm sạch dầu thừa mà không gây khô căng.', TRUE, '2026-09-02 10:00:00'),
                                                                                               (31, 2, 'Sữa rửa mặt Simple Refreshing Facial Wash', 'SIMPLE', 'Gel rửa mặt dịu nhẹ, không hương liệu, cho da nhạy cảm dùng hằng ngày.', TRUE, '2026-09-03 10:00:00'),
                                                                                               (32, 2, 'Sữa rửa mặt Simple Moisturising Facial Wash', 'SIMPLE', 'Sữa rửa mặt dưỡng ẩm nhẹ, phù hợp da khô và thường.', TRUE, '2026-09-04 10:00:00'),
                                                                                               (33, 2, 'Sữa rửa mặt La Roche-Posay Toleriane Dermo-Cleanser', 'LA ROCHE-POSAY', 'Sữa rửa mặt cho da rất khô và nhạy cảm, không gây kích ứng.', TRUE, '2026-09-05 10:00:00'),
                                                                                               (34, 2, 'Gel rửa mặt La Roche-Posay Effaclar Purifying Foaming Gel', 'LA ROCHE-POSAY', 'Gel rửa mặt làm sạch sâu cho da dầu, mụn, giúp giảm bóng dầu.', TRUE, '2026-09-06 10:00:00'),
                                                                                               (35, 2, 'Sữa rửa mặt Eucerin pH5 Facial Cleanser', 'EUCERIN', 'Sữa rửa mặt cân bằng pH5, dịu nhẹ cho da khô và nhạy cảm.', TRUE, '2026-09-07 10:00:00'),
                                                                                               (36, 2, 'Gel rửa mặt Bioderma Sensibio Gel Moussant', 'BIODERMA', 'Gel rửa mặt tạo bọt nhẹ cho da nhạy cảm, làm sạch mà không khô da.', TRUE, '2026-09-08 10:00:00'),
                                                                                               (37, 2, 'Sữa rửa mặt Garnier Pure Active', 'GARNIER', 'Sữa rửa mặt cho da dầu mụn, chứa Salicylic Acid giúp làm sạch lỗ chân lông.', TRUE, '2026-09-09 10:00:00'),
                                                                                               (38, 2, 'Gel rửa mặt Neutrogena Hydro Boost Cleanser', 'NEUTROGENA', 'Gel rửa mặt cấp nước với Hyaluronic Acid, cho da thường và khô.', TRUE, '2026-09-10 10:00:00'),
                                                                                               (39, 2, 'Gel rửa mặt Cosrx Low pH Good Morning', 'COSRX', 'Gel rửa mặt pH thấp dịu nhẹ, dùng được cả sáng và tối.', TRUE, '2026-09-11 10:00:00'),
                                                                                               (40, 2, 'Sữa rửa mặt Some By Mi AHA-BHA-PHA 30 Days Miracle Foam', 'SOME BY MI', 'Sữa rửa mặt cho da mụn với AHA, BHA, PHA và tràm trà.', TRUE, '2026-09-12 10:00:00'),
                                                                                               (41, 2, 'Sữa rửa mặt Innisfree Green Tea Foam Cleanser', 'INNISFREE', 'Sữa rửa mặt trà xanh tạo bọt mịn, làm sạch và chống oxy hóa.', TRUE, '2026-09-13 10:00:00'),
                                                                                               (42, 2, 'Sữa rửa mặt Hada Labo Gokujyun Face Wash', 'HADA LABO', 'Sữa rửa mặt cấp ẩm với Hyaluronic Acid, bọt mịn và dịu nhẹ.', TRUE, '2026-09-14 10:00:00'),
                                                                                               (43, 2, 'Sữa rửa mặt Skin1004 Madagascar Centella Ampoule Foam', 'SKIN1004', 'Sữa rửa mặt rau má giúp làm dịu da, phù hợp da nhạy cảm và mụn.', TRUE, '2026-09-15 10:00:00'),
                                                                                               (44, 2, 'Sữa rửa mặt Paula''s Choice Calm Ultra-Gentle Cleanser', 'PAULA''S CHOICE', 'Sữa rửa mặt siêu dịu nhẹ cho da nhạy cảm, không hương liệu.', TRUE, '2026-09-16 10:00:00'),
                                                                                               (45, 2, 'Sữa rửa mặt L''Oréal Paris Glycolic Bright', 'L''OREAL PARIS', 'Sữa rửa mặt sáng da với Glycolic Acid, giúp da đều màu hơn.', TRUE, '2026-09-17 10:00:00'),
                                                                                               (46, 2, 'Sữa rửa mặt Nivea Extra White Repair & Protect', 'NIVEA', 'Sữa rửa mặt làm sáng và phục hồi da xỉn màu.', TRUE, '2026-09-18 10:00:00'),
                                                                                               (47, 2, 'Gel rửa mặt Cocoon bí đao', 'COCOON', 'Gel rửa mặt bí đao giúp kiềm dầu, làm sạch dịu nhẹ cho da dầu mụn.', TRUE, '2026-09-19 10:00:00'),
                                                                                               (48, 2, 'Sữa rửa mặt CeraVe Hydrating Facial Cleanser', 'CERAVE', 'Sữa rửa mặt dưỡng ẩm với 3 loại ceramide, cho da thường và khô.', TRUE, '2026-09-20 10:00:00'),
                                                                                               (49, 3, 'Serum The Ordinary Hyaluronic Acid 2% + B5', 'THE ORDINARY', 'Serum cấp ẩm sâu với Hyaluronic Acid và Vitamin B5.', TRUE, '2026-09-01 11:00:00'),
                                                                                               (50, 3, 'Serum The Ordinary Alpha Arbutin 2% + HA', 'THE ORDINARY', 'Serum làm sáng da, mờ thâm với Alpha Arbutin và Hyaluronic Acid.', TRUE, '2026-09-02 11:00:00'),
                                                                                               (51, 3, 'Serum The Ordinary Retinol 0.5% in Squalane', 'THE ORDINARY', 'Serum chống lão hóa với Retinol 0.5% trong Squalane, dùng buổi tối.', TRUE, '2026-09-03 11:00:00'),
                                                                                               (52, 3, 'Serum Klairs Midnight Blue Youth Activating Drop', 'KLAIRS', 'Serum phục hồi ban đêm với Peptide và Guaiazulene làm dịu da.', TRUE, '2026-09-04 11:00:00'),
                                                                                               (53, 3, 'Serum Klairs Rich Moist Soothing Serum', 'KLAIRS', 'Serum cấp ẩm và làm dịu cho da khô, nhạy cảm.', TRUE, '2026-09-05 11:00:00'),
                                                                                               (54, 3, 'Tinh chất Skin1004 Madagascar Centella Ampoule', 'SKIN1004', 'Tinh chất rau má nguyên chất giúp làm dịu và phục hồi da.', TRUE, '2026-09-06 11:00:00'),
                                                                                               (55, 3, 'Tinh chất Cosrx Advanced Snail 96 Mucin Power Essence', 'COSRX', 'Tinh chất ốc sên 96% giúp phục hồi và cấp ẩm cho da.', TRUE, '2026-09-07 11:00:00'),
                                                                                               (56, 3, 'Serum Cosrx The Vitamin C 23', 'COSRX', 'Serum Vitamin C 23% giúp sáng da, mờ thâm, chống oxy hóa.', TRUE, '2026-09-08 11:00:00'),
                                                                                               (57, 3, 'Serum Some By Mi AHA-BHA-PHA 30 Days Miracle', 'SOME BY MI', 'Serum cho da mụn với AHA, BHA, PHA và tràm trà.', TRUE, '2026-09-09 11:00:00'),
                                                                                               (58, 3, 'Serum La Roche-Posay Pure Niacinamide 10', 'LA ROCHE-POSAY', 'Serum Niacinamide 10% giúp mờ thâm và đều màu da.', TRUE, '2026-09-10 11:00:00'),
                                                                                               (59, 3, 'Serum La Roche-Posay Hyalu B5', 'LA ROCHE-POSAY', 'Serum cấp ẩm và phục hồi với Hyaluronic Acid và Vitamin B5.', TRUE, '2026-09-11 11:00:00'),
                                                                                               (60, 3, 'Serum CeraVe Resurfacing Retinol', 'CERAVE', 'Serum Retinol giúp làm mịn da, mờ thâm mụn, có ceramide bảo vệ da.', TRUE, '2026-09-12 11:00:00'),
                                                                                               (61, 3, 'Dung dịch Paula''s Choice 2% BHA Liquid Exfoliant', 'PAULA''S CHOICE', 'Dung dịch tẩy tế bào chết hóa học BHA 2% cho da dầu mụn.', TRUE, '2026-09-13 11:00:00'),
                                                                                               (62, 3, 'Serum Paula''s Choice 10% Niacinamide Booster', 'PAULA''S CHOICE', 'Serum Niacinamide 10% giúp thu nhỏ lỗ chân lông và đều màu da.', TRUE, '2026-09-14 11:00:00'),
                                                                                               (63, 3, 'Serum L''Oréal Paris Glycolic Bright', 'L''OREAL PARIS', 'Serum sáng da với Glycolic Acid và Niacinamide.', TRUE, '2026-09-15 11:00:00'),
                                                                                               (64, 3, 'Serum Garnier Vitamin C', 'GARNIER', 'Serum Vitamin C giúp sáng da, mờ đốm nâu, giá tốt cho dùng hằng ngày.', TRUE, '2026-09-16 11:00:00'),
                                                                                               (65, 3, 'Serum Innisfree Green Tea Seed Serum', 'INNISFREE', 'Serum trà xanh cấp ẩm sâu, cho da thường và khô.', TRUE, '2026-09-17 11:00:00'),
                                                                                               (66, 3, 'Serum Laneige Water Bank Blue Hyaluronic', 'LANEIGE', 'Serum cấp ẩm với Blue Hyaluronic Acid, cho da khô và mất nước.', TRUE, '2026-09-18 11:00:00'),
                                                                                               (67, 3, 'Serum Neutrogena Hydro Boost Hyaluronic Acid', 'NEUTROGENA', 'Serum cấp nước tức thì với Hyaluronic Acid.', TRUE, '2026-09-19 11:00:00'),
                                                                                               (68, 3, 'Tinh chất Cocoon nghệ Hưng Yên', 'COCOON', 'Tinh chất nghệ giúp sáng da, mờ thâm, chiết xuất thiên nhiên Việt Nam.', TRUE, '2026-09-20 11:00:00'),
                                                                                               (69, 4, 'Kem chống nắng Anessa Perfect UV Sunscreen Milk', 'ANESSA', 'Sữa chống nắng SPF50+ PA++++, chống nước và mồ hôi, cho da dầu.', TRUE, '2026-09-01 12:00:00'),
                                                                                               (70, 4, 'Gel chống nắng Anessa Perfect UV Sunscreen Gel', 'ANESSA', 'Gel chống nắng SPF50+ PA++++, mỏng nhẹ, dùng được cho mặt và cơ thể.', TRUE, '2026-09-02 12:00:00'),
                                                                                               (71, 4, 'Tinh chất chống nắng Biore UV Aqua Rich Watery Essence', 'BIORE', 'Tinh chất chống nắng dạng nước SPF50+ PA++++, thấm nhanh, không bết.', TRUE, '2026-09-03 12:00:00'),
                                                                                               (72, 4, 'Gel chống nắng Biore UV Aqua Rich Watery Gel', 'BIORE', 'Gel chống nắng SPF50+ PA++++, cấp ẩm nhẹ, phù hợp dùng hằng ngày.', TRUE, '2026-09-04 12:00:00'),
                                                                                               (73, 4, 'Tinh chất chống nắng Skin Aqua Tone Up UV Essence', 'SKIN AQUA', 'Tinh chất chống nắng nâng tông nhẹ SPF50+ PA++++.', TRUE, '2026-09-05 12:00:00'),
                                                                                               (74, 4, 'Sữa chống nắng Skin Aqua UV Super Moisture Milk', 'SKIN AQUA', 'Sữa chống nắng SPF50+ PA++++, dưỡng ẩm, cho da khô và nhạy cảm.', TRUE, '2026-09-06 12:00:00'),
                                                                                               (75, 4, 'Kem chống nắng La Roche-Posay Anthelios Invisible Fluid', 'LA ROCHE-POSAY', 'Kem chống nắng dạng sữa lỏng SPF50+, không nhờn, không vệt trắng.', TRUE, '2026-09-07 12:00:00'),
                                                                                               (76, 4, 'Gel chống nắng Eucerin Sun Oil Control Gel-Creme', 'EUCERIN', 'Gel chống nắng kiềm dầu SPF50+ cho da dầu và hỗn hợp.', TRUE, '2026-09-08 12:00:00'),
                                                                                               (77, 4, 'Kem chống nắng Eucerin Sun Photoaging Control Fluid', 'EUCERIN', 'Kem chống nắng chống lão hóa do nắng SPF50, cho da thường và khô.', TRUE, '2026-09-09 12:00:00'),
                                                                                               (78, 4, 'Kem chống nắng Bioderma Photoderm Max Aquafluide', 'BIODERMA', 'Kem chống nắng dạng sữa lỏng SPF50+, cho da nhạy cảm.', TRUE, '2026-09-10 12:00:00'),
                                                                                               (79, 4, 'Kem chống nắng Neutrogena Ultra Sheer Dry-Touch SPF50', 'NEUTROGENA', 'Kem chống nắng khô ráo SPF50, không nhờn rít, cho da dầu.', TRUE, '2026-09-11 12:00:00'),
                                                                                               (80, 4, 'Kem chống nắng Neutrogena Hydro Boost Water Gel Lotion SPF50', 'NEUTROGENA', 'Kem chống nắng cấp nước SPF50, cho da khô và thường.', TRUE, '2026-09-12 12:00:00'),
                                                                                               (81, 4, 'Kem chống nắng L''Oréal Paris UV Defender Serum SPF50', 'L''OREAL PARIS', 'Serum chống nắng SPF50+ PA++++, mỏng nhẹ như serum.', TRUE, '2026-09-13 12:00:00'),
                                                                                               (82, 4, 'Kem chống nắng Garnier Ambre Solaire Super UV Serum', 'GARNIER', 'Serum chống nắng SPF50+ PA++++ với Vitamin C và Hyaluronic Acid.', TRUE, '2026-09-14 12:00:00'),
                                                                                               (83, 4, 'Kem chống nắng Nivea Sun UV Face Shine Control SPF50', 'NIVEA', 'Kem chống nắng kiềm dầu SPF50 cho vùng mặt, da dầu và hỗn hợp.', TRUE, '2026-09-15 12:00:00'),
                                                                                               (84, 4, 'Kem chống nắng Innisfree Daily UV Protection Cream Mild', 'INNISFREE', 'Kem chống nắng dịu nhẹ SPF35, cho da nhạy cảm dùng hằng ngày.', TRUE, '2026-09-16 12:00:00'),
                                                                                               (85, 4, 'Kem chống nắng Cosrx Aloe Soothing Sun Cream', 'COSRX', 'Kem chống nắng lô hội SPF50+ PA+++, làm dịu da.', TRUE, '2026-09-17 12:00:00'),
                                                                                               (86, 4, 'Kem chống nắng Some By Mi Truecica Mineral 100 Calming Suncream', 'SOME BY MI', 'Kem chống nắng vật lý Truecica giúp làm dịu da mụn, SPF50+ PA++++.', TRUE, '2026-09-18 12:00:00'),
                                                                                               (87, 4, 'Kem chống nắng Cocoon bí đao', 'COCOON', 'Kem chống nắng bí đao SPF50+ PA++++, kiềm dầu nhẹ cho da dầu mụn.', TRUE, '2026-09-19 12:00:00'),
                                                                                               (88, 4, 'Kem chống nắng La Roche-Posay Anthelios UVMune 400 Invisible Fluid', 'LA ROCHE-POSAY', 'Kem chống nắng phổ siêu rộng UVMune 400, SPF50+, dạng sữa lỏng.', TRUE, '2026-09-20 12:00:00'),
                                                                                               (89, 5, 'Kem dưỡng CeraVe Moisturising Lotion', 'CERAVE', 'Lotion dưỡng ẩm với 3 ceramide, thấm nhanh, cho da thường và khô.', TRUE, '2026-09-01 13:00:00'),
                                                                                               (90, 5, 'Kem dưỡng CeraVe PM Facial Moisturising Lotion', 'CERAVE', 'Lotion dưỡng ban đêm với Niacinamide và ceramide, phục hồi da.', TRUE, '2026-09-02 13:00:00'),
                                                                                               (91, 5, 'Lotion dưỡng Cetaphil Moisturizing Lotion', 'CETAPHIL', 'Lotion dưỡng ẩm dịu nhẹ, không hương liệu, cho da khô và nhạy cảm.', TRUE, '2026-09-03 13:00:00'),
                                                                                               (92, 5, 'Kem dưỡng Cetaphil Moisturizing Cream', 'CETAPHIL', 'Kem dưỡng ẩm sâu cho da rất khô, không gây bít tắc.', TRUE, '2026-09-04 13:00:00'),
                                                                                               (93, 5, 'Kem dưỡng La Roche-Posay Toleriane Double Repair Face Moisturizer', 'LA ROCHE-POSAY', 'Kem dưỡng phục hồi hàng rào da với ceramide và Niacinamide.', TRUE, '2026-09-05 13:00:00'),
                                                                                               (94, 5, 'Kem dưỡng La Roche-Posay Cicaplast Baume B5', 'LA ROCHE-POSAY', 'Kem phục hồi B5 cho da tổn thương, kích ứng, sau mụn.', TRUE, '2026-09-06 13:00:00'),
                                                                                               (95, 5, 'Gel dưỡng Neutrogena Hydro Boost Water Gel', 'NEUTROGENA', 'Gel dưỡng cấp nước tức thì, mỏng nhẹ, cho da dầu và hỗn hợp.', TRUE, '2026-09-07 13:00:00'),
                                                                                               (96, 5, 'Gel dưỡng Neutrogena Hydro Boost Gel-Cream', 'NEUTROGENA', 'Gel-cream dưỡng ẩm sâu hơn cho da khô, vẫn thấm nhanh.', TRUE, '2026-09-08 13:00:00'),
                                                                                               (97, 5, 'Kem dưỡng Klairs Rich Moist Soothing Cream', 'KLAIRS', 'Kem dưỡng ẩm và làm dịu cho da khô, nhạy cảm.', TRUE, '2026-09-09 13:00:00'),
                                                                                               (98, 5, 'Kem dưỡng Klairs Midnight Blue Calming Cream', 'KLAIRS', 'Kem dưỡng làm dịu ban đêm với Guaiazulene, cho da kích ứng.', TRUE, '2026-09-10 13:00:00'),
                                                                                               (99, 5, 'Kem dưỡng Cosrx Advanced Snail 92 All in One Cream', 'COSRX', 'Kem dưỡng ốc sên 92% giúp phục hồi và cấp ẩm.', TRUE, '2026-09-11 13:00:00'),
                                                                                               (100, 5, 'Lotion dưỡng Cosrx Oil-Free Ultra-Moisturizing Lotion', 'COSRX', 'Lotion dưỡng không dầu, mỏng nhẹ cho da dầu và hỗn hợp.', TRUE, '2026-09-12 13:00:00'),
                                                                                               (101, 5, 'Kem dưỡng Skin1004 Madagascar Centella Soothing Cream', 'SKIN1004', 'Kem dưỡng rau má làm dịu và cấp ẩm, cho da nhạy cảm.', TRUE, '2026-09-13 13:00:00'),
                                                                                               (102, 5, 'Kem dưỡng Innisfree Green Tea Seed Cream', 'INNISFREE', 'Kem dưỡng trà xanh cấp ẩm, chống oxy hóa, cho da thường và khô.', TRUE, '2026-09-14 13:00:00'),
                                                                                               (103, 5, 'Kem dưỡng Laneige Water Bank Blue Hyaluronic Cream', 'LANEIGE', 'Kem dưỡng cấp ẩm với Blue Hyaluronic Acid, cho da khô.', TRUE, '2026-09-15 13:00:00'),
                                                                                               (104, 5, 'Kem dưỡng Simple Replenishing Rich Moisturiser', 'SIMPLE', 'Kem dưỡng giàu ẩm, không hương liệu, cho da khô.', TRUE, '2026-09-16 13:00:00'),
                                                                                               (105, 5, 'Kem dưỡng Eucerin AtoControl Face Care Cream', 'EUCERIN', 'Kem dưỡng cho da khô, chàm, giúp giảm ngứa và khô ráp.', TRUE, '2026-09-17 13:00:00'),
                                                                                               (106, 5, 'Kem dưỡng Bioderma Atoderm Creme', 'BIODERMA', 'Kem dưỡng cho da rất khô, giúp phục hồi hàng rào da.', TRUE, '2026-09-18 13:00:00'),
                                                                                               (107, 5, 'Kem dưỡng L''Oréal Paris Revitalift Triple Action Day Cream', 'L''OREAL PARIS', 'Kem dưỡng ban ngày chống lão hóa với Pro-Retinol.', TRUE, '2026-09-19 13:00:00'),
                                                                                               (108, 5, 'Kem dưỡng Cocoon hoa hồng', 'COCOON', 'Kem dưỡng hoa hồng giúp cấp ẩm và làm dịu da.', TRUE, '2026-09-20 13:00:00'),
                                                                                               (109, 6, 'Toner Klairs Supple Preparation Facial Toner', 'KLAIRS', 'Toner cấp ẩm và cân bằng da, dùng được cho da nhạy cảm.', TRUE, '2026-09-11 14:00:00'),
                                                                                               (110, 6, 'Toner Cosrx AHA/BHA Clarifying Treatment Toner', 'COSRX', 'Toner làm sạch nhẹ với AHA, BHA, giúp da mịn và đều màu.', TRUE, '2026-09-12 14:00:00'),
                                                                                               (111, 6, 'Toner Some By Mi AHA-BHA-PHA 30 Days Miracle', 'SOME BY MI', 'Toner cho da mụn với AHA, BHA, PHA và tràm trà.', TRUE, '2026-09-13 14:00:00'),
                                                                                               (112, 6, 'Toner Innisfree Green Tea Balancing Skin', 'INNISFREE', 'Toner trà xanh cân bằng dầu và ẩm cho da hỗn hợp.', TRUE, '2026-09-14 14:00:00'),
                                                                                               (113, 6, 'Toner Laneige Cream Skin Refiner', 'LANEIGE', 'Toner dạng sữa cấp ẩm sâu, cho da khô và thường.', TRUE, '2026-09-15 14:00:00'),
                                                                                               (114, 6, 'Lotion Hada Labo Gokujyun Super Hyaluronic Acid', 'HADA LABO', 'Lotion cấp ẩm với nhiều loại Hyaluronic Acid.', TRUE, '2026-09-16 14:00:00'),
                                                                                               (115, 6, 'Toner Bioderma Sensibio Tonique', 'BIODERMA', 'Toner làm dịu cho da nhạy cảm, không cồn.', TRUE, '2026-09-17 14:00:00'),
                                                                                               (116, 6, 'Toner Simple Kind to Skin Soothing Facial Toner', 'SIMPLE', 'Toner dịu nhẹ, không hương liệu, cho da nhạy cảm.', TRUE, '2026-09-18 14:00:00'),
                                                                                               (117, 6, 'Toner Garnier Micellar Rose Water', 'GARNIER', 'Toner nước hoa hồng làm sạch và cân bằng da.', TRUE, '2026-09-19 14:00:00'),
                                                                                               (118, 6, 'Xịt khoáng La Roche-Posay Serozinc', 'LA ROCHE-POSAY', 'Xịt khoáng kẽm giúp kiềm dầu và làm dịu da dầu mụn.', TRUE, '2026-09-20 14:00:00'),
                                                                                               (119, 7, 'Son MAC Matte Lipstick Velvet Teddy', 'MAC', 'Son lì huyền thoại, màu nude nâu, lên màu chuẩn.', TRUE, '2026-09-11 15:00:00'),
                                                                                               (120, 7, 'Son Maybelline Color Sensational Creamy Matte', 'MAYBELLINE', 'Son lì kem, màu đỏ gạch, dưỡng ẩm nhẹ.', TRUE, '2026-09-12 15:00:00'),
                                                                                               (121, 7, 'Son L''Oréal Paris Color Riche Matte', 'L''OREAL PARIS', 'Son lì sang trọng, màu đỏ cổ điển.', TRUE, '2026-09-13 15:00:00'),
                                                                                               (122, 7, 'Son 3CE Velvet Lip Tint', '3CE', 'Son tint lì mịn, màu hồng đất, bám màu tốt.', TRUE, '2026-09-14 15:00:00'),
                                                                                               (123, 7, 'Son Romand Juicy Lasting Tint', 'ROMAND', 'Son tint bóng nhẹ, màu đỏ cam tươi tắn.', TRUE, '2026-09-15 15:00:00'),
                                                                                               (124, 7, 'Son Black Rouge Air Fit Velvet Tint', 'BLACK ROUGE', 'Son tint lì nhẹ môi, màu đỏ nâu.', TRUE, '2026-09-16 15:00:00'),
                                                                                               (125, 7, 'Son Bbia Last Velvet Lip Tint', 'BBIA', 'Son tint lì, màu cam đất, giá tốt.', TRUE, '2026-09-17 15:00:00'),
                                                                                               (126, 7, 'Son Peripera Ink Velvet', 'PERIPERA', 'Son tint mực, màu hồng đỏ, lâu trôi.', TRUE, '2026-09-18 15:00:00'),
                                                                                               (127, 7, 'Son Merzy The First Velvet Tint', 'MERZY', 'Son tint lì, màu đỏ rượu, cho da châu Á.', TRUE, '2026-09-19 15:00:00'),
                                                                                               (128, 7, 'Son Ofelia Uncovered Lip Mousse', 'OFELIA', 'Son kem lì, màu nude hồng, chất mousse nhẹ.', TRUE, '2026-09-20 15:00:00');

-- 6. ProductMedia (Đã cấu hình sẵn đường dẫn đúng thư mục phân loại cho tất cả)
INSERT INTO ProductMedia (mediaID, productID, mediaURL, mediaType, thumbnailUrl, displayOrder, altText) VALUES
                                                                                                            (1, 1, '/images/products/makeup-remover/nuoc-tay-trang-cocoon-bi-dao-1.jpg', 'IMAGE', '/images/products/makeup-remover/nuoc-tay-trang-cocoon-bi-dao-1.jpg', 1, 'Nước tẩy trang Cocoon bí đao 500ml'),
                                                                                                            (2, 1, 'https://example.com/images/cocoon-texture.jpg', 'IMAGE', 'https://example.com/images/thumbs/cocoon-texture.jpg', 2, 'Kết cấu nước tẩy trang Cocoon'),
                                                                                                            (3, 2, '/images/products/cleanser/sua-rua-mat-cerave-sach-sau-2.jpg', 'IMAGE', '/images/products/cleanser/sua-rua-mat-cerave-sach-sau-2.jpg', 1, 'Sữa rửa mặt CeraVe 473ml'),
                                                                                                            (4, 2, 'https://example.com/videos/cerave-guide.mp4', 'VIDEO', 'https://example.com/images/thumbs/cerave-guide.jpg', 2, 'Video hướng dẫn dùng sữa rửa mặt CeraVe'),
                                                                                                            (5, 3, '/images/products/cleanser/sua-rua-mat-cetaphil-diu-nhe-3.jpg', 'IMAGE', '/images/products/cleanser/sua-rua-mat-cetaphil-diu-nhe-3.jpg', 1, 'Sữa rửa mặt Cetaphil 500ml'),
                                                                                                            (6, 4, '/images/products/serum/serum-the-ordinary-niacinamide-4.jpg', 'IMAGE', '/images/products/serum/serum-the-ordinary-niacinamide-4.jpg', 1, 'Serum The Ordinary Niacinamide'),
                                                                                                            (7, 5, '/images/products/serum/serum-klairs-vitamin-c-5.jpg', 'IMAGE', '/images/products/serum/serum-klairs-vitamin-c-5.jpg', 1, 'Serum Klairs Vitamin C'),
                                                                                                            (8, 6, '/images/products/sunscreen/kem-chong-nang-la-roche-posay-anthelios-6.jpg', 'IMAGE', '/images/products/sunscreen/kem-chong-nang-la-roche-posay-anthelios-6.jpg', 1, 'Kem chống nắng La Roche-Posay Anthelios'),
                                                                                                            (9, 7, '/images/products/sunscreen/gel-chong-nang-skin-aqua-uv-7.jpg', 'IMAGE', '/images/products/sunscreen/gel-chong-nang-skin-aqua-uv-7.jpg', 1, 'Gel chống nắng Skin Aqua'),
                                                                                                            (10, 8, '/images/products/moisturizer/kem-duong-am-cerave-moisturising-8.jpg', 'IMAGE', '/images/products/moisturizer/kem-duong-am-cerave-moisturising-8.jpg', 1, 'Kem dưỡng ẩm CeraVe'),
                                                                                                            (11, 9, '/images/products/makeup-remover/nuoc-tay-trang-bioderma-sensibio-h2o-9.jpg', 'IMAGE', '/images/products/makeup-remover/nuoc-tay-trang-bioderma-sensibio-h2o-9.jpg', 1, 'Nước tẩy trang Bioderma Sensibio H2O'),
                                                                                                            (12, 10, '/images/products/makeup-remover/nuoc-tay-trang-garnier-micellar-hong-10.jpg', 'IMAGE', '/images/products/makeup-remover/nuoc-tay-trang-garnier-micellar-hong-10.jpg', 1, 'Nước tẩy trang Garnier Micellar hồng'),
                                                                                                            (13, 11, '/images/products/makeup-remover/nuoc-tay-trang-garnier-micellar-xanh-11.jpg', 'IMAGE', '/images/products/makeup-remover/nuoc-tay-trang-garnier-micellar-xanh-11.jpg', 1, 'Nước tẩy trang Garnier Micellar xanh'),
                                                                                                            (14, 12, '/images/products/makeup-remover/nuoc-tay-trang-l-oreal-paris-micellar-3-in-1-12.jpg', 'IMAGE', '/images/products/makeup-remover/nuoc-tay-trang-l-oreal-paris-micellar-3-in-1-12.jpg', 1, 'Nước tẩy trang L''Oréal Paris Micellar 3-in-1'),
                                                                                                            (15, 13, '/images/products/makeup-remover/nuoc-tay-trang-cocoon-hoa-hong-13.jpg', 'IMAGE', '/images/products/makeup-remover/nuoc-tay-trang-cocoon-hoa-hong-13.jpg', 1, 'Nước tẩy trang Cocoon hoa hồng'),
                                                                                                            (16, 14, '/images/products/makeup-remover/dau-tay-trang-cocoon-hoa-hong-14.jpg', 'IMAGE', '/images/products/makeup-remover/dau-tay-trang-cocoon-hoa-hong-14.jpg', 1, 'Dầu tẩy trang Cocoon hoa hồng'),
                                                                                                            (17, 15, '/images/products/makeup-remover/nuoc-tay-trang-simple-kind-to-skin-micellar-15.jpg', 'IMAGE', '/images/products/makeup-remover/nuoc-tay-trang-simple-kind-to-skin-micellar-15.jpg', 1, 'Nước tẩy trang Simple Kind to Skin Micellar'),
                                                                                                            (18, 16, '/images/products/makeup-remover/nuoc-tay-trang-nivea-micellair-skin-breathe-16.jpg', 'IMAGE', '/images/products/makeup-remover/nuoc-tay-trang-nivea-micellair-skin-breathe-16.jpg', 1, 'Nước tẩy trang Nivea MicellAir Skin Breathe'),
                                                                                                            (19, 17, '/images/products/makeup-remover/nuoc-tay-trang-la-roche-posay-micellar-water-ultra-17.jpg', 'IMAGE', '/images/products/makeup-remover/nuoc-tay-trang-la-roche-posay-micellar-water-ultra-17.jpg', 1, 'Nước tẩy trang La Roche-Posay Micellar Water Ultra'),
                                                                                                            (20, 18, '/images/products/makeup-remover/nuoc-tay-trang-eucerin-dermatoclean-micellar-water-18.jpg', 'IMAGE', '/images/products/makeup-remover/nuoc-tay-trang-eucerin-dermatoclean-micellar-water-18.jpg', 1, 'Nước tẩy trang Eucerin DermatoClean Micellar Water'),
                                                                                                            (21, 19, '/images/products/makeup-remover/nuoc-tay-trang-cetaphil-gentle-19.jpg', 'IMAGE', '/images/products/makeup-remover/nuoc-tay-trang-cetaphil-gentle-19.jpg', 1, 'Nước tẩy trang Cetaphil Gentle'),
                                                                                                            (22, 20, '/images/products/makeup-remover/nuoc-tay-trang-neutrogena-hydro-boost-micellar-20.jpg', 'IMAGE', '/images/products/makeup-remover/nuoc-tay-trang-neutrogena-hydro-boost-micellar-20.jpg', 1, 'Nước tẩy trang Neutrogena Hydro Boost Micellar'),
                                                                                                            (23, 21, '/images/products/makeup-remover/dau-tay-trang-hada-labo-gokujyun-cleansing-oil-21.jpg', 'IMAGE', '/images/products/makeup-remover/dau-tay-trang-hada-labo-gokujyun-cleansing-oil-21.jpg', 1, 'Dầu tẩy trang Hada Labo Gokujyun Cleansing Oil'),
                                                                                                            (24, 22, '/images/products/makeup-remover/dau-tay-trang-biore-perfect-cleansing-oil-22.jpg', 'IMAGE', '/images/products/makeup-remover/dau-tay-trang-biore-perfect-cleansing-oil-22.jpg', 1, 'Dầu tẩy trang Biore Perfect Cleansing Oil'),
                                                                                                            (25, 23, '/images/products/makeup-remover/nuoc-tay-trang-innisfree-green-tea-cleansing-water-23.jpg', 'IMAGE', '/images/products/makeup-remover/nuoc-tay-trang-innisfree-green-tea-cleansing-water-23.jpg', 1, 'Nước tẩy trang Innisfree Green Tea Cleansing Water'),
                                                                                                            (26, 24, '/images/products/makeup-remover/sap-tay-trang-heimish-all-clean-balm-24.jpg', 'IMAGE', '/images/products/makeup-remover/sap-tay-trang-heimish-all-clean-balm-24.jpg', 1, 'Sáp tẩy trang Heimish All Clean Balm'),
                                                                                                            (27, 25, '/images/products/makeup-remover/sap-tay-trang-banila-co-clean-it-zero-25.jpg', 'IMAGE', '/images/products/makeup-remover/sap-tay-trang-banila-co-clean-it-zero-25.jpg', 1, 'Sáp tẩy trang Banila Co Clean It Zero'),
                                                                                                            (28, 26, '/images/products/makeup-remover/dau-tay-trang-manyo-pure-cleansing-oil-26.jpg', 'IMAGE', '/images/products/makeup-remover/dau-tay-trang-manyo-pure-cleansing-oil-26.jpg', 1, 'Dầu tẩy trang Manyo Pure Cleansing Oil'),
                                                                                                            (29, 27, '/images/products/makeup-remover/dau-tay-trang-dhc-deep-cleansing-oil-27.jpg', 'IMAGE', '/images/products/makeup-remover/dau-tay-trang-dhc-deep-cleansing-oil-27.jpg', 1, 'Dầu tẩy trang DHC Deep Cleansing Oil'),
                                                                                                            (30, 28, '/images/products/makeup-remover/dau-tay-trang-kose-softymo-deep-cleansing-oil-28.jpg', 'IMAGE', '/images/products/makeup-remover/dau-tay-trang-kose-softymo-deep-cleansing-oil-28.jpg', 1, 'Dầu tẩy trang Kose Softymo Deep Cleansing Oil'),
                                                                                                            (31, 29, '/images/products/cleanser/sua-rua-mat-cerave-sa-smoothing-cleanser-29.jpg', 'IMAGE', '/images/products/cleanser/sua-rua-mat-cerave-sa-smoothing-cleanser-29.jpg', 1, 'Sữa rửa mặt CeraVe SA Smoothing Cleanser'),
                                                                                                            (32, 30, '/images/products/cleanser/sua-rua-mat-cetaphil-oily-skin-cleanser-30.jpg', 'IMAGE', '/images/products/cleanser/sua-rua-mat-cetaphil-oily-skin-cleanser-30.jpg', 1, 'Sữa rửa mặt Cetaphil Oily Skin Cleanser'),
                                                                                                            (33, 31, '/images/products/cleanser/sua-rua-mat-simple-refreshing-facial-wash-31.jpg', 'IMAGE', '/images/products/cleanser/sua-rua-mat-simple-refreshing-facial-wash-31.jpg', 1, 'Sữa rửa mặt Simple Refreshing Facial Wash'),
                                                                                                            (34, 32, '/images/products/cleanser/sua-rua-mat-simple-moisturising-facial-wash-32.jpg', 'IMAGE', '/images/products/cleanser/sua-rua-mat-simple-moisturising-facial-wash-32.jpg', 1, 'Sữa rửa mặt Simple Moisturising Facial Wash'),
                                                                                                            (35, 33, '/images/products/cleanser/sua-rua-mat-la-roche-posay-toleriane-dermo-cleanser-33.jpg', 'IMAGE', '/images/products/cleanser/sua-rua-mat-la-roche-posay-toleriane-dermo-cleanser-33.jpg', 1, 'Sữa rửa mặt La Roche-Posay Toleriane Dermo-Cleanser'),
                                                                                                            (36, 34, '/images/products/cleanser/gel-rua-mat-la-roche-posay-effaclar-purifying-foaming-gel-34.jpg', 'IMAGE', '/images/products/cleanser/gel-rua-mat-la-roche-posay-effaclar-purifying-foaming-gel-34.jpg', 1, 'Gel rửa mặt La Roche-Posay Effaclar Purifying Foaming Gel'),
                                                                                                            (37, 35, '/images/products/cleanser/sua-rua-mat-eucerin-ph5-facial-cleanser-35.jpg', 'IMAGE', '/images/products/cleanser/sua-rua-mat-eucerin-ph5-facial-cleanser-35.jpg', 1, 'Sữa rửa mặt Eucerin pH5 Facial Cleanser'),
                                                                                                            (38, 36, '/images/products/cleanser/gel-rua-mat-bioderma-sensibio-gel-moussant-36.jpg', 'IMAGE', '/images/products/cleanser/gel-rua-mat-bioderma-sensibio-gel-moussant-36.jpg', 1, 'Gel rửa mặt Bioderma Sensibio Gel Moussant'),
                                                                                                            (39, 37, '/images/products/cleanser/sua-rua-mat-garnier-pure-active-37.jpg', 'IMAGE', '/images/products/cleanser/sua-rua-mat-garnier-pure-active-37.jpg', 1, 'Sữa rửa mặt Garnier Pure Active'),
                                                                                                            (40, 38, '/images/products/cleanser/gel-rua-mat-neutrogena-hydro-boost-cleanser-38.jpg', 'IMAGE', '/images/products/cleanser/gel-rua-mat-neutrogena-hydro-boost-cleanser-38.jpg', 1, 'Gel rửa mặt Neutrogena Hydro Boost Cleanser'),
                                                                                                            (41, 39, '/images/products/cleanser/gel-rua-mat-cosrx-low-ph-good-morning-39.jpg', 'IMAGE', '/images/products/cleanser/gel-rua-mat-cosrx-low-ph-good-morning-39.jpg', 1, 'Gel rửa mặt Cosrx Low pH Good Morning'),
                                                                                                            (42, 40, '/images/products/cleanser/sua-rua-mat-some-by-mi-aha-bha-pha-30-days-miracle-foam-40.jpg', 'IMAGE', '/images/products/cleanser/sua-rua-mat-some-by-mi-aha-bha-pha-30-days-miracle-foam-40.jpg', 1, 'Sữa rửa mặt Some By Mi AHA-BHA-PHA 30 Days Miracle Foam'),
                                                                                                            (43, 41, '/images/products/cleanser/sua-rua-mat-innisfree-green-tea-foam-cleanser-41.jpg', 'IMAGE', '/images/products/cleanser/sua-rua-mat-innisfree-green-tea-foam-cleanser-41.jpg', 1, 'Sữa rửa mặt Innisfree Green Tea Foam Cleanser'),
                                                                                                            (44, 42, '/images/products/cleanser/sua-rua-mat-hada-labo-gokujyun-face-wash-42.jpg', 'IMAGE', '/images/products/cleanser/sua-rua-mat-hada-labo-gokujyun-face-wash-42.jpg', 1, 'Sữa rửa mặt Hada Labo Gokujyun Face Wash'),
                                                                                                            (45, 43, '/images/products/cleanser/sua-rua-mat-skin1004-madagascar-centella-ampoule-foam-43.jpg', 'IMAGE', '/images/products/cleanser/sua-rua-mat-skin1004-madagascar-centella-ampoule-foam-43.jpg', 1, 'Sữa rửa mặt Skin1004 Madagascar Centella Ampoule Foam'),
                                                                                                            (46, 44, '/images/products/cleanser/sua-rua-mat-paula-s-choice-calm-ultra-gentle-cleanser-44.jpg', 'IMAGE', '/images/products/cleanser/sua-rua-mat-paula-s-choice-calm-ultra-gentle-cleanser-44.jpg', 1, 'Sữa rửa mặt Paula''s Choice Calm Ultra-Gentle Cleanser'),
                                                                                                            (47, 45, '/images/products/cleanser/sua-rua-mat-l-oreal-paris-glycolic-bright-45.jpg', 'IMAGE', '/images/products/cleanser/sua-rua-mat-l-oreal-paris-glycolic-bright-45.jpg', 1, 'Sữa rửa mặt L''Oréal Paris Glycolic Bright'),
                                                                                                            (48, 46, '/images/products/cleanser/sua-rua-mat-nivea-extra-white-repair-protect-46.jpg', 'IMAGE', '/images/products/cleanser/sua-rua-mat-nivea-extra-white-repair-protect-46.jpg', 1, 'Sữa rửa mặt Nivea Extra White Repair & Protect'),
                                                                                                            (49, 47, '/images/products/cleanser/gel-rua-mat-cocoon-bi-dao-47.jpg', 'IMAGE', '/images/products/cleanser/gel-rua-mat-cocoon-bi-dao-47.jpg', 1, 'Gel rửa mặt Cocoon bí đao'),
                                                                                                            (50, 48, '/images/products/cleanser/sua-rua-mat-cerave-hydrating-facial-cleanser-48.jpg', 'IMAGE', '/images/products/cleanser/sua-rua-mat-cerave-hydrating-facial-cleanser-48.jpg', 1, 'Sữa rửa mặt CeraVe Hydrating Facial Cleanser'),
                                                                                                            (51, 49, '/images/products/serum/serum-the-ordinary-hyaluronic-acid-2-b5-49.jpg', 'IMAGE', '/images/products/serum/serum-the-ordinary-hyaluronic-acid-2-b5-49.jpg', 1, 'Serum The Ordinary Hyaluronic Acid 2% + B5'),
                                                                                                            (52, 50, '/images/products/serum/serum-the-ordinary-alpha-arbutin-2-ha-50.jpg', 'IMAGE', '/images/products/serum/serum-the-ordinary-alpha-arbutin-2-ha-50.jpg', 1, 'Serum The Ordinary Alpha Arbutin 2% + HA'),
                                                                                                            (53, 51, '/images/products/serum/serum-the-ordinary-retinol-0-5-in-squalane-51.jpg', 'IMAGE', '/images/products/serum/serum-the-ordinary-retinol-0-5-in-squalane-51.jpg', 1, 'Serum The Ordinary Retinol 0.5% in Squalane'),
                                                                                                            (54, 52, '/images/products/serum/serum-klairs-midnight-blue-youth-activating-drop-52.jpg', 'IMAGE', '/images/products/serum/serum-klairs-midnight-blue-youth-activating-drop-52.jpg', 1, 'Serum Klairs Midnight Blue Youth Activating Drop'),
                                                                                                            (55, 53, '/images/products/serum/serum-klairs-rich-moist-soothing-serum-53.jpg', 'IMAGE', '/images/products/serum/serum-klairs-rich-moist-soothing-serum-53.jpg', 1, 'Serum Klairs Rich Moist Soothing Serum'),
                                                                                                            (56, 54, '/images/products/serum/tinh-chat-skin1004-madagascar-centella-ampoule-54.jpg', 'IMAGE', '/images/products/serum/tinh-chat-skin1004-madagascar-centella-ampoule-54.jpg', 1, 'Tinh chất Skin1004 Madagascar Centella Ampoule'),
                                                                                                            (57, 55, '/images/products/serum/tinh-chat-cosrx-advanced-snail-96-mucin-power-essence-55.jpg', 'IMAGE', '/images/products/serum/tinh-chat-cosrx-advanced-snail-96-mucin-power-essence-55.jpg', 1, 'Tinh chất Cosrx Advanced Snail 96 Mucin Power Essence'),
                                                                                                            (58, 56, '/images/products/serum/serum-cosrx-the-vitamin-c-23-56.jpg', 'IMAGE', '/images/products/serum/serum-cosrx-the-vitamin-c-23-56.jpg', 1, 'Serum Cosrx The Vitamin C 23'),
                                                                                                            (59, 57, '/images/products/serum/serum-some-by-mi-aha-bha-pha-30-days-miracle-57.jpg', 'IMAGE', '/images/products/serum/serum-some-by-mi-aha-bha-pha-30-days-miracle-57.jpg', 1, 'Serum Some By Mi AHA-BHA-PHA 30 Days Miracle'),
                                                                                                            (60, 58, '/images/products/serum/serum-la-roche-posay-pure-niacinamide-10-58.jpg', 'IMAGE', '/images/products/serum/serum-la-roche-posay-pure-niacinamide-10-58.jpg', 1, 'Serum La Roche-Posay Pure Niacinamide 10'),
                                                                                                            (61, 59, '/images/products/serum/serum-la-roche-posay-hyalu-b5-59.jpg', 'IMAGE', '/images/products/serum/serum-la-roche-posay-hyalu-b5-59.jpg', 1, 'Serum La Roche-Posay Hyalu B5'),
                                                                                                            (62, 60, '/images/products/serum/serum-cerave-resurfacing-retinol-60.jpg', 'IMAGE', '/images/products/serum/serum-cerave-resurfacing-retinol-60.jpg', 1, 'Serum CeraVe Resurfacing Retinol'),
                                                                                                            (63, 61, '/images/products/serum/dung-dich-paula-s-choice-2-bha-liquid-exfoliant-61.jpg', 'IMAGE', '/images/products/serum/dung-dich-paula-s-choice-2-bha-liquid-exfoliant-61.jpg', 1, 'Dung dịch Paula''s Choice 2% BHA Liquid Exfoliant'),
                                                                                                            (64, 62, '/images/products/serum/serum-paula-s-choice-10-niacinamide-booster-62.jpg', 'IMAGE', '/images/products/serum/serum-paula-s-choice-10-niacinamide-booster-62.jpg', 1, 'Serum Paula''s Choice 10% Niacinamide Booster'),
                                                                                                            (65, 63, '/images/products/serum/serum-l-oreal-paris-glycolic-bright-63.jpg', 'IMAGE', '/images/products/serum/serum-l-oreal-paris-glycolic-bright-63.jpg', 1, 'Serum L''Oréal Paris Glycolic Bright'),
                                                                                                            (66, 64, '/images/products/serum/serum-garnier-vitamin-c-64.jpg', 'IMAGE', '/images/products/serum/serum-garnier-vitamin-c-64.jpg', 1, 'Serum Garnier Vitamin C'),
                                                                                                            (67, 65, '/images/products/serum/serum-innisfree-green-tea-seed-serum-65.jpg', 'IMAGE', '/images/products/serum/serum-innisfree-green-tea-seed-serum-65.jpg', 1, 'Serum Innisfree Green Tea Seed Serum'),
                                                                                                            (68, 66, '/images/products/serum/serum-laneige-water-bank-blue-hyaluronic-66.jpg', 'IMAGE', '/images/products/serum/serum-laneige-water-bank-blue-hyaluronic-66.jpg', 1, 'Serum Laneige Water Bank Blue Hyaluronic'),
                                                                                                            (69, 67, '/images/products/serum/serum-neutrogena-hydro-boost-hyaluronic-acid-67.jpg', 'IMAGE', '/images/products/serum/serum-neutrogena-hydro-boost-hyaluronic-acid-67.jpg', 1, 'Serum Neutrogena Hydro Boost Hyaluronic Acid'),
                                                                                                            (70, 68, '/images/products/serum/tinh-chat-cocoon-nghe-hung-yen-68.jpg', 'IMAGE', '/images/products/serum/tinh-chat-cocoon-nghe-hung-yen-68.jpg', 1, 'Tinh chất Cocoon nghệ Hưng Yên'),
                                                                                                            (71, 69, '/images/products/sunscreen/kem-chong-nang-anessa-perfect-uv-sunscreen-milk-69.jpg', 'IMAGE', '/images/products/sunscreen/kem-chong-nang-anessa-perfect-uv-sunscreen-milk-69.jpg', 1, 'Kem chống nắng Anessa Perfect UV Sunscreen Milk'),
                                                                                                            (72, 70, '/images/products/sunscreen/gel-chong-nang-anessa-perfect-uv-sunscreen-gel-70.jpg', 'IMAGE', '/images/products/sunscreen/gel-chong-nang-anessa-perfect-uv-sunscreen-gel-70.jpg', 1, 'Gel chống nắng Anessa Perfect UV Sunscreen Gel'),
                                                                                                            (73, 71, '/images/products/sunscreen/tinh-chat-chong-nang-biore-uv-aqua-rich-watery-essence-71.jpg', 'IMAGE', '/images/products/sunscreen/tinh-chat-chong-nang-biore-uv-aqua-rich-watery-essence-71.jpg', 1, 'Tinh chất chống nắng Biore UV Aqua Rich Watery Essence'),
                                                                                                            (74, 72, '/images/products/sunscreen/gel-chong-nang-biore-uv-aqua-rich-watery-gel-72.jpg', 'IMAGE', '/images/products/sunscreen/gel-chong-nang-biore-uv-aqua-rich-watery-gel-72.jpg', 1, 'Gel chống nắng Biore UV Aqua Rich Watery Gel'),
                                                                                                            (75, 73, '/images/products/sunscreen/tinh-chat-chong-nang-skin-aqua-tone-up-uv-essence-73.jpg', 'IMAGE', '/images/products/sunscreen/tinh-chat-chong-nang-skin-aqua-tone-up-uv-essence-73.jpg', 1, 'Tinh chất chống nắng Skin Aqua Tone Up UV Essence'),
                                                                                                            (76, 74, '/images/products/sunscreen/sua-chong-nang-skin-aqua-uv-super-moisture-milk-74.jpg', 'IMAGE', '/images/products/sunscreen/sua-chong-nang-skin-aqua-uv-super-moisture-milk-74.jpg', 1, 'Sữa chống nắng Skin Aqua UV Super Moisture Milk'),
                                                                                                            (77, 75, '/images/products/sunscreen/kem-chong-nang-la-roche-posay-anthelios-invisible-fluid-75.jpg', 'IMAGE', '/images/products/sunscreen/kem-chong-nang-la-roche-posay-anthelios-invisible-fluid-75.jpg', 1, 'Kem chống nắng La Roche-Posay Anthelios Invisible Fluid'),
                                                                                                            (78, 76, '/images/products/sunscreen/gel-chong-nang-eucerin-sun-oil-control-gel-creme-76.jpg', 'IMAGE', '/images/products/sunscreen/gel-chong-nang-eucerin-sun-oil-control-gel-creme-76.jpg', 1, 'Gel chống nắng Eucerin Sun Oil Control Gel-Creme'),
                                                                                                            (79, 77, '/images/products/sunscreen/kem-chong-nang-eucerin-sun-photoaging-control-fluid-77.jpg', 'IMAGE', '/images/products/sunscreen/kem-chong-nang-eucerin-sun-photoaging-control-fluid-77.jpg', 1, 'Kem chống nắng Eucerin Sun Photoaging Control Fluid'),
                                                                                                            (80, 78, '/images/products/sunscreen/kem-chong-nang-bioderma-photoderm-max-aquafluide-78.jpg', 'IMAGE', '/images/products/sunscreen/kem-chong-nang-bioderma-photoderm-max-aquafluide-78.jpg', 1, 'Kem chống nắng Bioderma Photoderm Max Aquafluide'),
                                                                                                            (81, 79, '/images/products/sunscreen/kem-chong-nang-neutrogena-ultra-sheer-dry-touch-spf50-79.jpg', 'IMAGE', '/images/products/sunscreen/kem-chong-nang-neutrogena-ultra-sheer-dry-touch-spf50-79.jpg', 1, 'Kem chống nắng Neutrogena Ultra Sheer Dry-Touch SPF50'),
                                                                                                            (82, 80, '/images/products/sunscreen/kem-chong-nang-neutrogena-hydro-boost-water-gel-lotion-spf50-80.jpg', 'IMAGE', '/images/products/sunscreen/kem-chong-nang-neutrogena-hydro-boost-water-gel-lotion-spf50-80.jpg', 1, 'Kem chống nắng Neutrogena Hydro Boost Water Gel Lotion SPF50'),
                                                                                                            (83, 81, '/images/products/sunscreen/kem-chong-nang-l-oreal-paris-uv-defender-serum-spf50-81.jpg', 'IMAGE', '/images/products/sunscreen/kem-chong-nang-l-oreal-paris-uv-defender-serum-spf50-81.jpg', 1, 'Kem chống nắng L''Oréal Paris UV Defender Serum SPF50'),
                                                                                                            (84, 82, '/images/products/sunscreen/kem-chong-nang-garnier-ambre-solaire-super-uv-serum-82.jpg', 'IMAGE', '/images/products/sunscreen/kem-chong-nang-garnier-ambre-solaire-super-uv-serum-82.jpg', 1, 'Kem chống nắng Garnier Ambre Solaire Super UV Serum'),
                                                                                                            (85, 83, '/images/products/sunscreen/kem-chong-nang-nivea-sun-uv-face-shine-control-spf50-83.jpg', 'IMAGE', '/images/products/sunscreen/kem-chong-nang-nivea-sun-uv-face-shine-control-spf50-83.jpg', 1, 'Kem chống nắng Nivea Sun UV Face Shine Control SPF50'),
                                                                                                            (86, 84, '/images/products/sunscreen/kem-chong-nang-innisfree-daily-uv-protection-cream-mild-84.jpg', 'IMAGE', '/images/products/sunscreen/kem-chong-nang-innisfree-daily-uv-protection-cream-mild-84.jpg', 1, 'Kem chống nắng Innisfree Daily UV Protection Cream Mild'),
                                                                                                            (87, 85, '/images/products/sunscreen/kem-chong-nang-cosrx-aloe-soothing-sun-cream-85.jpg', 'IMAGE', '/images/products/sunscreen/kem-chong-nang-cosrx-aloe-soothing-sun-cream-85.jpg', 1, 'Kem chống nắng Cosrx Aloe Soothing Sun Cream'),
                                                                                                            (88, 86, '/images/products/sunscreen/kem-chong-nang-some-by-mi-truecica-mineral-100-calming-suncream-86.jpg', 'IMAGE', '/images/products/sunscreen/kem-chong-nang-some-by-mi-truecica-mineral-100-calming-suncream-86.jpg', 1, 'Kem chống nắng Some By Mi Truecica Mineral 100 Calming Suncream'),
                                                                                                            (89, 87, '/images/products/sunscreen/kem-chong-nang-cocoon-bi-dao-87.jpg', 'IMAGE', '/images/products/sunscreen/kem-chong-nang-cocoon-bi-dao-87.jpg', 1, 'Kem chống nắng Cocoon bí đao'),
                                                                                                            (90, 88, '/images/products/sunscreen/kem-chong-nang-la-roche-posay-anthelios-uvmune-400-invisible-fluid-88.jpg', 'IMAGE', '/images/products/sunscreen/kem-chong-nang-la-roche-posay-anthelios-uvmune-400-invisible-fluid-88.jpg', 1, 'Kem chống nắng La Roche-Posay Anthelios UVMune 400 Invisible Fluid'),
                                                                                                            (91, 89, '/images/products/moisturizer/kem-duong-cerave-moisturising-lotion-89.jpg', 'IMAGE', '/images/products/moisturizer/kem-duong-cerave-moisturising-lotion-89.jpg', 1, 'Kem dưỡng CeraVe Moisturising Lotion'),
                                                                                                            (92, 90, '/images/products/moisturizer/kem-duong-cerave-pm-facial-moisturising-lotion-90.jpg', 'IMAGE', '/images/products/moisturizer/kem-duong-cerave-pm-facial-moisturising-lotion-90.jpg', 1, 'Kem dưỡng CeraVe PM Facial Moisturising Lotion'),
                                                                                                            (93, 91, '/images/products/moisturizer/lotion-duong-cetaphil-moisturizing-lotion-91.jpg', 'IMAGE', '/images/products/moisturizer/lotion-duong-cetaphil-moisturizing-lotion-91.jpg', 1, 'Lotion dưỡng Cetaphil Moisturizing Lotion'),
                                                                                                            (94, 92, '/images/products/moisturizer/kem-duong-cetaphil-moisturizing-cream-92.jpg', 'IMAGE', '/images/products/moisturizer/kem-duong-cetaphil-moisturizing-cream-92.jpg', 1, 'Kem dưỡng Cetaphil Moisturizing Cream'),
                                                                                                            (95, 93, '/images/products/moisturizer/kem-duong-la-roche-posay-toleriane-double-repair-face-moisturizer-93.jpg', 'IMAGE', '/images/products/moisturizer/kem-duong-la-roche-posay-toleriane-double-repair-face-moisturizer-93.jpg', 1, 'Kem dưỡng La Roche-Posay Toleriane Double Repair Face Moisturizer'),
                                                                                                            (96, 94, '/images/products/moisturizer/kem-duong-la-roche-posay-cicaplast-baume-b5-94.jpg', 'IMAGE', '/images/products/moisturizer/kem-duong-la-roche-posay-cicaplast-baume-b5-94.jpg', 1, 'Kem dưỡng La Roche-Posay Cicaplast Baume B5'),
                                                                                                            (97, 95, '/images/products/moisturizer/gel-duong-neutrogena-hydro-boost-water-gel-95.jpg', 'IMAGE', '/images/products/moisturizer/gel-duong-neutrogena-hydro-boost-water-gel-95.jpg', 1, 'Gel dưỡng Neutrogena Hydro Boost Water Gel'),
                                                                                                            (98, 96, '/images/products/moisturizer/gel-duong-neutrogena-hydro-boost-gel-cream-96.jpg', 'IMAGE', '/images/products/moisturizer/gel-duong-neutrogena-hydro-boost-gel-cream-96.jpg', 1, 'Gel dưỡng Neutrogena Hydro Boost Gel-Cream'),
                                                                                                            (99, 97, '/images/products/moisturizer/kem-duong-klairs-rich-moist-soothing-cream-97.jpg', 'IMAGE', '/images/products/moisturizer/kem-duong-klairs-rich-moist-soothing-cream-97.jpg', 1, 'Kem dưỡng Klairs Rich Moist Soothing Cream'),
                                                                                                            (100, 98, '/images/products/moisturizer/kem-duong-klairs-midnight-blue-calming-cream-98.jpg', 'IMAGE', '/images/products/moisturizer/kem-duong-klairs-midnight-blue-calming-cream-98.jpg', 1, 'Kem dưỡng Klairs Midnight Blue Calming Cream'),
                                                                                                            (101, 99, '/images/products/moisturizer/kem-duong-cosrx-advanced-snail-92-all-in-one-cream-99.jpg', 'IMAGE', '/images/products/moisturizer/kem-duong-cosrx-advanced-snail-92-all-in-one-cream-99.jpg', 1, 'Kem dưỡng Cosrx Advanced Snail 92 All in One Cream'),
                                                                                                            (102, 100, '/images/products/moisturizer/lotion-duong-cosrx-oil-free-ultra-moisturizing-lotion-100.jpg', 'IMAGE', '/images/products/moisturizer/lotion-duong-cosrx-oil-free-ultra-moisturizing-lotion-100.jpg', 1, 'Lotion dưỡng Cosrx Oil-Free Ultra-Moisturizing Lotion'),
                                                                                                            (103, 101, '/images/products/moisturizer/kem-duong-skin1004-madagascar-centella-soothing-cream-101.jpg', 'IMAGE', '/images/products/moisturizer/kem-duong-skin1004-madagascar-centella-soothing-cream-101.jpg', 1, 'Kem dưỡng Skin1004 Madagascar Centella Soothing Cream'),
                                                                                                            (104, 102, '/images/products/moisturizer/kem-duong-innisfree-green-tea-seed-cream-102.jpg', 'IMAGE', '/images/products/moisturizer/kem-duong-innisfree-green-tea-seed-cream-102.jpg', 1, 'Kem dưỡng Innisfree Green Tea Seed Cream'),
                                                                                                            (105, 103, '/images/products/moisturizer/kem-duong-laneige-water-bank-blue-hyaluronic-cream-103.jpg', 'IMAGE', '/images/products/moisturizer/kem-duong-laneige-water-bank-blue-hyaluronic-cream-103.jpg', 1, 'Kem dưỡng Laneige Water Bank Blue Hyaluronic Cream'),
                                                                                                            (106, 104, '/images/products/moisturizer/kem-duong-simple-replenishing-rich-moisturiser-104.jpg', 'IMAGE', '/images/products/moisturizer/kem-duong-simple-replenishing-rich-moisturiser-104.jpg', 1, 'Kem dưỡng Simple Replenishing Rich Moisturiser'),
                                                                                                            (107, 105, '/images/products/moisturizer/kem-duong-eucerin-atocontrol-face-care-cream-105.jpg', 'IMAGE', '/images/products/moisturizer/kem-duong-eucerin-atocontrol-face-care-cream-105.jpg', 1, 'Kem dưỡng Eucerin AtoControl Face Care Cream'),
                                                                                                            (108, 106, '/images/products/moisturizer/kem-duong-bioderma-atoderm-creme-106.jpg', 'IMAGE', '/images/products/moisturizer/kem-duong-bioderma-atoderm-creme-106.jpg', 1, 'Kem dưỡng Bioderma Atoderm Creme'),
                                                                                                            (109, 107, '/images/products/moisturizer/kem-duong-l-oreal-paris-revitalift-triple-action-day-cream-107.jpg', 'IMAGE', '/images/products/moisturizer/kem-duong-l-oreal-paris-revitalift-triple-action-day-cream-107.jpg', 1, 'Kem dưỡng L''Oréal Paris Revitalift Triple Action Day Cream'),
                                                                                                            (110, 108, '/images/products/moisturizer/kem-duong-cocoon-hoa-hong-108.jpg', 'IMAGE', '/images/products/moisturizer/kem-duong-cocoon-hoa-hong-108.jpg', 1, 'Kem dưỡng Cocoon hoa hồng'),
                                                                                                            (111, 109, '/images/products/toner/toner-klairs-supple-preparation-facial-toner-109.jpg', 'IMAGE', '/images/products/toner/toner-klairs-supple-preparation-facial-toner-109.jpg', 1, 'Toner Klairs Supple Preparation Facial Toner'),
                                                                                                            (112, 110, '/images/products/toner/toner-cosrx-aha-bha-clarifying-treatment-toner-110.jpg', 'IMAGE', '/images/products/toner/toner-cosrx-aha-bha-clarifying-treatment-toner-110.jpg', 1, 'Toner Cosrx AHA/BHA Clarifying Treatment Toner'),
                                                                                                            (113, 111, '/images/products/toner/toner-some-by-mi-aha-bha-pha-30-days-miracle-111.jpg', 'IMAGE', '/images/products/toner/toner-some-by-mi-aha-bha-pha-30-days-miracle-111.jpg', 1, 'Toner Some By Mi AHA-BHA-PHA 30 Days Miracle'),
                                                                                                            (114, 112, '/images/products/toner/toner-innisfree-green-tea-balancing-skin-112.jpg', 'IMAGE', '/images/products/toner/toner-innisfree-green-tea-balancing-skin-112.jpg', 1, 'Toner Innisfree Green Tea Balancing Skin'),
                                                                                                            (115, 113, '/images/products/toner/toner-laneige-cream-skin-refiner-113.jpg', 'IMAGE', '/images/products/toner/toner-laneige-cream-skin-refiner-113.jpg', 1, 'Toner Laneige Cream Skin Refiner'),
                                                                                                            (116, 114, '/images/products/toner/lotion-hada-labo-gokujyun-super-hyaluronic-acid-114.jpg', 'IMAGE', '/images/products/toner/lotion-hada-labo-gokujyun-super-hyaluronic-acid-114.jpg', 1, 'Lotion Hada Labo Gokujyun Super Hyaluronic Acid'),
                                                                                                            (117, 115, '/images/products/toner/toner-bioderma-sensibio-tonique-115.jpg', 'IMAGE', '/images/products/toner/toner-bioderma-sensibio-tonique-115.jpg', 1, 'Toner Bioderma Sensibio Tonique'),
                                                                                                            (118, 116, '/images/products/toner/toner-simple-kind-to-skin-soothing-facial-toner-116.jpg', 'IMAGE', '/images/products/toner/toner-simple-kind-to-skin-soothing-facial-toner-116.jpg', 1, 'Toner Simple Kind to Skin Soothing Facial Toner'),
                                                                                                            (119, 117, '/images/products/toner/toner-garnier-micellar-rose-water-117.jpg', 'IMAGE', '/images/products/toner/toner-garnier-micellar-rose-water-117.jpg', 1, 'Toner Garnier Micellar Rose Water'),
                                                                                                            (120, 118, '/images/products/toner/xit-khoang-la-roche-posay-serozinc-118.jpg', 'IMAGE', '/images/products/toner/xit-khoang-la-roche-posay-serozinc-118.jpg', 1, 'Xịt khoáng La Roche-Posay Serozinc'),
                                                                                                            (121, 119, '/images/products/lipstick/son-mac-matte-lipstick-velvet-teddy-119.jpg', 'IMAGE', '/images/products/lipstick/son-mac-matte-lipstick-velvet-teddy-119.jpg', 1, 'Son MAC Matte Lipstick Velvet Teddy'),
                                                                                                            (122, 120, '/images/products/lipstick/son-maybelline-color-sensational-creamy-matte-120.jpg', 'IMAGE', '/images/products/lipstick/son-maybelline-color-sensational-creamy-matte-120.jpg', 1, 'Son Maybelline Color Sensational Creamy Matte'),
                                                                                                            (123, 121, '/images/products/lipstick/son-l-oreal-paris-color-riche-matte-121.jpg', 'IMAGE', '/images/products/lipstick/son-l-oreal-paris-color-riche-matte-121.jpg', 1, 'Son L''Oréal Paris Color Riche Matte'),
                                                                                                            (124, 122, '/images/products/lipstick/son-3ce-velvet-lip-tint-122.jpg', 'IMAGE', '/images/products/lipstick/son-3ce-velvet-lip-tint-122.jpg', 1, 'Son 3CE Velvet Lip Tint'),
                                                                                                            (125, 123, '/images/products/lipstick/son-romand-juicy-lasting-tint-123.jpg', 'IMAGE', '/images/products/lipstick/son-romand-juicy-lasting-tint-123.jpg', 1, 'Son Romand Juicy Lasting Tint'),
                                                                                                            (126, 124, '/images/products/lipstick/son-black-rouge-air-fit-velvet-tint-124.jpg', 'IMAGE', '/images/products/lipstick/son-black-rouge-air-fit-velvet-tint-124.jpg', 1, 'Son Black Rouge Air Fit Velvet Tint'),
                                                                                                            (127, 125, '/images/products/lipstick/son-bbia-last-velvet-lip-tint-125.jpg', 'IMAGE', '/images/products/lipstick/son-bbia-last-velvet-lip-tint-125.jpg', 1, 'Son Bbia Last Velvet Lip Tint'),
                                                                                                            (128, 126, '/images/products/lipstick/son-peripera-ink-velvet-126.jpg', 'IMAGE', '/images/products/lipstick/son-peripera-ink-velvet-126.jpg', 1, 'Son Peripera Ink Velvet'),
                                                                                                            (129, 127, '/images/products/lipstick/son-merzy-the-first-velvet-tint-127.jpg', 'IMAGE', '/images/products/lipstick/son-merzy-the-first-velvet-tint-127.jpg', 1, 'Son Merzy The First Velvet Tint'),
                                                                                                            (130, 128, '/images/products/lipstick/son-ofelia-uncovered-lip-mousse-128.jpg', 'IMAGE', '/images/products/lipstick/son-ofelia-uncovered-lip-mousse-128.jpg', 1, 'Son Ofelia Uncovered Lip Mousse');

-- 7. ProductVariant (131 variants)
INSERT INTO ProductVariant (variantID, productID, sku, barcode, size, shadeName, hexCode, price, salePrice, stockQuantity, ingredients, suitableSkinType, imgUrl, isActive) VALUES
                                                                                                                                                                                (1, 1, 'COC-TT-500', '8936182500123', '500ml', NULL, NULL, 298000.00, 245000.00, 150, 'Bí đao, rau má, tinh dầu tràm trà', 'OILY,COMBINATION', '/images/products/makeup-remover/nuoc-tay-trang-cocoon-bi-dao-1.jpg', TRUE),
                                                                                                                                                                                (2, 1, 'COC-TT-140', '8936182500130', '140ml', NULL, NULL, 119000.00, NULL, 200, 'Bí đao, rau má, tinh dầu tràm trà', 'OILY,COMBINATION', 'https://example.com/images/cocoon-140ml.jpg', TRUE),
                                                                                                                                                                                (3, 2, 'CER-SRM-473', '3337875597357', '473ml', NULL, NULL, 450000.00, 399000.00, 80, 'Ceramides, Niacinamide, Hyaluronic Acid', 'OILY,COMBINATION,NORMAL', '/images/products/cleanser/sua-rua-mat-cerave-sach-sau-2.jpg', TRUE),
                                                                                                                                                                                (4, 2, 'CER-SRM-236', '3337875597340', '236ml', NULL, NULL, 280000.00, NULL, 120, 'Ceramides, Niacinamide, Hyaluronic Acid', 'OILY,COMBINATION,NORMAL', 'https://example.com/images/cerave-236ml.jpg', TRUE),
                                                                                                                                                                                (5, 3, 'CET-SRM-500', '3499320002116', '500ml', NULL, NULL, 215000.00, 189000.00, 90, 'Glycerin, Panthenol, Niacinamide', 'DRY,SENSITIVE,NORMAL', '/images/products/cleanser/sua-rua-mat-cetaphil-diu-nhe-3.jpg', TRUE),
                                                                                                                                                                                (6, 4, 'ORD-NIA-30', '769915190304', '30ml', NULL, NULL, 190000.00, 165000.00, 60, 'Niacinamide 10%, Zinc PCA 1%', 'OILY,COMBINATION', '/images/products/serum/serum-the-ordinary-niacinamide-4.jpg', TRUE),
                                                                                                                                                                                (7, 5, 'KLA-VTC-35', '8809572890011', '35ml', NULL, NULL, 360000.00, 320000.00, 45, 'Ethyl Ascorbic Acid, Hyaluronic Acid, Panthenol', 'NORMAL,DRY,SENSITIVE', '/images/products/serum/serum-klairs-vitamin-c-5.jpg', TRUE),
                                                                                                                                                                                (8, 6, 'LRP-ANT-50', '3337875546324', '50ml', NULL, NULL, 520000.00, 468000.00, 70, 'Mexoryl 400, Mexoryl XL, Glycerin', 'OILY,COMBINATION,SENSITIVE', '/images/products/sunscreen/kem-chong-nang-la-roche-posay-anthelios-6.jpg', TRUE),
                                                                                                                                                                                (9, 7, 'SKA-UV-80', '4901417695004', '80g', NULL, NULL, 175000.00, 155000.00, 100, 'Hyaluronic Acid, Collagen, Royal Jelly', 'NORMAL,DRY,COMBINATION', '/images/products/sunscreen/gel-chong-nang-skin-aqua-uv-7.jpg', TRUE),
                                                                                                                                                                                (10, 8, 'CER-MOI-340', '3337875597388', '340g', NULL, NULL, 380000.00, 342000.00, 55, '3 Ceramides, Hyaluronic Acid, MVE Technology', 'DRY,SENSITIVE,NORMAL', '/images/products/moisturizer/kem-duong-am-cerave-moisturising-8.jpg', TRUE),
                                                                                                                                                                                (11, 8, 'CER-MOI-85', '3337875597395', '85g', NULL, NULL, 190000.00, NULL, 0, '3 Ceramides, Hyaluronic Acid, MVE Technology', 'DRY,SENSITIVE,NORMAL', 'https://example.com/images/cerave-moisturising-85.jpg', TRUE),
                                                                                                                                                                                (12, 9, 'LUM-TT-009', NULL, '500ml', NULL, NULL, 349000.00, 307000.00, 93, 'Micellar, Glycerin, Panthenol', 'OILY,COMBINATION', '/images/products/makeup-remover/nuoc-tay-trang-bioderma-sensibio-h2o-9.jpg', TRUE),
                                                                                                                                                                                (13, 10, 'LUM-TT-010', NULL, '400ml', NULL, NULL, 269000.00, NULL, 130, 'Micellar, Glycerin, Niacinamide', 'DRY,SENSITIVE,NORMAL', '/images/products/makeup-remover/nuoc-tay-trang-garnier-micellar-hong-10.jpg', TRUE),
                                                                                                                                                                                (14, 11, 'LUM-TT-011', NULL, '400ml', NULL, NULL, 279000.00, NULL, 167, 'Micellar, Zinc, Niacinamide', 'NORMAL', '/images/products/makeup-remover/nuoc-tay-trang-garnier-micellar-xanh-11.jpg', TRUE),
                                                                                                                                                                                (15, 12, 'LUM-TT-012', NULL, '400ml', NULL, NULL, 259000.00, 228000.00, 64, 'Micellar, Glycerin, Panthenol', 'OILY', '/images/products/makeup-remover/nuoc-tay-trang-l-oreal-paris-micellar-3-in-1-12.jpg', TRUE),
                                                                                                                                                                                (16, 13, 'LUM-TT-013', NULL, '500ml', NULL, NULL, 289000.00, NULL, 101, 'Hoa hồng, Glycerin, Panthenol', 'SENSITIVE', '/images/products/makeup-remover/nuoc-tay-trang-cocoon-hoa-hong-13.jpg', TRUE),
                                                                                                                                                                                (17, 14, 'LUM-TT-014', NULL, '140ml', NULL, NULL, 219000.00, NULL, 138, 'Hoa hồng, Dầu thực vật, Vitamin E', 'COMBINATION,NORMAL', '/images/products/makeup-remover/dau-tay-trang-cocoon-hoa-hong-14.jpg', TRUE),
                                                                                                                                                                                (18, 15, 'LUM-TT-015', NULL, '400ml', NULL, NULL, 229000.00, 202000.00, 175, 'Micellar, Glycerin, Panthenol', 'DRY', '/images/products/makeup-remover/nuoc-tay-trang-simple-kind-to-skin-micellar-15.jpg', TRUE),
                                                                                                                                                                                (19, 16, 'LUM-TT-016', NULL, '400ml', NULL, NULL, 199000.00, NULL, 72, 'Micellar, Glycerin, Panthenol', 'OILY,COMBINATION,SENSITIVE', '/images/products/makeup-remover/nuoc-tay-trang-nivea-micellair-skin-breathe-16.jpg', TRUE),
                                                                                                                                                                                (20, 17, 'LUM-TT-017', NULL, '400ml', NULL, NULL, 389000.00, NULL, 109, 'Micellar, Glycerin, Panthenol', 'NORMAL,DRY', '/images/products/makeup-remover/nuoc-tay-trang-la-roche-posay-micellar-water-ultra-17.jpg', TRUE),
                                                                                                                                                                                (21, 18, 'LUM-TT-018', NULL, '400ml', NULL, NULL, 359000.00, 316000.00, 146, 'Micellar, Glycerin, Hyaluronic Acid', 'OILY,DRY,COMBINATION,NORMAL,SENSITIVE', '/images/products/makeup-remover/nuoc-tay-trang-eucerin-dermatoclean-micellar-water-18.jpg', TRUE),
                                                                                                                                                                                (22, 19, 'LUM-TT-019', NULL, '400ml', NULL, NULL, 329000.00, NULL, 43, 'Glycerin, Panthenol, Niacinamide', 'COMBINATION', '/images/products/makeup-remover/nuoc-tay-trang-cetaphil-gentle-19.jpg', TRUE),
                                                                                                                                                                                (23, 20, 'LUM-TT-020', NULL, '400ml', NULL, NULL, 299000.00, NULL, 80, 'Micellar, Hyaluronic Acid, Glycerin', 'DRY,SENSITIVE', '/images/products/makeup-remover/nuoc-tay-trang-neutrogena-hydro-boost-micellar-20.jpg', TRUE),
                                                                                                                                                                                (24, 21, 'LUM-TT-021', NULL, '200ml', NULL, NULL, 319000.00, 281000.00, 117, 'Hyaluronic Acid, Dầu thực vật, Glycerin', 'OILY,NORMAL', '/images/products/makeup-remover/dau-tay-trang-hada-labo-gokujyun-cleansing-oil-21.jpg', TRUE),
                                                                                                                                                                                (25, 22, 'LUM-TT-022', NULL, '230ml', NULL, NULL, 289000.00, NULL, 154, 'Dầu thực vật, Vitamin E, Glycerin', 'SENSITIVE,NORMAL', '/images/products/makeup-remover/dau-tay-trang-biore-perfect-cleansing-oil-22.jpg', TRUE),
                                                                                                                                                                                (26, 23, 'LUM-TT-023', NULL, '300ml', NULL, NULL, 279000.00, NULL, 51, 'Trà xanh, Glycerin, Panthenol', 'OILY,COMBINATION,NORMAL', '/images/products/makeup-remover/nuoc-tay-trang-innisfree-green-tea-cleansing-water-23.jpg', TRUE),
                                                                                                                                                                                (27, 24, 'LUM-TT-024', NULL, '120ml', NULL, NULL, 399000.00, 351000.00, 88, 'Dầu thực vật, Vitamin E, Bơ hạt mỡ', 'DRY,NORMAL', '/images/products/makeup-remover/sap-tay-trang-heimish-all-clean-balm-24.jpg', TRUE),
                                                                                                                                                                                (28, 25, 'LUM-TT-025', NULL, '100ml', NULL, NULL, 389000.00, NULL, 125, 'Dầu thực vật, Vitamin E, Glycerin', 'COMBINATION,SENSITIVE', '/images/products/makeup-remover/sap-tay-trang-banila-co-clean-it-zero-25.jpg', TRUE),
                                                                                                                                                                                (29, 26, 'LUM-TT-026', NULL, '200ml', NULL, NULL, 419000.00, NULL, 162, 'Dầu thực vật, Vitamin E, Panthenol', 'OILY,SENSITIVE', '/images/products/makeup-remover/dau-tay-trang-manyo-pure-cleansing-oil-26.jpg', TRUE),
                                                                                                                                                                                (30, 27, 'LUM-TT-027', NULL, '200ml', NULL, NULL, 439000.00, 386000.00, 59, 'Dầu ô liu, Vitamin E, Glycerin', 'NORMAL,SENSITIVE', '/images/products/makeup-remover/dau-tay-trang-dhc-deep-cleansing-oil-27.jpg', TRUE),
                                                                                                                                                                                (31, 28, 'LUM-TT-028', NULL, '230ml', NULL, NULL, 249000.00, NULL, 96, 'Dầu thực vật, Vitamin E, Glycerin', 'DRY,COMBINATION', '/images/products/makeup-remover/dau-tay-trang-kose-softymo-deep-cleansing-oil-28.jpg', TRUE),
                                                                                                                                                                                (32, 29, 'LUM-SRM-029', NULL, '236ml', NULL, NULL, 339000.00, 298000.00, 133, 'Salicylic Acid, Ceramides, Hyaluronic Acid', 'OILY,COMBINATION', '/images/products/cleanser/sua-rua-mat-cerave-sa-smoothing-cleanser-29.jpg', TRUE),
                                                                                                                                                                                (33, 30, 'LUM-SRM-030', NULL, '236ml', NULL, NULL, 299000.00, NULL, 170, 'Glycerin, Niacinamide, Panthenol', 'DRY,SENSITIVE,NORMAL', '/images/products/cleanser/sua-rua-mat-cetaphil-oily-skin-cleanser-30.jpg', TRUE),
                                                                                                                                                                                (34, 31, 'LUM-SRM-031', NULL, '150ml', NULL, NULL, 179000.00, NULL, 67, 'Glycerin, Panthenol, Vitamin E', 'NORMAL', '/images/products/cleanser/sua-rua-mat-simple-refreshing-facial-wash-31.jpg', TRUE),
                                                                                                                                                                                (35, 32, 'LUM-SRM-032', NULL, '150ml', NULL, NULL, 189000.00, 166000.00, 104, 'Glycerin, Panthenol, Vitamin E', 'OILY', '/images/products/cleanser/sua-rua-mat-simple-moisturising-facial-wash-32.jpg', TRUE),
                                                                                                                                                                                (36, 33, 'LUM-SRM-033', NULL, '200ml', NULL, NULL, 389000.00, NULL, 141, 'Glycerin, Panthenol, Niacinamide', 'SENSITIVE', '/images/products/cleanser/sua-rua-mat-la-roche-posay-toleriane-dermo-cleanser-33.jpg', TRUE),
                                                                                                                                                                                (37, 34, 'LUM-SRM-034', NULL, '200ml', NULL, NULL, 369000.00, NULL, 178, 'Zinc, Niacinamide, Glycerin', 'COMBINATION,NORMAL', '/images/products/cleanser/gel-rua-mat-la-roche-posay-effaclar-purifying-foaming-gel-34.jpg', TRUE),
                                                                                                                                                                                (38, 35, 'LUM-SRM-035', NULL, '200ml', NULL, NULL, 339000.00, 298000.00, 75, 'Glycerin, Panthenol, Dexpanthenol', 'DRY', '/images/products/cleanser/sua-rua-mat-eucerin-ph5-facial-cleanser-35.jpg', TRUE),
                                                                                                                                                                                (39, 36, 'LUM-SRM-036', NULL, '200ml', NULL, NULL, 359000.00, NULL, 112, 'Glycerin, Panthenol, Micellar', 'OILY,COMBINATION,SENSITIVE', '/images/products/cleanser/gel-rua-mat-bioderma-sensibio-gel-moussant-36.jpg', TRUE),
                                                                                                                                                                                (40, 37, 'LUM-SRM-037', NULL, '150ml', NULL, NULL, 169000.00, NULL, 149, 'Salicylic Acid, Zinc, Niacinamide', 'NORMAL,DRY', '/images/products/cleanser/sua-rua-mat-garnier-pure-active-37.jpg', TRUE),
                                                                                                                                                                                (41, 38, 'LUM-SRM-038', NULL, '200ml', NULL, NULL, 289000.00, 254000.00, 46, 'Hyaluronic Acid, Glycerin, Panthenol', 'OILY,DRY,COMBINATION,NORMAL,SENSITIVE', '/images/products/cleanser/gel-rua-mat-neutrogena-hydro-boost-cleanser-38.jpg', TRUE),
                                                                                                                                                                                (42, 39, 'LUM-SRM-039', NULL, '150ml', NULL, NULL, 249000.00, NULL, 83, 'Tràm trà, Glycerin, Panthenol', 'COMBINATION', '/images/products/cleanser/gel-rua-mat-cosrx-low-ph-good-morning-39.jpg', TRUE),
                                                                                                                                                                                (43, 40, 'LUM-SRM-040', NULL, '100ml', NULL, NULL, 259000.00, NULL, 120, 'Tràm trà, Salicylic Acid, Niacinamide', 'DRY,SENSITIVE', '/images/products/cleanser/sua-rua-mat-some-by-mi-aha-bha-pha-30-days-miracle-foam-40.jpg', TRUE),
                                                                                                                                                                                (44, 41, 'LUM-SRM-041', NULL, '150ml', NULL, NULL, 229000.00, 202000.00, 157, 'Trà xanh, Glycerin, Panthenol', 'OILY,NORMAL', '/images/products/cleanser/sua-rua-mat-innisfree-green-tea-foam-cleanser-41.jpg', TRUE),
                                                                                                                                                                                (45, 42, 'LUM-SRM-042', NULL, '100g', NULL, NULL, 209000.00, NULL, 54, 'Hyaluronic Acid, Glycerin, Panthenol', 'SENSITIVE,NORMAL', '/images/products/cleanser/sua-rua-mat-hada-labo-gokujyun-face-wash-42.jpg', TRUE),
                                                                                                                                                                                (46, 43, 'LUM-SRM-043', NULL, '125ml', NULL, NULL, 269000.00, NULL, 91, 'Rau má, Panthenol, Glycerin', 'OILY,COMBINATION,NORMAL', '/images/products/cleanser/sua-rua-mat-skin1004-madagascar-centella-ampoule-foam-43.jpg', TRUE),
                                                                                                                                                                                (47, 44, 'LUM-SRM-044', NULL, '198ml', NULL, NULL, 519000.00, 457000.00, 128, 'Glycerin, Panthenol, Niacinamide', 'DRY,NORMAL', '/images/products/cleanser/sua-rua-mat-paula-s-choice-calm-ultra-gentle-cleanser-44.jpg', TRUE),
                                                                                                                                                                                (48, 45, 'LUM-SRM-045', NULL, '100ml', NULL, NULL, 199000.00, NULL, 165, 'Glycolic Acid, Niacinamide, Vitamin C', 'COMBINATION,SENSITIVE', '/images/products/cleanser/sua-rua-mat-l-oreal-paris-glycolic-bright-45.jpg', TRUE),
                                                                                                                                                                                (49, 46, 'LUM-SRM-046', NULL, '100g', NULL, NULL, 139000.00, NULL, 62, 'Vitamin C, Glycerin, Niacinamide', 'OILY,SENSITIVE', '/images/products/cleanser/sua-rua-mat-nivea-extra-white-repair-protect-46.jpg', TRUE),
                                                                                                                                                                                (50, 47, 'LUM-SRM-047', NULL, '140ml', NULL, NULL, 189000.00, 166000.00, 99, 'Bí đao, Rau má, Tràm trà', 'NORMAL,SENSITIVE', '/images/products/cleanser/gel-rua-mat-cocoon-bi-dao-47.jpg', TRUE),
                                                                                                                                                                                (51, 48, 'LUM-SRM-048', NULL, '236ml', NULL, NULL, 329000.00, NULL, 136, 'Ceramides, Hyaluronic Acid, Glycerin', 'DRY,COMBINATION', '/images/products/cleanser/sua-rua-mat-cerave-hydrating-facial-cleanser-48.jpg', TRUE),
                                                                                                                                                                                (52, 49, 'LUM-SER-049', NULL, '30ml', NULL, NULL, 269000.00, 237000.00, 173, 'Hyaluronic Acid, Panthenol, Glycerin', 'OILY,COMBINATION', '/images/products/serum/serum-the-ordinary-hyaluronic-acid-2-b5-49.jpg', TRUE),
                                                                                                                                                                                (53, 50, 'LUM-SER-050', NULL, '30ml', NULL, NULL, 289000.00, NULL, 70, 'Alpha Arbutin, Hyaluronic Acid, Vitamin C', 'DRY,SENSITIVE,NORMAL', '/images/products/serum/serum-the-ordinary-alpha-arbutin-2-ha-50.jpg', TRUE),
                                                                                                                                                                                (54, 51, 'LUM-SER-051', NULL, '30ml', NULL, NULL, 259000.00, NULL, 107, 'Retinol, Squalane, Vitamin E', 'NORMAL', '/images/products/serum/serum-the-ordinary-retinol-0-5-in-squalane-51.jpg', TRUE),
                                                                                                                                                                                (55, 52, 'LUM-SER-052', NULL, '20ml', NULL, NULL, 419000.00, 369000.00, 144, 'Peptide, Panthenol, Glycerin', 'OILY', '/images/products/serum/serum-klairs-midnight-blue-youth-activating-drop-52.jpg', TRUE),
                                                                                                                                                                                (56, 53, 'LUM-SER-053', NULL, '80ml', NULL, NULL, 379000.00, NULL, 41, 'Hyaluronic Acid, Panthenol, Glycerin', 'SENSITIVE', '/images/products/serum/serum-klairs-rich-moist-soothing-serum-53.jpg', TRUE),
                                                                                                                                                                                (57, 54, 'LUM-SER-054', NULL, '55ml', NULL, NULL, 339000.00, NULL, 78, 'Rau má, Panthenol, Glycerin', 'COMBINATION,NORMAL', '/images/products/serum/tinh-chat-skin1004-madagascar-centella-ampoule-54.jpg', TRUE),
                                                                                                                                                                                (58, 55, 'LUM-SER-055', NULL, '100ml', NULL, NULL, 359000.00, 316000.00, 115, 'Ốc sên, Hyaluronic Acid, Panthenol', 'DRY', '/images/products/serum/tinh-chat-cosrx-advanced-snail-96-mucin-power-essence-55.jpg', TRUE),
                                                                                                                                                                                (59, 56, 'LUM-SER-056', NULL, '20ml', NULL, NULL, 389000.00, NULL, 152, 'Vitamin C, Ascorbic Acid, Panthenol', 'OILY,COMBINATION,SENSITIVE', '/images/products/serum/serum-cosrx-the-vitamin-c-23-56.jpg', TRUE),
                                                                                                                                                                                (60, 57, 'LUM-SER-057', NULL, '50ml', NULL, NULL, 349000.00, NULL, 49, 'Tràm trà, Salicylic Acid, Niacinamide', 'NORMAL,DRY', '/images/products/serum/serum-some-by-mi-aha-bha-pha-30-days-miracle-57.jpg', TRUE),
                                                                                                                                                                                (61, 58, 'LUM-SER-058', NULL, '30ml', NULL, NULL, 749000.00, 659000.00, 86, 'Niacinamide, Panthenol, Glycerin', 'OILY,DRY,COMBINATION,NORMAL,SENSITIVE', '/images/products/serum/serum-la-roche-posay-pure-niacinamide-10-58.jpg', TRUE),
                                                                                                                                                                                (62, 59, 'LUM-SER-059', NULL, '30ml', NULL, NULL, 949000.00, NULL, 123, 'Hyaluronic Acid, Panthenol, Vitamin B5', 'COMBINATION', '/images/products/serum/serum-la-roche-posay-hyalu-b5-59.jpg', TRUE),
                                                                                                                                                                                (63, 60, 'LUM-SER-060', NULL, '30ml', NULL, NULL, 519000.00, NULL, 160, 'Retinol, Ceramides, Niacinamide', 'DRY,SENSITIVE', '/images/products/serum/serum-cerave-resurfacing-retinol-60.jpg', TRUE),
                                                                                                                                                                                (64, 61, 'LUM-SER-061', NULL, '118ml', NULL, NULL, 699000.00, 615000.00, 57, 'Salicylic Acid, Niacinamide, Panthenol', 'OILY,NORMAL', '/images/products/serum/dung-dich-paula-s-choice-2-bha-liquid-exfoliant-61.jpg', TRUE),
                                                                                                                                                                                (65, 62, 'LUM-SER-062', NULL, '20ml', NULL, NULL, 799000.00, NULL, 94, 'Niacinamide, Panthenol, Vitamin C', 'SENSITIVE,NORMAL', '/images/products/serum/serum-paula-s-choice-10-niacinamide-booster-62.jpg', TRUE),
                                                                                                                                                                                (66, 63, 'LUM-SER-063', NULL, '30ml', NULL, NULL, 499000.00, NULL, 131, 'Glycolic Acid, Niacinamide, Vitamin C', 'OILY,COMBINATION,NORMAL', '/images/products/serum/serum-l-oreal-paris-glycolic-bright-63.jpg', TRUE),
                                                                                                                                                                                (67, 64, 'LUM-SER-064', NULL, '30ml', NULL, NULL, 329000.00, 290000.00, 168, 'Vitamin C, Niacinamide, Salicylic Acid', 'DRY,NORMAL', '/images/products/serum/serum-garnier-vitamin-c-64.jpg', TRUE),
                                                                                                                                                                                (68, 65, 'LUM-SER-065', NULL, '80ml', NULL, NULL, 459000.00, NULL, 65, 'Trà xanh, Hyaluronic Acid, Panthenol', 'COMBINATION,SENSITIVE', '/images/products/serum/serum-innisfree-green-tea-seed-serum-65.jpg', TRUE),
                                                                                                                                                                                (69, 66, 'LUM-SER-066', NULL, '50ml', NULL, NULL, 899000.00, NULL, 102, 'Hyaluronic Acid, Panthenol, Glycerin', 'OILY,SENSITIVE', '/images/products/serum/serum-laneige-water-bank-blue-hyaluronic-66.jpg', TRUE),
                                                                                                                                                                                (70, 67, 'LUM-SER-067', NULL, '30ml', NULL, NULL, 429000.00, 378000.00, 139, 'Hyaluronic Acid, Glycerin, Panthenol', 'NORMAL,SENSITIVE', '/images/products/serum/serum-neutrogena-hydro-boost-hyaluronic-acid-67.jpg', TRUE),
                                                                                                                                                                                (71, 68, 'LUM-SER-068', NULL, '30ml', NULL, NULL, 319000.00, NULL, 176, 'Nghệ, Vitamin C, Niacinamide', 'DRY,COMBINATION', '/images/products/serum/tinh-chat-cocoon-nghe-hung-yen-68.jpg', TRUE),
                                                                                                                                                                                (72, 69, 'LUM-CN-069', NULL, '60ml', NULL, NULL, 549000.00, 483000.00, 73, 'Zinc, Hyaluronic Acid, Glycerin', 'OILY,COMBINATION', '/images/products/sunscreen/kem-chong-nang-anessa-perfect-uv-sunscreen-milk-69.jpg', TRUE),
                                                                                                                                                                                (73, 70, 'LUM-CN-070', NULL, '90g', NULL, NULL, 499000.00, NULL, 110, 'Hyaluronic Acid, Glycerin, Panthenol', 'DRY,SENSITIVE,NORMAL', '/images/products/sunscreen/gel-chong-nang-anessa-perfect-uv-sunscreen-gel-70.jpg', TRUE),
                                                                                                                                                                                (74, 71, 'LUM-CN-071', NULL, '70g', NULL, NULL, 359000.00, NULL, 147, 'Hyaluronic Acid, Glycerin, Vitamin E', 'NORMAL', '/images/products/sunscreen/tinh-chat-chong-nang-biore-uv-aqua-rich-watery-essence-71.jpg', TRUE),
                                                                                                                                                                                (75, 72, 'LUM-CN-072', NULL, '90ml', NULL, NULL, 329000.00, 290000.00, 44, 'Hyaluronic Acid, Glycerin, Panthenol', 'OILY', '/images/products/sunscreen/gel-chong-nang-biore-uv-aqua-rich-watery-gel-72.jpg', TRUE),
                                                                                                                                                                                (76, 73, 'LUM-CN-073', NULL, '80g', NULL, NULL, 279000.00, NULL, 81, 'Hyaluronic Acid, Vitamin C, Glycerin', 'SENSITIVE', '/images/products/sunscreen/tinh-chat-chong-nang-skin-aqua-tone-up-uv-essence-73.jpg', TRUE),
                                                                                                                                                                                (77, 74, 'LUM-CN-074', NULL, '40ml', NULL, NULL, 229000.00, NULL, 118, 'Hyaluronic Acid, Collagen, Glycerin', 'COMBINATION,NORMAL', '/images/products/sunscreen/sua-chong-nang-skin-aqua-uv-super-moisture-milk-74.jpg', TRUE),
                                                                                                                                                                                (78, 75, 'LUM-CN-075', NULL, '50ml', NULL, NULL, 529000.00, 466000.00, 155, 'Mexoryl, Glycerin, Panthenol', 'DRY', '/images/products/sunscreen/kem-chong-nang-la-roche-posay-anthelios-invisible-fluid-75.jpg', TRUE),
                                                                                                                                                                                (79, 76, 'LUM-CN-076', NULL, '50ml', NULL, NULL, 469000.00, NULL, 52, 'Zinc, Glycerin, Panthenol', 'OILY,COMBINATION,SENSITIVE', '/images/products/sunscreen/gel-chong-nang-eucerin-sun-oil-control-gel-creme-76.jpg', TRUE),
                                                                                                                                                                                (80, 77, 'LUM-CN-077', NULL, '50ml', NULL, NULL, 499000.00, NULL, 89, 'Hyaluronic Acid, Glycerin, Vitamin E', 'NORMAL,DRY', '/images/products/sunscreen/kem-chong-nang-eucerin-sun-photoaging-control-fluid-77.jpg', TRUE),
                                                                                                                                                                                (81, 78, 'LUM-CN-078', NULL, '40ml', NULL, NULL, 439000.00, 386000.00, 126, 'Glycerin, Panthenol, Vitamin E', 'OILY,DRY,COMBINATION,NORMAL,SENSITIVE', '/images/products/sunscreen/kem-chong-nang-bioderma-photoderm-max-aquafluide-78.jpg', TRUE),
                                                                                                                                                                                (82, 79, 'LUM-CN-079', NULL, '88ml', NULL, NULL, 359000.00, NULL, 163, 'Glycerin, Panthenol, Vitamin E', 'COMBINATION', '/images/products/sunscreen/kem-chong-nang-neutrogena-ultra-sheer-dry-touch-spf50-79.jpg', TRUE),
                                                                                                                                                                                (83, 80, 'LUM-CN-080', NULL, '88ml', NULL, NULL, 389000.00, NULL, 60, 'Hyaluronic Acid, Glycerin, Panthenol', 'DRY,SENSITIVE', '/images/products/sunscreen/kem-chong-nang-neutrogena-hydro-boost-water-gel-lotion-spf50-80.jpg', TRUE),
                                                                                                                                                                                (84, 81, 'LUM-CN-081', NULL, '50ml', NULL, NULL, 329000.00, 290000.00, 97, 'Hyaluronic Acid, Niacinamide, Glycerin', 'OILY,NORMAL', '/images/products/sunscreen/kem-chong-nang-l-oreal-paris-uv-defender-serum-spf50-81.jpg', TRUE),
                                                                                                                                                                                (85, 82, 'LUM-CN-082', NULL, '30ml', NULL, NULL, 299000.00, NULL, 134, 'Vitamin C, Hyaluronic Acid, Glycerin', 'SENSITIVE,NORMAL', '/images/products/sunscreen/kem-chong-nang-garnier-ambre-solaire-super-uv-serum-82.jpg', TRUE),
                                                                                                                                                                                (86, 83, 'LUM-CN-083', NULL, '50ml', NULL, NULL, 219000.00, NULL, 171, 'Zinc, Glycerin, Panthenol', 'OILY,COMBINATION,NORMAL', '/images/products/sunscreen/kem-chong-nang-nivea-sun-uv-face-shine-control-spf50-83.jpg', TRUE),
                                                                                                                                                                                (87, 84, 'LUM-CN-084', NULL, '50ml', NULL, NULL, 259000.00, 228000.00, 68, 'Trà xanh, Glycerin, Panthenol', 'DRY,NORMAL', '/images/products/sunscreen/kem-chong-nang-innisfree-daily-uv-protection-cream-mild-84.jpg', TRUE),
                                                                                                                                                                                (88, 85, 'LUM-CN-085', NULL, '50ml', NULL, NULL, 269000.00, NULL, 105, 'Lô hội, Panthenol, Glycerin', 'COMBINATION,SENSITIVE', '/images/products/sunscreen/kem-chong-nang-cosrx-aloe-soothing-sun-cream-85.jpg', TRUE),
                                                                                                                                                                                (89, 86, 'LUM-CN-086', NULL, '50ml', NULL, NULL, 299000.00, NULL, 142, 'Rau má, Zinc, Panthenol', 'OILY,SENSITIVE', '/images/products/sunscreen/kem-chong-nang-some-by-mi-truecica-mineral-100-calming-suncream-86.jpg', TRUE),
                                                                                                                                                                                (90, 87, 'LUM-CN-087', NULL, '50ml', NULL, NULL, 279000.00, 246000.00, 179, 'Bí đao, Rau má, Niacinamide', 'NORMAL,SENSITIVE', '/images/products/sunscreen/kem-chong-nang-cocoon-bi-dao-87.jpg', TRUE),
                                                                                                                                                                                (91, 88, 'LUM-CN-088', NULL, '50ml', NULL, NULL, 559000.00, NULL, 76, 'Mexoryl 400, Glycerin, Panthenol', 'DRY,COMBINATION', '/images/products/sunscreen/kem-chong-nang-la-roche-posay-anthelios-uvmune-400-invisible-fluid-88.jpg', TRUE),
                                                                                                                                                                                (92, 89, 'LUM-DA-089', NULL, '236ml', NULL, NULL, 339000.00, 298000.00, 113, 'Ceramides, Hyaluronic Acid, Glycerin', 'OILY,COMBINATION', '/images/products/moisturizer/kem-duong-cerave-moisturising-lotion-89.jpg', TRUE),
                                                                                                                                                                                (93, 90, 'LUM-DA-090', NULL, '89ml', NULL, NULL, 379000.00, NULL, 150, 'Ceramides, Niacinamide, Hyaluronic Acid', 'DRY,SENSITIVE,NORMAL', '/images/products/moisturizer/kem-duong-cerave-pm-facial-moisturising-lotion-90.jpg', TRUE),
                                                                                                                                                                                (94, 91, 'LUM-DA-091', NULL, '236ml', NULL, NULL, 309000.00, NULL, 47, 'Glycerin, Panthenol, Niacinamide', 'NORMAL', '/images/products/moisturizer/lotion-duong-cetaphil-moisturizing-lotion-91.jpg', TRUE),
                                                                                                                                                                                (95, 92, 'LUM-DA-092', NULL, '100g', NULL, NULL, 289000.00, 254000.00, 84, 'Glycerin, Panthenol, Niacinamide', 'OILY', '/images/products/moisturizer/kem-duong-cetaphil-moisturizing-cream-92.jpg', TRUE),
                                                                                                                                                                                (96, 93, 'LUM-DA-093', NULL, '100ml', NULL, NULL, 549000.00, NULL, 121, 'Ceramides, Niacinamide, Glycerin', 'SENSITIVE', '/images/products/moisturizer/kem-duong-la-roche-posay-toleriane-double-repair-face-moisturizer-93.jpg', TRUE),
                                                                                                                                                                                (97, 94, 'LUM-DA-094', NULL, '100ml', NULL, NULL, 459000.00, NULL, 158, 'Panthenol, Glycerin, Niacinamide', 'COMBINATION,NORMAL', '/images/products/moisturizer/kem-duong-la-roche-posay-cicaplast-baume-b5-94.jpg', TRUE),
                                                                                                                                                                                (98, 95, 'LUM-DA-095', NULL, '50g', NULL, NULL, 349000.00, 307000.00, 55, 'Hyaluronic Acid, Glycerin, Panthenol', 'DRY', '/images/products/moisturizer/gel-duong-neutrogena-hydro-boost-water-gel-95.jpg', TRUE),
                                                                                                                                                                                (99, 96, 'LUM-DA-096', NULL, '50g', NULL, NULL, 369000.00, NULL, 92, 'Hyaluronic Acid, Glycerin, Panthenol', 'OILY,COMBINATION,SENSITIVE', '/images/products/moisturizer/gel-duong-neutrogena-hydro-boost-gel-cream-96.jpg', TRUE),
                                                                                                                                                                                (100, 97, 'LUM-DA-097', NULL, '60ml', NULL, NULL, 399000.00, NULL, 129, 'Hyaluronic Acid, Panthenol, Glycerin', 'NORMAL,DRY', '/images/products/moisturizer/kem-duong-klairs-rich-moist-soothing-cream-97.jpg', TRUE),
                                                                                                                                                                                (101, 98, 'LUM-DA-098', NULL, '60ml', NULL, NULL, 419000.00, 369000.00, 166, 'Panthenol, Glycerin, Rau má', 'OILY,DRY,COMBINATION,NORMAL,SENSITIVE', '/images/products/moisturizer/kem-duong-klairs-midnight-blue-calming-cream-98.jpg', TRUE),
                                                                                                                                                                                (102, 99, 'LUM-DA-099', NULL, '100g', NULL, NULL, 379000.00, NULL, 63, 'Ốc sên, Hyaluronic Acid, Panthenol', 'COMBINATION', '/images/products/moisturizer/kem-duong-cosrx-advanced-snail-92-all-in-one-cream-99.jpg', TRUE),
                                                                                                                                                                                (103, 100, 'LUM-DA-100', NULL, '100ml', NULL, NULL, 329000.00, NULL, 100, 'Hyaluronic Acid, Glycerin, Panthenol', 'DRY,SENSITIVE', '/images/products/moisturizer/lotion-duong-cosrx-oil-free-ultra-moisturizing-lotion-100.jpg', TRUE),
                                                                                                                                                                                (104, 101, 'LUM-DA-101', NULL, '75ml', NULL, NULL, 359000.00, 316000.00, 137, 'Rau má, Panthenol, Glycerin', 'OILY,NORMAL', '/images/products/moisturizer/kem-duong-skin1004-madagascar-centella-soothing-cream-101.jpg', TRUE),
                                                                                                                                                                                (105, 102, 'LUM-DA-102', NULL, '50ml', NULL, NULL, 389000.00, NULL, 174, 'Trà xanh, Hyaluronic Acid, Panthenol', 'SENSITIVE,NORMAL', '/images/products/moisturizer/kem-duong-innisfree-green-tea-seed-cream-102.jpg', TRUE),
                                                                                                                                                                                (106, 103, 'LUM-DA-103', NULL, '50ml', NULL, NULL, 719000.00, NULL, 71, 'Hyaluronic Acid, Panthenol, Glycerin', 'OILY,COMBINATION,NORMAL', '/images/products/moisturizer/kem-duong-laneige-water-bank-blue-hyaluronic-cream-103.jpg', TRUE),
                                                                                                                                                                                (107, 104, 'LUM-DA-104', NULL, '125ml', NULL, NULL, 199000.00, 175000.00, 108, 'Glycerin, Panthenol, Vitamin E', 'DRY,NORMAL', '/images/products/moisturizer/kem-duong-simple-replenishing-rich-moisturiser-104.jpg', TRUE),
                                                                                                                                                                                (108, 105, 'LUM-DA-105', NULL, '50ml', NULL, NULL, 429000.00, NULL, 145, 'Glycerin, Panthenol, Ceramides', 'COMBINATION,SENSITIVE', '/images/products/moisturizer/kem-duong-eucerin-atocontrol-face-care-cream-105.jpg', TRUE),
                                                                                                                                                                                (109, 106, 'LUM-DA-106', NULL, '200ml', NULL, NULL, 449000.00, NULL, 42, 'Glycerin, Panthenol, Niacinamide', 'OILY,SENSITIVE', '/images/products/moisturizer/kem-duong-bioderma-atoderm-creme-106.jpg', TRUE),
                                                                                                                                                                                (110, 107, 'LUM-DA-107', NULL, '50ml', NULL, NULL, 459000.00, 404000.00, 79, 'Retinol, Hyaluronic Acid, Glycerin', 'NORMAL,SENSITIVE', '/images/products/moisturizer/kem-duong-l-oreal-paris-revitalift-triple-action-day-cream-107.jpg', TRUE),
                                                                                                                                                                                (111, 108, 'LUM-DA-108', NULL, '50ml', NULL, NULL, 259000.00, NULL, 116, 'Hoa hồng, Glycerin, Panthenol', 'DRY,COMBINATION', '/images/products/moisturizer/kem-duong-cocoon-hoa-hong-108.jpg', TRUE),
                                                                                                                                                                                (112, 109, 'LUM-TON-109', NULL, '180ml', NULL, NULL, 349000.00, 307000.00, 91, 'Hyaluronic Acid, Panthenol, Glycerin', 'DRY,SENSITIVE,NORMAL', '/images/products/toner/toner-klairs-supple-preparation-facial-toner-109.jpg', TRUE),
                                                                                                                                                                                (113, 110, 'LUM-TON-110', NULL, '150ml', NULL, NULL, 329000.00, NULL, 120, 'Salicylic Acid, Panthenol, Glycerin', 'OILY,COMBINATION', '/images/products/toner/toner-cosrx-aha-bha-clarifying-treatment-toner-110.jpg', TRUE),
                                                                                                                                                                                (114, 111, 'LUM-TON-111', NULL, '150ml', NULL, NULL, 339000.00, NULL, 149, 'Tràm trà, Salicylic Acid, Niacinamide', 'OILY,COMBINATION', '/images/products/toner/toner-some-by-mi-aha-bha-pha-30-days-miracle-111.jpg', TRUE),
                                                                                                                                                                                (115, 112, 'LUM-TON-112', NULL, '200ml', NULL, NULL, 299000.00, 263000.00, 58, 'Trà xanh, Glycerin, Panthenol', 'COMBINATION,NORMAL', '/images/products/toner/toner-innisfree-green-tea-balancing-skin-112.jpg', TRUE),
                                                                                                                                                                                (116, 113, 'LUM-TON-113', NULL, '150ml', NULL, NULL, 549000.00, NULL, 87, 'Hyaluronic Acid, Panthenol, Glycerin', 'DRY,NORMAL', '/images/products/toner/toner-laneige-cream-skin-refiner-113.jpg', TRUE),
                                                                                                                                                                                (117, 114, 'LUM-TON-114', NULL, '170ml', NULL, NULL, 289000.00, NULL, 116, 'Hyaluronic Acid, Glycerin, Panthenol', 'DRY,SENSITIVE,NORMAL', '/images/products/toner/lotion-hada-labo-gokujyun-super-hyaluronic-acid-114.jpg', TRUE),
                                                                                                                                                                                (118, 115, 'LUM-TON-115', NULL, '250ml', NULL, NULL, 359000.00, 316000.00, 145, 'Glycerin, Panthenol, Micellar', 'SENSITIVE,NORMAL', '/images/products/toner/toner-bioderma-sensibio-tonique-115.jpg', TRUE),
                                                                                                                                                                                (119, 116, 'LUM-TON-116', NULL, '200ml', NULL, NULL, 199000.00, NULL, 54, 'Glycerin, Panthenol, Vitamin E', 'SENSITIVE,DRY,NORMAL', '/images/products/toner/toner-simple-kind-to-skin-soothing-facial-toner-116.jpg', TRUE),
                                                                                                                                                                                (120, 117, 'LUM-TON-117', NULL, '200ml', NULL, NULL, 229000.00, NULL, 83, 'Hoa hồng, Glycerin, Panthenol', 'NORMAL,DRY', '/images/products/toner/toner-garnier-micellar-rose-water-117.jpg', TRUE),
                                                                                                                                                                                (121, 118, 'LUM-TON-118', NULL, '150ml', NULL, NULL, 379000.00, 334000.00, 112, 'Zinc, Glycerin, Panthenol', 'OILY,COMBINATION', '/images/products/toner/xit-khoang-la-roche-posay-serozinc-118.jpg', TRUE),
                                                                                                                                                                                (122, 119, 'LUM-LIP-119', NULL, '3g', 'Velvet Teddy', '#8B4A3C', 549000.00, 483000.00, 149, 'Sáp, Vitamin E, Dầu thực vật', NULL, '/images/products/lipstick/son-mac-matte-lipstick-velvet-teddy-119.jpg', TRUE),
                                                                                                                                                                                (123, 120, 'LUM-LIP-120', NULL, '3.9g', 'Đỏ gạch', '#A62B2B', 199000.00, NULL, 60, 'Sáp, Vitamin E, Dầu thực vật', NULL, '/images/products/lipstick/son-maybelline-color-sensational-creamy-matte-120.jpg', TRUE),
                                                                                                                                                                                (124, 121, 'LUM-LIP-121', NULL, '4.8g', 'Đỏ cổ điển', '#9E1B1B', 259000.00, NULL, 91, 'Sáp, Vitamin E, Dầu thực vật', NULL, '/images/products/lipstick/son-l-oreal-paris-color-riche-matte-121.jpg', TRUE),
                                                                                                                                                                                (125, 122, 'LUM-LIP-122', NULL, '4g', 'Hồng đất', '#B85C5C', 329000.00, 290000.00, 122, 'Sáp, Vitamin E, Dầu thực vật', NULL, '/images/products/lipstick/son-3ce-velvet-lip-tint-122.jpg', TRUE),
                                                                                                                                                                                (126, 123, 'LUM-LIP-123', NULL, '5.5g', 'Đỏ cam', '#C94A3A', 219000.00, NULL, 153, 'Sáp, Vitamin E, Dầu thực vật', NULL, '/images/products/lipstick/son-romand-juicy-lasting-tint-123.jpg', TRUE),
                                                                                                                                                                                (127, 124, 'LUM-LIP-124', NULL, '4.5g', 'Đỏ nâu', '#8E2F2F', 229000.00, NULL, 64, 'Sáp, Vitamin E, Dầu thực vật', NULL, '/images/products/lipstick/son-black-rouge-air-fit-velvet-tint-124.jpg', TRUE),
                                                                                                                                                                                (128, 125, 'LUM-LIP-125', NULL, '5g', 'Cam đất', '#C07A4A', 189000.00, 166000.00, 95, 'Sáp, Vitamin E, Dầu thực vật', NULL, '/images/products/lipstick/son-bbia-last-velvet-lip-tint-125.jpg', TRUE),
                                                                                                                                                                                (129, 126, 'LUM-LIP-126', NULL, '4g', 'Hồng đỏ', '#B03A4A', 179000.00, NULL, 126, 'Sáp, Vitamin E, Dầu thực vật', NULL, '/images/products/lipstick/son-peripera-ink-velvet-126.jpg', TRUE),
                                                                                                                                                                                (130, 127, 'LUM-LIP-127', NULL, '3.8g', 'Đỏ rượu', '#7A1F2B', 199000.00, NULL, 157, 'Sáp, Vitamin E, Dầu thực vật', NULL, '/images/products/lipstick/son-merzy-the-first-velvet-tint-127.jpg', TRUE),
                                                                                                                                                                                (131, 128, 'LUM-LIP-128', NULL, '4.3g', 'Nude hồng', '#C08A7A', 289000.00, 254000.00, 68, 'Sáp, Vitamin E, Dầu thực vật', NULL, '/images/products/lipstick/son-ofelia-uncovered-lip-mousse-128.jpg', TRUE);

-- 8. Cart & CartItem
INSERT INTO Cart (cartID, userID, updatedAt) VALUES
                                                 (1, 2, '2026-10-26 21:00:00'),
                                                 (2, 3, '2026-10-25 18:30:00'),
                                                 (3, 4, '2026-10-27 08:15:00');

INSERT INTO CartItem (cartItemID, cartID, variantID, quantity) VALUES
                                                                   (1, 1, 6, 1),
                                                                   (2, 1, 9, 1),
                                                                   (3, 2, 4, 2),
                                                                   (4, 3, 10, 1),
                                                                   (5, 3, 7, 1);

-- 9. Orders
INSERT INTO Orders (orderID, userID, orderCode, orderDate, recipientName, recipientPhone, shippingAddress, subtotal, shippingFee, discountAmount, usedPoints, totalAmount, status, note) VALUES
                                                                                                                                                                                             (1, 2, 'DHCMT001', '2026-09-05 10:15:00', 'Ứng Thị Thanh Trúc', '0972606924', '12 Nguyễn Văn Bảo, Phường 1, Quận Gò Vấp, TP.HCM', 245000.00, 30000.00, 20000.00, 2000, 253000.00, 'COMPLETED', 'Giao giờ hành chính giúp em'),
                                                                                                                                                                                             (2, 3, 'DHHD002', '2026-09-12 14:30:00', 'Nguyễn Trần Hải Đăng', '0952606925', '84/10/1 Lý Thường Kiệt, Phường Bến Nghé, Quận 1, TP.HCM', 564000.00, 30000.00, 30000.00, 0, 564000.00, 'COMPLETED', NULL),
                                                                                                                                                                                             (3, 2, 'DHCMT003', '2026-09-20 09:45:00', 'Ứng Thị Thanh Trúc', '0972606924', '12 Nguyễn Văn Bảo, Phường 1, Quận Gò Vấp, TP.HCM', 778000.00, 30000.00, 50000.00, 0, 758000.00, 'COMPLETED', NULL),
                                                                                                                                                                                             (4, 4, 'DHGH004', '2026-10-24 16:20:00', 'Trần Gia Hân', '0912345678', '1 Võ Văn Ngân, Phường Linh Trung, TP. Thủ Đức, TP.HCM', 378000.00, 30000.00, 0.00, 0, 408000.00, 'SHIPPING', 'Gọi trước khi giao'),
                                                                                                                                                                                             (5, 2, 'DHCMT005', '2026-10-26 20:05:00', 'Ứng Thị Thanh Trúc', '0972606924', '45 Tô Ký, Phường Tân Chánh Hiệp, Quận 12, TP.HCM', 439000.00, 30000.00, 0.00, 1000, 468000.00, 'PENDING', NULL),
                                                                                                                                                                                             (6, 3, 'DHHD006', '2026-10-10 11:00:00', 'Nguyễn Trần Hải Đăng', '0952606925', '84/10/1 Lý Thường Kiệt, Phường Bến Nghé, Quận 1, TP.HCM', 342000.00, 30000.00, 0.00, 0, 372000.00, 'CANCELLED', 'Thanh toán không thành công');

-- 10. Payment
INSERT INTO Payment (paymentID, orderID, paymentMethod, amount, paymentStatus, transactionCode, paidAt) VALUES
                                                                                                            (1, 1, 'COD', 253000.00, 'PAID', 'COD-DHCMT001', '2026-09-08 15:00:00'),
                                                                                                            (2, 2, 'MOMO', 564000.00, 'PAID', 'MOMO-20260912-88213', '2026-09-12 14:32:00'),
                                                                                                            (3, 3, 'VNPAY', 758000.00, 'FAILED', 'VNP-20260920-00417', NULL),
                                                                                                            (4, 3, 'VNPAY', 758000.00, 'PAID', 'VNP-20260920-00452', '2026-09-20 09:50:00'),
                                                                                                            (5, 4, 'BANK_TRANSFER', 408000.00, 'PAID', 'BANK-20261024-7731', '2026-10-24 16:40:00'),
                                                                                                            (6, 5, 'COD', 468000.00, 'PENDING', 'COD-DHCMT005', NULL),
                                                                                                            (7, 6, 'VNPAY', 372000.00, 'FAILED', 'VNP-20261010-01188', NULL);

-- 11. OrderDetail
INSERT INTO OrderDetail (orderDetailID, orderID, variantID, quantity, unitPrice, totalPrice) VALUES
                                                                                                 (1, 1, 1, 1, 245000.00, 245000.00),
                                                                                                 (2, 2, 3, 1, 399000.00, 399000.00),
                                                                                                 (3, 2, 6, 1, 165000.00, 165000.00),
                                                                                                 (4, 3, 8, 1, 468000.00, 468000.00),
                                                                                                 (5, 3, 9, 2, 155000.00, 310000.00),
                                                                                                 (6, 4, 5, 2, 189000.00, 378000.00),
                                                                                                 (7, 5, 7, 1, 320000.00, 320000.00),
                                                                                                 (8, 5, 2, 1, 119000.00, 119000.00),
                                                                                                 (9, 6, 10, 1, 342000.00, 342000.00);

-- 12. VoucherType, Voucher, OrderVoucher
INSERT INTO VoucherType (voucherTypeID, typeName, description) VALUES
                                                                   (1, 'Giảm giá cố định', 'Giảm trực tiếp số tiền VND vào đơn hàng'),
                                                                   (2, 'Miễn phí vận chuyển', 'Hỗ trợ cước phí giao hàng'),
                                                                   (3, 'Giảm giá theo phần trăm', 'Giảm theo % giá trị đơn hàng, có mức giảm tối đa');

INSERT INTO Voucher (voucherID, voucherTypeID, voucherCode, voucherName, description, discountType, discountValue, maxDiscountAmount, minOrderValue, usageLimit, usedCount, startDate, endDate, applicableScope, imgUrl, isActive) VALUES
                                                                                                                                                                                                                                       (1, 1, 'SUMMER2026', 'Ưu đãi mùa hè 2026', 'Giảm 20K cho đơn hàng từ 200K', 'FIXED_AMOUNT', 20000.00, 20000.00, 200000.00, 100, 1, '2026-08-01 00:00:00', '2026-11-30 23:59:59', 'ALL', 'https://example.com/vouchers/summer2026.png', TRUE),
                                                                                                                                                                                                                                       (2, 2, 'FREESHIP30', 'Miễn phí vận chuyển 30K', 'Hỗ trợ tối đa 30K phí ship cho đơn từ 150K', 'FIXED_AMOUNT', 30000.00, 30000.00, 150000.00, 200, 1, '2026-09-01 00:00:00', '2026-12-31 23:59:59', 'SHIPPING', 'https://example.com/vouchers/freeship30.png', TRUE),
                                                                                                                                                                                                                                       (3, 3, 'WELCOME10', 'Chào bạn mới giảm 10%', 'Giảm 10% tối đa 50K cho đơn từ 300K', 'PERCENTAGE', 10.00, 50000.00, 300000.00, 500, 1, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'ALL', 'https://example.com/vouchers/welcome10.png', TRUE),
                                                                                                                                                                                                                                       (4, 3, 'BLACKFRIDAY', 'Black Friday giảm 20%', 'Giảm 20% tối đa 100K cho đơn từ 500K', 'PERCENTAGE', 20.00, 100000.00, 500000.00, 300, 0, '2026-11-25 00:00:00', '2026-11-30 23:59:59', 'ALL', 'https://example.com/vouchers/blackfriday.png', TRUE),
                                                                                                                                                                                                                                       (5, 1, 'NEWYEAR2026', 'Tết dương lịch 2026', 'Giảm 50K cho đơn từ 400K (đã hết hạn)', 'FIXED_AMOUNT', 50000.00, 50000.00, 400000.00, 50, 12, '2025-12-25 00:00:00', '2026-01-05 23:59:59', 'ALL', 'https://example.com/vouchers/newyear2026.png', FALSE);

INSERT INTO OrderVoucher (orderVoucherID, voucherID, userID, orderID, discountAmount, appliedAt) VALUES
                                                                                                     (1, 1, 2, 1, 20000.00, '2026-09-05 10:15:00'),
                                                                                                     (2, 2, 3, 2, 30000.00, '2026-09-12 14:30:00'),
                                                                                                     (3, 3, 2, 3, 50000.00, '2026-09-20 09:45:00');

-- 13. PointTransaction
INSERT INTO PointTransaction (historyID, userID, orderID, type, points, balanceAfter, description, createdAt) VALUES
                                                                                                                  (1, 2, NULL, 'EARN', 2000, 2000, 'Điểm chào mừng thành viên mới', '2026-08-15 09:30:00'),
                                                                                                                  (2, 3, NULL, 'EARN', 100, 100, 'Điểm chào mừng thành viên mới', '2026-08-20 19:10:00'),
                                                                                                                  (3, 2, 1, 'SPEND', -2000, 0, 'Sử dụng điểm giảm tiền cho đơn hàng DHCMT001', '2026-09-05 10:15:00'),
                                                                                                                  (4, 2, 1, 'EARN', 253, 253, 'Tích điểm từ đơn hàng DHCMT001', '2026-09-08 15:00:00'),
                                                                                                                  (5, 3, 2, 'EARN', 564, 664, 'Tích điểm từ đơn hàng DHHD002', '2026-09-15 10:00:00'),
                                                                                                                  (6, 2, 3, 'EARN', 758, 1011, 'Tích điểm từ đơn hàng DHCMT003', '2026-09-23 10:00:00'),
                                                                                                                  (7, 4, NULL, 'EARN', 100, 100, 'Điểm chào mừng thành viên mới', '2026-10-01 10:00:00'),
                                                                                                                  (8, 2, 5, 'SPEND', -1000, 11, 'Sử dụng điểm giảm tiền cho đơn hàng DHCMT005', '2026-10-26 20:05:00');

-- 14. Review & ReviewMedia
INSERT INTO Review (reviewID, userID, orderDetailID, rating, comment, createdAt, isVisible, isAnonymous) VALUES
                                                                                                             (1, 2, 1, 5, 'Sản phẩm dùng rất thích, sạch sâu và không bị khô da!', '2026-09-10 20:00:00', TRUE, FALSE),
                                                                                                             (2, 3, 2, 4, 'Rửa mặt sạch, tạo bọt vừa phải, da không bị căng. Chai hơi khó bơm.', '2026-09-16 21:15:00', TRUE, TRUE),
                                                                                                             (3, 2, 4, 5, 'Kem chống nắng mỏng nhẹ, không bết dính, không để lại vệt trắng.', '2026-09-25 08:30:00', TRUE, FALSE),
                                                                                                             (4, 2, 5, 3, 'Gel hơi bóng dầu với da mình, dùng ban ngày ổn nhưng không hợp lắm.', '2026-09-25 08:40:00', TRUE, FALSE);

INSERT INTO ReviewMedia (mediaID, reviewID, mediaUrl, mediaType) VALUES
                                                                     (1, 1, 'https://example.com/reviews/feedback1.jpg', 'IMAGE'),
                                                                     (2, 3, 'https://example.com/reviews/feedback3a.jpg', 'IMAGE'),
                                                                     (3, 3, 'https://example.com/reviews/feedback3b.jpg', 'IMAGE');

-- 15. ReturnRequest
INSERT INTO ReturnRequest (requestID, userID, orderID, orderDetailID, reason, description, adminNote, mediaUrl, typeMedia, quantity, refundAmount, refundMethod, status, createdAt) VALUES
                                                                                                                                                                                        (1, 3, 2, 3, 'Hàng bể vỡ', 'Chai serum bị nứt khi nhận hàng', 'Đã duyệt hoàn tiền qua Momo', 'https://example.com/returns/serum-broken.jpg', 'IMAGE', 1, 165000.00, 'MOMO', 'COMPLETED', '2026-09-14 09:00:00'),
                                                                                                                                                                                        (2, 2, 3, 5, 'Sản phẩm không như mong đợi', 'Gel chống nắng không hợp với da dầu, còn nguyên seal', NULL, NULL, NULL, 1, 155000.00, 'BANK_TRANSFER', 'PENDING', '2026-10-26 09:00:00'),
                                                                                                                                                                                        (3, 2, 1, 1, 'Đổi ý', 'Muốn đổi sang loại khác', 'Quá thời hạn đổi trả 7 ngày kể từ khi nhận hàng', NULL, NULL, 1, 245000.00, 'BANK_TRANSFER', 'REJECTED', '2026-09-20 18:00:00');

-- 16. Notification
INSERT INTO Notification (notificationID, userID, requestID, orderID, notificationType, title, content, actionUrl, isRead, createdAt) VALUES
                                                                                                                                          (1, 2, NULL, 1, 'ORDER_STATUS', 'Đơn hàng giao thành công', 'Đơn hàng DHCMT001 đã được giao thành công đến bạn.', '/orders/1', TRUE, '2026-09-08 15:00:00'),
                                                                                                                                          (2, 3, NULL, 2, 'ORDER_STATUS', 'Đơn hàng giao thành công', 'Đơn hàng DHHD002 đã được giao thành công đến bạn.', '/orders/2', TRUE, '2026-09-14 08:00:00'),
                                                                                                                                          (3, 3, 1, 2, 'RETURN_STATUS', 'Yêu cầu trả hàng đã được duyệt', 'Yêu cầu trả hàng #1 đã được duyệt và hoàn tiền qua Momo.', '/returns/1', TRUE, '2026-09-15 10:00:00'),
                                                                                                                                          (4, 2, 2, 3, 'RETURN_STATUS', 'Đã tiếp nhận yêu cầu trả hàng', 'Yêu cầu trả hàng #2 của bạn đang chờ được xử lý.', '/returns/2', FALSE, '2026-10-26 09:01:00'),
                                                                                                                                          (5, 4, NULL, 4, 'ORDER_STATUS', 'Đơn hàng đang được giao', 'Đơn hàng DHGH004 đang trên đường giao đến bạn.', '/orders/4', FALSE, '2026-10-25 09:00:00'),
                                                                                                                                          (6, 2, NULL, 5, 'ORDER_STATUS', 'Đặt hàng thành công', 'Đơn hàng DHCMT005 đã được ghi nhận, chúng tôi sẽ sớm xác nhận.', '/orders/5', FALSE, '2026-10-26 20:06:00'),
                                                                                                                                          (7, 2, NULL, NULL, 'PROMOTION', 'Voucher mới dành cho bạn', 'Dùng mã BLACKFRIDAY giảm 20% tối đa 100K từ 25/11.', '/vouchers', FALSE, '2026-10-27 08:00:00');

-- 17. SkinQuiz
INSERT INTO SkinQuiz (quizID, title, description, questions, isActive) VALUES
    (1, 'Trắc nghiệm chẩn đoán loại da cá nhân hóa', 'Bài kiểm tra giúp bạn tìm đúng quy trình chăm sóc da',
     '{"questions":[
       {"id":1,"question":"Sau khi rửa mặt 30 phút (chưa thoa gì), da bạn cảm thấy thế nào?","answers":[
         {"id":"A","text":"Đổ dầu toàn mặt","skinType":"OILY"},
         {"id":"B","text":"Khô căng, thậm chí hơi bong","skinType":"DRY"},
         {"id":"C","text":"Dầu ở vùng chữ T, hai má bình thường hoặc khô","skinType":"COMBINATION"},
         {"id":"D","text":"Thoải mái, không dầu cũng không khô","skinType":"NORMAL"}]},
       {"id":2,"question":"Vào giữa trưa, khi soi gương dưới ánh sáng tự nhiên, da bạn trông như thế nào?","answers":[
         {"id":"A","text":"Bóng dầu rõ rệt trên toàn mặt","skinType":"OILY"},
         {"id":"B","text":"Xỉn màu, có vảy khô nhỏ","skinType":"DRY"},
         {"id":"C","text":"Chỉ trán và mũi bóng dầu","skinType":"COMBINATION"},
         {"id":"D","text":"Gần như không thay đổi so với buổi sáng","skinType":"NORMAL"}]},
       {"id":3,"question":"Lỗ chân lông trên mặt bạn có kích thước thế nào?","answers":[
         {"id":"A","text":"To và dễ thấy trên toàn mặt","skinType":"OILY"},
         {"id":"B","text":"Rất nhỏ, gần như không nhìn thấy","skinType":"DRY"},
         {"id":"C","text":"To ở mũi và má trong, nhỏ ở vùng còn lại","skinType":"COMBINATION"},
         {"id":"D","text":"Nhỏ đến vừa, đều và mịn","skinType":"NORMAL"}]},
       {"id":4,"question":"Khi thử một sản phẩm skincare mới, da bạn thường phản ứng ra sao?","answers":[
         {"id":"A","text":"Dễ đỏ, ngứa hoặc châm chích","skinType":"SENSITIVE"},
         {"id":"B","text":"Hơi căng rát, cần dưỡng ẩm thêm","skinType":"DRY"},
         {"id":"C","text":"Dễ bít tắc và nổi thêm mụn, dầu nhiều hơn","skinType":"OILY"},
         {"id":"D","text":"Hầu như không có phản ứng gì","skinType":"NORMAL"}]},
       {"id":5,"question":"Bạn thường gặp vấn đề nào về mụn và kích ứng?","answers":[
         {"id":"A","text":"Thường xuyên có mụn đầu đen, mụn viêm trên nhiều vùng","skinType":"OILY"},
         {"id":"B","text":"Hiếm khi có mụn nhưng da hay đỏ, rát khi đổi thời tiết","skinType":"SENSITIVE"},
         {"id":"C","text":"Chỉ nổi mụn ở vùng chữ T","skinType":"COMBINATION"},
         {"id":"D","text":"Rất hiếm khi có vấn đề gì","skinType":"NORMAL"}]}
     ]}', TRUE);

-- 18. SkinQuizResult
INSERT INTO SkinQuizResult (resultID, userID, quizID, skinType, skincareAdvice, suitableProducts, completedAt) VALUES
                                                                                                                   (1, 2, 1, 'OILY', 'Nên dùng nước tẩy trang bí đao và sữa rửa mặt kiềm dầu 2 lần/ngày, kết hợp serum Niacinamide để giảm bóng dầu.', '[1, 3, 6]', '2026-08-15 09:45:00'),
                                                                                                                   (2, 3, 1, 'COMBINATION', 'Làm sạch dịu nhẹ, dùng kem chống nắng kết cấu mỏng nhẹ, tập trung kiềm dầu ở vùng chữ T.', '[3, 6, 8]', '2026-08-20 19:30:00'),
                                                                                                                   (3, 4, 1, 'SENSITIVE', 'Ưu tiên sản phẩm không hương liệu, hạn chế thay đổi sản phẩm liên tục và luôn dưỡng ẩm đầy đủ.', '[5, 7, 10]', '2026-10-01 10:20:00');


-- ============================================================
-- KIEM TRA SAU KHI CHAY
-- ============================================================
SELECT COUNT(*) AS SoSanPham FROM Product;        -- phai = 128
SELECT COUNT(*) AS SoVariant FROM ProductVariant; -- phai = 131
SELECT COUNT(*) AS SoMedia FROM ProductMedia;     -- phai = 130
SELECT COUNT(*) AS SoDanhMuc FROM Category;       -- phai = 7