package com.luminous.repository.flashsale;

import com.luminous.model.FlashSaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FlashSaleItemRepository extends JpaRepository<FlashSaleItem, Integer> {

    List<FlashSaleItem> findByFlashSaleIdOrderByIdAsc(Integer flashSaleId);

    Optional<FlashSaleItem> findByFlashSaleIdAndVariantId(Integer flashSaleId, Integer variantId);

    void deleteByFlashSaleId(Integer flashSaleId);

    @Query("SELECT i FROM FlashSaleItem i JOIN FETCH i.flashSale f JOIN FETCH i.variant"
            + " WHERE f.status = 'PUBLISHED' AND f.startAt <= :now AND f.endAt >= :now")
    List<FlashSaleItem> findLiveItems(@Param("now") LocalDateTime now);
}
