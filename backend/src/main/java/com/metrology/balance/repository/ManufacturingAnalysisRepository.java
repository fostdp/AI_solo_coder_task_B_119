package com.metrology.balance.repository;

import com.metrology.balance.entity.ManufacturingAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManufacturingAnalysisRepository extends JpaRepository<ManufacturingAnalysis, Integer> {

    Page<ManufacturingAnalysis> findByBalanceIdOrderByAnalysisTimeDesc(Integer balanceId, Pageable pageable);

    List<ManufacturingAnalysis> findByBalanceIdOrderByAnalysisTimeDesc(Integer balanceId);

    Optional<ManufacturingAnalysis> findTopByBalanceIdOrderByAnalysisTimeDesc(Integer balanceId);

    List<ManufacturingAnalysis> findByOverallTechnologyGradeOrderByAnalysisTimeDesc(String grade);

    @Query("SELECT m FROM ManufacturingAnalysis m WHERE m.balanceId = :balanceId ORDER BY m.analysisTime DESC")
    List<ManufacturingAnalysis> findLatestByBalanceId(@Param("balanceId") Integer balanceId, Pageable pageable);
}
