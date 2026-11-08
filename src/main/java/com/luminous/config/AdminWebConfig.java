package com.luminous.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths;

@Configuration
public class AdminWebConfig implements WebMvcConfigurer {

    private final String uploadDir;
    private final String productImageDir;
    private final String productMediaDir;

    public AdminWebConfig(@Value("${app.upload-dir:uploads}") String uploadDir,
                          @Value("${app.product-image-dir:src/main/resources/static/images/products}") String productImageDir,
                          @Value("${app.product-media-dir:src/main/resources/static/products}") String productMediaDir) {
        this.uploadDir = uploadDir;
        this.productImageDir = productImageDir;
        this.productMediaDir = productMediaDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
        String productLocation = Paths.get(productImageDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/images/products/**")
                .addResourceLocations(productLocation);
        // Anh/video san pham theo cau truc moi: /products/{loai}/{id}/images|videos/...
        // (khong trung voi route /products cua shop vi controller duoc uu tien truoc,
        // resource handler chi phuc vu khi khong co controller nao khop duong dan file).
        String mediaLocation = Paths.get(productMediaDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/products/**")
                .addResourceLocations(mediaLocation);
    }
}
