USE LUMINOUS_DB;

UPDATE ProductMedia
SET mediaURL = '/images/products/nuoc-tay-trang-cocoon-bi-dao-1.jpg',
    thumbnailUrl = '/images/products/nuoc-tay-trang-cocoon-bi-dao-1.jpg'
WHERE productID = 1 AND displayOrder = 1 AND mediaType = 'IMAGE';

UPDATE ProductMedia
SET mediaURL = '/images/products/sua-rua-mat-cerave-sach-sau-2.jpg',
    thumbnailUrl = '/images/products/sua-rua-mat-cerave-sach-sau-2.jpg'
WHERE productID = 2 AND displayOrder = 1 AND mediaType = 'IMAGE';

UPDATE ProductMedia
SET mediaURL = '/images/products/sua-rua-mat-cetaphil-diu-nhe-3.jpg',
    thumbnailUrl = '/images/products/sua-rua-mat-cetaphil-diu-nhe-3.jpg'
WHERE productID = 3 AND displayOrder = 1 AND mediaType = 'IMAGE';

UPDATE ProductMedia
SET mediaURL = '/images/products/serum-the-ordinary-niacinamide-4.jpg',
    thumbnailUrl = '/images/products/serum-the-ordinary-niacinamide-4.jpg'
WHERE productID = 4 AND displayOrder = 1 AND mediaType = 'IMAGE';

UPDATE ProductMedia
SET mediaURL = '/images/products/serum-klairs-vitamin-c-5.jpg',
    thumbnailUrl = '/images/products/serum-klairs-vitamin-c-5.jpg'
WHERE productID = 5 AND displayOrder = 1 AND mediaType = 'IMAGE';

UPDATE ProductMedia
SET mediaURL = '/images/products/kem-chong-nang-la-roche-posay-anthelios-6.jpg',
    thumbnailUrl = '/images/products/kem-chong-nang-la-roche-posay-anthelios-6.jpg'
WHERE productID = 6 AND displayOrder = 1 AND mediaType = 'IMAGE';

UPDATE ProductMedia
SET mediaURL = '/images/products/gel-chong-nang-skin-aqua-uv-7.jpg',
    thumbnailUrl = '/images/products/gel-chong-nang-skin-aqua-uv-7.jpg'
WHERE productID = 7 AND displayOrder = 1 AND mediaType = 'IMAGE';

UPDATE ProductMedia
SET mediaURL = '/images/products/kem-duong-am-cerave-moisturising-8.jpg',
    thumbnailUrl = '/images/products/kem-duong-am-cerave-moisturising-8.jpg'
WHERE productID = 8 AND displayOrder = 1 AND mediaType = 'IMAGE';

-- Kiểm tra sau khi chạy: phải ra đúng 8 dòng, không còn example.com.
SELECT mediaID, productID, mediaURL, displayOrder
FROM ProductMedia
WHERE mediaType = 'IMAGE' AND displayOrder = 1
ORDER BY productID;
