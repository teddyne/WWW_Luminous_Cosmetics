package com.luminous.controller;

import com.luminous.repository.CategoryRepository;
import com.luminous.repository.ProductRepository;
import com.luminous.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Trang quan tri o giai doan 1 chi la trang tong quan (so lieu that tu DB)
 * de chung minh phan quyen ADMIN. CRUD san pham/danh muc/tai khoan/don hang
 * la buoc tiep theo, xem README muc "Buoc tiep theo".
 */
@Controller
public class AdminController {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final UserRepository users;

    public AdminController(ProductRepository products, CategoryRepository categories, UserRepository users) {
        this.products = products;
        this.categories = categories;
        this.users = users;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("productCount", products.count());
        model.addAttribute("categoryCount", categories.count());
        model.addAttribute("userCount", users.count());
        model.addAttribute("activeProductCount", products.findByIsActiveTrueOrderByCreatedAtDesc().size());
        return "admin/index";
    }
}
