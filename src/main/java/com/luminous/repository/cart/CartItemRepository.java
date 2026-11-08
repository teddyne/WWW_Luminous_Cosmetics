package com.luminous.repository.cart;

import com.luminous.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    List<CartItem> findByCartIdOrderByIdAsc(Integer cartId);

    Optional<CartItem> findByCartIdAndVariantId(Integer cartId, Integer variantId);

    void deleteByCartIdAndVariantId(Integer cartId, Integer variantId);
}
