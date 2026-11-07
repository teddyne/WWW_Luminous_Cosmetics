package com.luminous.repository.voucher;

import com.luminous.model.VoucherProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VoucherProductRepository extends JpaRepository<VoucherProduct, Integer> {

    List<VoucherProduct> findByVoucherId(Integer voucherId);

    void deleteByVoucherId(Integer voucherId);
}
