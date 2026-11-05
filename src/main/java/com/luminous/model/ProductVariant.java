package com.luminous.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ProductVariant")
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variantID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productID", nullable = false)
    private Product product;

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @Column(name = "barcode", unique = true, length = 50)
    private String barcode;

    @Column(name = "size", length = 50)
    private String size;

    @Column(name = "shadeName", length = 100)
    private String shadeName;

    @Column(name = "hexCode", length = 10)
    private String hexCode;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** Gia uu dai; null = khong giam gia. DB rang buoc salePrice <= price. */
    @Column(name = "salePrice", precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "stockQuantity", nullable = false)
    private Integer stockQuantity = 0;

    @Column(name = "ingredients", columnDefinition = "TEXT")
    private String ingredients;

    @Column(name = "suitableSkinType", length = 100)
    private String suitableSkinType;

    @Column(name = "imgUrl", length = 500)
    private String imgUrl;

    @Column(name = "isActive", nullable = false)
    private Boolean isActive = true;

    /** Gia hien thi: uu tien gia sale neu co. */
    public BigDecimal displayPrice() {
        return salePrice != null ? salePrice : price;
    }

    public boolean onSale() {
        return salePrice != null && price != null && salePrice.compareTo(price) < 0;
    }
}
