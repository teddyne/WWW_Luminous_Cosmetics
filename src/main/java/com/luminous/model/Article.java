package com.luminous.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Bai viet tren trang Blog. Noi dung luu dang HTML don gian (cac the &lt;p&gt;,
 * &lt;h3&gt;, &lt;ul&gt;) do admin nhap, template hien bang th:utext.
 * category la ma ngan: skincare | sunscreen | green | product.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Article")
public class Article {

    public static final String CAT_SKINCARE = "skincare";
    public static final String CAT_SUNSCREEN = "sunscreen";
    public static final String CAT_GREEN = "green";
    public static final String CAT_PRODUCT = "product";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "articleID")
    private Integer id;

    @Column(name = "slug", nullable = false, unique = true, length = 200)
    private String slug;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "excerpt", length = 500)
    private String excerpt;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "category", nullable = false, length = 20)
    private String category = CAT_SKINCARE;

    @Column(name = "coverImage", length = 500)
    private String coverImage;

    @Column(name = "author", length = 100)
    private String author = "LUMINOUS";

    @Column(name = "readingMinutes")
    private Integer readingMinutes = 4;

    @Column(name = "isPublished", nullable = false)
    private Boolean isPublished = true;

    @Column(name = "publishedAt")
    private LocalDateTime publishedAt;

    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public String categoryLabel() {
        return switch (category == null ? "" : category) {
            case CAT_SKINCARE -> "Chăm sóc da";
            case CAT_SUNSCREEN -> "Chống nắng";
            case CAT_GREEN -> "Sống xanh";
            case CAT_PRODUCT -> "Sản phẩm";
            default -> "Bài viết";
        };
    }
}
