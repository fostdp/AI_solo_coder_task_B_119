package com.metrology.balance.repository;

import com.metrology.balance.entity.CivilizationBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CivilizationBalanceRepository extends JpaRepository<CivilizationBalance, Integer> {

    Optional<CivilizationBalance> findByCivilizationCode(String code);

    List<CivilizationBalance> findAllByOrderByPeriodStartYearAsc();

    List<CivilizationBalance> findByBalanceTypeOrderByPeriodStartYearAsc(String balanceType);

    @Query("SELECT c FROM CivilizationBalance c ORDER BY c.periodStartYear ASC")
    List<CivilizationBalance> findAllForComparison();
}
