package com.luminous.repository;

import com.luminous.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByStatusOrderByIdAsc(String status);
}
