package com.luminous.service.shop;

import com.luminous.model.IdempotencyKey;
import com.luminous.repository.order.IdempotencyKeyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

/**
 * Giu khoa chong trung. Moi ham chay o transaction RIENG (REQUIRES_NEW)
 * de ket qua claim khong bi cuon theo rollback cua transaction dat hang
 * chinh, va de request trung khong lam hong transaction dang chay.
 */
@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository keys;

    public IdempotencyService(IdempotencyKeyRepository keys) {
        this.keys = keys;
    }

    /**
     * Giu khoa cho 1 don sap tao. Tra ve false neu khoa da co (request lap
     * lai) hoac co 2 request cung luc chen trung khoa (unique constraint).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(String key, Integer userId, String orderCode) {
        if (key == null || key.isBlank() || userId == null) {
            return true; // Khong co khoa -> khong chan (tuong thich form cu).
        }
        if (keys.findByKeyAndUserId(key, userId).isPresent()) {
            return false;
        }
        try {
            keys.save(new IdempotencyKey(key, userId, orderCode));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false; // Request khac vua chen truoc trong tich tac.
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<IdempotencyKey> find(String key, Integer userId) {
        return keys.findByKeyAndUserId(key, userId);
    }

    /**
     * Xoa khoa "mo coi": lan dat truoc da giu khoa nhung that bai truoc khi
     * tao don (het hang, het luot voucher...). Khong co don nao thi cho
     * khach dat lai voi cung khoa thay vi ket luan don da ton tai.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteStale(String key, Integer userId) {
        keys.deleteByKeyAndUserId(key, userId);
    }
}
