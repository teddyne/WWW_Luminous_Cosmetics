package com.luminous.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ProductMedia")
public class ProductMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mediaID")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productID", nullable = false)
    private Product product;

    @Column(name = "mediaURL", nullable = false, length = 500)
    private String mediaUrl;

    @Column(name = "mediaType", nullable = false, length = 20)
    private String mediaType = "IMAGE";

    @Column(name = "thumbnailUrl", length = 500)
    private String thumbnailUrl;

    @Column(name = "displayOrder", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "altText", length = 255)
    private String altText;
}
