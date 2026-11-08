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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thong so san pham (key-value) hien dang bang o trang chi tiet, vi du
 * Barcode, Thuong hieu, Xuat xu thuong hieu, Noi san xuat, Loai da, Dung tich.
 * Admin nhap tu do o form san pham, khong gioi han truoc danh sach thong so.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ProductAttribute")
public class ProductAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attributeID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productID", nullable = false)
    private Product product;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "value", nullable = false, length = 500)
    private String value;

    @Column(name = "displayOrder", nullable = false)
    private Integer displayOrder = 0;
}
