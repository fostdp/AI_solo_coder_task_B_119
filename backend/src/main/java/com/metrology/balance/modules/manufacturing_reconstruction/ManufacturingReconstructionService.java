package com.metrology.balance.modules.manufacturing_reconstruction;

import com.metrology.balance.entity.Balance;
import com.metrology.balance.entity.BalanceMeasurement;
import com.metrology.balance.entity.ManufacturingAnalysis;
import com.metrology.balance.model.KnifeEdgeWearModel;
import com.metrology.balance.repository.BalanceMeasurementRepository;
import com.metrology.balance.repository.BalanceRepository;
import com.metrology.balance.repository.ManufacturingAnalysisRepository;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ManufacturingReconstructionService {

    private static final Logger logger = LoggerFactory.getLogger(ManufacturingReconstructionService.class);

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private BalanceMeasurementRepository measurementRepository;

    @Autowired
    private ManufacturingAnalysisRepository analysisRepository;

    private static final Map<String, CraftMethodKnowledge> CRAFT_METHODS = new HashMap<>();

    static {
        CRAFT_METHODS.put("青铜-范铸", new CraftMethodKnowledge("青铜范铸法", "战国-汉",
                new double[]{0.8, 1.5}, new double[]{50.0, 80.0}, new double[]{0.001, 0.002},
                Arrays.asList("泥范", "石范", "铜范"), "精度中等，适合批量生产"));
        CRAFT_METHODS.put("青铜-失蜡", new CraftMethodKnowledge("青铜失蜡法", "春秋-清",
                new double[]{0.3, 0.8}, new double[]{80.0, 95.0}, new double[]{0.0005, 0.001},
                Arrays.asList("失蜡", "熔模"), "高精度，适合复杂器形"));
        CRAFT_METHODS.put("钢铁-锻打", new CraftMethodKnowledge("钢铁锻打法", "战国-现代",
                new double[]{0.5, 1.2}, new double[]{70.0, 90.0}, new double[]{0.0008, 0.0015},
                Arrays.asList("热锻", "冷锻", "淬火"), "强度高，刀口锋利"));
        CRAFT_METHODS.put("玉石-琢磨", new CraftMethodKnowledge("玉石琢磨法", "新石器-现代",
                new double[]{0.2, 0.5}, new double[]{90.0, 98.0}, new double[]{0.0003, 0.0006},
                Arrays.asList("解玉砂", "琢磨", "抛光"), "极高光洁度，硬度大"));
        CRAFT_METHODS.put("玛瑙-钻孔", new CraftMethodKnowledge("玛瑙钻孔法", "唐-现代",
                new double[]{0.15, 0.4}, new double[]{92.0, 99.0}, new double[]{0.0002, 0.0005},
                Arrays.asList("金刚砂钻孔", "抛光"), "极高精度，耐磨极佳"));
        CRAFT_METHODS.put("木-切削", new CraftMethodKnowledge("木切削法", "新石器-现代",
                new double[]{1.5, 3.0}, new double[]{30.0, 60.0}, new double[]{0.002, 0.003},
                Arrays.asList("凿", "刨", "磨"), "轻便，精度较低"));
    }

    @Transactional
    public ManufacturingAnalysis analyzeManufacturingTechnology(Integer balanceId) {
        Balance balance = balanceRepository.findById(balanceId)
                .orElseThrow(() -> new IllegalArgumentException("天平不存在: " + balanceId));

        List<BalanceMeasurement> measurements = measurementRepository
                .findTop100ByBalanceIdOrderByMeasurementTimeDesc(balanceId);

        ManufacturingAnalysis analysis = new ManufacturingAnalysis();
        analysis.setBalanceId(balanceId);
        analysis.setAnalysisTime(LocalDateTime.now());

        double knifeRadius = balance.getKnifeEdgeRadius();
        double armLeft = balance.getLeftArmLength();
        double armRight = balance.getRightArmLength();
        String material = balance.getKnifeEdgeMaterial();
        if (material == null) material = "青铜";

        double avgWear = 0.0;
        double avgFriction = 0.0;
        double stdFriction = 0.0;

        if (!measurements.isEmpty()) {
            DescriptiveStatistics wearStats = new DescriptiveStatistics();
            DescriptiveStatistics frictionStats = new DescriptiveStatistics();
            for (BalanceMeasurement m : measurements) {
                if (m.getKnifeEdgeWearDepth() != null) {
                    wearStats.addValue(m.getKnifeEdgeWearDepth());
                }
                if (m.getKnifeEdgeFriction() != null) {
                    frictionStats.addValue(m.getKnifeEdgeFriction());
                }
            }
            avgWear = wearStats.getN() > 0 ? wearStats.getMean() : 0.0;
            avgFriction = frictionStats.getN() > 0 ? frictionStats.getMean() : 0.0012;
            stdFriction = frictionStats.getN() > 1 ? frictionStats.getStandardDeviation() : 0.0001;
        } else {
            avgFriction = KnifeEdgeWearModel.createWithMaterial(material)
                    .calculateDynamicFriction(20.0, 50.0);
        }

        double geometryScore = calculateGeometryScore(knifeRadius, armLeft, armRight);
        double surfaceScore = calculateSurfaceRoughnessScore(avgFriction, stdFriction);
        double materialScore = calculateMaterialQualityScore(material, avgWear, stdFriction);
        double assemblyScore = calculateAssemblyPrecisionScore(armLeft, armRight);

        analysis.setKnifeEdgeGeometryScore(geometryScore);
        analysis.setSurfaceRoughnessScore(surfaceScore);
        analysis.setMaterialQualityScore(materialScore);
        analysis.setAssemblyPrecisionScore(assemblyScore);

        double overallScore = (geometryScore + surfaceScore + materialScore + assemblyScore) / 4.0;
        String grade = determineOverallGrade(overallScore);
        analysis.setOverallTechnologyGrade(grade);

        analysis.setGeometryToleranceMicrom(knifeRadius * 1000.0);
        analysis.setInferredSurfaceRoughnessRa(inferSurfaceRoughnessRa(avgFriction));
        analysis.setMaterialHomogeneity(100.0 - stdFriction / avgFriction * 1000.0);
        analysis.setArmLengthRatioError(Math.abs(armLeft - armRight) / ((armLeft + armRight) / 2.0));

        String era = inferManufacturingEra(overallScore, material, knifeRadius);
        analysis.setEstimatedManufacturingEra(era);

        String craftMethod = inferCraftMethod(material, knifeRadius, avgFriction, overallScore);
        analysis.setInferredCraftMethod(craftMethod);

        Map<String, Object> rawData = new HashMap<>();
        rawData.put("knifeEdgeRadius", knifeRadius);
        rawData.put("armLengthLeft", armLeft);
        rawData.put("armLengthRight", armRight);
        rawData.put("material", material);
        rawData.put("avgWearDepth", avgWear);
        rawData.put("avgFrictionCoefficient", avgFriction);
        rawData.put("frictionStdDev", stdFriction);
        rawData.put("measurementCount", measurements.size());
        rawData.put("overallScore", overallScore);
        rawData.put("materialHardness", KnifeEdgeWearModel.createWithMaterial(material).getHardnessHB());
        rawData.put("craftMethodDetails", getCraftMethodDetails(craftMethod));
        analysis.setRawData(rawData);

        analysis = analysisRepository.save(analysis);
        logger.info("制造工艺分析完成: balanceId={}, grade={}, era={}, craft={}",
                balanceId, grade, era, craftMethod);

        return analysis;
    }

    private double calculateGeometryScore(double knifeRadius, double armLeft, double armRight) {
        double radiusScore;
        if (knifeRadius < 0.3) radiusScore = 98.0;
        else if (knifeRadius < 0.5) radiusScore = 95.0;
        else if (knifeRadius < 0.8) radiusScore = 85.0;
        else if (knifeRadius < 1.0) radiusScore = 75.0;
        else if (knifeRadius < 1.5) radiusScore = 65.0;
        else if (knifeRadius < 2.0) radiusScore = 55.0;
        else radiusScore = 40.0;

        double armStraightScore;
        double ratio = Math.abs(armLeft - armRight) / ((armLeft + armRight) / 2.0);
        if (ratio < 0.00005) armStraightScore = 99.0;
        else if (ratio < 0.0001) armStraightScore = 95.0;
        else if (ratio < 0.0003) armStraightScore = 88.0;
        else if (ratio < 0.0005) armStraightScore = 80.0;
        else if (ratio < 0.001) armStraightScore = 70.0;
        else armStraightScore = 55.0;

        return radiusScore * 0.6 + armStraightScore * 0.4;
    }

    private double calculateSurfaceRoughnessScore(double avgFriction, double stdFriction) {
        double frictionScore;
        if (avgFriction < 0.0003) frictionScore = 98.0;
        else if (avgFriction < 0.0005) frictionScore = 95.0;
        else if (avgFriction < 0.0008) frictionScore = 88.0;
        else if (avgFriction < 0.0010) frictionScore = 80.0;
        else if (avgFriction < 0.0015) frictionScore = 70.0;
        else if (avgFriction < 0.0020) frictionScore = 60.0;
        else frictionScore = 45.0;

        double consistencyScore = Math.max(0.0, 100.0 - (stdFriction / avgFriction) * 500.0);

        return frictionScore * 0.7 + consistencyScore * 0.3;
    }

    private double calculateMaterialQualityScore(String material, double avgWear, double stdFriction) {
        KnifeEdgeWearModel model = KnifeEdgeWearModel.createWithMaterial(material);
        double hardness = model.getHardnessHB();

        double hardnessScore;
        if (hardness >= 600) hardnessScore = 98.0;
        else if (hardness >= 500) hardnessScore = 92.0;
        else if (hardness >= 350) hardnessScore = 85.0;
        else if (hardness >= 200) hardnessScore = 75.0;
        else if (hardness >= 150) hardnessScore = 65.0;
        else hardnessScore = 50.0;

        double wearResistanceScore;
        if (avgWear < 0.01) wearResistanceScore = 95.0;
        else if (avgWear < 0.05) wearResistanceScore = 85.0;
        else if (avgWear < 0.10) wearResistanceScore = 70.0;
        else if (avgWear < 0.15) wearResistanceScore = 55.0;
        else wearResistanceScore = 40.0;

        return hardnessScore * 0.5 + wearResistanceScore * 0.5;
    }

    private double calculateAssemblyPrecisionScore(double armLeft, double armRight) {
        double ratioError = Math.abs(armLeft - armRight) / ((armLeft + armRight) / 2.0);

        if (ratioError < 0.00005) return 99.0;
        else if (ratioError < 0.0001) return 95.0;
        else if (ratioError < 0.0003) return 88.0;
        else if (ratioError < 0.0005) return 80.0;
        else if (ratioError < 0.001) return 70.0;
        else if (ratioError < 0.003) return 55.0;
        else return 40.0;
    }

    private String determineOverallGrade(double score) {
        if (score >= 95) return "神品";
        else if (score >= 85) return "妙品";
        else if (score >= 75) return "能品";
        else if (score >= 65) return "佳品";
        else if (score >= 50) return "常品";
        else return "残品";
    }

    private double inferSurfaceRoughnessRa(double friction) {
        return friction * 100.0;
    }

    private String inferManufacturingEra(double score, String material, double knifeRadius) {
        if (knifeRadius < 0.3 && "玛瑙".equals(material)) return "唐代-清代 精密戥秤工艺";
        else if (knifeRadius < 0.5 && ("玉石".equals(material) || "玛瑙".equals(material))) return "唐代-现代 精密天平工艺";
        else if (knifeRadius < 0.8 && score >= 85) return "战国-汉代 高精度工艺";
        else if (knifeRadius < 1.0 && score >= 75) return "战国-明代 标准工艺";
        else if (knifeRadius < 1.5) return "各代 普通商用衡器";
        else return "早期 / 民间工艺";
    }

    private String inferCraftMethod(String material, double knifeRadius, double friction, double score) {
        String materialKey = material;
        if ("青铜".equals(material)) {
            if (friction < 0.0008 && knifeRadius < 0.8) return "青铜-失蜡";
            else return "青铜-范铸";
        } else if ("钢".equals(material) || "铁".equals(material)) {
            return "钢铁-锻打";
        } else if ("玉石".equals(material)) {
            return "玉石-琢磨";
        } else if ("玛瑙".equals(material)) {
            return "玛瑙-钻孔";
        } else if ("木".equals(material)) {
            return "木-切削";
        }
        return "青铜-范铸";
    }

    private Map<String, Object> getCraftMethodDetails(String key) {
        CraftMethodKnowledge craft = CRAFT_METHODS.get(key);
        if (craft == null) return null;
        Map<String, Object> details = new HashMap<>();
        details.put("methodName", craft.methodName);
        details.put("period", craft.period);
        details.put("knifeRadiusRange_mm", craft.knifeRadiusRange);
        details.put("achievableScoreRange", craft.scoreRange);
        details.put("achievableFrictionRange", craft.frictionRange);
        details.put("processSteps", craft.processSteps);
        details.put("description", craft.description);
        return details;
    }

    public List<ManufacturingAnalysis> getAnalysisHistory(Integer balanceId) {
        return analysisRepository.findByBalanceIdOrderByAnalysisTimeDesc(balanceId);
    }

    public Optional<ManufacturingAnalysis> getLatestAnalysis(Integer balanceId) {
        return analysisRepository.findTopByBalanceIdOrderByAnalysisTimeDesc(balanceId);
    }

    private static class CraftMethodKnowledge {
        String methodName;
        String period;
        double[] knifeRadiusRange;
        double[] scoreRange;
        double[] frictionRange;
        List<String> processSteps;
        String description;

        public CraftMethodKnowledge(String methodName, String period, double[] knifeRadiusRange,
                                    double[] scoreRange, double[] frictionRange,
                                    List<String> processSteps, String description) {
            this.methodName = methodName;
            this.period = period;
            this.knifeRadiusRange = knifeRadiusRange;
            this.scoreRange = scoreRange;
            this.frictionRange = frictionRange;
            this.processSteps = processSteps;
            this.description = description;
        }
    }
}
