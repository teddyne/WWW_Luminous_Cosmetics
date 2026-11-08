package com.luminous.repository.cart;

import com.luminous.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Integer> {

    /** Moi user toi da 1 gio (userID UNIQUE). */
    Optional<Cart> findByUserId(Integer userId);
}
