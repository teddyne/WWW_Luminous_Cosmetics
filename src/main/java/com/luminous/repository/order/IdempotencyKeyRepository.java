package com.luminous.repository.order;

import com.luminous.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    Optional<IdempotencyKey> findByKeyAndUserId(String key, Integer userId);

    void deleteByKeyAndUserId(String key, Integer userId);
}
