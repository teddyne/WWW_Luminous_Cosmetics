package com.luminous.service.notification;

import com.luminous.model.Notification;
import com.luminous.model.Orders;
import com.luminous.model.User;
import com.luminous.repository.notification.NotificationRepository;
import com.luminous.repository.order.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Thong bao cho khach. Cac su kien don hang / diem goi notify() o dung cho
 * no phat sinh (CheckoutService, AdminOrderService, CustomerReviewService).
 * Khach cu chua co thong bao nao thi lan dau mo trang se tu sinh tu cac don
 * hang da co (khong phai du lieu gia, chi tom tat lai don that).
 */
@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final OrderRepository orders;

    public NotificationService(NotificationRepository notifications, OrderRepository orders) {
        this.notifications = notifications;
        this.orders = orders;
    }

    @Transactional
    public Notification notify(Integer userId, String type, String title, String message, String link) {
        if (userId == null) {
            return null;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        return notifications.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> listForUser(Integer userId) {
        return notifications.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Integer userId) {
        return notifications.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAllRead(Integer userId) {
        notifications.markAllRead(userId);
    }

    @Transactional
    public void markRead(Integer userId, Integer notificationId) {
        notifications.findById(notificationId).ifPresent(n -> {
            if (userId.equals(n.getUserId()) && !Boolean.TRUE.equals(n.getIsRead())) {
                n.setIsRead(true);
                notifications.save(n);
            }
        });
    }

    /** Lan dau mo trang Thong bao: sinh 1 thong bao cho moi don da co (toi da 20 don gan nhat). */
    @Transactional
    public void ensureSeededFromOrders(User user) {
        if (notifications.countByUserId(user.getId()) > 0) {
            return;
        }
        List<Orders> list = orders.findByUserIdOrderByOrderDateDesc(user.getId());
        list.stream().limit(20).forEach(order -> {
            Notification n = new Notification();
            n.setUserId(user.getId());
            n.setType("ORDER");
            n.setTitle("Đơn hàng " + order.getOrderCode());
            n.setMessage("Trạng thái hiện tại: " + order.getStatus().label() + ".");
            n.setLink("/account/orders");
            n.setIsRead(false);
            n.setCreatedAt(order.getOrderDate() != null ? order.getOrderDate() : LocalDateTime.now());
            notifications.save(n);
        });
    }
}
