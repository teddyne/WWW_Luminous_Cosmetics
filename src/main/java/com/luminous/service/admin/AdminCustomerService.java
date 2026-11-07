package com.luminous.service.admin;

import com.luminous.dto.CustomerCreateForm;
import com.luminous.dto.CustomerRow;
import com.luminous.dto.OrderRow;
import com.luminous.dto.ReviewRow;
import com.luminous.model.enums.OrderStatus;
import com.luminous.model.Role;
import com.luminous.model.User;
import com.luminous.repository.RoleRepository;
import com.luminous.repository.UserRepository;
import com.luminous.repository.order.OrderDetailRepository;
import com.luminous.repository.order.OrderRepository;
import com.luminous.repository.review.ReviewRepository;
import com.luminous.service.ProductImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminCustomerService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final OrderRepository orders;
    private final OrderDetailRepository orderDetails;
    private final ReviewRepository reviews;
    private final PasswordEncoder passwords;

    public AdminCustomerService(UserRepository users,
                                RoleRepository roles,
                                OrderRepository orders,
                                OrderDetailRepository orderDetails,
                                ReviewRepository reviews,
                                PasswordEncoder passwords) {
        this.users = users;
        this.roles = roles;
        this.orders = orders;
        this.orderDetails = orderDetails;
        this.reviews = reviews;
        this.passwords = passwords;
    }

    public Page<CustomerRow> page(String q, String role, Boolean active, String sort,
                              BigDecimal minSpent, Integer minOrders, int page, int size) {
        List<User> all = users.findAll(spec(q, role, active), Sort.by("createdAt").descending());
        List<CustomerRow> rows = all.stream()
                .map(this::toRow)
                .filter(row -> (minSpent == null || row.totalSpent().compareTo(minSpent) >= 0)
                        && (minOrders == null || row.orderCount() >= minOrders))
                .sorted(comparator(sort))
                .toList();
        int from = Math.min(page * size, rows.size());
        int to = Math.min(from + size, rows.size());
        return new PageImpl<>(rows.subList(from, to), PageRequest.of(page, size), rows.size());
    }

    private Comparator<CustomerRow> comparator(String sort) {
        return switch (sort == null ? "newest" : sort) {
            case "oldest" -> Comparator.comparing(CustomerRow::createdAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "spentDesc" -> Comparator.comparing(CustomerRow::totalSpent,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case "spentAsc" -> Comparator.comparing(CustomerRow::totalSpent,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "ordersDesc" -> Comparator.comparingLong(CustomerRow::orderCount).reversed();
            case "ordersAsc" -> Comparator.comparingLong(CustomerRow::orderCount);
            default -> Comparator.comparing(CustomerRow::createdAt,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }

    private Specification<User> spec(String q, String role, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> and = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                and.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("phone")), like)));
            }
            if (role != null && !role.isBlank()) {
                and.add(cb.equal(root.get("role").get("name"), role.trim()));
            }
            if (active != null) {
                and.add(cb.equal(root.get("isActive"), active));
            }
            return and.isEmpty() ? null : cb.and(and.toArray(Predicate[]::new));
        };
    }

    @Transactional
    public int bulkSetActive(List<Integer> ids, boolean active, Integer adminId) {
        int changed = 0;
        for (Integer id : ids) {
            if (adminId != null && adminId.equals(id)) {
                continue;
            }
            User user = users.findById(id).orElse(null);
            if (user == null || Boolean.valueOf(active).equals(user.getIsActive())) {
                continue;
            }
            user.setIsActive(active);
            users.save(user);
            changed++;
        }
        return changed;
    }

    public CustomerRow detailRow(Integer id) {
        return toRow(find(id));
    }

    public List<OrderRow> ordersOf(Integer userId) {
        return orders.findByUserIdOrderByOrderDateDesc(userId).stream()
                .map(order -> new OrderRow(
                        order.getId(),
                        order.getOrderCode(),
                        order.getRecipientName(),
                        order.getRecipientPhone(),
                        order.getOrderDate(),
                        order.getTotalAmount(),
                        order.getStatus(),
                        AdminOrderService.statusLabel(order.getStatus()),
                        (int) orderDetails.countByOrderId(order.getId()),
                        order.getSalesChannel()))
                .toList();
    }

    public List<ReviewRow> reviewsOf(Integer userId) {
        String customerName = find(userId).getFullName();
        return reviews.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(review -> {
                    var product = review.getOrderDetail().getVariant().getProduct();
                    return new ReviewRow(
                            review.getId(),
                            customerName,
                            product.getName(),
                            ProductImage.imageFor(product),
                            review.getRating(),
                            review.getComment(),
                            review.getCreatedAt(),
                            Boolean.TRUE.equals(review.getIsVisible()),
                            Boolean.TRUE.equals(review.getIsAnonymous()));
                })
                .toList();
    }

    @Transactional
    public Integer create(CustomerCreateForm form) {
        String email = form.getEmail().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được dùng bởi tài khoản khác");
        }
        String phone = form.getPhone() == null || form.getPhone().isBlank() ? null : form.getPhone().trim();
        if (phone != null && users.findByPhone(phone).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại đã được dùng bởi tài khoản khác");
        }
        Role role = roles.findByName(form.getRoleName())
                .orElseThrow(() -> new IllegalArgumentException("Vai trò không tồn tại"));
        User user = new User();
        user.setFullName(form.getFullName().trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwords.encode(form.getPassword()));
        user.setRole(role);
        user.setPointBalance(0);
        user.setIsActive(true);
        users.save(user);
        return user.getId();
    }

    @Transactional
    public void toggleActive(Integer id, Integer adminId) {
        if (id.equals(adminId)) {
            throw new IllegalArgumentException("Không thể khóa chính tài khoản của mình");
        }
        User user = find(id);
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        users.save(user);
    }

    @Transactional
    public void update(Integer id, String fullName, String phone, String roleName, Integer adminId) {
        User user = find(id);
        String cleanPhone = phone == null || phone.isBlank() ? null : phone.trim();
        if (cleanPhone != null && users.findByPhone(cleanPhone)
                .map(other -> !other.getId().equals(id)).orElse(false)) {
            throw new IllegalArgumentException("Số điện thoại đã được dùng bởi tài khoản khác");
        }
        if (id.equals(adminId) && !"ROLE_ADMIN".equals(roleName)) {
            throw new IllegalArgumentException("Không thể tự hạ quyền quản trị của mình");
        }
        Role role = roles.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Vai trò không tồn tại"));
        user.setFullName(fullName.trim());
        user.setPhone(cleanPhone);
        user.setRole(role);
        users.save(user);
    }

    private User find(Integer id) {
        return users.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
    }

    private CustomerRow toRow(User user) {
        BigDecimal spent = orders.completedRevenueByUser(user.getId(), OrderStatus.COMPLETED);
        return new CustomerRow(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getPointBalance() == null ? 0 : user.getPointBalance(),
                Boolean.TRUE.equals(user.getIsActive()),
                user.roleName(),
                user.getCreatedAt(),
                orders.countByUserId(user.getId()),
                spent == null ? BigDecimal.ZERO : spent);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
