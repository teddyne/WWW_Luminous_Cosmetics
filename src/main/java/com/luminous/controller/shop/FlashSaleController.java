package com.luminous.controller.shop;

import com.luminous.dto.FlashSaleCard;
import com.luminous.model.FlashSale;
import com.luminous.service.flashsale.FlashSaleCardBuilder;
import com.luminous.service.flashsale.FlashSaleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class FlashSaleController {

    private final FlashSaleService flashSales;
    private final FlashSaleCardBuilder cardBuilder;

    public FlashSaleController(FlashSaleService flashSales, FlashSaleCardBuilder cardBuilder) {
        this.flashSales = flashSales;
        this.cardBuilder = cardBuilder;
    }

    @GetMapping("/flash-sale")
    public String page(Model model) {
        Optional<FlashSale> sale = flashSales.showcaseSale();
        List<FlashSaleCard> cards = sale.map(cardBuilder::build).orElse(List.of());
        model.addAttribute("sale", sale.orElse(null));
        model.addAttribute("flashCards", cards);
        if (sale.isPresent()) {
            LocalDateTime now = LocalDateTime.now();
            boolean live = sale.get().isLive(now);
            model.addAttribute("countdownTarget", live ? sale.get().getEndAt() : sale.get().getStartAt());
            model.addAttribute("countdownLabel", live ? "Kết thúc sau" : "Bắt đầu sau");
        }
        return "product/flash-sale";
    }
}
