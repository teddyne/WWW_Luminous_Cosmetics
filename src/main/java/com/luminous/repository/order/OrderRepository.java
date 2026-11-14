package com.luminous.repository.order;

import com.luminous.model.enums.OrderStatus;
import com.luminous.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Orders, Integer>, JpaSpecificationExecutor<Orders> {

    /** Tim don theo ma don (ma duy nhat cua don). */
    Optional<Orders> findByOrderCode(String orderCode);

    long countByStatus(OrderStatus status);

    List<Orders> findByUserIdOrderByOrderDateDesc(Integer userId);

    /** Don DELIVERED da qua han 3 ngay: job tu dong hoan thanh chay moi gio. */
    List<Orders> findByStatusAndDeliveredAtLessThanEqual(OrderStatus status, LocalDateTime cutoff);

    /** Chi lay don cua chinh khach nay (khong cho xem / sua don cua nguoi khac). */
    Optional<Orders> findByIdAndUserId(Integer id, Integer userId);

    List<Orders> findByUserIdAndStatusInOrderByOrderDateDesc(Integer userId, List<OrderStatus> statuses);

    long countByUserId(Integer userId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Orders o WHERE o.status = :status")
    BigDecimal completedRevenue(@Param("status") OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Orders o WHERE o.user.id = :userId AND o.status = :status")
    BigDecimal completedRevenueByUser(@Param("userId") Integer userId, @Param("status") OrderStatus status);


    @Query("SELECT DATE(o.orderDate), COALESCE(SUM(o.totalAmount), 0) FROM Orders o"
            + " WHERE o.status = :status AND o.orderDate >= :from AND o.orderDate < :to"
            + " GROUP BY DATE(o.orderDate) ORDER BY DATE(o.orderDate)")
    List<Object[]> dailyRevenue(@Param("status") OrderStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT YEAR(o.orderDate), MONTH(o.orderDate), COALESCE(SUM(o.totalAmount), 0) FROM Orders o"
            + " WHERE o.status = :status AND o.orderDate >= :from AND o.orderDate < :to"
            + " GROUP BY YEAR(o.orderDate), MONTH(o.orderDate)"
            + " ORDER BY YEAR(o.orderDate), MONTH(o.orderDate)")
    List<Object[]> monthlyRevenue(@Param("status") OrderStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
