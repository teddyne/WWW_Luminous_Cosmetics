package com.luminous.repository;

import com.luminous.model.ShopSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ShopSettingRepository extends JpaRepository<ShopSetting, Integer> {

    Optional<ShopSetting> findBySettingKey(String settingKey);
}
