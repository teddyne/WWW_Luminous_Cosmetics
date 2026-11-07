package com.luminous.controller.product;

import com.luminous.dto.ProductCard;
import com.luminous.model.Category;
import com.luminous.model.enums.CategoryStatus;
import com.luminous.repository.category.CategoryRepository;
import com.luminous.service.product.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class ProductController {
    private final ProductService productService;
    private final CategoryRepository categories;

    public ProductController(ProductService productService, CategoryRepository categories) {
        this.productService = productService;
        this.categories = categories;
    }

    @GetMapping("/products")
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) List<Integer> categoryId,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(required = false) String minRating,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String onSale,
            Model model) {

        List<Integer> effectiveCategoryIds = categoryId;
        String notice = null;
        if ((categoryId == null || categoryId.isEmpty()) && categoryName != null && !categoryName.isBlank()) {
            String wanted = categoryName.trim();
            Category matched = null;
            for (Category c : categories.findByStatusOrderByIdAsc(CategoryStatus.ACTIVE)) {
                if (c.getName().equalsIgnoreCase(wanted)) {
                    matched = c;
                    break;
                }
            }
            if (matched != null) {
                effectiveCategoryIds = List.of(matched.getId());
            } else {
                notice = "Danh mục \"" + wanted
                        + "\" chưa có sản phẩm trong dữ liệu mẫu. Bạn đang xem toàn bộ sản phẩm.";
            }
        }

        List<ProductCard> products = productService.search(
                q, brand, effectiveCategoryIds,
                toDecimal(minPrice), toDecimal(maxPrice), toDouble(minRating), sort,
                "true".equalsIgnoreCase(onSale));

        model.addAttribute("products", products);
        model.addAttribute("categories", categories.findByStatusOrderByIdAsc(CategoryStatus.ACTIVE));
        model.addAttribute("brands", productService.allActiveCards().stream()
                .map(ProductCard::brand).filter(b -> !b.isBlank()).distinct().sorted().toList());
        // Giua nguyen chuoi nguoi dung go de form hien lai dung trang thai da chon.
        model.addAttribute("q", q);
        model.addAttribute("brand", brand);
        model.addAttribute("categoryId", effectiveCategoryIds);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("minRating", minRating);
        model.addAttribute("sort", sort);
        model.addAttribute("notice", notice);
        return "product/list";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        ProductCard card = productService.detailCard(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("card", card);
        model.addAttribute("variants", productService.activeVariantCards(id));
        model.addAttribute("galleryImages", productService.galleryImages(card));
        model.addAttribute("relatedProducts", productService.relatedCards(
                card.getProduct().getCategory().getId(), id, 4));
        return "product/detail";
    }

    /** Chuoi rong hoac sai dinh dang -> null (khong loc), khong nem loi. */
    private BigDecimal toDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double toDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
