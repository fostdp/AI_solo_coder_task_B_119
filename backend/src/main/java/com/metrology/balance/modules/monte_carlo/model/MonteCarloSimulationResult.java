package com.metrology.balance.modules.monte_carlo.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonteCarloSimulationResult {

    private String simulationId;
    private String status;
    private Integer totalSimulations;
    private Integer completedSimulations;
    private Long startTime;
    private Long endTime;
    private Long durationMs;

    private BigDecimal meanError;
    private BigDecimal stdDeviation;
    private BigDecimal combinedUncertainty;
    private BigDecimal expandedUncertainty;
    private BigDecimal coverageFactor;

    private BigDecimal frictionContribution;
    private BigDecimal armLengthContribution;
    private BigDecimal knifeEdgeContribution;
    private BigDecimal temperatureContribution;

    private String accuracyGrade;

    private List<BigDecimal> percentiles;
    private List<BigDecimal> histogramBins;
    private List<Integer> histogramCounts;

    private List<BigDecimal> errorSamples;
    private Map<String, Object> simulationParams;
    private String errorMessage;
}
