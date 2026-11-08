package com.luminous.repository.order;

import com.luminous.model.OrderDetail;
import com.luminous.model.enums.OrderStatus;
import com.luminous.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

    @Query("SELECT od.variant.product FROM OrderDetail od GROUP BY od.variant.product ORDER BY SUM(od.quantity) DESC")
    List<Product> findBestSellers();

    @Query("SELECT COALESCE(SUM(od.quantity), 0) FROM OrderDetail od WHERE od.variant.product.id = :productId")
    long soldCount(@Param("productId") Integer productId);

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
