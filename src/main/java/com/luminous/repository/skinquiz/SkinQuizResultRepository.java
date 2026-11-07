package com.luminous.repository.skinquiz;

import com.luminous.model.SkinQuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SkinQuizResultRepository extends JpaRepository<SkinQuizResult, Integer> {
    /** Ket qua moi nhat cua user (lam lai trac nghiem se co dong moi). */
    Optional<SkinQuizResult> findFirstByUserIdOrderByCompletedAtDesc(Integer userId);

    List<SkinQuizResult> findByUserIdOrderByCompletedAtDesc(Integer userId);
}
