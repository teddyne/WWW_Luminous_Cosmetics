package com.luminous.repository.order;

import com.luminous.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    Optional<Payment> findTopByOrderIdOrderByIdDesc(Integer orderId);
}
