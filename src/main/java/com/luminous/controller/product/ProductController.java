package com.luminous.controller.product;

import com.luminous.dto.ProductCard;
import com.luminous.model.Category;
import com.luminous.model.enums.CategoryStatus;
import com.luminous.repository.category.CategoryRepository;
import com.luminous.service.product.ProductService;
import org.springframework.data.domain.Page;
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
    //12 sp 1 trang
    private static final int PAGE_SIZE = 12;
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
            @RequestParam(defaultValue = "1") int page,
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

        Page<ProductCard> result = productService.searchPage(
                q, brand, effectiveCategoryIds,
                toDecimal(minPrice), toDecimal(maxPrice), toDouble(minRating), sort,
                "true".equalsIgnoreCase(onSale), page - 1, PAGE_SIZE);

        model.addAttribute("products", result.getContent());
        model.addAttribute("categories", categories.findByStatusOrderByIdAsc(CategoryStatus.ACTIVE));
        model.addAttribute("brands", productService.allActiveCards().stream()
                .map(ProductCard::brand).filter(b -> !b.isBlank()).distinct().sorted().toList());
        model.addAttribute("q", q);
        model.addAttribute("brand", brand);
        model.addAttribute("categoryId", effectiveCategoryIds);
        model.addAttribute("categoryName", categoryName);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("minRating", minRating);
        model.addAttribute("sort", sort);
        model.addAttribute("onSale", onSale);
        model.addAttribute("notice", notice);
        model.addAttribute("totalItems", result.getTotalElements());
        model.addAttribute("totalPages", result.getTotalPages());
        int currentPage = result.getNumber() + 1;
        model.addAttribute("currentPage", currentPage);
        int windowStart = Math.max(1, Math.min(currentPage - 2, result.getTotalPages() - 4));
        int windowEnd = Math.min(result.getTotalPages(), windowStart + 4);
        model.addAttribute("pageWindowStart", windowStart);
        model.addAttribute("pageWindowEnd", windowEnd);
        return "product/list";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Integer id,
                         @RequestParam(required = false) Integer rating,
                         @RequestParam(defaultValue = "1") int reviewPage,
                         Model model) {
        ProductCard card = productService.detailCard(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("card", card);
        model.addAttribute("variants", productService.activeVariantCards(id));
        model.addAttribute("galleryImages", productService.galleryImages(card));
        model.addAttribute("productVideoUrl", productService.productVideoUrl(card));
        model.addAttribute("attributes", productService.productAttributes(id));
        model.addAttribute("relatedProducts", productService.relatedCards(
                card.getProduct().getCategory().getId(), id, 4));
        // Danh gia: 6 bai/trang, loc theo sao, phan trang giu nguyen bo loc.
        model.addAttribute("reviewSummary", productService.reviewSummary(id));
        model.addAttribute("reviews", productService.reviewPage(id, rating, reviewPage - 1, 6));
        model.addAttribute("selectedRating", rating);
        model.addAttribute("currentReviewPage", reviewPage);
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
