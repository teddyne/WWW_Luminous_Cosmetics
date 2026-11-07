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

    public AdminWebConfig(@Value("${app.upload-dir:uploads}") String uploadDir,
                          @Value("${app.product-image-dir:src/main/resources/static/images/products}") String productImageDir) {
        this.uploadDir = uploadDir;
        this.productImageDir = productImageDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
        String productLocation = Paths.get(productImageDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/images/products/**")
                .addResourceLocations(productLocation);
    }
}
