-- ============================================================
-- LUMINOUS_DB - PHIEN BAN (MariaDB 10.4+ / Spring Data JPA)
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
-- 2. USER
-- ============================================================
CREATE TABLE `User` (
    userID INT AUTO_INCREMENT PRIMARY KEY,
    roleID INT NOT NULL,
    fullName VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(15) UNIQUE,
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
-- (Moi user chi co 1 dia chi mac dinh: xu ly o tang Service)
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
-- 4. CATEGORY
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
-- suitableSkinType: ma phan cach bang dau phay, vd 'OILY,COMBINATION'
-- (OILY, DRY, COMBINATION, NORMAL, SENSITIVE) de khop voi ket qua SkinQuiz
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
-- 10. ORDERS (doi ten tu `Order` de khoi phai dung backtick)
-- Cong thuc: totalAmount = subtotal + shippingFee - discountAmount - usedPoints
-- (quy uoc 1 diem = 1 VND). Cong thuc nay kiem tra o tang Service.
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
-- 11. PAYMENT (1 don co the co nhieu lan thanh toan: that bai -> thu lai)
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
-- 14. VOUCHER (them voucherCode de khach nhap ma)
-- applicableScope: 'ALL' hoac 'SHIPPING' (giam phi van chuyen)
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
-- Quy uoc dau: EARN/REFUND > 0, SPEND < 0, ADJUST tuy y (khac 0)
-- balanceAfter = so du sau giao dich
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
-- 17. REVIEW (tham chieu orderDetailID: moi dong san pham trong don chi danh gia 1 lan)
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
-- 19. RETURNREQUEST (them orderDetailID de biet tra san pham nao)
-- Tang Service can kiem tra: order.userID = userID va quantity <= orderDetail.quantity
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
-- 20. NOTIFICATION (them userID: nguoi nhan thong bao)
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
-- 21. SKINQUIZ & SKINQUIZRESULT
-- Cot JSON trong MariaDB thuc chat la LONGTEXT (co JSON_VALID tu 10.4.3)
-- -> trong Entity map thanh String.
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


-- ============================================================
-- ============  DU LIEU MAU  =================================
-- ============================================================

-- 1. Role
INSERT INTO Role (roleID, roleName) VALUES
(1, 'ROLE_ADMIN'),
(2, 'ROLE_CUSTOMER');

-- 2. User (mat khau chung: Luminous@123)
-- pointBalance khop voi PointTransaction ben duoi
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

-- 4. Category
INSERT INTO Category (categoryID, categoryName, description, status) VALUES
(1, 'Tẩy trang', 'Các sản phẩm làm sạch sâu dịu nhẹ', 'ACTIVE'),
(2, 'Sữa rửa mặt', 'Sữa rửa mặt cân bằng độ ẩm và kiềm dầu', 'ACTIVE'),
(3, 'Serum & Dưỡng chất', 'Serum tinh chất phục hồi và dưỡng sáng da', 'ACTIVE'),
(4, 'Kem chống nắng', 'Bảo vệ da khỏi tia UVA/UVB hằng ngày', 'ACTIVE'),
(5, 'Kem dưỡng ẩm', 'Cấp ẩm và phục hồi hàng rào bảo vệ da', 'ACTIVE');

-- 5. Product
INSERT INTO Product (productID, categoryID, name, brand, description, isActive, createdAt) VALUES
(1, 1, 'Nước tẩy trang Cocoon bí đao làm sạch dịu nhẹ', 'COCOON', 'Nước tẩy trang bí đao Cocoon giúp làm sạch bụi bẩn, giảm dầu thừa hiệu quả.', TRUE, '2026-08-01 09:00:00'),
(2, 2, 'Sữa Rửa Mặt CeraVe Sạch Sâu Dành Cho Da Dầu', 'CERAVE', 'Sữa rửa mặt CeraVe Foaming Cleanser làm sạch dịu nhẹ, cân bằng độ ẩm tự nhiên.', TRUE, '2026-08-01 09:05:00'),
(3, 2, 'Sữa Rửa Mặt Cetaphil Dịu Nhẹ Cho Da Nhạy Cảm', 'CETAPHIL', 'Sữa rửa mặt Cetaphil Gentle Skin Cleanser không xà phòng, không hương liệu, phù hợp da khô và nhạy cảm.', TRUE, '2026-08-01 09:10:00'),
(4, 3, 'Serum The Ordinary Niacinamide 10% + Zinc 1%', 'THE ORDINARY', 'Serum giúp giảm bóng dầu, thu nhỏ lỗ chân lông và mờ thâm mụn.', TRUE, '2026-08-02 09:00:00'),
(5, 3, 'Serum Klairs Freshly Juiced Vitamin Drop', 'KLAIRS', 'Serum vitamin C dịu nhẹ giúp sáng da, mờ vết thâm, phù hợp da nhạy cảm.', TRUE, '2026-08-02 09:05:00'),
(6, 4, 'Kem Chống Nắng La Roche-Posay Anthelios UVMune 400', 'LA ROCHE-POSAY', 'Kem chống nắng phổ rộng SPF50+, kết cấu mỏng nhẹ, không gây bết dính.', TRUE, '2026-08-03 09:00:00'),
(7, 4, 'Gel Chống Nắng Skin Aqua UV Super Moisture Gel', 'SKIN AQUA', 'Gel chống nắng SPF50+ PA++++ dưỡng ẩm, dùng được hằng ngày.', TRUE, '2026-08-03 09:05:00'),
(8, 5, 'Kem Dưỡng Ẩm CeraVe Moisturising Cream', 'CERAVE', 'Kem dưỡng ẩm chứa 3 loại ceramide thiết yếu, phục hồi hàng rào bảo vệ da.', TRUE, '2026-08-04 09:00:00');

-- 6. ProductMedia
INSERT INTO ProductMedia (mediaID, productID, mediaURL, mediaType, thumbnailUrl, displayOrder, altText) VALUES
(1, 1, 'https://example.com/images/cocoon-500ml.jpg', 'IMAGE', 'https://example.com/images/thumbs/cocoon-500ml.jpg', 1, 'Nước tẩy trang Cocoon bí đao 500ml'),
(2, 1, 'https://example.com/images/cocoon-texture.jpg', 'IMAGE', 'https://example.com/images/thumbs/cocoon-texture.jpg', 2, 'Kết cấu nước tẩy trang Cocoon'),
(3, 2, 'https://example.com/images/cerave-473ml.jpg', 'IMAGE', 'https://example.com/images/thumbs/cerave-473ml.jpg', 1, 'Sữa rửa mặt CeraVe 473ml'),
(4, 2, 'https://example.com/videos/cerave-guide.mp4', 'VIDEO', 'https://example.com/images/thumbs/cerave-guide.jpg', 2, 'Video hướng dẫn dùng sữa rửa mặt CeraVe'),
(5, 3, 'https://example.com/images/cetaphil-500ml.jpg', 'IMAGE', 'https://example.com/images/thumbs/cetaphil-500ml.jpg', 1, 'Sữa rửa mặt Cetaphil 500ml'),
(6, 4, 'https://example.com/images/ordinary-niacinamide.jpg', 'IMAGE', 'https://example.com/images/thumbs/ordinary-niacinamide.jpg', 1, 'Serum The Ordinary Niacinamide'),
(7, 5, 'https://example.com/images/klairs-vitc.jpg', 'IMAGE', 'https://example.com/images/thumbs/klairs-vitc.jpg', 1, 'Serum Klairs Vitamin C'),
(8, 6, 'https://example.com/images/lrp-anthelios.jpg', 'IMAGE', 'https://example.com/images/thumbs/lrp-anthelios.jpg', 1, 'Kem chống nắng La Roche-Posay Anthelios'),
(9, 7, 'https://example.com/images/skinaqua-gel.jpg', 'IMAGE', 'https://example.com/images/thumbs/skinaqua-gel.jpg', 1, 'Gel chống nắng Skin Aqua'),
(10, 8, 'https://example.com/images/cerave-moisturising.jpg', 'IMAGE', 'https://example.com/images/thumbs/cerave-moisturising.jpg', 1, 'Kem dưỡng ẩm CeraVe');

-- 7. ProductVariant
INSERT INTO ProductVariant (variantID, productID, sku, barcode, size, shadeName, hexCode, price, salePrice, stockQuantity, ingredients, suitableSkinType, imgUrl, isActive) VALUES
(1, 1, 'COC-TT-500', '8936182500123', '500ml', NULL, NULL, 298000.00, 245000.00, 150, 'Bí đao, rau má, tinh dầu tràm trà', 'OILY,COMBINATION', 'https://example.com/images/cocoon-500ml.jpg', TRUE),
(2, 1, 'COC-TT-140', '8936182500130', '140ml', NULL, NULL, 119000.00, NULL, 200, 'Bí đao, rau má, tinh dầu tràm trà', 'OILY,COMBINATION', 'https://example.com/images/cocoon-140ml.jpg', TRUE),
(3, 2, 'CER-SRM-473', '3337875597357', '473ml', NULL, NULL, 450000.00, 399000.00, 80, 'Ceramides, Niacinamide, Hyaluronic Acid', 'OILY,COMBINATION,NORMAL', 'https://example.com/images/cerave-473ml.jpg', TRUE),
(4, 2, 'CER-SRM-236', '3337875597340', '236ml', NULL, NULL, 280000.00, NULL, 120, 'Ceramides, Niacinamide, Hyaluronic Acid', 'OILY,COMBINATION,NORMAL', 'https://example.com/images/cerave-236ml.jpg', TRUE),
(5, 3, 'CET-SRM-500', '3499320002116', '500ml', NULL, NULL, 215000.00, 189000.00, 90, 'Glycerin, Panthenol, Niacinamide', 'DRY,SENSITIVE,NORMAL', 'https://example.com/images/cetaphil-500ml.jpg', TRUE),
(6, 4, 'ORD-NIA-30', '769915190304', '30ml', NULL, NULL, 190000.00, 165000.00, 60, 'Niacinamide 10%, Zinc PCA 1%', 'OILY,COMBINATION', 'https://example.com/images/ordinary-niacinamide.jpg', TRUE),
(7, 5, 'KLA-VTC-35', '8809572890011', '35ml', NULL, NULL, 360000.00, 320000.00, 45, 'Ethyl Ascorbic Acid, Hyaluronic Acid, Panthenol', 'NORMAL,DRY,SENSITIVE', 'https://example.com/images/klairs-vitc.jpg', TRUE),
(8, 6, 'LRP-ANT-50', '3337875546324', '50ml', NULL, NULL, 520000.00, 468000.00, 70, 'Mexoryl 400, Mexoryl XL, Glycerin', 'OILY,COMBINATION,SENSITIVE', 'https://example.com/images/lrp-anthelios.jpg', TRUE),
(9, 7, 'SKA-UV-80', '4901417695004', '80g', NULL, NULL, 175000.00, 155000.00, 100, 'Hyaluronic Acid, Collagen, Royal Jelly', 'NORMAL,DRY,COMBINATION', 'https://example.com/images/skinaqua-gel.jpg', TRUE),
(10, 8, 'CER-MOI-340', '3337875597388', '340g', NULL, NULL, 380000.00, 342000.00, 55, '3 Ceramides, Hyaluronic Acid, MVE Technology', 'DRY,SENSITIVE,NORMAL', 'https://example.com/images/cerave-moisturising.jpg', TRUE),
(11, 8, 'CER-MOI-85', '3337875597395', '85g', NULL, NULL, 190000.00, NULL, 0, '3 Ceramides, Hyaluronic Acid, MVE Technology', 'DRY,SENSITIVE,NORMAL', 'https://example.com/images/cerave-moisturising-85.jpg', TRUE);

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
-- totalAmount = subtotal + shippingFee - discountAmount - usedPoints
INSERT INTO Orders (orderID, userID, orderCode, orderDate, recipientName, recipientPhone, shippingAddress, subtotal, shippingFee, discountAmount, usedPoints, totalAmount, status, note) VALUES
(1, 2, 'DHCMT001', '2026-09-05 10:15:00', 'Ứng Thị Thanh Trúc', '0972606924', '12 Nguyễn Văn Bảo, Phường 1, Quận Gò Vấp, TP.HCM', 245000.00, 30000.00, 20000.00, 2000, 253000.00, 'COMPLETED', 'Giao giờ hành chính giúp em'),
(2, 3, 'DHHD002', '2026-09-12 14:30:00', 'Nguyễn Trần Hải Đăng', '0952606925', '84/10/1 Lý Thường Kiệt, Phường Bến Nghé, Quận 1, TP.HCM', 564000.00, 30000.00, 30000.00, 0, 564000.00, 'COMPLETED', NULL),
(3, 2, 'DHCMT003', '2026-09-20 09:45:00', 'Ứng Thị Thanh Trúc', '0972606924', '12 Nguyễn Văn Bảo, Phường 1, Quận Gò Vấp, TP.HCM', 778000.00, 30000.00, 50000.00, 0, 758000.00, 'COMPLETED', NULL),
(4, 4, 'DHGH004', '2026-10-24 16:20:00', 'Trần Gia Hân', '0912345678', '1 Võ Văn Ngân, Phường Linh Trung, TP. Thủ Đức, TP.HCM', 378000.00, 30000.00, 0.00, 0, 408000.00, 'SHIPPING', 'Gọi trước khi giao'),
(5, 2, 'DHCMT005', '2026-10-26 20:05:00', 'Ứng Thị Thanh Trúc', '0972606924', '45 Tô Ký, Phường Tân Chánh Hiệp, Quận 12, TP.HCM', 439000.00, 30000.00, 0.00, 1000, 468000.00, 'PENDING', NULL),
(6, 3, 'DHHD006', '2026-10-10 11:00:00', 'Nguyễn Trần Hải Đăng', '0952606925', '84/10/1 Lý Thường Kiệt, Phường Bến Nghé, Quận 1, TP.HCM', 342000.00, 30000.00, 0.00, 0, 372000.00, 'CANCELLED', 'Thanh toán không thành công');

-- 10. Payment (don 3 thanh toan that bai roi thu lai; don 6 that bai nen huy)
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

-- 13. PointTransaction (1 diem = 1 VND; tich 1 diem / 1.000 VND sau khi don COMPLETED)
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

-- 17. SkinQuiz: 5 cau hoi, moi dap an gan voi 1 loai da
-- (OILY, DRY, COMBINATION, NORMAL, SENSITIVE)
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

-- 18. SkinQuizResult (suitableProducts = danh sach variantID)
INSERT INTO SkinQuizResult (resultID, userID, quizID, skinType, skincareAdvice, suitableProducts, completedAt) VALUES
(1, 2, 1, 'OILY', 'Nên dùng nước tẩy trang bí đao và sữa rửa mặt kiềm dầu 2 lần/ngày, kết hợp serum Niacinamide để giảm bóng dầu.', '[1, 3, 6]', '2026-08-15 09:45:00'),
(2, 3, 1, 'COMBINATION', 'Làm sạch dịu nhẹ, dùng kem chống nắng kết cấu mỏng nhẹ, tập trung kiềm dầu ở vùng chữ T.', '[3, 6, 8]', '2026-08-20 19:30:00'),
(3, 4, 1, 'SENSITIVE', 'Ưu tiên sản phẩm không hương liệu, hạn chế thay đổi sản phẩm liên tục và luôn dưỡng ẩm đầy đủ.', '[5, 7, 10]', '2026-10-01 10:20:00');
