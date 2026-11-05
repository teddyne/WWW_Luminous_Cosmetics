package com.luminous.repository.voucher;

import com.luminous.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Integer>, JpaSpecificationExecutor<Voucher> {

    Optional<Voucher> findByVoucherCodeIgnoreCase(String voucherCode);

    boolean existsByVoucherCodeIgnoreCase(String voucherCode);


    @Query(value = "SELECT COUNT(*) FROM OrderVoucher WHERE voucherID = :voucherId", nativeQuery = true)
    long countOrderVoucherByVoucherId(@Param("voucherId") Integer voucherId);
}
