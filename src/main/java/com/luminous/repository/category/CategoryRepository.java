package com.luminous.repository.category;

import com.luminous.model.Category;
import com.luminous.model.enums.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer>, JpaSpecificationExecutor<Category> {

    List<Category> findByStatusOrderByIdAsc(CategoryStatus status);

    List<Category> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    Optional<Category> findByNameIgnoreCase(String name);

}
