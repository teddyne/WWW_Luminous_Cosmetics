package com.luminous.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 1 dong trong gio hang: 1 bien the san pham + so luong.
 * DB rang buoc UNIQUE(cartID, variantID) nen them trung variant chi tang so luong,
 * khong tao dong moi; CHECK(quantity > 0).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "CartItem",
        uniqueConstraints = @UniqueConstraint(name = "uq_cartitem", columnNames = {"cartID", "variantID"}))
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cartItemID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartID", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variantID", nullable = false)
    private ProductVariant variant;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;
}
