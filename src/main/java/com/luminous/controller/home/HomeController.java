package com.luminous.controller.home;

import com.luminous.dto.FlashSaleCard;
import com.luminous.dto.ProductCard;
import com.luminous.model.FlashSale;
import com.luminous.model.enums.CategoryStatus;
import com.luminous.repository.blog.ArticleRepository;
import com.luminous.repository.category.CategoryRepository;
import com.luminous.service.flashsale.FlashSaleCardBuilder;
import com.luminous.service.flashsale.FlashSaleService;
import com.luminous.service.product.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class HomeController {
    private final ProductService productService;
    private final CategoryRepository categories;
    private final ArticleRepository articles;
    private final FlashSaleService flashSales;
    private final FlashSaleCardBuilder flashCardBuilder;

    public HomeController(ProductService productService, CategoryRepository categories,
                          ArticleRepository articles, FlashSaleService flashSales,
                          FlashSaleCardBuilder flashCardBuilder) {
        this.productService = productService;
        this.categories = categories;
        this.articles = articles;
        this.flashSales = flashSales;
        this.flashCardBuilder = flashCardBuilder;
    }

    @GetMapping("/")
    public String home(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String categoryName,
            Model model) {

        List<ProductCard> deals = productService.deals(12);
        Set<Integer> dealIds = deals.stream()
                .map(c -> c.getProduct() != null ? c.getProduct().getId() : null)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        List<ProductCard> newest = productService.newestExcluding(dealIds, 12);
        List<ProductCard> best = productService.bestSellers(12);

        String notice = null;
        if (q != null && !q.isBlank()) {
            deals = productService.filter(deals, q, categoryId);
            newest = productService.filter(newest, q, categoryId);
            best = productService.filter(best, q, categoryId);
            notice = "Kết quả tìm kiếm cho \"" + q.trim() + "\"";
        } else if (categoryId != null) {
            deals = productService.filter(deals, null, categoryId);
            newest = productService.filter(newest, null, categoryId);
            best = productService.filter(best, null, categoryId);
            notice = categories.findById(categoryId)
                    .map(c -> "Danh mục: " + c.getName())
                    .orElse("Danh mục không tồn tại");
        } else if (categoryName != null && !categoryName.isBlank()) {
            boolean exists = categories.findByStatusOrderByIdAsc(CategoryStatus.ACTIVE).stream()
                    .anyMatch(c -> c.getName().equalsIgnoreCase(categoryName.trim()));
            if (!exists) {
                notice = "Danh mục \"" + categoryName.trim()
                        + "\" chưa có sản phẩm trong dữ liệu mẫu. Bạn đang xem toàn bộ sản phẩm.";
            }
        }

        model.addAttribute("dealProducts", deals);
        model.addAttribute("newProducts", newest);
        model.addAttribute("bestProducts", best);
        Optional<FlashSale> flashSale = flashSales.showcaseSale();
        model.addAttribute("flashSale", flashSale.orElse(null));
        List<FlashSaleCard> flashCards = flashSale.map(flashCardBuilder::build).orElse(List.of());
        model.addAttribute("flashCards", flashCards);
        if (flashSale.isPresent()) {
            LocalDateTime now = LocalDateTime.now();
            boolean live = flashSale.get().isLive(now);
            model.addAttribute("countdownTarget", live ? flashSale.get().getEndAt() : flashSale.get().getStartAt());
            model.addAttribute("countdownLabel", live ? "Kết thúc sau" : "Bắt đầu sau");
        }
        model.addAttribute("latestArticles", articles.findByIsPublishedTrueOrderByPublishedAtDesc()
                .stream().limit(4).toList());
        // So lieu that cho khoi "Ve chung toi" tren trang chu.
        List<ProductCard> allCards = productService.allActiveCards();
        double avgRating = allCards.stream()
                .filter(c -> c.getReviewCount() > 0)
                .mapToDouble(ProductCard::getRating).average().orElse(0);
        model.addAttribute("totalProducts", allCards.size());
        model.addAttribute("totalCategories",
                categories.findByStatusOrderByIdAsc(CategoryStatus.ACTIVE).size());
        model.addAttribute("averageRating", avgRating);
        model.addAttribute("categories", categories.findByStatusOrderByIdAsc(CategoryStatus.ACTIVE));
        model.addAttribute("brands", allCards.stream()
                .map(ProductCard::brand).filter(b -> !b.isBlank()).distinct().sorted().toList());
        model.addAttribute("searchQuery", q);
        model.addAttribute("notice", notice);
        return "homepage/home";
    }
    @GetMapping("/about")
    public String about() {
        return "about/about";
    }
    @GetMapping("/contact")
    public String contact() {
        return "contact/contact";
    }
    @GetMapping("/support")
    public String support() {
        return "support/support";
    }
}
