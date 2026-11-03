package com.luminous.controller;

import com.luminous.repository.CategoryRepository;
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
        model.addAttribute("categories", categories.findByStatusOrderByIdAsc("ACTIVE"));
        return "menu/menu";
    }
}
