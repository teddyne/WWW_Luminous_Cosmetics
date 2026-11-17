package com.luminous.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luminous.dto.CartLine;
import com.luminous.dto.ai.AiProductView;
import com.luminous.model.User;
import com.luminous.repository.ai.AiChatMessageRepository;
import com.luminous.service.admin.ShopSettingsService;
import com.luminous.service.ai.AiChatHistoryService.ChatTurn;
import com.luminous.service.ai.AiOrderDraftService.DraftView;
import com.luminous.service.ai.AiVectorService.PolicyHit;
import com.luminous.service.ai.AiVectorService.ProductHit;
import com.luminous.service.ai.AiVectorService.ReviewHit;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiChatService {

    public record ChatReply(String reply, List<AiProductView> products, DraftView draft, boolean fallback) {
    }

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private volatile String lastGeminiError;

    private static final String SYSTEM_PROMPT = """
            Bạn là trợ lý AI của shop mỹ phẩm Luminous, trả lời bằng tiếng Việt có đầy đủ dấu, ngắn gọn và thân thiện.
            Quy tắc bắt buộc:
            - Luôn trả lời bằng tiếng Việt có đầy đủ dấu, không viết tiếng Việt không dấu.
            - Giá, tồn kho, thành phần và đánh giá chỉ lấy từ kết quả tool gọi vào DB, không tự bịa.
            - Chỉ gợi ý sản phẩm có trong shop Luminous. Nếu khách hỏi sản phẩm shop không bán thì nói rõ shop không bán sản phẩm đó, rồi gợi ý sản phẩm tương tự của Luminous nếu có.
            - Thêm vào giỏ chưa phải đặt hàng. Chỉ soạn đơn nháp khi khách đồng ý, đơn nháp hết hạn sau 30 phút và không phải đơn hàng thật. Không bao giờ tự tạo đơn hàng thật.
            - Tra cứu đơn hàng chỉ được khi khách đã đăng nhập, và chỉ tra đơn của đúng tài khoản đó.
            - Không đưa ra chẩn đoán y tế. Tư vấn mỹ phẩm chỉ ở mức tham khảo, khuyên khách thử trước ở vùng da nhỏ nếu da nhạy cảm.
            - Trả lời cả những câu trò chuyện ngắn (chào hỏi, hỏi giờ, cảm ơn, tạm biệt) một cách thân thiện, rồi gợi ý hỗ trợ tiếp về shop. Khi khách hỏi giờ/ngày thì trả lời bằng thời gian hiện tại ở dưới, không nói là không biết.
            """;

    private final ObjectProvider<ChatClient> chatClientProvider;
    private final AiVectorService vectorService;
    private final AiFallbackService fallbackService;
    private final AiChatHistoryService historyService;
    private final AiRequestContext context;
    private final ObjectMapper objectMapper;
    private final ShopSettingsService settings;
    private final AiChatMessageRepository chatMessages;

    @Value("${app.ai.enabled:true}")
    private boolean aiEnabled;

    public ChatReply processMessage(User user, HttpSession session, String message,
                                    AiImageService.ImageData image) {
        String sessionId = session.getId();
        Integer userId = user != null ? user.getId() : null;
        ChatReply blocked = blockedReply(userId, sessionId);
        if (blocked != null) {
            return blocked;
        }
        List<ChatTurn> recentTurns = historyService.history(userId, sessionId, 6);
        historyService.save(userId, sessionId, "USER", message);
        ChatClient chatClient = effectiveAiEnabled() ? chatClientProvider.getIfAvailable() : null;
        if (chatClient == null) {
            AiFallbackService.FallbackAnswer fallback = fallbackService.answer(message, user, session);
            historyService.save(userId, sessionId, "AI", fallback.text());
            return new ChatReply(fallback.text(), fallback.products(), fallback.draft(), true);
        }
        context.begin(user, session);
        try {
            String ragContext = shouldUseRag(message) ? buildRagContext(message) : "Không có thông tin tham khảo thêm.";
            String historyText = buildHistoryText(recentTurns);
            String userText = userTextWithImage(message, image);
            String finalUserText = userText;
            var spec = chatClient.prompt()
                    .system(SYSTEM_PROMPT + currentTimeLine() + historyText
                            + "\nThông tin tham khảo từ shop (chỉ dùng khi liên quan):\n" + ragContext)
                    .user(userSpec -> {
                        userSpec.text(finalUserText);
                        if (image != null) {
                            userSpec.media(MimeTypeUtils.parseMimeType(image.mimeType()),
                                    new ByteArrayResource(image.bytes()));
                        }
                    });
            String reply = spec.call().content();
            if (reply == null || reply.isBlank()) {
                throw new IllegalStateException("Trợ lý AI trả về câu trả lời trống");
            }
            lastGeminiError = null;
            historyService.save(userId, sessionId, "AI", reply);
            return new ChatReply(reply, context.lastProducts(), context.lastDraft(), false);
        } catch (Exception e) {
            lastGeminiError = e.getMessage();
            log.warn("Loi khi goi Gemini ({}), dung che do tra loi khong dung AI", e.getMessage());
            try {
                AiFallbackService.FallbackAnswer fallback = fallbackService.answer(message, user, session);
                saveHistoryQuietly(userId, sessionId, "AI", fallback.text());
                return new ChatReply(fallback.text(), fallback.products(), fallback.draft(), true);
            } catch (Exception fallbackError) {
                log.warn("Khong tra loi du phong duoc: {}", fallbackError.getMessage());
                return new ChatReply("Xin lỗi, trợ lý AI đang bận. Bạn thử gõ tên sản phẩm, thương hiệu"
                        + " hoặc loại sản phẩm cần tìm (ví dụ: sữa rửa mặt, serum, kem chống nắng),"
                        + " hoặc dùng ô tìm kiếm trên thanh menu của shop nhé!",
                        List.of(), null, true);
            }
        } finally {
            context.end();
        }
    }

    private void saveHistoryQuietly(Integer userId, String sessionId, String role, String content) {
        try {
            historyService.save(userId, sessionId, role, content);
        } catch (Exception e) {
            log.warn("Khong luu duoc lich su chat: {}", e.getMessage());
        }
    }

    public void streamMessage(User user, HttpSession session, String message,
                              AiImageService.ImageData image, SseEmitter emitter) {
        Thread worker = new Thread(() -> runStream(user, session, message, image, emitter), "ai-chat-stream");
        worker.setDaemon(true);
        worker.start();
    }

    private void runStream(User user, HttpSession session, String message,
                           AiImageService.ImageData image, SseEmitter emitter) {
        String sessionId = session.getId();
        Integer userId = user != null ? user.getId() : null;
        ChatReply blocked = blockedReply(userId, sessionId);
        if (blocked != null) {
            sendChunk(emitter, blocked.reply());
            sendDone(emitter, blocked, null);
            emitter.complete();
            return;
        }
        List<ChatTurn> recentTurns = historyService.history(userId, sessionId, 6);
        historyService.save(userId, sessionId, "USER", message);
        ChatClient chatClient = effectiveAiEnabled() ? chatClientProvider.getIfAvailable() : null;
        if (chatClient == null) {
            AiFallbackService.FallbackAnswer fallback = fallbackService.answer(message, user, session);
            historyService.save(userId, sessionId, "AI", fallback.text());
            sendChunk(emitter, fallback.text());
            sendDone(emitter, new ChatReply(fallback.text(), fallback.products(), fallback.draft(), true), null);
            emitter.complete();
            return;
        }
        if (needsSyncForTools(message)) {
            try {
                ChatReply reply = processMessage(user, session, message, image);
                sendChunk(emitter, reply.reply());
                sendDone(emitter, reply, null);
            } catch (Exception e) {
                log.warn("Loi khi tra loi dong bo trong stream: {}", e.getMessage());
                sendDone(emitter, null, "Xin lỗi, mình chưa trả lời được lúc này.");
            }
            emitter.complete();
            return;
        }
        context.begin(user, session);
        StringBuilder full = new StringBuilder();
        boolean[] emitted = {false};
        try {
            String ragContext = shouldUseRag(message) ? buildRagContext(message) : "Không có thông tin tham khảo thêm.";
            String historyText = buildHistoryText(recentTurns);
            String userText = userTextWithImage(message, image);
            String finalUserText = userText;
            Iterable<String> chunks = chatClient.prompt()
                    .system(SYSTEM_PROMPT + currentTimeLine() + historyText
                            + "\nThông tin tham khảo từ shop (chỉ dùng khi liên quan):\n" + ragContext)
                    .user(userSpec -> {
                        userSpec.text(finalUserText);
                        if (image != null) {
                            userSpec.media(MimeTypeUtils.parseMimeType(image.mimeType()),
                                    new ByteArrayResource(image.bytes()));
                        }
                    })
                    .stream().content()
                    .toIterable();
            for (String chunk : chunks) {
                if (chunk == null || chunk.isEmpty()) {
                    continue;
                }
                full.append(chunk);
                emitted[0] = true;
                if (!sendChunk(emitter, chunk)) {
                    break;
                }
            }
            String reply = full.toString();
            if (reply.isBlank()) {
                throw new IllegalStateException("Trợ lý AI trả về câu trả lời trống");
            }
            lastGeminiError = null;
            historyService.save(userId, sessionId, "AI", reply);
            sendDone(emitter, new ChatReply(reply, context.lastProducts(), context.lastDraft(), false), null);
            emitter.complete();
        } catch (Exception e) {
            lastGeminiError = e.getMessage();
            log.warn("Loi khi goi Gemini ({}), dung che do tra loi khong dung AI", e.getMessage());
            if (!emitted[0]) {
                try {
                    AiFallbackService.FallbackAnswer fallback = fallbackService.answer(message, user, session);
                    historyService.save(userId, sessionId, "AI", fallback.text());
                    sendChunk(emitter, fallback.text());
                    sendDone(emitter, new ChatReply(fallback.text(), fallback.products(), fallback.draft(), true), null);
                } catch (Exception fallbackError) {
                    log.warn("Loi khi tra loi du phong trong stream: {}", fallbackError.getMessage());
                    sendDone(emitter, null, "Xin lỗi, mình chưa trả lời được lúc này.");
                }
            } else {
                try {
                    historyService.save(userId, sessionId, "AI", full.toString());
                    sendDone(emitter, new ChatReply(full.toString(), context.lastProducts(), context.lastDraft(), false), null);
                } catch (Exception saveError) {
                    log.warn("Loi khi luu lich su stream: {}", saveError.getMessage());
                }
            }
            emitter.complete();
        } finally {
            context.end();
        }
    }

    private static final List<String> SYNC_TOOL_WORDS = List.of(
            "them", "vao gio", "gio hang", "mua", "soan don", "tao don", "len don",
            "dat hang", "don hang", "tra cuu", "don cua", "kiem tra don");

    private boolean needsSyncForTools(String message) {
        String normalized = AiTextUtils.normalize(message);
        return SYNC_TOOL_WORDS.stream().anyMatch(normalized::contains);
    }

    private boolean sendChunk(SseEmitter emitter, String chunk) {
        try {
            emitter.send(SseEmitter.event().name("chunk").data(chunk));
            return true;
        } catch (Exception e) {
            log.warn("Khong gui duoc chunk stream cho khach: {}", e.getMessage());
            return false;
        }
    }

    private void sendDone(SseEmitter emitter, ChatReply reply, String error) {
        try {
            emitter.send(SseEmitter.event().name("done").data(objectMapper.writeValueAsString(donePayload(reply, error))));
        } catch (Exception e) {
            log.warn("Khong gui duoc su kien done cua stream: {}", e.getMessage());
        }
    }

    private Map<String, Object> donePayload(ChatReply reply, String error) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        if (reply == null) {
            body.put("reply", "");
            body.put("products", List.of());
            body.put("draft", null);
            body.put("fallback", false);
            return body;
        }
        body.put("reply", reply.reply());
        body.put("products", reply.products());
        body.put("fallback", reply.fallback());
        if (reply.draft() == null) {
            body.put("draft", null);
        } else {
            Map<String, Object> draft = new HashMap<>();
            draft.put("id", reply.draft().id());
            draft.put("subtotalText", CartLine.formatVnd(reply.draft().subtotal()));
            draft.put("shippingFeeText", CartLine.formatVnd(reply.draft().shippingFee()));
            draft.put("totalText", CartLine.formatVnd(reply.draft().total()));
            body.put("draft", draft);
        }
        return body;
    }

    private String currentTimeLine() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        return "\nThời gian hiện tại (giờ Việt Nam): "
                + now.format(DateTimeFormatter.ofPattern("HH:mm, EEEE dd/MM/yyyy", new Locale("vi", "VN"))) + ".";
    }

    private String userTextWithImage(String message, AiImageService.ImageData image) {
        String userText = message == null ? "" : message;
        if (image != null) {
            userText = userText.isBlank()
                    ? "Hãy nhận diện sản phẩm trong ảnh này, đọc thành phần trên bao bì nếu thấy rõ, và cho biết shop Luminous có bán sản phẩm đó hay sản phẩm tương tự không."
                    : userText + "\n(Khách có đính kèm ảnh để nhờ nhận diện sản phẩm và thành phần trên bao bì.)";
        }
        return userText;
    }

    private boolean shouldUseRag(String message) {
        String normalized = AiTextUtils.normalize(message);
        if (AiFallbackService.isSmallTalk(normalized)) {
            return false;
        }
        if (AiFallbackService.isDraftIntent(normalized)) {
            return false;
        }
        if (normalized.contains("them") && normalized.contains("gio")) {
            return false;
        }
        if (normalized.contains("vao gio")) {
            return false;
        }
        return true;
    }

    /** AI bat khi ca co dinh trong properties lan cai dat trong DB deu bat. */
    private boolean effectiveAiEnabled() {
        return aiEnabled && settings.isAiEnabled();
    }

    /**
     * Tra ve cau tra loi chan khi AI bi tat trong cai dat hoac het luot chat
     * trong ngay; tra ve null neu duoc phep chat tiep. Cau bi chan khong luu
     * vao lich su chat.
     */
    private ChatReply blockedReply(Integer userId, String sessionId) {
        if (!effectiveAiEnabled()) {
            return new ChatReply("Trợ lý AI đang tạm tắt. Bạn dùng ô tìm kiếm trên thanh menu "
                    + "của shop để tìm sản phẩm nhé!", List.of(), null, true);
        }
        int limit = settings.aiDailyLimit();
        if (limit > 0) {
            LocalDateTime startOfDay = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).atStartOfDay();
            long used = userId != null
                    ? chatMessages.countByUserIdAndRoleAndCreatedAtAfter(userId, "USER", startOfDay)
                    : chatMessages.countBySessionIdAndRoleAndCreatedAtAfter(sessionId, "USER", startOfDay);
            if (used >= limit) {
                return new ChatReply("Hôm nay bạn đã dùng hết " + limit + " lượt chat với Luminous AI. "
                        + "Bạn quay lại vào ngày mai nhé, hoặc dùng ô tìm kiếm trên thanh menu của shop!",
                        List.of(), null, true);
            }
        }
        return null;
    }

    public boolean isAiEnabled() {
        return effectiveAiEnabled();
    }

    public boolean isAiAvailable() {
        return effectiveAiEnabled() && chatClientProvider.getIfAvailable() != null;
    }

    public String getLastGeminiError() {
        return lastGeminiError;
    }

    private String buildHistoryText(List<ChatTurn> turns) {
        if (turns == null || turns.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder("\nLịch sử trò chuyện trước đó:\n");
        for (ChatTurn turn : turns) {
            text.append("USER".equals(turn.role()) ? "Khách: " : "Trợ lý: ")
                    .append(turn.content()).append("\n");
        }
        return text.toString();
    }

    private String buildRagContext(String message) {
        StringBuilder contextText = new StringBuilder();
        try {
            List<ProductHit> products = vectorService.searchProducts(message, 5);
            if (!products.isEmpty()) {
                contextText.append("Sản phẩm liên quan (tìm bằng vector): ");
                for (ProductHit hit : products) {
                    contextText.append("id ").append(hit.productId()).append(" ");
                }
                contextText.append("\n");
            }
            List<PolicyHit> policies = vectorService.searchPolicies(message, 2);
            for (PolicyHit hit : policies) {
                contextText.append("Chính sách: ").append(hit.content()).append("\n");
            }
            List<ReviewHit> reviews = vectorService.searchReviews(message, 2);
            for (ReviewHit hit : reviews) {
                contextText.append("Đánh giá: ").append(hit.content()).append("\n");
            }
        } catch (Exception e) {
            log.warn("Khong lay duoc ngu canh RAG: {}", e.getMessage());
        }
        if (contextText.isEmpty()) {
            return "Không có thông tin tham khảo thêm.";
        }
        return contextText.toString();
    }
}
