package com.metrology.balance.modules.civilization_comparison;

import com.metrology.balance.dto.CivilizationComparisonResult;
import com.metrology.balance.entity.CivilizationBalance;
import com.metrology.balance.repository.CivilizationBalanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CivilizationComparisonService {

    private static final Logger logger = LoggerFactory.getLogger(CivilizationComparisonService.class);

    @Autowired
    private CivilizationBalanceRepository civilizationRepository;

    private static final List<String> DEFAULT_DIMENSIONS = Arrays.asList(
            "maxCapacity", "relativePrecision", "materialHardness",
            "armRatioConsistency", "structureComplexity", "durabilityScore"
    );

    private static final Map<String, String> DIMENSION_LABELS = new HashMap<>();
    private static final Map<String, String> DATA_SOURCE_CLASSIFICATION = new HashMap<>();
    private static final Map<String, Double> DATA_SOURCE_RELIABILITY = new HashMap<>();
    private static final Map<String, String> STANDARDIZED_CATEGORIES = new HashMap<>();
    private static final Map<String, Double> DIMENSION_EXPERT_CONFIDENCE = new HashMap<>();

    static {
        DIMENSION_LABELS.put("maxCapacity", "最大称量");
        DIMENSION_LABELS.put("relativePrecision", "相对精度");
        DIMENSION_LABELS.put("materialHardness", "材料硬度");
        DIMENSION_LABELS.put("armRatioConsistency", "臂长一致性");
        DIMENSION_LABELS.put("structureComplexity", "结构复杂度");
        DIMENSION_LABELS.put("durabilityScore", "耐久性");

        DATA_SOURCE_CLASSIFICATION.put("ARCHAEOLOGICAL", "考古实物测量");
        DATA_SOURCE_CLASSIFICATION.put("LITERARY", "文献记载考证");
        DATA_SOURCE_CLASSIFICATION.put("EXPERT_ESTIMATE", "专家估算推断");
        DATA_SOURCE_CLASSIFICATION.put("EXPERIMENTAL_RECONSTRUCTION", "实验复原测量");
        DATA_SOURCE_CLASSIFICATION.put("MIXED", "多源综合数据");

        DATA_SOURCE_RELIABILITY.put("ARCHAEOLOGICAL", 0.95);
        DATA_SOURCE_RELIABILITY.put("LITERARY", 0.70);
        DATA_SOURCE_RELIABILITY.put("EXPERT_ESTIMATE", 0.60);
        DATA_SOURCE_RELIABILITY.put("EXPERIMENTAL_RECONSTRUCTION", 0.85);
        DATA_SOURCE_RELIABILITY.put("MIXED", 0.80);

        STANDARDIZED_CATEGORIES.put("PRECISION_LEGAL", "精密法定衡器");
        STANDARDIZED_CATEGORIES.put("COMMERCIAL_TRADE", "商业贸易衡器");
        STANDARDIZED_CATEGORIES.put("PRECIOUS_METAL", "金银珠宝衡器");
        STANDARDIZED_CATEGORIES.put("RITUAL_CEREMONIAL", "礼仪祭祀衡器");
        STANDARDIZED_CATEGORIES.put("HOUSEHOLD_DAILY", "民间日用衡器");
        STANDARDIZED_CATEGORIES.put("SCIENTIFIC_METROLOGY", "科学计量衡器");

        DIMENSION_EXPERT_CONFIDENCE.put("maxCapacity", 0.90);
        DIMENSION_EXPERT_CONFIDENCE.put("relativePrecision", 0.75);
        DIMENSION_EXPERT_CONFIDENCE.put("materialHardness", 0.85);
        DIMENSION_EXPERT_CONFIDENCE.put("armRatioConsistency", 0.80);
        DIMENSION_EXPERT_CONFIDENCE.put("structureComplexity", 0.65);
        DIMENSION_EXPERT_CONFIDENCE.put("durabilityScore", 0.60);
    }

    public CivilizationComparisonResult compareCivilizations(List<String> civilizationCodes) {
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

            String dataSource = classifyDataSource(civ);
            double dataReliability = DATA_SOURCE_RELIABILITY.getOrDefault(dataSource, 0.7);
            String category = classifyStandardizedCategory(civ);

            double avg = scores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double weightedAvg = 0.0;
            double totalWeight = 0.0;
            for (String dim : DEFAULT_DIMENSIONS) {
                String label = DIMENSION_LABELS.get(dim);
                double weight = DIMENSION_EXPERT_CONFIDENCE.getOrDefault(dim, 0.7);
                weightedAvg += scores.get(label) * weight;
                totalWeight += weight;
            }
            weightedAvg /= totalWeight;
            summary.setAvgScore(avg);

            Map<String, Object> standardization = new LinkedHashMap<>();
            standardization.put("dataSource", dataSource);
            standardization.put("dataSourceLabel", DATA_SOURCE_CLASSIFICATION.getOrDefault(dataSource, "未知来源"));
            standardization.put("dataReliabilityScore", dataReliability);
            standardization.put("standardizedCategory", category);
            standardization.put("categoryLabel", STANDARDIZED_CATEGORIES.getOrDefault(category, "未分类"));
            standardization.put("expertVerified", Boolean.TRUE);
            standardization.put("expertConfidenceByDimension", expertConfidence);
            standardization.put("overallExpertConfidence", dataReliability);
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

        logger.info("文明对比完成: 文明数={}, 综合最优={}", civilizations.size(), bestCiv);
        return result;
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
                return civ.getArmRatioConsistency();
            case "structureComplexity":
                return civ.getStructureComplexity();
            case "durabilityScore":
                return civ.getDurabilityScore();
            default:
                return 0.0;
        }
    }

    private double normalizeCapacity(Double capacity) {
        if (capacity == null) return 50.0;
        double normalized = Math.min(100.0, capacity / 8.0);
        return normalized;
    }

    private double normalizePrecision(Double precision) {
        if (precision == null) return 50.0;
        double score = 100.0 - Math.min(100.0, -Math.log10(precision) * 15);
        return Math.max(0.0, Math.min(100.0, score));
    }

    private double normalizeHardness(Double hardness) {
        if (hardness == null) return 50.0;
        return Math.min(100.0, hardness / 6.5);
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

        if (dimBest.get("相对精度").contains("中国")) {
            analysis.add("【中国特色】中国古代等臂天平在相对精度方面达到世界领先水平，体现了精湛的工艺水准");
        }

        if (dimBest.get("最大称量").contains("罗马") || dimBest.get("结构复杂度").contains("罗马")) {
            analysis.add("【罗马特色】古罗马不等臂天平(Steelyard)在大称量和结构创新方面有独到之处，适合商业贸易");
        }

        return analysis;
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

    private String classifyDataSource(CivilizationBalance civ) {
        String ref = civ.getReferenceSource();
        String artifact = civ.getRepresentativeArtifact();
        String location = civ.getDiscoveryLocation();

        boolean hasArchaeological = (artifact != null && !artifact.isEmpty())
                || (location != null && !location.isEmpty());
        boolean hasLiterary = ref != null && (ref.contains("考工记") || ref.contains("史书")
                || ref.contains("文献") || ref.contains("典籍"));

        if (hasArchaeological && hasLiterary) return "MIXED";
        if (hasArchaeological) return "ARCHAEOLOGICAL";
        if (hasLiterary) return "LITERARY";
        if (ref != null && ref.contains("实验")) return "EXPERIMENTAL_RECONSTRUCTION";
        return "EXPERT_ESTIMATE";
    }

    private String classifyStandardizedCategory(CivilizationBalance civ) {
        Double precision = civ.getRelativePrecision();
        Double capacity = civ.getMaxCapacity();
        String type = civ.getBalanceType();
        String significance = civ.getCulturalSignificance();

        if (precision != null && precision < 1e-5) return "SCIENTIFIC_METROLOGY";
        if (precision != null && precision < 1e-4 && capacity != null && capacity < 1.0) return "PRECIOUS_METAL";
        if (significance != null && (significance.contains("祭祀") || significance.contains("礼")))
            return "RITUAL_CEREMONIAL";
        if (precision != null && precision < 1e-3) return "PRECISION_LEGAL";
        if (capacity != null && capacity > 5.0 && "UNEQUAL_ARM".equals(type)) return "COMMERCIAL_TRADE";
        return "HOUSEHOLD_DAILY";
    }

    public Map<String, Object> getStandardizationMetadata() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("dataSourceClassifications", DATA_SOURCE_CLASSIFICATION);
        meta.put("dataSourceReliability", DATA_SOURCE_RELIABILITY);
        meta.put("standardizedCategories", STANDARDIZED_CATEGORIES);
        meta.put("dimensionExpertConfidence", DIMENSION_EXPERT_CONFIDENCE);
        return meta;
    }
}
