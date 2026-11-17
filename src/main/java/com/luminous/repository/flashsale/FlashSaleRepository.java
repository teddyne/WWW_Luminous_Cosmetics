package com.luminous.repository.flashsale;

import com.luminous.model.FlashSale;
import com.luminous.model.enums.FlashSaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface FlashSaleRepository extends JpaRepository<FlashSale, Integer> {

    List<FlashSale> findAllByOrderByStartAtDesc();

    @Query("SELECT f FROM FlashSale f WHERE f.status = :status AND f.startAt <= :now AND f.endAt >= :now ORDER BY f.endAt ASC")
    List<FlashSale> findLive(@Param("status") FlashSaleStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT f FROM FlashSale f WHERE f.status = :status AND f.startAt > :now ORDER BY f.startAt ASC")
    List<FlashSale> findUpcoming(@Param("status") FlashSaleStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT f FROM FlashSale f WHERE "
            + "(:q IS NULL OR :q = '' OR LOWER(f.name) LIKE LOWER(CONCAT('%', :q, '%'))) AND ("
            + " :display = '' OR "
            + " (:display = 'DRAFT' AND f.status = com.luminous.model.enums.FlashSaleStatus.DRAFT) OR "
            + " (:display = 'PAUSED' AND f.status = com.luminous.model.enums.FlashSaleStatus.PAUSED) OR "
            + " (:display = 'UPCOMING' AND f.status = com.luminous.model.enums.FlashSaleStatus.PUBLISHED AND f.startAt > :now) OR "
            + " (:display = 'LIVE' AND f.status = com.luminous.model.enums.FlashSaleStatus.PUBLISHED AND f.startAt <= :now AND f.endAt >= :now) OR "
            + " (:display = 'ENDED' AND f.status = com.luminous.model.enums.FlashSaleStatus.PUBLISHED AND f.endAt < :now)"
            + ") ORDER BY f.startAt DESC")
    Page<FlashSale> search(@Param("q") String q, @Param("display") String display,
                           @Param("now") LocalDateTime now, Pageable pageable);
}
