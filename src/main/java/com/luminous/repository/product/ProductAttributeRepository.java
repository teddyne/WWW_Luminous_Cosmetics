package com.luminous.repository.product;

import com.luminous.model.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Integer> {

    List<ProductAttribute> findByProductIdOrderByDisplayOrderAsc(Integer productId);

    void deleteByProductId(Integer productId);
}
