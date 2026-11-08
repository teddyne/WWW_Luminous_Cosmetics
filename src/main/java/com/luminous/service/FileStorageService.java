package com.luminous.service;

import com.luminous.model.Category;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.Set;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov");
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 50L * 1024 * 1024;

    private final String uploadDir;
    private final String productImageDir;
    private final String productMediaDir;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir,
                              @Value("${app.product-image-dir:src/main/resources/static/images/products}") String productImageDir,
                              @Value("${app.product-media-dir:src/main/resources/static/products}") String productMediaDir) {
        this.uploadDir = uploadDir;
        this.productImageDir = productImageDir;
        this.productMediaDir = productMediaDir;
    }

    /**
     * Luu 1 file media cua san pham theo cau truc moi:
     * static/products/{categoryId}-{slug-ten-loai}/{productId}/images/image_{n}.jpg
     * static/products/{categoryId}-{slug-ten-loai}/{productId}/videos/video_{n}.mp4
     * Tra ve duong dan URL (vi du /products/1-skincare/101/images/image_1.jpg).
     * kind = "images" hoac "videos".
     */
    public String storeProductMedia(MultipartFile file, Category category, Integer productId,
                                    String kind, int index) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        boolean video = "videos".equals(kind);
        String extension = checkedExtension(file, video);
        String fileName = (video ? "video_" : "image_") + index + "." + extension;
        String folder = productMediaFolder(category, productId) + "/" + kind;
        try {
            Path dir = Paths.get(productMediaDir, folder);
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            file.transferTo(target);
            return "/products/" + folder + "/" + fileName;
        } catch (IOException e) {
            throw new IllegalStateException("Không lưu được " + (video ? "video" : "ảnh"), e);
        }
    }

    /** Thu muc goc cua 1 san pham, vi du "1-skincare/101". */
    public static String productMediaFolder(Category category, Integer productId) {
        int categoryId = category == null ? 0 : category.getId();
        String categorySlug = category == null ? "khac" : slug(category.getName());
        return categoryId + "-" + categorySlug + "/" + productId;
    }

    public String store(MultipartFile file, String folder, String baseName) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String extension = checkedExtension(file);
        String fileName = slug(baseName) + "-" + System.currentTimeMillis() + "." + extension;
        try {
            Path dir = Paths.get(uploadDir, folder);
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            file.transferTo(target);
            return "/uploads/" + folder + "/" + fileName;
        } catch (IOException e) {
            throw new IllegalStateException("Không lưu được ảnh", e);
        }
    }

    public Path categoryFolder(String categoryName) {
        return Paths.get(productImageDir, categoryFolderName(categoryName));
    }

    private static String categoryFolderName(String categoryName) {
        return (categoryName == null || categoryName.isBlank()) ? "khac" : slug(categoryName);
    }

    public void ensureCategoryFolder(String categoryName) {
        try {
            Files.createDirectories(categoryFolder(categoryName));
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được thư mục ảnh cho loại sản phẩm", e);
        }
    }

    public String storeProductImage(MultipartFile file, String categoryName, String baseName) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String extension = checkedExtension(file);
        String fileName = slug(baseName) + "-" + System.currentTimeMillis() + "." + extension;
        String folder = categoryFolderName(categoryName);
        try {
            Path dir = Paths.get(productImageDir, folder);
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            file.transferTo(target);
            return "/images/products/" + folder + "/" + fileName;
        } catch (IOException e) {
            throw new IllegalStateException("Không lưu được ảnh", e);
        }
    }

    private String checkedExtension(MultipartFile file) {
        return checkedExtension(file, false);
    }

    private String checkedExtension(MultipartFile file, boolean video) {
        long maxSize = video ? MAX_VIDEO_SIZE : MAX_SIZE;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(video ? "Video vượt quá 50MB" : "Ảnh vượt quá 5MB");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = extensionOf(original);
        Set<String> allowed = video ? VIDEO_EXTENSIONS : ALLOWED_EXTENSIONS;
        if (!allowed.contains(extension)) {
            throw new IllegalArgumentException(video
                    ? "Chỉ nhận video mp4, webm, mov"
                    : "Chỉ nhận ảnh jpg, jpeg, png, webp, gif");
        }
        return extension;
    }

    public static String slug(String value) {
        if (value == null || value.isBlank()) {
            return "anh";
        }
        String text = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase();
        text = text.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return text.isBlank() ? "anh" : text;
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase();
    }
}
