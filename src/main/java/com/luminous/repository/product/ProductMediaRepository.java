package com.luminous.repository.product;

import com.luminous.model.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductMediaRepository extends JpaRepository<ProductMedia, Integer> {

    List<ProductMedia> findByProductIdOrderByDisplayOrderAsc(Integer productId);

    void deleteByProductId(Integer productId);
}
