package com.luminous.repository.skinquiz;

import com.luminous.model.SkinRoutine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SkinRoutineRepository extends JpaRepository<SkinRoutine, Integer> {
    Optional<SkinRoutine> findByUserId(Integer userId);
}
