package com.luminous.controller.menu;

import com.luminous.model.enums.CategoryStatus;
import com.luminous.repository.category.CategoryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MenuController {
    private final CategoryRepository categories;

    public MenuController(CategoryRepository categories) {
        this.categories = categories;
    }

    @GetMapping("/menu")
    public String menu(Model model) {
        model.addAttribute("categories", categories.findByStatusOrderByIdAsc(CategoryStatus.ACTIVE));
        return "menu/menu";
    }
}
