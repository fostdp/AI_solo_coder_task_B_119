package com.metrology.balance.repository;

import com.metrology.balance.entity.CalibrationResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalibrationResultRepository extends JpaRepository<CalibrationResult, Integer> {

    Page<CalibrationResult> findByDeviceIdOrderByCalibrationTimeDesc(Integer deviceId, Pageable pageable);

    Page<CalibrationResult> findByBalanceIdOrderByCalibrationTimeDesc(Integer balanceId, Pageable pageable);

    List<CalibrationResult> findByBalanceIdOrderByCalibrationTimeDesc(Integer balanceId);

    Optional<CalibrationResult> findTopByBalanceIdOrderByCalibrationTimeDesc(Integer balanceId);

    Optional<CalibrationResult> findTopByDeviceIdOrderByCalibrationTimeDesc(Integer deviceId);

    List<CalibrationResult> findByCalibrationGradeOrderByCalibrationTimeDesc(String grade);
}
