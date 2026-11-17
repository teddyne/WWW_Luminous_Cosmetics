package com.luminous.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "FlashSaleItem",
        uniqueConstraints = @UniqueConstraint(name = "uq_flash_item", columnNames = {"flashSaleID", "variantID"}))
public class FlashSaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flashSaleItemID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flashSaleID", nullable = false)
    private FlashSale flashSale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variantID", nullable = false)
    private ProductVariant variant;

    @Column(name = "discountPercent", nullable = false)
    private Integer discountPercent;

    @Column(name = "promoQuantity", nullable = false)
    private Integer promoQuantity;

    @Column(name = "maxPerCustomer")
    private Integer maxPerCustomer;
}
