package com.luminous.repository.order;

import com.luminous.model.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Integer> {

    boolean existsByOrderIdAndType(Integer orderId, String type);

    List<PointTransaction> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
