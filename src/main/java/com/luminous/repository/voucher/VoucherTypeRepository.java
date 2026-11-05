package com.luminous.repository.voucher;

import com.luminous.model.VoucherType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VoucherTypeRepository extends JpaRepository<VoucherType, Integer> {

    Optional<VoucherType> findByTypeName(String typeName);
}
