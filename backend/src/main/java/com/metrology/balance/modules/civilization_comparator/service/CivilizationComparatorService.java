package com.metrology.balance.modules.civilization_comparator.service;

import com.metrology.balance.dto.CivilizationComparisonResult;
import com.metrology.balance.entity.CivilizationBalance;
import com.metrology.balance.modules.civilization_comparator.model.DataSourceClassification;
import com.metrology.balance.modules.civilization_comparator.model.ExpertValidationResult;
import com.metrology.balance.modules.civilization_comparator.model.StandardizedCategory;
import com.metrology.balance.repository.CivilizationBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CivilizationComparatorService {

    private final CivilizationBalanceRepository civilizationRepository;

    private static final List<String> DEFAULT_DIMENSIONS = Arrays.asList(
            "maxCapacity", "relativePrecision", "materialHardness",
            "armRatioConsistency", "structureComplexity", "durabilityScore"
    );

    private static final Map<String, String> DIMENSION_LABELS = new LinkedHashMap<>();
    private static final Map<String, Double> DIMENSION_EXPERT_CONFIDENCE = new LinkedHashMap<>();

    static {
        DIMENSION_LABELS.put("maxCapacity", "最大称量");
        DIMENSION_LABELS.put("relativePrecision", "相对精度");
        DIMENSION_LABELS.put("materialHardness", "材料硬度");
        DIMENSION_LABELS.put("armRatioConsistency", "臂长一致性");
        DIMENSION_LABELS.put("structureComplexity", "结构复杂度");
        DIMENSION_LABELS.put("durabilityScore", "耐久性");

        DIMENSION_EXPERT_CONFIDENCE.put("maxCapacity", 0.90);
        DIMENSION_EXPERT_CONFIDENCE.put("relativePrecision", 0.75);
        DIMENSION_EXPERT_CONFIDENCE.put("materialHardness", 0.85);
        DIMENSION_EXPERT_CONFIDENCE.put("armRatioConsistency", 0.80);
        DIMENSION_EXPERT_CONFIDENCE.put("structureComplexity", 0.65);
        DIMENSION_EXPERT_CONFIDENCE.put("durabilityScore", 0.60);
    }

    private static final List<String> EASTERN_CIVILIZATIONS = Arrays.asList(
            "CHN-WARRING", "CHN-WEST-HAN", "CHN-TANG", "CHN-MING", "INDIA", "PERSIA"
    );

    private static final List<String> WESTERN_CIVILIZATIONS = Arrays.asList(
            "EGYPT", "BABYLON", "GREECE", "ROME", "EUROPE-MED", "EUROPE-RENAISS"
    );

    public CivilizationComparisonResult compareCivilizations(List<String> civilizationCodes) {
        List<CivilizationBalance> civilizations = loadCivilizations(civilizationCodes);

        if (civilizations.size() < 2) {
            throw new IllegalArgumentException("至少需要2个文明进行对比");
        }

        CivilizationComparisonResult result = new CivilizationComparisonResult();

        List<String> names = civilizations.stream()
                .map(CivilizationBalance::getCivilizationName)
                .collect(Collectors.toList());
        List<String> codes = civilizations.stream()
                .map(CivilizationBalance::getCivilizationCode)
                .collect(Collectors.toList());

        result.setCivilizationNames(names);
        result.setCivilizationCodes(codes);
        result.setDimensions(DEFAULT_DIMENSIONS.stream()
                .map(DIMENSION_LABELS::get)
                .collect(Collectors.toList()));

        Map<String, List<Double>> radarData = new LinkedHashMap<>();
        for (String dim : DEFAULT_DIMENSIONS) {
            List<Double> values = civilizations.stream()
                    .map(c -> getDimensionValue(c, dim))
                    .collect(Collectors.toList());
            radarData.put(DIMENSION_LABELS.get(dim), values);
        }
        result.setRadarData(radarData);

        Map<String, CivilizationComparisonResult.CivilizationSummary> summaries = new LinkedHashMap<>();
        String bestCiv = null;
        double bestAvg = 0.0;

        for (CivilizationBalance civ : civilizations) {
            CivilizationComparisonResult.CivilizationSummary summary =
                    new CivilizationComparisonResult.CivilizationSummary();

            summary.setName(civ.getCivilizationName());
            summary.setCode(civ.getCivilizationCode());
            summary.setPeriodStart(civ.getPeriodStartYear());
            summary.setPeriodEnd(civ.getPeriodEndYear());
            summary.setBalanceType("EQUAL_ARM".equals(civ.getBalanceType()) ? "等臂天平" : "不等臂天平");
            summary.setCulturalSignificance(civ.getCulturalSignificance());
            summary.setRepresentativeArtifact(civ.getRepresentativeArtifact());

            Map<String, Double> scores = new LinkedHashMap<>();
            Map<String, Double> expertConfidence = new LinkedHashMap<>();
            for (String dim : DEFAULT_DIMENSIONS) {
                Double val = getDimensionValue(civ, dim);
                scores.put(DIMENSION_LABELS.get(dim), val);
                expertConfidence.put(DIMENSION_LABELS.get(dim),
                        DIMENSION_EXPERT_CONFIDENCE.getOrDefault(dim, 0.7));
            }
            summary.setScores(scores);

            DataSourceClassification dataSource = classifyDataSource(civ);
            StandardizedCategory category = classifyStandardizedCategory(civ);

            double avg = scores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double weightedAvg = calculateWeightedAverage(scores);

            Map<String, Object> standardization = new LinkedHashMap<>();
            standardization.put("dataSource", dataSource.getCode());
            standardization.put("dataSourceLabel", dataSource.getLabel());
            standardization.put("dataReliabilityScore", dataSource.getReliabilityScore());
            standardization.put("dataSourceDescription", dataSource.getDescription());
            standardization.put("standardizedCategory", category.getCode());
            standardization.put("categoryLabel", category.getLabel());
            standardization.put("categoryDescription", category.getDescription());
            standardization.put("expertVerified", Boolean.TRUE);
            standardization.put("expertConfidenceByDimension", expertConfidence);
            standardization.put("overallExpertConfidence", dataSource.getReliabilityScore());
            standardization.put("weightedAverageScore", weightedAvg);
            summary.setStandardizationInfo(standardization);

            List<String> strengths = new ArrayList<>();
            List<String> weaknesses = new ArrayList<>();
            for (Map.Entry<String, Double> entry : scores.entrySet()) {
                if (entry.getValue() >= 85) {
                    strengths.add(entry.getKey() + "突出 (" + String.format("%.1f", entry.getValue()) + "分)");
                } else if (entry.getValue() < 65) {
                    weaknesses.add(entry.getKey() + "偏弱 (" + String.format("%.1f", entry.getValue()) + "分)");
                }
            }
            summary.setStrengths(strengths);
            summary.setWeaknesses(weaknesses);

            summaries.put(civ.getCivilizationCode(), summary);

            if (avg > bestAvg) {
                bestAvg = avg;
                bestCiv = civ.getCivilizationName();
            }
        }
        result.setSummaries(summaries);
        result.setOverallWinner(bestCiv);

        List<String> analysis = generateComparativeAnalysis(civilizations, summaries);
        result.setComparativeAnalysis(analysis);

        log.info("文明对比完成: 文明数={}, 综合最优={}", civilizations.size(), bestCiv);
        return result;
    }

    public ExpertValidationResult validateCivilizationData(String code) {
        Optional<CivilizationBalance> civOpt = civilizationRepository.findByCivilizationCode(code);

        if (!civOpt.isPresent()) {
            return ExpertValidationResult.builder()
                    .civilizationCode(code)
                    .civilizationName("未知文明")
                    .status(ExpertValidationResult.ValidationStatus.FAIL)
                    .overallConfidence(0.0)
                    .failItems(Arrays.asList("文明代码不存在: " + code))
                    .validator("系统自动校验")
                    .build();
        }

        CivilizationBalance civ = civOpt.get();
        ExpertValidationResult.ExpertValidationResultBuilder resultBuilder =
                ExpertValidationResult.builder()
                        .civilizationCode(civ.getCivilizationCode())
                        .civilizationName(civ.getCivilizationName())
                        .validator("系统自动校验");

        List<String> passItems = new ArrayList<>();
        List<String> warningItems = new ArrayList<>();
        List<String> failItems = new ArrayList<>();

        DataSourceClassification dataSource = classifyDataSource(civ);
        StandardizedCategory category = classifyStandardizedCategory(civ);

        resultBuilder.dataSourceClassification(dataSource);
        resultBuilder.standardizedCategory(category);

        if (dataSource.getReliabilityScore() >= 0.80) {
            passItems.add("数据源可靠度高: " + dataSource.getLabel() +
                    " (可靠性: " + String.format("%.0f%%", dataSource.getReliabilityScore() * 100) + ")");
        } else if (dataSource.getReliabilityScore() >= 0.60) {
            warningItems.add("数据源可靠度中等: " + dataSource.getLabel() +
                    " (可靠性: " + String.format("%.0f%%", dataSource.getReliabilityScore() * 100) + ")，建议补充考古实证");
        } else {
            failItems.add("数据源可靠度不足: " + dataSource.getLabel() +
                    " (可靠性: " + String.format("%.0f%%", dataSource.getReliabilityScore() * 100) + ")");
        }

        validateDimension(civ.getMaxCapacity(), "最大称量", 0.0, 10000.0, passItems, warningItems, failItems);
        validateDimension(civ.getRelativePrecision(), "相对精度", 1e-6, 0.1, passItems, warningItems, failItems);
        validateDimension(civ.getMaterialHardness(), "材料硬度", 0.0, 1000.0, passItems, warningItems, failItems);
        validateDimension(civ.getArmRatioConsistency(), "臂长一致性", 0.0, 100.0, passItems, warningItems, failItems);
        validateDimension(civ.getStructureComplexity(), "结构复杂度", 0.0, 100.0, passItems, warningItems, failItems);
        validateDimension(civ.getDurabilityScore(), "耐久性", 0.0, 100.0, passItems, warningItems, failItems);

        if (civ.getCivilizationName() != null && !civ.getCivilizationName().isEmpty()) {
            passItems.add("文明名称完整: " + civ.getCivilizationName());
        } else {
            failItems.add("文明名称缺失");
        }

        if (civ.getPeriodStartYear() != null && civ.getPeriodEndYear() != null) {
            if (civ.getPeriodStartYear() < civ.getPeriodEndYear()) {
                passItems.add("年代范围合理: " + civ.getPeriodStartYear() + " - " + civ.getPeriodEndYear() + "年");
            } else {
                failItems.add("年代范围异常: 起始年(" + civ.getPeriodStartYear() +
                        ") 晚于 结束年(" + civ.getPeriodEndYear() + ")");
            }
        } else {
            warningItems.add("年代数据不完整");
        }

        if (civ.getRepresentativeArtifact() != null && !civ.getRepresentativeArtifact().isEmpty()) {
            passItems.add("有代表文物记载: " + civ.getRepresentativeArtifact());
        } else {
            warningItems.add("缺少代表文物记载");
        }

        if (civ.getCulturalSignificance() != null && !civ.getCulturalSignificance().isEmpty()) {
            passItems.add("有文化意义描述");
        } else {
            warningItems.add("缺少文化意义描述");
        }

        double confidence = calculateOverallConfidence(dataSource.getReliabilityScore(),
                passItems.size(), warningItems.size(), failItems.size());

        ExpertValidationResult.ValidationStatus status;
        if (!failItems.isEmpty()) {
            status = ExpertValidationResult.ValidationStatus.FAIL;
        } else if (!warningItems.isEmpty()) {
            status = ExpertValidationResult.ValidationStatus.WARNING;
        } else {
            status = ExpertValidationResult.ValidationStatus.PASS;
        }

        resultBuilder.passItems(passItems)
                .warningItems(warningItems)
                .failItems(failItems)
                .overallConfidence(confidence)
                .status(status);

        log.info("文明数据校验完成: code={}, status={}, confidence={}",
                code, status, confidence);

        return resultBuilder.build();
    }

    public List<CivilizationBalance> getAllCivilizations() {
        return civilizationRepository.findAllByOrderByPeriodStartYearAsc();
    }

    public Optional<CivilizationBalance> getCivilization(String code) {
        return civilizationRepository.findByCivilizationCode(code);
    }

    public Map<String, String> getDimensionLabels() {
        return DIMENSION_LABELS;
    }

    public Map<String, Object> getStandardizationMetadata() {
        Map<String, Object> meta = new LinkedHashMap<>();

        Map<String, Map<String, Object>> dataSourceMap = new LinkedHashMap<>();
        for (DataSourceClassification.DataSourceType type :
                DataSourceClassification.DataSourceType.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", type.getCode());
            item.put("label", type.getLabel());
            item.put("reliability", type.getReliability());
            item.put("description", type.getDescription());
            dataSourceMap.put(type.getCode(), item);
        }
        meta.put("dataSourceClassifications", dataSourceMap);

        Map<String, Map<String, Object>> categoryMap = new LinkedHashMap<>();
        for (StandardizedCategory.CategoryType type :
                StandardizedCategory.CategoryType.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", type.getCode());
            item.put("label", type.getLabel());
            item.put("description", type.getDescription());
            item.put("typicalPrecision", type.getTypicalPrecision());
            item.put("typicalCapacity", type.getTypicalCapacity());
            categoryMap.put(type.getCode(), item);
        }
        meta.put("standardizedCategories", categoryMap);

        meta.put("dimensionExpertConfidence", DIMENSION_EXPERT_CONFIDENCE);

        Map<String, List<String>> civilizationGroups = new LinkedHashMap<>();
        civilizationGroups.put("eastern", EASTERN_CIVILIZATIONS);
        civilizationGroups.put("western", WESTERN_CIVILIZATIONS);
        meta.put("civilizationGroups", civilizationGroups);

        return meta;
    }

    public List<String> getEasternCivilizationCodes() {
        return EASTERN_CIVILIZATIONS;
    }

    public List<String> getWesternCivilizationCodes() {
        return WESTERN_CIVILIZATIONS;
    }

    public Map<String, Object> getCivilizationGroups() {
        Map<String, Object> groups = new LinkedHashMap<>();
        groups.put("eastern", EASTERN_CIVILIZATIONS);
        groups.put("western", WESTERN_CIVILIZATIONS);
        return groups;
    }

    private List<CivilizationBalance> loadCivilizations(List<String> civilizationCodes) {
        List<CivilizationBalance> civilizations;

        if (civilizationCodes == null || civilizationCodes.isEmpty()) {
            civilizations = civilizationRepository.findAllByOrderByPeriodStartYearAsc();
        } else {
            civilizations = civilizationCodes.stream()
                    .map(code -> civilizationRepository.findByCivilizationCode(code))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .sorted(Comparator.comparingInt(CivilizationBalance::getPeriodStartYear))
                    .collect(Collectors.toList());
        }

        return civilizations;
    }

    private Double getDimensionValue(CivilizationBalance civ, String dimension) {
        switch (dimension) {
            case "maxCapacity":
                return normalizeCapacity(civ.getMaxCapacity());
            case "relativePrecision":
                return normalizePrecision(civ.getRelativePrecision());
            case "materialHardness":
                return normalizeHardness(civ.getMaterialHardness());
            case "armRatioConsistency":
                return civ.getArmRatioConsistency() != null ? civ.getArmRatioConsistency() : 50.0;
            case "structureComplexity":
                return civ.getStructureComplexity() != null ? civ.getStructureComplexity() : 50.0;
            case "durabilityScore":
                return civ.getDurabilityScore() != null ? civ.getDurabilityScore() : 50.0;
            default:
                return 50.0;
        }
    }

    private double normalizeCapacity(Double capacity) {
        if (capacity == null) return 50.0;
        double normalized = Math.min(100.0, capacity / 8.0);
        return Math.max(0.0, normalized);
    }

    private double normalizePrecision(Double precision) {
        if (precision == null) return 50.0;
        if (precision <= 0) return 0.0;
        double score = 100.0 - Math.min(100.0, -Math.log10(precision) * 15);
        return Math.max(0.0, Math.min(100.0, score));
    }

    private double normalizeHardness(Double hardness) {
        if (hardness == null) return 50.0;
        return Math.min(100.0, hardness / 6.5);
    }

    private double calculateWeightedAverage(Map<String, Double> scores) {
        double weightedSum = 0.0;
        double totalWeight = 0.0;
        for (String dim : DEFAULT_DIMENSIONS) {
            String label = DIMENSION_LABELS.get(dim);
            double weight = DIMENSION_EXPERT_CONFIDENCE.getOrDefault(dim, 0.7);
            weightedSum += scores.get(label) * weight;
            totalWeight += weight;
        }
        return totalWeight > 0 ? weightedSum / totalWeight : 0.0;
    }

    private DataSourceClassification classifyDataSource(CivilizationBalance civ) {
        String ref = civ.getReferenceSource();
        String artifact = civ.getRepresentativeArtifact();
        String location = civ.getDiscoveryLocation();

        boolean hasArchaeological = (artifact != null && !artifact.isEmpty())
                || (location != null && !location.isEmpty());
        boolean hasLiterary = ref != null && (ref.contains("考工记") || ref.contains("史书")
                || ref.contains("文献") || ref.contains("典籍"));

        String code;
        if (hasArchaeological && hasLiterary) {
            code = "MIXED";
        } else if (hasArchaeological) {
            code = "ARCHAEOLOGICAL";
        } else if (hasLiterary) {
            code = "LITERARY";
        } else if (ref != null && ref.contains("实验")) {
            code = "EXPERIMENTAL_RECONSTRUCTION";
        } else {
            code = "EXPERT_ESTIMATE";
        }

        return DataSourceClassification.DataSourceType.fromCode(code);
    }

    private StandardizedCategory classifyStandardizedCategory(CivilizationBalance civ) {
        Double precision = civ.getRelativePrecision();
        Double capacity = civ.getMaxCapacity();
        String type = civ.getBalanceType();
        String significance = civ.getCulturalSignificance();

        String code;
        if (precision != null && precision < 1e-5) {
            code = "SCIENTIFIC_METROLOGY";
        } else if (precision != null && precision < 1e-4 && capacity != null && capacity < 1.0) {
            code = "PRECIOUS_METAL";
        } else if (significance != null && (significance.contains("祭祀") || significance.contains("礼"))) {
            code = "RITUAL_CEREMONIAL";
        } else if (precision != null && precision < 1e-3) {
            code = "PRECISION_LEGAL";
        } else if (capacity != null && capacity > 5.0 && "UNEQUAL_ARM".equals(type)) {
            code = "COMMERCIAL_TRADE";
        } else {
            code = "HOUSEHOLD_DAILY";
        }

        return StandardizedCategory.CategoryType.fromCode(code);
    }

    private void validateDimension(Double value, String name, double min, double max,
                                   List<String> passItems, List<String> warningItems,
                                   List<String> failItems) {
        if (value == null) {
            warningItems.add(name + "数据缺失，使用默认值");
            return;
        }

        if (value < min || value > max) {
            failItems.add(name + "数值异常: " + value + " (正常范围: " + min + " - " + max + ")");
        } else {
            passItems.add(name + "数据正常: " + value);
        }
    }

    private double calculateOverallConfidence(double dataReliability,
                                              int passCount, int warningCount, int failCount) {
        if (failCount > 0) {
            return Math.max(0.0, dataReliability * 0.5 - failCount * 0.1);
        }

        double baseScore = dataReliability * 0.6;
        double passBonus = Math.min(0.3, passCount * 0.03);
        double warningPenalty = warningCount * 0.05;

        return Math.min(1.0, Math.max(0.0, baseScore + passBonus - warningPenalty));
    }

    private List<String> generateComparativeAnalysis(List<CivilizationBalance> civilizations,
                                                     Map<String, CivilizationComparisonResult.CivilizationSummary> summaries) {
        List<String> analysis = new ArrayList<>();

        CivilizationComparisonResult.CivilizationSummary best = null;
        double bestAvg = 0;
        for (CivilizationComparisonResult.CivilizationSummary s : summaries.values()) {
            if (s.getAvgScore() > bestAvg) {
                bestAvg = s.getAvgScore();
                best = s;
            }
        }

        if (best != null) {
            analysis.add(String.format("【综合评估】%s 天平综合得分 %.1f 分，在所有对比文明中表现最优",
                    best.getName(), bestAvg));
        }

        Map<String, Double> dimMax = new HashMap<>();
        Map<String, String> dimBest = new HashMap<>();
        for (String dim : DEFAULT_DIMENSIONS) {
            String label = DIMENSION_LABELS.get(dim);
            double max = 0;
            String bestName = "";
            for (CivilizationComparisonResult.CivilizationSummary s : summaries.values()) {
                double val = s.getScores().get(label);
                if (val > max) {
                    max = val;
                    bestName = s.getName();
                }
            }
            dimMax.put(label, max);
            dimBest.put(label, bestName);
            if (max >= 85) {
                analysis.add(String.format("【%s领先】%s 在%s方面达到 %.1f 分，技术领先",
                        label, bestName, label, max));
            }
        }

        List<CivilizationBalance> equalArm = civilizations.stream()
                .filter(c -> "EQUAL_ARM".equals(c.getBalanceType()))
                .collect(Collectors.toList());
        List<CivilizationBalance> unequalArm = civilizations.stream()
                .filter(c -> "UNEQUAL_ARM".equals(c.getBalanceType()))
                .collect(Collectors.toList());

        if (!equalArm.isEmpty() && !unequalArm.isEmpty()) {
            double equalAvg = equalArm.stream()
                    .mapToDouble(c -> summaries.get(c.getCivilizationCode()).getAvgScore())
                    .average().orElse(0);
            double unequalAvg = unequalArm.stream()
                    .mapToDouble(c -> summaries.get(c.getCivilizationCode()).getAvgScore())
                    .average().orElse(0);

            if (equalAvg > unequalAvg) {
                analysis.add(String.format("【技术路线对比】等臂天平路线(%.1f分)整体优于不等臂路线(%.1f分)，适合精密计量",
                        equalAvg, unequalAvg));
            } else {
                analysis.add(String.format("【技术路线对比】不等臂天平路线(%.1f分)在商业实用性上优于等臂路线(%.1f分)",
                        unequalAvg, equalAvg));
            }
        }

        if (civilizations.size() >= 2) {
            CivilizationBalance earliest = civilizations.get(0);
            CivilizationBalance latest = civilizations.get(civilizations.size() - 1);
            double earliestScore = summaries.get(earliest.getCivilizationCode()).getAvgScore();
            double latestScore = summaries.get(latest.getCivilizationCode()).getAvgScore();

            if (latestScore > earliestScore) {
                analysis.add(String.format("【技术演进】从%s(%d年)到%s(%d年)，天平技术综合得分从%.1f提升到%.1f，体现了计量技术的进步",
                        earliest.getCivilizationName(), earliest.getPeriodStartYear(),
                        latest.getCivilizationName(), latest.getPeriodEndYear(),
                        earliestScore, latestScore));
            }
        }

        boolean hasChineseCivilization = civilizations.stream()
                .anyMatch(c -> c.getCivilizationCode().startsWith("CHN"));
        if (hasChineseCivilization && dimBest.get("相对精度").contains("中国")) {
            analysis.add("【中国特色】中国古代等臂天平在相对精度方面达到世界领先水平，体现了精湛的工艺水准");
        }

        boolean hasRoman = civilizations.stream()
                .anyMatch(c -> "ROME".equals(c.getCivilizationCode()));
        if (hasRoman && (dimBest.get("最大称量").contains("罗马") ||
                dimBest.get("结构复杂度").contains("罗马"))) {
            analysis.add("【罗马特色】古罗马不等臂天平(Steelyard)在大称量和结构创新方面有独到之处，适合商业贸易");
        }

        long easternCount = civilizations.stream()
                .filter(c -> EASTERN_CIVILIZATIONS.contains(c.getCivilizationCode()))
                .count();
        long westernCount = civilizations.stream()
                .filter(c -> WESTERN_CIVILIZATIONS.contains(c.getCivilizationCode()))
                .count();

        if (easternCount > 0 && westernCount > 0) {
            analysis.add(String.format("【东西方对比】本次对比涵盖东方文明 %d 个、西方文明 %d 个，展现了不同文明的计量技术发展路径",
                    easternCount, westernCount));
        }

        return analysis;
    }
}
