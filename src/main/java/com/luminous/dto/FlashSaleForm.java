package com.luminous.dto;

import com.luminous.model.Product;
import com.luminous.model.ProductVariant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FlashSaleForm {

    private Integer id;

    private Long version;

    @NotBlank(message = "Tên chương trình không được để trống")
    @Size(max = 200, message = "Tên chương trình tối đa 200 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startAt;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endAt;

    @NotBlank(message = "Vui lòng chọn trạng thái")
    private String status = "DRAFT";

    private String bannerUrl;

    @Valid
    private List<ItemForm> items = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public List<ItemForm> getItems() {
        return items;
    }

    public void setItems(List<ItemForm> items) {
        this.items = items;
    }

    public List<ItemForm> selectedItems() {
        return items.stream().filter(ItemForm::isSelected).toList();
    }

    public static class ItemForm {

        private Integer variantId;

        private boolean selected;

        @Min(value = 1, message = "Phần trăm giảm phải từ 1% trở lên")
        @Max(value = 99, message = "Phần trăm giảm tối đa 99%")
        private Integer discountPercent;

        @Min(value = 1, message = "Số lượng khuyến mãi phải từ 1 trở lên")
        private Integer promoQuantity;

        @Min(value = 1, message = "Giới hạn mua phải từ 1 trở lên")
        private Integer maxPerCustomer;

        public Integer getVariantId() {
            return variantId;
        }

        public void setVariantId(Integer variantId) {
            this.variantId = variantId;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public Integer getDiscountPercent() {
            return discountPercent;
        }

        public void setDiscountPercent(Integer discountPercent) {
            this.discountPercent = discountPercent;
        }

        public Integer getPromoQuantity() {
            return promoQuantity;
        }

        public void setPromoQuantity(Integer promoQuantity) {
            this.promoQuantity = promoQuantity;
        }

        public Integer getMaxPerCustomer() {
            return maxPerCustomer;
        }

        public void setMaxPerCustomer(Integer maxPerCustomer) {
            this.maxPerCustomer = maxPerCustomer;
        }
    }

    public record VariantOption(Integer variantId, Integer productId, String productName,
                                String brand, String size, BigDecimal price, Integer stock,
                                String imageUrl) {
    }

    public record ProductOption(Product product, List<ProductVariant> variants) {
    }
}
