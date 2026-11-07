package com.luminous.repository.voucher;

import com.luminous.model.VoucherCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VoucherCategoryRepository extends JpaRepository<VoucherCategory, Integer> {

    List<VoucherCategory> findByVoucherId(Integer voucherId);

    void deleteByVoucherId(Integer voucherId);
}
