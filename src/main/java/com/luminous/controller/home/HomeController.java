package com.luminous.controller.home;

import com.luminous.dto.ProductCard;
import com.luminous.model.enums.CategoryStatus;
import com.luminous.repository.category.CategoryRepository;
import com.luminous.service.product.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Trang chu: khoi Uu dai va khoi Moi KHONG trung san pham.
 * Ly do trung truoc day: ca 2 khoi deu lay 12 san pham moi nhat truoc
 * (deals = san pham giam gia sap theo ngay tao, newest = san pham sap
 * theo ngay tao), nen 1 san pham vua moi vua giam gia se hien o ca 2 khoi.
 * Cach sua: lay deals truoc, roi lay newest trong so con lai (loai tru
 * id cua deals). Loc theo tu khoa/danh muc van giu tinh khong trung vi
 * loc chi thu hep tung danh sach da khong trung nhau.
 * Luu y: neu HomeController cua ban da co logic rieng them (ngoai ban
 * goc), chi copy 3 dong deals/newestExcluding duoi day sang file cua
 * ban, dung ghi de ca file.
 */
@Controller
public class HomeController {
    private final ProductService productService;
    private final CategoryRepository categories;

    public HomeController(ProductService productService, CategoryRepository categories) {
        this.productService = productService;
        this.categories = categories;
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
        model.addAttribute("categories", categories.findByStatusOrderByIdAsc(CategoryStatus.ACTIVE));
        model.addAttribute("brands", productService.allActiveCards().stream()
                .map(ProductCard::brand).filter(b -> !b.isBlank()).distinct().sorted().toList());
        model.addAttribute("searchQuery", q);
        model.addAttribute("notice", notice);
        return "homepage/home";
    }
}
