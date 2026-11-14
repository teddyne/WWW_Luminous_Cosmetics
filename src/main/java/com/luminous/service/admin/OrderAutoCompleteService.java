package com.luminous.service.admin;

import com.luminous.model.Orders;
import com.luminous.model.enums.OrderStatus;
import com.luminous.repository.order.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Quy tac 3 ngay sau khi giao (theo yeu cau moi, thay cho muc 2 ngay trong
 * mo ta du an cu):
 * - Trong 3 ngay, khach duoc chon: xac nhan da nhan hang, yeu cau tra hang /
 *   hoan tien, danh gia san pham.
 * - Het 3 ngay ma khach chua chon gi: don tu doi sang HOAN THANH (cong diem
 *   nhu thuong) va het han danh gia / tra hang.
 * - Admin KHONG tu doi trang thai thanh HOAN THANH duoc: DELIVERED chi duoc
 *   chuyen sang RETURNED (xem OrderStatus.next); chi co khach xac nhan hoac
 *   job nay moi hoan thanh don.
 */
@Service
public class OrderAutoCompleteService {

    /** Han chon cua khach sau khi giao (ngay). */
    public static final int REVIEW_WINDOW_DAYS = 3;

    private static final Logger log = LoggerFactory.getLogger(OrderAutoCompleteService.class);

    private final OrderRepository orders;
    private final AdminOrderService adminOrders;

    public OrderAutoCompleteService(OrderRepository orders, AdminOrderService adminOrders) {
        this.orders = orders;
        this.adminOrders = adminOrders;
    }

    /** Han danh gia / tra hang / xac nhan nhan hang: ngay giao + 3 ngay (don tai quay: ngay hoan thanh). */
    public static LocalDateTime reviewDeadline(Orders order) {
        LocalDateTime base = order.getDeliveredAt() != null ? order.getDeliveredAt()
                : order.getCompletedAt() != null ? order.getCompletedAt() : order.getOrderDate();
        return base.plusDays(REVIEW_WINDOW_DAYS);
    }

    public static boolean isWithinReviewWindow(Orders order, LocalDateTime now) {
        return !now.isAfter(reviewDeadline(order));
    }

    /** Chay moi gio: don DELIVERED qua 3 ngay khong ai chon thi tu hoan thanh. */
    @Scheduled(initialDelay = 60000, fixedRate = 3600000)
    public void autoCompleteDeliveredOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(REVIEW_WINDOW_DAYS);
        List<Orders> due = orders.findByStatusAndDeliveredAtLessThanEqual(OrderStatus.DELIVERED, cutoff);
        for (Orders order : due) {
            try {
                adminOrders.completeOrder(order.getId());
                log.info("Tu dong hoan thanh don {} (da giao tu {})", order.getOrderCode(), order.getDeliveredAt());
            } catch (RuntimeException e) {
                log.warn("Khong tu dong hoan thanh duoc don {}: {}", order.getOrderCode(), e.getMessage());
            }
        }
    }
}
