package com.luminous.service.ai;

import com.luminous.dto.CartLine;
import com.luminous.dto.ProductCard;
import com.luminous.dto.ai.AiProductView;
import com.luminous.model.OrderDetail;
import com.luminous.model.Orders;
import com.luminous.model.ProductVariant;
import com.luminous.model.User;
import com.luminous.repository.order.OrderRepository;
import com.luminous.service.ai.AiOrderDraftService.DraftItem;
import com.luminous.service.ai.AiOrderDraftService.DraftView;
import com.luminous.service.ai.AiVectorService.PolicyHit;
import com.luminous.service.product.ProductService;
import com.luminous.service.shop.CartService;
import com.luminous.service.shop.CheckoutService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AiTools {

    private final ProductService productService;
    private final CartService cartService;
    private final CheckoutService checkoutService;
    private final OrderRepository orderRepository;
    private final AiOrderDraftService draftService;
    private final AiFallbackService fallbackService;
    private final AiVectorService vectorService;
    private final AiPolicyDocuments policyDocuments;
    private final AiRequestContext context;

    @Tool(description = "Tìm sản phẩm trong shop theo từ khóa (tên, thương hiệu, loại sản phẩm) và/hoặc giá tối đa (VND). Trả về tối đa limit sản phẩm kèm giá và tồn kho thật trong DB.")
    public List<AiProductView> searchProducts(
            @ToolParam(description = "Từ khóa: tên sản phẩm, thương hiệu hoặc loại sản phẩm, ví dụ: sữa rửa mặt, serum, Klairs") String keyword,
            @ToolParam(description = "Giá tối đa tính theo VND, ví dụ 500000. Bỏ trống được nếu không lọc giá.", required = false) BigDecimal maxPrice,
            @ToolParam(description = "true = sắp xếp giá rẻ nhất trước. Bỏ trống được, mặc định false.", required = false) Boolean cheapestFirst,
            @ToolParam(description = "Số sản phẩm tối đa trả về. Bỏ trống được, mặc định 5.", required = false) Integer limit) {
        try {
            boolean cheap = Boolean.TRUE.equals(cheapestFirst);
            int max = limit == null || limit <= 0 ? 5 : limit;
            List<ProductCard> found = productService.search(keyword, null, null, null, maxPrice, null,
                    cheap ? "price_asc" : null, false);
            List<AiProductView> views = found.stream()
                    .limit(max)
                    .map(AiProductView::from)
                    .toList();
            context.lastProducts(views);
            return views;
        } catch (Exception e) {
            return List.of();
        }
    }

    @Tool(description = "Lấy chi tiết 1 sản phẩm theo id: các biến thể, giá, giá khuyến mãi, tồn kho, thành phần, loại da phù hợp, hướng dẫn sử dụng và đánh giá trung bình.")
    public String getProductDetail(
            @ToolParam(description = "Id sản phẩm") Integer productId) {
        try {
            Optional<ProductCard> card = productService.detailCard(productId);
            if (card.isEmpty()) {
                return "Không tìm thấy sản phẩm có id " + productId + " trong shop.";
            }
            ProductCard product = card.get();
            StringBuilder text = new StringBuilder();
            text.append(product.name()).append(" (").append(product.brand()).append(")");
            if (product.getProduct().getDescription() != null) {
                text.append("\nMô tả: ").append(product.getProduct().getDescription());
            }
            if (product.getProduct().getUsageInstructions() != null) {
                text.append("\nCách dùng: ").append(product.getProduct().getUsageInstructions());
            }
            List<ProductCard> variants = productService.activeVariantCards(productId);
            for (ProductCard variantCard : variants) {
                ProductVariant variant = variantCard.getVariant();
                text.append("\n- Biến thể ").append(variantCard.sizeLabel())
                        .append(": giá ").append(variantCard.displayPriceText());
                if (variantCard.onSale()) {
                    text.append(" (giá gốc ").append(variantCard.priceText()).append(")");
                }
                text.append(", tồn kho ").append(variant.getStockQuantity());
                if (variant.getIngredients() != null && !variant.getIngredients().isBlank()) {
                    text.append(", thành phần: ").append(variant.getIngredients());
                }
                if (variant.getSuitableSkinType() != null && !variant.getSuitableSkinType().isBlank()) {
                    text.append(", phù hợp da: ").append(variant.getSuitableSkinType());
                }
            }
            text.append("\n").append(fallbackService.reviewSummaryText(productId, product.name()));
            return text.toString();
        } catch (Exception e) {
            return "Không lấy được chi tiết sản phẩm lúc này, bạn thử lại nhé.";
        }
    }

    @Tool(description = "So sánh nhiều sản phẩm với nhau theo giá, tồn kho và đánh giá trung bình. Chỉ so sánh sản phẩm có trong shop.")
    public String compareProducts(
            @ToolParam(description = "Danh sách id sản phẩm cần so sánh, tối đa 3 sản phẩm") List<Integer> productIds) {
        try {
            if (productIds == null || productIds.isEmpty()) {
                return "Chưa có sản phẩm nào để so sánh.";
            }
            StringBuilder text = new StringBuilder("So sánh sản phẩm:\n");
            for (Integer id : productIds.stream().limit(3).toList()) {
                Optional<ProductCard> card = productService.detailCard(id);
                if (card.isEmpty()) {
                    text.append("\n- Sản phẩm id ").append(id).append(": không tìm thấy trong shop.");
                    continue;
                }
                ProductCard product = card.get();
                text.append("\n- ").append(product.name()).append(" (").append(product.brand()).append("): giá ")
                        .append(product.displayPriceText());
                if (product.onSale()) {
                    text.append(" (giá gốc ").append(product.priceText()).append(")");
                }
                int stock = productService.activeVariantCards(id).stream()
                        .mapToInt(v -> v.getVariant().getStockQuantity() == null ? 0 : v.getVariant().getStockQuantity())
                        .sum();
                text.append(", tồn kho ").append(stock);
                if (product.getReviewCount() > 0) {
                    text.append(", đánh giá ").append(String.format("%.1f", product.getRating()))
                            .append(" sao (").append(product.getReviewCount()).append(" đánh giá)");
                } else {
                    text.append(", chưa có đánh giá");
                }
            }
            return text.toString();
        } catch (Exception e) {
            return "Không so sánh được sản phẩm lúc này, bạn thử lại nhé.";
        }
    }

    @Tool(description = "Thêm sản phẩm vào giỏ hàng của khách (khách vãng lai lưu theo phiên, khách đã đăng nhập lưu theo tài khoản). Thêm vào giỏ chưa phải đặt hàng.")
    public String addToCart(
            @ToolParam(description = "Id sản phẩm") Integer productId,
            @ToolParam(description = "Số lượng. Bỏ trống được, mặc định 1.", required = false) Integer quantity) {
        try {
            Optional<ProductCard> card = productService.detailCard(productId);
            if (card.isEmpty()) {
                return "Không tìm thấy sản phẩm có id " + productId + " trong shop.";
            }
            List<ProductCard> variants = productService.activeVariantCards(productId);
            ProductCard chosen = null;
            for (ProductCard variantCard : variants) {
                Integer stock = variantCard.getVariant().getStockQuantity();
                if (stock != null && stock > 0) {
                    chosen = variantCard;
                    break;
                }
            }
            if (chosen == null) {
                return "Sản phẩm \"" + card.get().name() + "\" hiện đã hết hàng.";
            }
            int qty = quantity == null ? 1 : Math.max(1, quantity);
            User user = context.user();
            HttpSession session = context.session();
            if (user == null && session == null) {
                return "Phiên chat của bạn đã hết, bạn tải lại trang rồi thử thêm vào giỏ lại nhé.";
            }
            boolean added = user != null
                    ? cartService.addItem(user, chosen.getVariant().getId(), qty)
                    : cartService.addGuestItem(session, chosen.getVariant().getId(), qty);
            if (!added) {
                return "Không thêm được \"" + card.get().name() + "\" vào giỏ hàng, sản phẩm có thể vừa hết hàng.";
            }
            return "Đã thêm " + qty + " x \"" + card.get().name() + "\" (" + chosen.sizeLabel() + ", "
                    + chosen.displayPriceText() + ") vào giỏ hàng. Bạn có thể xem lại giỏ ở trang Giỏ hàng.";
        } catch (Exception e) {
            return "Không thêm được sản phẩm vào giỏ hàng lúc này, bạn thử lại nhé.";
        }
    }

    @Tool(description = "Soạn đơn nháp từ các sản phẩm đang có trong giỏ hàng. Đơn nháp chỉ lưu tạm 30 phút và KHÔNG phải đơn hàng thật: khách phải bấm xác nhận ở trang giỏ hàng thì đơn mới được tạo.")
    public String createDraftOrder() {
        try {
            User user = context.user();
            HttpSession session = context.session();
            if (user == null && session == null) {
                return "Phiên chat của bạn đã hết, bạn tải lại trang rồi thử soạn đơn lại nhé.";
            }
            List<CartLine> lines = user != null ? cartService.lines(user) : cartService.guestLines(session);
            List<DraftItem> items = new ArrayList<>();
            for (CartLine line : lines) {
                if (line.available()) {
                    items.add(new DraftItem(line.card().getVariant().getId(), line.quantity(),
                            line.card().displayPrice()));
                }
            }
            if (items.isEmpty()) {
                return "Giỏ hàng đang trống hoặc toàn sản phẩm hết hàng nên chưa soạn được đơn nháp. Hãy thêm sản phẩm vào giỏ trước.";
            }
            Integer userId = user != null ? user.getId() : null;
            DraftView draft = draftService.createDraft(userId, session.getId(), items);
            context.lastDraft(draft);
            return "Đã soạn đơn nháp #" + draft.id() + ": tạm tính " + CartLine.formatVnd(draft.subtotal())
                    + ", phí vận chuyển " + CartLine.formatVnd(draft.shippingFee())
                    + ", tổng " + CartLine.formatVnd(draft.total())
                    + ". Đơn nháp hết hạn sau 30 phút và chưa phải đơn hàng thật. Bạn bấm nút chuyển vào giỏ để xem lại và xác nhận đặt hàng.";
        } catch (Exception e) {
            return "Không soạn được đơn nháp lúc này, bạn thử lại nhé.";
        }
    }

    @Tool(description = "Tra cứu đơn hàng của khách đã đăng nhập theo mã đơn (ví dụ LM20260901001) hoặc id đơn. Chỉ tra được đơn của đúng tài khoản đang chat; khách chưa đăng nhập không tra được đơn.")
    public String lookupOrder(
            @ToolParam(description = "Mã đơn (LM...) hoặc id đơn dạng số") String orderRef) {
        try {
            User user = context.user();
            if (user == null) {
                return "Tra cứu đơn hàng yêu cầu đăng nhập. Bạn đăng nhập rồi hỏi lại mã đơn nhé.";
            }
            if (orderRef == null || orderRef.isBlank()) {
                return "Bạn cho mình mã đơn (ví dụ LM20260901001) hoặc id đơn để tra cứu.";
            }
            String ref = orderRef.trim();
            Optional<Orders> found;
            if (ref.toUpperCase().startsWith("LM")) {
                found = checkoutService.findByCode(ref);
                if (found.isPresent() && (found.get().getUser() == null
                        || !found.get().getUser().getId().equals(user.getId()))) {
                    return "Không tìm thấy đơn hàng có mã " + ref + " trong tài khoản của bạn.";
                }
            } else {
                try {
                    found = orderRepository.findByIdAndUserId(Integer.parseInt(ref), user.getId());
                } catch (NumberFormatException e) {
                    return "Mã đơn không hợp lệ. Mã đơn của shop có dạng LM + ngày giờ, ví dụ LM20260901001.";
                }
            }
            if (found.isEmpty()) {
                return "Không tìm thấy đơn hàng " + ref + " trong tài khoản của bạn.";
            }
            Orders order = found.get();
            StringBuilder text = new StringBuilder();
            text.append("Đơn ").append(order.getOrderCode()).append(": trạng thái ")
                    .append(order.getStatus().label()).append(", tổng ")
                    .append(CartLine.formatVnd(order.getTotalAmount()));
            List<OrderDetail> details = checkoutService.detailsOf(order.getId());
            for (OrderDetail detail : details) {
                text.append("\n- ").append(detail.getVariant().getProduct().getName())
                        .append(" x").append(detail.getQuantity())
                        .append(" (").append(CartLine.formatVnd(detail.getTotalPrice())).append(")");
            }
            return text.toString();
        } catch (Exception e) {
            return "Không tra cứu được đơn hàng lúc này, bạn thử lại nhé.";
        }
    }

    @Tool(description = "Tổng hợp đánh giá thật của 1 sản phẩm: điểm trung bình, số lượng đánh giá theo từng mức sao và một số bình luận của khách.")
    public String reviewSummary(
            @ToolParam(description = "Id sản phẩm") Integer productId) {
        try {
            Optional<ProductCard> card = productService.detailCard(productId);
            if (card.isEmpty()) {
                return "Không tìm thấy sản phẩm có id " + productId + " trong shop.";
            }
            return fallbackService.reviewSummaryText(productId, card.get().name());
        } catch (Exception e) {
            return "Không lấy được đánh giá lúc này, bạn thử lại nhé.";
        }
    }

    @Tool(description = "Trả lời câu hỏi về chính sách của shop (vận chuyển, thanh toán, đổi trả, điểm thưởng, voucher, tài khoản, mua tại cửa hàng) dựa trên tài liệu chính sách của shop.")
    public String askPolicy(
            @ToolParam(description = "Câu hỏi về chính sách, ví dụ: phí ship bao nhiêu, đổi trả trong bao lâu") String question) {
        try {
            List<PolicyHit> hits = vectorService.searchPolicies(question, 2);
            if (!hits.isEmpty()) {
                StringBuilder text = new StringBuilder();
                for (PolicyHit hit : hits) {
                    text.append(hit.content()).append("\n");
                }
                return text.toString().trim();
            }
            List<AiPolicyDocuments.PolicyDoc> docs = policyDocuments.search(question, 1);
            if (!docs.isEmpty()) {
                return docs.get(0).content();
            }
            return "Shop chưa có quy định cụ thể cho câu hỏi này. Bạn có thể liên hệ shop qua trang Liên hệ để được tư vấn thêm.";
        } catch (Exception e) {
            return "Không tìm được chính sách lúc này, bạn thử lại nhé.";
        }
    }
}
