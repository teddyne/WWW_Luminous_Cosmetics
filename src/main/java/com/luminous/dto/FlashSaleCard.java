package com.luminous.dto;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;

public class FlashSaleCard {

    private final ProductCard card;
    private final Integer saleId;
    private final String saleName;
    private final BigDecimal flashPrice;
    private final int discountPercent;
    private final int promoQuantity;
    private final long soldQuantity;
    private final Integer maxPerCustomer;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final boolean live;

    public FlashSaleCard(ProductCard card, Integer saleId, String saleName,
                         BigDecimal flashPrice, int discountPercent,
                         int promoQuantity, long soldQuantity, Integer maxPerCustomer,
                         LocalDateTime startAt, LocalDateTime endAt, boolean live) {
        this.card = card;
        this.saleId = saleId;
        this.saleName = saleName;
        this.flashPrice = flashPrice;
        this.discountPercent = discountPercent;
        this.promoQuantity = promoQuantity;
        this.soldQuantity = soldQuantity;
        this.maxPerCustomer = maxPerCustomer;
        this.startAt = startAt;
        this.endAt = endAt;
        this.live = live;
    }

    public ProductCard getCard() {
        return card;
    }

    public Integer getSaleId() {
        return saleId;
    }

    public String getSaleName() {
        return saleName;
    }

    public BigDecimal getFlashPrice() {
        return flashPrice;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public int getPromoQuantity() {
        return promoQuantity;
    }

    public long getSoldQuantity() {
        return soldQuantity;
    }

    public Integer getMaxPerCustomer() {
        return maxPerCustomer;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public boolean isLive() {
        return live;
    }

    public int soldPercent() {
        if (promoQuantity <= 0) {
            return 0;
        }
        return (int) Math.min(100, soldQuantity * 100 / promoQuantity);
    }

    public boolean isHot() {
        return live && soldPercent() >= 80;
    }

    public boolean isSoldOut() {
        return promoQuantity > 0 && soldQuantity >= promoQuantity;
    }

    public String statusLabel() {
        if (!live) {
            return "Sắp diễn ra";
        }
        if (isSoldOut()) {
            return "Đã hết lượt";
        }
        return isHot() ? "Đang bán chạy" : "Đang diễn ra";
    }

    public String flashPriceText() {
        return format(flashPrice);
    }

    private String format(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(value) + "đ";
    }
}
