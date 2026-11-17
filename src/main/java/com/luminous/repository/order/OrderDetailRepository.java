package com.luminous.repository.order;

import com.luminous.model.OrderDetail;
import com.luminous.model.enums.OrderStatus;
import com.luminous.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

    @Query("SELECT od.variant.product FROM OrderDetail od GROUP BY od.variant.product ORDER BY SUM(od.quantity) DESC")
    List<Product> findBestSellers();

    @Query("SELECT COALESCE(SUM(od.quantity), 0) FROM OrderDetail od WHERE od.variant.product.id = :productId")
    long soldCount(@Param("productId") Integer productId);

    @Query("SELECT COALESCE(SUM(od.quantity), 0) FROM OrderDetail od JOIN Orders o ON o.id = od.orderId"
            + " WHERE od.variant.id = :variantId AND o.orderDate >= :start AND o.orderDate <= :end"
            + " AND o.status NOT IN :excluded")
    long soldInPeriod(@Param("variantId") Integer variantId,
                      @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end,
                      @Param("excluded") List<OrderStatus> excluded);

    @Query("SELECT COALESCE(SUM(od.quantity), 0) FROM OrderDetail od JOIN Orders o ON o.id = od.orderId"
            + " WHERE od.variant.id = :variantId AND o.user.id = :userId"
            + " AND o.orderDate >= :start AND o.orderDate <= :end AND o.status NOT IN :excluded")
    long boughtByUserInPeriod(@Param("variantId") Integer variantId,
                              @Param("userId") Integer userId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end,
                              @Param("excluded") List<OrderStatus> excluded);

    List<OrderDetail> findByOrderIdOrderByIdAsc(Integer orderId);

    long countByOrderId(Integer orderId);

    boolean existsByVariant_Product_Id(Integer productId);

    boolean existsByVariantId(Integer variantId);

    @Query("SELECT od.variant.product.id, SUM(od.quantity) FROM OrderDetail od"
            + " JOIN Orders o ON o.id = od.orderId WHERE o.status <> :status"
            + " GROUP BY od.variant.product.id ORDER BY SUM(od.quantity) DESC")
    List<Object[]> topSellerProductIds(@Param("status") OrderStatus status);

    @Query("SELECT od.variant.product.id, SUM(od.quantity) FROM OrderDetail od"
            + " JOIN Orders o ON o.id = od.orderId WHERE o.status <> :status"
            + " GROUP BY od.variant.product.id")
    List<Object[]> soldByProduct(@Param("status") OrderStatus status);

    @Query("SELECT od.variant.product.id, SUM(od.totalPrice) FROM OrderDetail od"
            + " JOIN Orders o ON o.id = od.orderId WHERE o.status <> :status"
            + " GROUP BY od.variant.product.id")
    List<Object[]> revenueByProduct(@Param("status") OrderStatus status);
}
