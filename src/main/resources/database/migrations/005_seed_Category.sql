-- Luminous - Phan 3/4: Danh muc 6 Toner + 7 Son moi, 20 san pham 109-128
-- + variant 112-131 + media 111-130. Ngay tao toner/son da dan deu 11-20/09/2026.
-- Chay SAU phan 2. Chi chay 1 lan.
USE LUMINOUS_DB;

-- Phan 3 + 4: Danh muc 6-7 va 20 san pham 109-128 (ngay tao da sua)
INSERT INTO Category (categoryID, categoryName, description, status) VALUES
(6, 'Toner', 'Nước cân bằng da, cấp ẩm và làm sạch nhẹ sau bước rửa mặt.', 'ACTIVE'),
(7, 'Son môi', 'Son lì, son tint và son kem cho trang điểm hằng ngày.', 'ACTIVE');

INSERT INTO Product (productID, categoryID, name, brand, description, isActive, createdAt) VALUES
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

INSERT INTO ProductVariant (variantID, productID, sku, barcode, size, shadeName, hexCode, price, salePrice, stockQuantity, ingredients, suitableSkinType, imgUrl, isActive) VALUES
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

INSERT INTO ProductMedia (mediaID, productID, mediaURL, mediaType, thumbnailUrl, displayOrder, altText) VALUES
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

-- Kiem tra: SELECT COUNT(*) FROM Product; -- phai = 128 (8 cu + 100 + 20)
-- ============================================================
