package com.metrology.balance.repository;

import com.metrology.balance.entity.CalibrationDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalibrationDeviceRepository extends JpaRepository<CalibrationDevice, Integer> {

    Optional<CalibrationDevice> findByDeviceCode(String deviceCode);

    List<CalibrationDevice> findByDeviceTypeOrderByCreatedAtDesc(String deviceType);

    List<CalibrationDevice> findByBalanceTypeOrderByCreatedAtDesc(String balanceType);
}
