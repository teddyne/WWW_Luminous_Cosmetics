package com.luminous.service.flashsale;

import com.luminous.dto.FlashSaleCard;
import com.luminous.dto.ProductCard;
import com.luminous.model.FlashSale;
import com.luminous.model.FlashSaleItem;
import com.luminous.service.product.ProductService;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class FlashSaleCardBuilder {

    private final ProductService productService;
    private final FlashSaleService flashSales;

    public FlashSaleCardBuilder(ProductService productService, FlashSaleService flashSales) {
        this.productService = productService;
        this.flashSales = flashSales;
    }

    public List<FlashSaleCard> build(FlashSale sale) {
        if (sale == null) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        boolean live = sale.isLive(now);
        List<FlashSaleItem> items = new ArrayList<>(flashSales.itemsOf(sale.getId()));
        items.sort(Comparator.comparing(FlashSaleItem::getDiscountPercent).reversed()
                .thenComparing(item -> flashSales.soldCount(sale, item), Comparator.reverseOrder()));
        List<FlashSaleCard> result = new ArrayList<>();
        for (FlashSaleItem item : items) {
            if (item.getVariant() == null || !Boolean.TRUE.equals(item.getVariant().getIsActive())) {
                continue;
            }
            ProductCard card = productService.cardForVariant(item.getVariant());
            BigDecimal flashPrice = flashSales.flashPriceForItem(item);
            if (flashPrice == null) {
                continue;
            }
            int percent = ProductCard.discountPercentOf(item.getVariant().getPrice(), flashPrice);
            long sold = flashSales.soldCount(sale, item);
            result.add(new FlashSaleCard(card, sale.getId(), sale.getName(),
                    flashPrice, percent, item.getPromoQuantity(), sold,
                    item.getMaxPerCustomer(), sale.getStartAt(), sale.getEndAt(), live));
        }
        return result;
    }
}
