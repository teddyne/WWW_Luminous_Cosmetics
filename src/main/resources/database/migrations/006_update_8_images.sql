-- Luminous - Phan 4/4: Sua anh 8 san pham cu 1-8 ve dung thu muc phan loai
-- (1 makeup-remover, 2-3 cleanser, 4-5 serum, 6-7 sunscreen, 8 moisturizer).
-- Chay CUOI CUNG. Phan nay la UPDATE nen chay lai cung chi ghi de dung gia tri cu.
USE LUMINOUS_DB;

-- Phan 5: Anh 8 san pham cu 1-8 ve dung thu muc phan loai
-- (1 makeup-remover, 2-3 cleanser, 4-5 serum, 6-7 sunscreen, 8 moisturizer)
-- ============================================================
UPDATE ProductVariant SET imgUrl = '/images/products/makeup-remover/nuoc-tay-trang-cocoon-bi-dao-1.jpg' WHERE productID = 1;
UPDATE ProductVariant SET imgUrl = '/images/products/cleanser/sua-rua-mat-cerave-sach-sau-2.jpg' WHERE productID = 2;
UPDATE ProductVariant SET imgUrl = '/images/products/cleanser/sua-rua-mat-cetaphil-diu-nhe-3.jpg' WHERE productID = 3;
UPDATE ProductVariant SET imgUrl = '/images/products/serum/serum-the-ordinary-niacinamide-4.jpg' WHERE productID = 4;
UPDATE ProductVariant SET imgUrl = '/images/products/serum/serum-klairs-vitamin-c-5.jpg' WHERE productID = 5;
UPDATE ProductVariant SET imgUrl = '/images/products/sunscreen/kem-chong-nang-la-roche-posay-anthelios-6.jpg' WHERE productID = 6;
UPDATE ProductVariant SET imgUrl = '/images/products/sunscreen/gel-chong-nang-skin-aqua-uv-7.jpg' WHERE productID = 7;
UPDATE ProductVariant SET imgUrl = '/images/products/moisturizer/kem-duong-am-cerave-moisturising-8.jpg' WHERE productID = 8;

UPDATE ProductMedia SET mediaURL = '/images/products/makeup-remover/nuoc-tay-trang-cocoon-bi-dao-1.jpg', thumbnailUrl = '/images/products/makeup-remover/nuoc-tay-trang-cocoon-bi-dao-1.jpg' WHERE productID = 1 AND mediaType = 'IMAGE' AND displayOrder = 1;
UPDATE ProductMedia SET mediaURL = '/images/products/cleanser/sua-rua-mat-cerave-sach-sau-2.jpg', thumbnailUrl = '/images/products/cleanser/sua-rua-mat-cerave-sach-sau-2.jpg' WHERE productID = 2 AND mediaType = 'IMAGE' AND displayOrder = 1;
UPDATE ProductMedia SET mediaURL = '/images/products/cleanser/sua-rua-mat-cetaphil-diu-nhe-3.jpg', thumbnailUrl = '/images/products/cleanser/sua-rua-mat-cetaphil-diu-nhe-3.jpg' WHERE productID = 3 AND mediaType = 'IMAGE' AND displayOrder = 1;
UPDATE ProductMedia SET mediaURL = '/images/products/serum/serum-the-ordinary-niacinamide-4.jpg', thumbnailUrl = '/images/products/serum/serum-the-ordinary-niacinamide-4.jpg' WHERE productID = 4 AND mediaType = 'IMAGE' AND displayOrder = 1;
UPDATE ProductMedia SET mediaURL = '/images/products/serum/serum-klairs-vitamin-c-5.jpg', thumbnailUrl = '/images/products/serum/serum-klairs-vitamin-c-5.jpg' WHERE productID = 5 AND mediaType = 'IMAGE' AND displayOrder = 1;
UPDATE ProductMedia SET mediaURL = '/images/products/sunscreen/kem-chong-nang-la-roche-posay-anthelios-6.jpg', thumbnailUrl = '/images/products/sunscreen/kem-chong-nang-la-roche-posay-anthelios-6.jpg' WHERE productID = 6 AND mediaType = 'IMAGE' AND displayOrder = 1;
UPDATE ProductMedia SET mediaURL = '/images/products/sunscreen/gel-chong-nang-skin-aqua-uv-7.jpg', thumbnailUrl = '/images/products/sunscreen/gel-chong-nang-skin-aqua-uv-7.jpg' WHERE productID = 7 AND mediaType = 'IMAGE' AND displayOrder = 1;
UPDATE ProductMedia SET mediaURL = '/images/products/moisturizer/kem-duong-am-cerave-moisturising-8.jpg', thumbnailUrl = '/images/products/moisturizer/kem-duong-am-cerave-moisturising-8.jpg' WHERE productID = 8 AND mediaType = 'IMAGE' AND displayOrder = 1;

-- ============================================================
-- Kiem tra sau khi chay (chay tung lenh, ket qua phai dung so nay)
-- ============================================================
-- SELECT COUNT(*) FROM Product; -- phai = 128 (8 cu + 100 + 20)
-- SELECT COUNT(*) FROM ProductVariant; -- phai = 131 (11 cu + 100 + 20)
-- SELECT COUNT(*) FROM ProductMedia; -- phai = 130 (10 cu + 100 + 20)
-- SELECT COUNT(*) FROM Category; -- phai = 7
-- SELECT productID, imgUrl FROM ProductVariant WHERE productID BETWEEN 1 AND 8;
