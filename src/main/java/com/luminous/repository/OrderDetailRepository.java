package com.luminous.repository;

import com.luminous.model.OrderDetail;
import com.luminous.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {
    /** San pham sap theo tong so luong da ban (tu OrderDetail that trong DB, khong phai so gia). */
    @Query("SELECT od.variant.product FROM OrderDetail od " +
           "GROUP BY od.variant.product ORDER BY SUM(od.quantity) DESC")
    List<Product> findBestSellers();

    @Query("SELECT COALESCE(SUM(od.quantity), 0) FROM OrderDetail od " +
           "WHERE od.variant.product.id = :productId")
    long soldCount(@Param("productId") Integer productId);
}
