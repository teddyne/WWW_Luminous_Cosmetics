package com.luminous.repository.voucher;

import com.luminous.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Integer>, JpaSpecificationExecutor<Voucher> {

    // Tang luot dung nguyen tu: chi tang khi chua vuot usageLimit;
    // tra ve 0 dong = ma da het luot (khong bao gio vuot gioi han).
    @Modifying
    @Query("UPDATE Voucher v SET v.usedCount = v.usedCount + 1 "
            + "WHERE v.id = :id AND v.usedCount < v.usageLimit")
    int incrementUsedCount(@Param("id") Integer id);

    Optional<Voucher> findByVoucherCodeIgnoreCase(String voucherCode);

    boolean existsByVoucherCodeIgnoreCase(String voucherCode);

    /** Voucher dang bat, sap het han truoc (cho trang Kho Voucher cua khach). */
    java.util.List<Voucher> findByIsActiveTrueOrderByEndDateAsc();


    @Query(value = "SELECT COUNT(*) FROM OrderVoucher WHERE voucherID = :voucherId", nativeQuery = true)
    long countOrderVoucherByVoucherId(@Param("voucherId") Integer voucherId);
}
