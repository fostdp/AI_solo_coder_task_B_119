package com.metrology.balance.modules.monte_carlo.service;

import com.metrology.balance.modules.monte_carlo.model.MonteCarloSimulationResult;
import com.metrology.balance.modules.monte_carlo.model.SimulationParameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MonteCarloSimulationService {

    private static final int DEFAULT_SIMULATION_COUNT = 10000;
    private static final int MAX_SIMULATION_COUNT = 1000000;
    private static final int HISTOGRAM_BINS = 30;
    private static final double COVERAGE_FACTOR = 2.0;
    private static final String ACCURACY_GRADE_E1 = "E1";
    private static final String ACCURACY_GRADE_E2 = "E2";
    private static final String ACCURACY_GRADE_F1 = "F1";
    private static final String ACCURACY_GRADE_F2 = "F2";
    private static final String ACCURACY_GRADE_M1 = "M1";
    private static final String ACCURACY_GRADE_M2 = "M2";

    private final Map<String, MonteCarloSimulationResult> simulationResults = new ConcurrentHashMap<>();

    @Async("monteCarloExecutor")
    public void runBalanceErrorSimulation(String simulationId, List<SimulationParameter> parameters,
                                           int simulationCount) {
        log.info("蒙特卡洛模拟开始: simulationId={}, count={}", simulationId, simulationCount);

        int count = normalizeCount(simulationCount);
        MonteCarloSimulationResult result = MonteCarloSimulationResult.builder()
                .simulationId(simulationId)
                .status("RUNNING")
                .totalSimulations(count)
                .completedSimulations(0)
                .startTime(Instant.now().toEpochMilli())
                .build();
        simulationResults.put(simulationId, result);

        try {
            double[] errors = performSimulation(parameters, count);
            DescriptiveStatistics stats = new DescriptiveStatistics(errors);

            double meanError = stats.getMean();
            double stdDev = stats.getStandardDeviation();
            double expandedUncertainty = stdDev * COVERAGE_FACTOR;

            double[] contributions = calculateContributions(parameters, errors);
            double frictionContrib = contributions[0];
            double armLengthContrib = contributions[1];
            double knifeEdgeContrib = contributions[2];
            double temperatureContrib = contributions[3];

            String accuracyGrade = determineAccuracyGrade(expandedUncertainty);

            int[] histogramCounts = buildHistogram(errors, HISTOGRAM_BINS);
            List<BigDecimal> histogramBins = buildHistogramBins(stats.getMin(), stats.getMax(), HISTOGRAM_BINS);
            List<Integer> histogramCountList = new ArrayList<>();
            for (int hc : histogramCounts) histogramCountList.add(hc);

            List<BigDecimal> percentiles = calculatePercentiles(stats);
            List<BigDecimal> errorSamples = sampleErrors(errors, 100);

            result.setStatus("COMPLETED");
            result.setCompletedSimulations(count);
            result.setEndTime(Instant.now().toEpochMilli());
            result.setDurationMs(result.getEndTime() - result.getStartTime());
            result.setMeanError(bd(meanError, 8));
            result.setStdDeviation(bd(stdDev, 8));
            result.setCombinedUncertainty(bd(stdDev, 8));
            result.setExpandedUncertainty(bd(expandedUncertainty, 8));
            result.setCoverageFactor(bd(COVERAGE_FACTOR));
            result.setFrictionContribution(bd(frictionContrib, 2));
            result.setArmLengthContribution(bd(armLengthContrib, 2));
            result.setKnifeEdgeContribution(bd(knifeEdgeContrib, 2));
            result.setTemperatureContribution(bd(temperatureContrib, 2));
            result.setAccuracyGrade(accuracyGrade);
            result.setPercentiles(percentiles);
            result.setHistogramBins(histogramBins);
            result.setHistogramCounts(histogramCountList);
            result.setErrorSamples(errorSamples);

            Map<String, Object> simParams = new LinkedHashMap<>();
            simParams.put("simulationCount", count);
            simParams.put("coverageFactor", COVERAGE_FACTOR);
            simParams.put("histogramBins", HISTOGRAM_BINS);
            simParams.put("parameterCount", parameters.size());
            result.setSimulationParams(simParams);

            log.info("蒙特卡洛模拟完成: simulationId={}, duration={}ms, stdDev={}",
                    simulationId, result.getDurationMs(), stdDev);

        } catch (Exception e) {
            log.error("蒙特卡洛模拟失败: simulationId={}", simulationId, e);
            result.setStatus("FAILED");
            result.setErrorMessage(e.getMessage());
            result.setEndTime(Instant.now().toEpochMilli());
        }
    }

    public MonteCarloSimulationResult getSimulationResult(String simulationId) {
        return simulationResults.get(simulationId);
    }

    public List<MonteCarloSimulationResult> getRunningSimulations() {
        List<MonteCarloSimulationResult> running = new ArrayList<>();
        for (MonteCarloSimulationResult r : simulationResults.values()) {
            if ("RUNNING".equals(r.getStatus())) {
                running.add(r);
            }
        }
        return running;
    }

    public String generateSimulationId() {
        return "mc_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public List<SimulationParameter> getDefaultBalanceParameters() {
        List<SimulationParameter> params = new ArrayList<>();
        params.add(SimulationParameter.builder()
                .name("frictionCoefficient")
                .distribution("NORMAL")
                .mean(0.0012)
                .stdDev(0.0002)
                .minValue(0.0005)
                .maxValue(0.003)
                .unit("-")
                .sensitivityCoefficient(1.0)
                .build());
        params.add(SimulationParameter.builder()
                .name("armLengthError")
                .distribution("NORMAL")
                .mean(0.0)
                .stdDev(0.05)
                .minValue(-0.2)
                .maxValue(0.2)
                .unit("mm")
                .sensitivityCoefficient(1.0)
                .build());
        params.add(SimulationParameter.builder()
                .name("knifeEdgeRadius")
                .distribution("NORMAL")
                .mean(0.3)
                .stdDev(0.05)
                .minValue(0.1)
                .maxValue(0.8)
                .unit("mm")
                .sensitivityCoefficient(0.5)
                .build());
        params.add(SimulationParameter.builder()
                .name("temperature")
                .distribution("NORMAL")
                .mean(20.0)
                .stdDev(2.0)
                .minValue(10.0)
                .maxValue(30.0)
                .unit("°C")
                .sensitivityCoefficient(0.001)
                .build());
        return params;
    }

    private double[] performSimulation(List<SimulationParameter> parameters, int count) {
        double[] errors = new double[count];
        Random random = new Random(42);

        Map<String, NormalDistribution> distributions = new HashMap<>();
        for (SimulationParameter p : parameters) {
            distributions.put(p.getName(),
                    new NormalDistribution(random.nextLong(), p.getMean(), p.getStdDev()));
        }

        for (int i = 0; i < count; i++) {
            double totalError = 0.0;
            for (SimulationParameter p : parameters) {
                NormalDistribution dist = distributions.get(p.getName());
                double sample = dist.sample();
                sample = Math.max(p.getMinValue(), Math.min(p.getMaxValue(), sample));
                double deviation = sample - p.getMean();
                totalError += deviation * p.getSensitivityCoefficient();
            }
            errors[i] = totalError;
        }

        return errors;
    }

    private double[] calculateContributions(List<SimulationParameter> parameters, double[] errors) {
        double totalVar = 0.0;
        double[] variances = new double[4];

        for (int i = 0; i < parameters.size() && i < 4; i++) {
            SimulationParameter p = parameters.get(i);
            double variance = p.getStdDev() * p.getStdDev() * p.getSensitivityCoefficient() * p.getSensitivityCoefficient();
            variances[i] = variance;
            totalVar += variance;
        }

        double[] contributions = new double[4];
        if (totalVar > 0) {
            for (int i = 0; i < 4; i++) {
                contributions[i] = variances[i] / totalVar * 100.0;
            }
        }

        return contributions;
    }

    private String determineAccuracyGrade(double expandedUncertainty) {
        double relUncertainty = Math.abs(expandedUncertainty) / 100.0;
        if (relUncertainty < 1e-5) return ACCURACY_GRADE_E1;
        if (relUncertainty < 1e-4) return ACCURACY_GRADE_E2;
        if (relUncertainty < 1e-3) return ACCURACY_GRADE_F1;
        if (relUncertainty < 1e-2) return ACCURACY_GRADE_F2;
        if (relUncertainty < 1e-1) return ACCURACY_GRADE_M1;
        return ACCURACY_GRADE_M2;
    }

    private int[] buildHistogram(double[] data, int bins) {
        if (data == null || data.length == 0) return new int[bins];
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double d : data) {
            if (d < min) min = d;
            if (d > max) max = d;
        }
        if (min == max) {
            int[] result = new int[bins];
            result[bins / 2] = data.length;
            return result;
        }
        double binWidth = (max - min) / bins;
        int[] counts = new int[bins];
        for (double d : data) {
            int idx = (int) Math.floor((d - min) / binWidth);
            if (idx >= bins) idx = bins - 1;
            if (idx < 0) idx = 0;
            counts[idx]++;
        }
        return counts;
    }

    private List<BigDecimal> buildHistogramBins(double min, double max, int bins) {
        List<BigDecimal> binList = new ArrayList<>();
        double binWidth = (max - min) / bins;
        for (int i = 0; i <= bins; i++) {
            binList.add(bd(min + i * binWidth, 6));
        }
        return binList;
    }

    private List<BigDecimal> calculatePercentiles(DescriptiveStatistics stats) {
        List<BigDecimal> percentiles = new ArrayList<>();
        double[] percents = {0.1, 2.5, 16.0, 50.0, 84.0, 97.5, 99.9};
        for (double p : percents) {
            percentiles.add(bd(stats.getPercentile(p), 6));
        }
        return percentiles;
    }

    private List<BigDecimal> sampleErrors(double[] errors, int sampleCount) {
        List<BigDecimal> samples = new ArrayList<>();
        int step = Math.max(1, errors.length / sampleCount);
        for (int i = 0; i < errors.length && samples.size() < sampleCount; i += step) {
            samples.add(bd(errors[i], 8));
        }
        return samples;
    }

    private int normalizeCount(int requestedCount) {
        if (requestedCount <= 0) return DEFAULT_SIMULATION_COUNT;
        return Math.min(requestedCount, MAX_SIMULATION_COUNT);
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v);
    }

    private static BigDecimal bd(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP);
    }
}
