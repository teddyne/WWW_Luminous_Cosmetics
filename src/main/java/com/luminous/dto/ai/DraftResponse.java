package com.luminous.dto.ai;

import com.luminous.dto.CartLine;
import com.luminous.service.ai.AiOrderDraftService.DraftItem;
import com.luminous.service.ai.AiOrderDraftService.DraftView;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DraftResponse(Long id, List<DraftItemView> items, String subtotalText,
                            String shippingFeeText, String totalText,
                            LocalDateTime expiresAt, String status) {

    public record DraftItemView(Integer variantId, String name, int quantity,
                                String unitPriceText, String lineTotalText) {
    }

    public static DraftResponse from(DraftView draft, java.util.function.Function<Integer, String> nameOf) {
        List<DraftItemView> items = draft.items().stream()
                .map(item -> new DraftItemView(item.variantId(), nameOf.apply(item.variantId()),
                        item.quantity(), CartLine.formatVnd(item.unitPrice()),
                        CartLine.formatVnd(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))))
                .toList();
        return new DraftResponse(draft.id(), items,
                CartLine.formatVnd(draft.subtotal()),
                CartLine.formatVnd(draft.shippingFee()),
                CartLine.formatVnd(draft.total()),
                draft.expiresAt(), draft.status());
    }
}
