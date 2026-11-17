package com.luminous.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luminous.model.AiOrderDraft;
import com.luminous.model.ProductVariant;
import com.luminous.model.User;
import com.luminous.repository.ai.AiOrderDraftRepository;
import com.luminous.service.product.ProductService;
import com.luminous.service.shop.CartService;
import com.luminous.service.shop.CheckoutService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiOrderDraftService {

    public record DraftItem(Integer variantId, int quantity, BigDecimal unitPrice) {
    }

    public record DraftView(Long id, List<DraftItem> items, BigDecimal subtotal,
                            BigDecimal shippingFee, BigDecimal total,
                            LocalDateTime expiresAt, String status) {
    }

    private static final Logger log = LoggerFactory.getLogger(AiOrderDraftService.class);

    private final AiOrderDraftRepository repository;
    private final ProductService productService;
    private final CartService cartService;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.draft.ttl-minutes:30}")
    private int ttlMinutes;

    @Transactional
    public DraftView createDraft(Integer userId, String sessionId, List<DraftItem> requested) {
        List<DraftItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (DraftItem wanted : requested) {
            if (wanted == null || wanted.variantId() == null || wanted.quantity() <= 0) {
                continue;
            }
            Optional<ProductVariant> variant = productService.findVariant(wanted.variantId());
            if (variant.isEmpty() || !Boolean.TRUE.equals(variant.get().getIsActive())) {
                continue;
            }
            ProductVariant v = variant.get();
            int stock = v.getStockQuantity() == null ? 0 : v.getStockQuantity();
            if (stock <= 0) {
                continue;
            }
            int qty = Math.min(wanted.quantity(), stock);
            BigDecimal unitPrice = v.displayPrice();
            items.add(new DraftItem(v.getId(), qty, unitPrice));
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(qty)));
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("Không có sản phẩm nào còn hàng để soạn đơn nháp");
        }
        BigDecimal shippingFee = CheckoutService.SHIPPING_FEE;
        AiOrderDraft draft = new AiOrderDraft();
        draft.setUserId(userId);
        draft.setSessionId(sessionId);
        draft.setItemsJson(toJson(items));
        draft.setSubtotal(subtotal);
        draft.setShippingFee(shippingFee);
        draft.setTotal(subtotal.add(shippingFee));
        draft.setStatus(AiOrderDraft.STATUS_DRAFT);
        draft.setExpiresAt(LocalDateTime.now().plusMinutes(ttlMinutes));
        repository.save(draft);
        return toView(draft, items);
    }

    @Transactional(readOnly = true)
    public Optional<DraftView> findDraft(Long id, Integer userId, String sessionId) {
        Optional<AiOrderDraft> found = userId != null
                ? repository.findByIdAndUserId(id, userId)
                : repository.findByIdAndSessionId(id, sessionId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        AiOrderDraft draft = found.get();
        if (draft.isExpired(LocalDateTime.now())) {
            return Optional.empty();
        }
        return Optional.of(toView(draft, parseItems(draft.getItemsJson())));
    }

    @Transactional
    public DraftView confirmDraft(Long id, User user, HttpSession session) {
        String sessionId = session.getId();
        Optional<AiOrderDraft> found = user != null
                ? repository.findByIdAndUserId(id, user.getId())
                : repository.findByIdAndSessionId(id, sessionId);
        AiOrderDraft draft = found.orElseThrow(() -> new IllegalStateException("Không tìm thấy đơn nháp"));
        if (draft.isExpired(LocalDateTime.now())) {
            draft.setStatus(AiOrderDraft.STATUS_EXPIRED);
            repository.save(draft);
            throw new IllegalStateException("Đơn nháp đã hết hạn sau 30 phút, vui lòng soạn đơn mới");
        }
        if (!AiOrderDraft.STATUS_DRAFT.equals(draft.getStatus())) {
            throw new IllegalStateException("Đơn nháp này không còn hiệu lực");
        }
        List<DraftItem> items = parseItems(draft.getItemsJson());
        for (DraftItem item : items) {
            boolean added = user != null
                    ? cartService.addItem(user, item.variantId(), item.quantity())
                    : cartService.addGuestItem(session, item.variantId(), item.quantity());
            if (!added) {
                log.warn("Không chuyển được variant {} vào giỏ từ đơn nháp {}", item.variantId(), id);
            }
        }
        draft.setStatus(AiOrderDraft.STATUS_CONFIRMED);
        repository.save(draft);
        return toView(draft, items);
    }

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void cleanupExpiredDrafts() {
        int marked = repository.markExpired(LocalDateTime.now());
        if (marked > 0) {
            log.info("Đã đánh dấu hết hạn {} đơn nháp AI", marked);
        }
    }

    private DraftView toView(AiOrderDraft draft, List<DraftItem> items) {
        return new DraftView(draft.getId(), items, draft.getSubtotal(),
                draft.getShippingFee(), draft.getTotal(), draft.getExpiresAt(), draft.getStatus());
    }

    private String toJson(List<DraftItem> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được đơn nháp", e);
        }
    }

    private List<DraftItem> parseItems(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<DraftItem>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }
}
