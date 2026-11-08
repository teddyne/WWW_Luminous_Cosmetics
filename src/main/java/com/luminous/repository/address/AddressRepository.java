package com.luminous.repository.address;

import com.luminous.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Integer> {

    /** Dia chi cua khach, dia chi mac dinh len truoc. */
    List<Address> findByUserIdOrderByIsDefaultDescIdAsc(Integer userId);

    /** Chi lay dia chi cua chinh khach nay (khong cho dung dia chi cua nguoi khac). */
    Optional<Address> findByIdAndUserId(Integer id, Integer userId);
}
