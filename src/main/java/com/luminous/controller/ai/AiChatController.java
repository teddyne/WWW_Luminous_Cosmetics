package com.luminous.controller.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luminous.dto.CartLine;
import com.luminous.dto.ProductCard;
import com.luminous.dto.ai.CartAddRequest;
import com.luminous.dto.ai.ChatRequest;
import com.luminous.dto.ai.ChatResponse;
import com.luminous.dto.ai.DraftResponse;
import com.luminous.model.User;
import com.luminous.security.CustomUserDetails;
import com.luminous.service.ai.AiChatHistoryService;
import com.luminous.service.ai.AiChatHistoryService.ChatTurn;
import com.luminous.service.ai.AiChatService;
import com.luminous.service.ai.AiChatService.ChatReply;
import com.luminous.service.ai.AiImageService;
import com.luminous.service.ai.AiImageService.ImageData;
import com.luminous.service.ai.AiOrderDraftService;
import com.luminous.service.ai.AiOrderDraftService.DraftView;
import com.luminous.service.ai.AiVectorService;
import com.luminous.service.product.ProductService;
import com.luminous.service.shop.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService chatService;
    private final AiChatHistoryService historyService;
    private final AiVectorService vectorService;
    private final AiOrderDraftService draftService;
    private final AiImageService imageService;
    private final ProductService productService;
    private final CartService cartService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse chatJson(@RequestBody ChatRequest request, Authentication auth, HttpSession session) {
        String message = request == null || request.message() == null ? "" : request.message().trim();
        ImageData image = null;
        try {
            if (request != null && request.imageUrl() != null && !request.imageUrl().isBlank()) {
                image = imageService.downloadFromUrl(request.imageUrl());
            }
        } catch (Exception e) {
            return ChatResponse.error(e.getMessage());
        }
        if (message.isBlank() && image == null) {
            return ChatResponse.error("Hãy nhập tin nhắn hoặc đính kèm ảnh để trợ lý hỗ trợ bạn.");
        }
        ChatReply reply = chatService.processMessage(currentUser(auth), session, message, image);
        return ChatResponse.ok(reply.reply(), reply.products(),
                toDraftResponse(reply.draft()), reply.fallback());
    }

    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatResponse chatMultipart(@RequestParam(value = "message", required = false) String message,
                                      @RequestPart(value = "image", required = false) MultipartFile image,
                                      Authentication auth, HttpSession session) {
        String text = message == null ? "" : message.trim();
        ImageData imageData = null;
        try {
            if (image != null && !image.isEmpty()) {
                imageData = imageService.readUpload(image);
            }
        } catch (Exception e) {
            return ChatResponse.error(e.getMessage());
        }
        if (text.isBlank() && imageData == null) {
            return ChatResponse.error("Hãy nhập tin nhắn hoặc đính kèm ảnh để trợ lý hỗ trợ bạn.");
        }
        ChatReply reply = chatService.processMessage(currentUser(auth), session, text, imageData);
        return ChatResponse.ok(reply.reply(), reply.products(),
                toDraftResponse(reply.draft()), reply.fallback());
    }

    @PostMapping(value = "/chat/stream", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request, Authentication auth, HttpSession session) {
        SseEmitter emitter = new SseEmitter(120_000L);
        emitter.onTimeout(emitter::complete);
        String message = request == null || request.message() == null ? "" : request.message().trim();
        ImageData image = null;
        try {
            if (request != null && request.imageUrl() != null && !request.imageUrl().isBlank()) {
                image = imageService.downloadFromUrl(request.imageUrl());
            }
        } catch (Exception e) {
            sendStreamError(emitter, e.getMessage());
            return emitter;
        }
        if (message.isBlank() && image == null) {
            sendStreamError(emitter, "Hãy nhập tin nhắn hoặc đính kèm ảnh để trợ lý hỗ trợ bạn.");
            return emitter;
        }
        chatService.streamMessage(currentUser(auth), session, message, image, emitter);
        return emitter;
    }

    private void sendStreamError(SseEmitter emitter, String error) {
        try {
            Map<String, Object> done = new HashMap<>();
            done.put("error", error);
            done.put("fallback", false);
            emitter.send(SseEmitter.event().name("done").data(objectMapper.writeValueAsString(done)));
        } catch (Exception ignored) {
        }
        emitter.complete();
    }

    @GetMapping("/history")
    public Map<String, Object> history(Authentication auth, HttpSession session) {
        User user = currentUser(auth);
        Integer userId = user != null ? user.getId() : null;
        List<ChatTurn> turns = historyService.history(userId, session.getId(), 20);
        List<Map<String, String>> messages = turns.stream()
                .map(t -> Map.of("role", t.role(), "content", t.content()))
                .toList();
        Map<String, Object> body = new HashMap<>();
        body.put("ok", true);
        body.put("messages", messages);
        return body;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> body = new HashMap<>();
        body.put("ok", true);
        body.put("aiEnabled", chatService.isAiEnabled());
        body.put("aiAvailable", chatService.isAiAvailable());
        body.put("mode", chatService.isAiAvailable() ? "gemini" : "fallback");
        body.put("lastError", chatService.getLastGeminiError());
        return body;
    }

    @DeleteMapping("/history")
    public Map<String, Object> clearHistory(Authentication auth, HttpSession session) {
        User user = currentUser(auth);
        Integer userId = user != null ? user.getId() : null;
        historyService.clear(userId, session.getId());
        Map<String, Object> body = new HashMap<>();
        body.put("ok", true);
        return body;
    }

    @PostMapping("/reindex")
    public Map<String, Object> reindex() {
        Map<String, Object> body = new HashMap<>();
        try {
            int count = vectorService.rebuildAll();
            body.put("ok", true);
            body.put("indexed", count);
            body.put("ready", vectorService.isReady());
        } catch (Exception e) {
            body.put("ok", false);
            body.put("error", e.getMessage());
        }
        return body;
    }

    @PostMapping("/cart/add")
    public Map<String, Object> addToCart(@RequestBody CartAddRequest request, Authentication auth, HttpSession session) {
        Map<String, Object> body = new HashMap<>();
        if (request == null || request.productId() == null) {
            body.put("ok", false);
            body.put("error", "Thiếu sản phẩm cần thêm vào giỏ.");
            return body;
        }
        Optional<ProductCard> card = productService.detailCard(request.productId());
        if (card.isEmpty()) {
            body.put("ok", false);
            body.put("error", "Không tìm thấy sản phẩm trong shop.");
            return body;
        }
        ProductCard chosen = null;
        for (ProductCard variantCard : productService.activeVariantCards(request.productId())) {
            Integer stock = variantCard.getVariant().getStockQuantity();
            if (stock != null && stock > 0) {
                chosen = variantCard;
                break;
            }
        }
        if (chosen == null) {
            body.put("ok", false);
            body.put("error", "Sản phẩm \"" + card.get().name() + "\" hiện đã hết hàng.");
            return body;
        }
        User user = currentUser(auth);
        int qty = Math.max(1, request.quantity());
        boolean added = user != null
                ? cartService.addItem(user, chosen.getVariant().getId(), qty)
                : cartService.addGuestItem(session, chosen.getVariant().getId(), qty);
        body.put("ok", added);
        if (added) {
            body.put("productName", card.get().name());
            body.put("cartCount", user != null
                    ? cartService.countItems(user)
                    : cartService.countGuestItems(session));
        } else {
            body.put("error", "Không thêm được san pham vao gio hang.");
        }
        return body;
    }

    @GetMapping("/draft/{id}")
    public Map<String, Object> getDraft(@PathVariable Long id, Authentication auth, HttpSession session) {
        User user = currentUser(auth);
        Integer userId = user != null ? user.getId() : null;
        Optional<DraftView> draft = draftService.findDraft(id, userId, session.getId());
        Map<String, Object> body = new HashMap<>();
        if (draft.isEmpty()) {
            body.put("ok", false);
            body.put("error", "Không tìm thấy đơn nháp hoặc đơn nháp đã hết hạn.");
            return body;
        }
        body.put("ok", true);
        body.put("draft", toDraftResponse(draft.get()));
        return body;
    }

    @PostMapping("/draft/{id}/confirm")
    public Map<String, Object> confirmDraft(@PathVariable Long id, Authentication auth, HttpSession session) {
        Map<String, Object> body = new HashMap<>();
        try {
            DraftView draft = draftService.confirmDraft(id, currentUser(auth), session);
            body.put("ok", true);
            body.put("draft", toDraftResponse(draft));
            body.put("redirectUrl", "/cart");
        } catch (Exception e) {
            body.put("ok", false);
            body.put("error", e.getMessage());
        }
        return body;
    }

    private DraftResponse toDraftResponse(DraftView draft) {
        if (draft == null) {
            return null;
        }
        return DraftResponse.from(draft, variantId -> productService.findVariant(variantId)
                .map(v -> v.getProduct().getName())
                .orElse("Sản phẩm"));
    }

    private User currentUser(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUser();
        }
        return null;
    }
}
