package com.metrology.balance.modules.craft_inferrer.service;

import com.metrology.balance.entity.Balance;
import com.metrology.balance.entity.BalanceMeasurement;
import com.metrology.balance.model.KnifeEdgeWearModel;
import com.metrology.balance.modules.craft_inferrer.model.CraftMethodKnowledge;
import com.metrology.balance.modules.craft_inferrer.model.LiteratureReference;
import com.metrology.balance.modules.craft_inferrer.model.UncertaintyBudget;
import com.metrology.balance.repository.BalanceMeasurementRepository;
import com.metrology.balance.repository.BalanceRepository;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CraftInferrerService {

    private static final Logger logger = LoggerFactory.getLogger(CraftInferrerService.class);

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private BalanceMeasurementRepository measurementRepository;

    private static final Map<String, CraftMethodKnowledge> CRAFT_METHODS = new HashMap<>();
    private static final Map<String, LiteratureReference> LITERATURE_REFERENCES = new HashMap<>();
    private static final int MIN_EXPERIMENTAL_SAMPLES = 10;
    private static final int SUFFICIENT_EXPERIMENTAL_SAMPLES = 30;

    private final Map<Long, MonteCarloResult> monteCarloResults = new ConcurrentHashMap<>();

    static {
        CRAFT_METHODS.put("青铜-范铸", CraftMethodKnowledge.builder()
                .methodKey("青铜-范铸")
                .methodName("青铜范铸法")
                .period("战国-汉")
                .knifeRadiusRange(new double[]{0.8, 1.5})
                .scoreRange(new double[]{50.0, 80.0})
                .frictionRange(new double[]{0.001, 0.002})
                .processSteps(Arrays.asList("泥范", "石范", "铜范"))
                .description("精度中等，适合批量生产")
                .build());

        CRAFT_METHODS.put("青铜-失蜡", CraftMethodKnowledge.builder()
                .methodKey("青铜-失蜡")
                .methodName("青铜失蜡法")
                .period("春秋-清")
                .knifeRadiusRange(new double[]{0.3, 0.8})
                .scoreRange(new double[]{80.0, 95.0})
                .frictionRange(new double[]{0.0005, 0.001})
                .processSteps(Arrays.asList("失蜡", "熔模"))
                .description("高精度，适合复杂器形")
                .build());

        CRAFT_METHODS.put("钢铁-锻打", CraftMethodKnowledge.builder()
                .methodKey("钢铁-锻打")
                .methodName("钢铁锻打法")
                .period("战国-现代")
                .knifeRadiusRange(new double[]{0.5, 1.2})
                .scoreRange(new double[]{70.0, 90.0})
                .frictionRange(new double[]{0.0008, 0.0015})
                .processSteps(Arrays.asList("热锻", "冷锻", "淬火"))
                .description("强度高，刀口锋利")
                .build());

        CRAFT_METHODS.put("玉石-琢磨", CraftMethodKnowledge.builder()
                .methodKey("玉石-琢磨")
                .methodName("玉石琢磨法")
                .period("新石器-现代")
                .knifeRadiusRange(new double[]{0.2, 0.5})
                .scoreRange(new double[]{90.0, 98.0})
                .frictionRange(new double[]{0.0003, 0.0006})
                .processSteps(Arrays.asList("解玉砂", "琢磨", "抛光"))
                .description("极高光洁度，硬度大")
                .build());

        CRAFT_METHODS.put("玛瑙-钻孔", CraftMethodKnowledge.builder()
                .methodKey("玛瑙-钻孔")
                .methodName("玛瑙钻孔法")
                .period("唐-现代")
                .knifeRadiusRange(new double[]{0.15, 0.4})
                .scoreRange(new double[]{92.0, 99.0})
                .frictionRange(new double[]{0.0002, 0.0005})
                .processSteps(Arrays.asList("金刚砂钻孔", "抛光"))
                .description("极高精度，耐磨极佳")
                .build());

        CRAFT_METHODS.put("木-切削", CraftMethodKnowledge.builder()
                .methodKey("木-切削")
                .methodName("木切削法")
                .period("新石器-现代")
                .knifeRadiusRange(new double[]{1.5, 3.0})
                .scoreRange(new double[]{30.0, 60.0})
                .frictionRange(new double[]{0.002, 0.003})
                .processSteps(Arrays.asList("凿", "刨", "磨"))
                .description("轻便，精度较低")
                .build());

        LITERATURE_REFERENCES.put("青铜", LiteratureReference.builder()
                .material("青铜")
                .source("《中国古代度量衡图集》《考工记》")
                .frictionRange(new double[]{0.0010, 0.0018})
                .wearRange(new double[]{0.0008, 0.0015})
                .knifeRadiusRange(new double[]{0.5, 1.2})
                .baseUncertainty(0.15)
                .build());

        LITERATURE_REFERENCES.put("钢", LiteratureReference.builder()
                .material("钢")
                .source("《天工开物》《中国古代金属技术》")
                .frictionRange(new double[]{0.0007, 0.0014})
                .wearRange(new double[]{0.0006, 0.0012})
                .knifeRadiusRange(new double[]{0.4, 1.0})
                .baseUncertainty(0.12)
                .build());

        LITERATURE_REFERENCES.put("铁", LiteratureReference.builder()
                .material("铁")
                .source("《天工开物》《中国古代金属技术》")
                .frictionRange(new double[]{0.0008, 0.0015})
                .wearRange(new double[]{0.0007, 0.0013})
                .knifeRadiusRange(new double[]{0.5, 1.1})
                .baseUncertainty(0.13)
                .build());

        LITERATURE_REFERENCES.put("玉石", LiteratureReference.builder()
                .material("玉石")
                .source("《中国古代玉器科学研究》")
                .frictionRange(new double[]{0.00025, 0.00055})
                .wearRange(new double[]{0.0002, 0.0005})
                .knifeRadiusRange(new double[]{0.2, 0.45})
                .baseUncertainty(0.08)
                .build());

        LITERATURE_REFERENCES.put("玛瑙", LiteratureReference.builder()
                .material("玛瑙")
                .source("《唐代戥秤工艺研究》《中国精密衡器史》")
                .frictionRange(new double[]{0.00018, 0.00045})
                .wearRange(new double[]{0.00015, 0.0004})
                .knifeRadiusRange(new double[]{0.15, 0.35})
                .baseUncertainty(0.06)
                .build());

        LITERATURE_REFERENCES.put("木", LiteratureReference.builder()
                .material("木")
                .source("《中国传统工艺全集》")
                .frictionRange(new double[]{0.0018, 0.0028})
                .wearRange(new double[]{0.0015, 0.0025})
                .knifeRadiusRange(new double[]{1.2, 2.8})
                .baseUncertainty(0.20)
                .build());
    }

    public Map<String, Object> inferCraftMethod(Long balanceId) {
        Balance balance = balanceRepository.findById(balanceId)
                .orElseThrow(() -> new IllegalArgumentException("天平不存在: " + balanceId));

        List<BalanceMeasurement> measurements = measurementRepository
                .findTop100ByBalanceIdOrderByMeasurementTimeDesc(balanceId);

        double knifeRadius = balance.getKnifeEdgeRadius() != null ?
                balance.getKnifeEdgeRadius().doubleValue() : 0.5;
        double armLeft = balance.getLeftArmLength() != null ?
                balance.getLeftArmLength().doubleValue() : 180.0;
        double armRight = balance.getRightArmLength() != null ?
                balance.getRightArmLength().doubleValue() : 180.0;
        String material = balance.getMaterial();
        if (material == null) material = "青铜";

        double avgWear = 0.0;
        double avgFriction = 0.0;
        double stdFriction = 0.0;
        double typeAUncertainty = 0.0;
        double typeBUncertainty = 0.0;
        double combinedUncertainty = 0.0;
        double expandedUncertainty = 0.0;
        String dataSufficiency = "INSUFFICIENT";
        boolean usesLiteratureEstimate = false;
        LiteratureReference litRef = LITERATURE_REFERENCES.getOrDefault(material,
                LITERATURE_REFERENCES.get("青铜"));

        int validWearCount = 0;
        int validFrictionCount = 0;
        DescriptiveStatistics wearStats = new DescriptiveStatistics();
        DescriptiveStatistics frictionStats = new DescriptiveStatistics();

        if (!measurements.isEmpty()) {
            for (BalanceMeasurement m : measurements) {
                if (m.getKnifeEdgeWearDepth() != null) {
                    wearStats.addValue(m.getKnifeEdgeWearDepth().doubleValue());
                    validWearCount++;
                }
                if (m.getKnifeEdgeFriction() != null) {
                    frictionStats.addValue(m.getKnifeEdgeFriction().doubleValue());
                    validFrictionCount++;
                }
            }
        }

        if (validFrictionCount >= MIN_EXPERIMENTAL_SAMPLES) {
            avgFriction = frictionStats.getMean();
            stdFriction = frictionStats.getStandardDeviation();
            typeAUncertainty = stdFriction / Math.sqrt(validFrictionCount);
            dataSufficiency = validFrictionCount >= SUFFICIENT_EXPERIMENTAL_SAMPLES ? "SUFFICIENT" : "PARTIAL";
        } else {
            usesLiteratureEstimate = true;
            avgFriction = (litRef.getFrictionRange()[0] + litRef.getFrictionRange()[1]) / 2.0;
            double litRange = litRef.getFrictionRange()[1] - litRef.getFrictionRange()[0];
            stdFriction = litRange / 4.0;
            typeAUncertainty = 0.0;
            dataSufficiency = validFrictionCount == 0 ? "LITERATURE_ONLY" : "LITERATURE_SUPPLEMENTED";
        }

        if (validWearCount >= MIN_EXPERIMENTAL_SAMPLES) {
            avgWear = wearStats.getMean();
        } else {
            avgWear = (litRef.getWearRange()[0] + litRef.getWearRange()[1]) / 2.0;
            usesLiteratureEstimate = true;
        }

        typeBUncertainty = litRef.getBaseUncertainty() * avgFriction;
        combinedUncertainty = Math.sqrt(typeAUncertainty * typeAUncertainty + typeBUncertainty * typeBUncertainty);
        expandedUncertainty = combinedUncertainty * 2.0;

        double geometryScore = calculateGeometryScore(knifeRadius, armLeft, armRight);
        double surfaceScore = calculateSurfaceRoughnessScore(avgFriction, stdFriction);
        double materialScore = calculateMaterialQualityScore(material, avgWear, stdFriction);
        double assemblyScore = calculateAssemblyPrecisionScore(armLeft, armRight);

        double overallScore = (geometryScore + surfaceScore + materialScore + assemblyScore) / 4.0;
        double sixSigmaScore = calculateSixSigmaScore(avgFriction, stdFriction, combinedUncertainty);
        String grade = determineOverallGrade(overallScore);

        String era = inferManufacturingEra(overallScore, material, knifeRadius);
        String craftMethodKey = inferCraftMethod(material, knifeRadius, avgFriction, overallScore);
        CraftMethodKnowledge craftMethod = CRAFT_METHODS.get(craftMethodKey);

        List<UncertaintyBudget> uncertaintyBudgets = buildUncertaintyBudgets(
                typeAUncertainty, typeBUncertainty, avgFriction, validFrictionCount,
                knifeRadius, armLeft, armRight, litRef
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("balanceId", balanceId);
        result.put("material", material);
        result.put("knifeEdgeRadius", knifeRadius);
        result.put("armLengthLeft", armLeft);
        result.put("armLengthRight", armRight);

        result.put("inferredCraftMethod", craftMethodKey);
        result.put("craftMethodDetails", craftMethod);
        result.put("estimatedManufacturingEra", era);
        result.put("overallTechnologyGrade", grade);
        result.put("overallScore", overallScore);
        result.put("sixSigmaScore", sixSigmaScore);

        result.put("knifeEdgeGeometryScore", geometryScore);
        result.put("surfaceRoughnessScore", surfaceScore);
        result.put("materialQualityScore", materialScore);
        result.put("assemblyPrecisionScore", assemblyScore);

        result.put("avgWearDepth", avgWear);
        result.put("avgFrictionCoefficient", avgFriction);
        result.put("frictionStdDev", stdFriction);
        result.put("inferredSurfaceRoughnessRa", inferSurfaceRoughnessRa(avgFriction));
        result.put("materialHomogeneity", 100.0 - stdFriction / avgFriction * 1000.0);
        result.put("armLengthRatioError", Math.abs(armLeft - armRight) / ((armLeft + armRight) / 2.0));
        result.put("geometryToleranceMicrom", knifeRadius * 1000.0);

        result.put("dataSufficiency", dataSufficiency);
        result.put("usesLiteratureEstimate", usesLiteratureEstimate);
        result.put("validMeasurementCount", validFrictionCount);
        result.put("totalMeasurementCount", measurements.size());

        result.put("literatureSource", litRef.getSource());
        result.put("literatureFrictionRange", litRef.getFrictionRange());
        result.put("literatureWearRange", litRef.getWearRange());
        result.put("literatureKnifeRadiusRange", litRef.getKnifeRadiusRange());

        Map<String, Object> uncertainty = new LinkedHashMap<>();
        uncertainty.put("typeAUncertainty", typeAUncertainty);
        uncertainty.put("typeBUncertainty", typeBUncertainty);
        uncertainty.put("combinedUncertainty", combinedUncertainty);
        uncertainty.put("expandedUncertainty_k2", expandedUncertainty);
        uncertainty.put("confidenceLevel", "95%");
        uncertainty.put("coverageFactor", 2.0);
        uncertainty.put("relativeUncertainty", combinedUncertainty / avgFriction);
        uncertainty.put("budgets", uncertaintyBudgets);
        result.put("uncertainty", uncertainty);

        result.put("materialHardness", KnifeEdgeWearModel.createWithMaterial(material).getMaterialHardness());

        logger.info("工艺推断完成: balanceId={}, grade={}, craft={}, dataSufficiency={}",
                balanceId, grade, craftMethodKey, dataSufficiency);

        return result;
    }

    private double calculateSixSigmaScore(double avgFriction, double stdFriction, double combinedUncertainty) {
        if (avgFriction <= 0 || stdFriction <= 0) return 0.0;

        double cv = stdFriction / avgFriction;
        double relativeUncertainty = combinedUncertainty / avgFriction;

        double sigmaLevel = (1.0 - cv * 100) / (relativeUncertainty * 100);
        sigmaLevel = Math.max(0.5, Math.min(6.0, sigmaLevel));

        double score = sigmaLevel / 6.0 * 100.0;
        return Math.max(0.0, Math.min(100.0, score));
    }

    private List<UncertaintyBudget> buildUncertaintyBudgets(
            double typeAUnc, double typeBUnc, double avgFriction, int sampleCount,
            double knifeRadius, double armLeft, double armRight, LiteratureReference litRef) {

        List<UncertaintyBudget> budgets = new ArrayList<>();

        double armError = Math.abs(armLeft - armRight) / ((armLeft + armRight) / 2.0);

        budgets.add(UncertaintyBudget.builder()
                .source("测量重复性(A类)")
                .type("A")
                .value(typeAUnc)
                .degreesOfFreedom(sampleCount > 1 ? (double) (sampleCount - 1) : 0.0)
                .sensitivity(1.0)
                .contribution(typeAUnc * typeAUnc)
                .build());

        budgets.add(UncertaintyBudget.builder()
                .source("文献数据不确定度(B类)")
                .type("B")
                .value(typeBUnc)
                .degreesOfFreedom(50.0)
                .sensitivity(1.0)
                .contribution(typeBUnc * typeBUnc)
                .build());

        double armLengthUnc = armError * avgFriction * 0.5;
        budgets.add(UncertaintyBudget.builder()
                .source("臂长误差")
                .type("B")
                .value(armLengthUnc)
                .degreesOfFreedom(20.0)
                .sensitivity(0.5)
                .contribution(armLengthUnc * armLengthUnc * 0.25)
                .build());

        double knifeRadiusUnc = knifeRadius * 0.001;
        budgets.add(UncertaintyBudget.builder()
                .source("刀口半径误差")
                .type("B")
                .value(knifeRadiusUnc)
                .degreesOfFreedom(30.0)
                .sensitivity(0.3)
                .contribution(knifeRadiusUnc * knifeRadiusUnc * 0.09)
                .build());

        return budgets;
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
        double hardness = model.getMaterialHardness();

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

    public List<CraftMethodKnowledge> getAllCraftMethods() {
        return new ArrayList<>(CRAFT_METHODS.values());
    }

    public List<LiteratureReference> getAllLiteratureReferences() {
        return new ArrayList<>(LITERATURE_REFERENCES.values());
    }

    @Async
    public void runMonteCarloSimulation(Long balanceId, int simCount) {
        logger.info("开始蒙特卡洛模拟: balanceId={}, simCount={}", balanceId, simCount);

        Balance balance = balanceRepository.findById(balanceId)
                .orElseThrow(() -> new IllegalArgumentException("天平不存在: " + balanceId));

        double knifeRadius = balance.getKnifeEdgeRadius() != null ?
                balance.getKnifeEdgeRadius().doubleValue() : 0.5;
        double armLeft = balance.getLeftArmLength() != null ?
                balance.getLeftArmLength().doubleValue() : 180.0;
        double armRight = balance.getRightArmLength() != null ?
                balance.getRightArmLength().doubleValue() : 180.0;
        String material = balance.getMaterial();
        if (material == null) material = "青铜";

        LiteratureReference litRef = LITERATURE_REFERENCES.getOrDefault(material,
                LITERATURE_REFERENCES.get("青铜"));

        double avgFriction = (litRef.getFrictionRange()[0] + litRef.getFrictionRange()[1]) / 2.0;
        double frictionRange = litRef.getFrictionRange()[1] - litRef.getFrictionRange()[0];
        double frictionStd = frictionRange / 4.0;

        double avgArmLength = (armLeft + armRight) / 2.0;
        double armErrorStd = Math.abs(armLeft - armRight) / 3.0;
        if (armErrorStd < avgArmLength * 0.0001) {
            armErrorStd = avgArmLength * 0.0005;
        }

        double knifeRadiusStd = knifeRadius * 0.005;
        if (knifeRadiusStd < 0.001) knifeRadiusStd = 0.001;

        NormalDistribution frictionDist = new NormalDistribution(avgFriction, frictionStd);
        NormalDistribution armErrorDist = new NormalDistribution(0, armErrorStd);
        NormalDistribution knifeRadiusDist = new NormalDistribution(knifeRadius, knifeRadiusStd);

        DescriptiveStatistics combinedUncertaintyStats = new DescriptiveStatistics();
        DescriptiveStatistics frictionSampleStats = new DescriptiveStatistics();
        DescriptiveStatistics armErrorSampleStats = new DescriptiveStatistics();
        DescriptiveStatistics knifeRadiusSampleStats = new DescriptiveStatistics();

        Random random = new Random(42);

        for (int i = 0; i < simCount; i++) {
            double frictionSample = frictionDist.sample();
            double armErrorSample = armErrorDist.sample();
            double knifeRadiusSample = Math.max(0.001, knifeRadiusDist.sample());

            double frictionUnc = frictionStd;
            double armUnc = Math.abs(armErrorSample) / avgArmLength * frictionSample;
            double knifeUnc = (knifeRadiusSample - knifeRadius) / knifeRadius * frictionSample * 0.3;

            double combined = Math.sqrt(
                    frictionUnc * frictionUnc +
                    armUnc * armUnc +
                    knifeUnc * knifeUnc
            );

            combinedUncertaintyStats.addValue(combined);
            frictionSampleStats.addValue(frictionSample);
            armErrorSampleStats.addValue(armErrorSample);
            knifeRadiusSampleStats.addValue(knifeRadiusSample);
        }

        double meanCombined = combinedUncertaintyStats.getMean();
        double stdCombined = combinedUncertaintyStats.getStandardDeviation();
        double expandedUncertainty = meanCombined * 2.0;

        double[] histogram = calculateHistogram(combinedUncertaintyStats, 50);
        double[] percentiles = calculatePercentiles(combinedUncertaintyStats);

        MonteCarloResult result = MonteCarloResult.builder()
                .balanceId(balanceId)
                .simulationCount(simCount)
                .status("COMPLETED")
                .meanCombinedUncertainty(meanCombined)
                .stdCombinedUncertainty(stdCombined)
                .expandedUncertainty_k2(expandedUncertainty)
                .confidenceLevel95(percentiles[95])
                .confidenceLevel99(percentiles[99])
                .minCombinedUncertainty(combinedUncertaintyStats.getMin())
                .maxCombinedUncertainty(combinedUncertaintyStats.getMax())
                .histogram(histogram)
                .histogramBinCount(50)
                .frictionMean(frictionSampleStats.getMean())
                .frictionStd(frictionSampleStats.getStandardDeviation())
                .armErrorMean(armErrorSampleStats.getMean())
                .armErrorStd(armErrorSampleStats.getStandardDeviation())
                .knifeRadiusMean(knifeRadiusSampleStats.getMean())
                .knifeRadiusStd(knifeRadiusSampleStats.getStandardDeviation())
                .build();

        monteCarloResults.put(balanceId, result);

        logger.info("蒙特卡洛模拟完成: balanceId={}, meanCombined={}, expanded_k2={}",
                balanceId, meanCombined, expandedUncertainty);
    }

    public MonteCarloResult getMonteCarloResult(Long balanceId) {
        return monteCarloResults.get(balanceId);
    }

    public Map<String, Object> getMonteCarloStatus(Long balanceId) {
        Map<String, Object> status = new HashMap<>();
        MonteCarloResult result = monteCarloResults.get(balanceId);
        if (result == null) {
            status.put("status", "NOT_STARTED");
            status.put("message", "尚未执行蒙特卡洛模拟");
        } else {
            status.put("status", result.getStatus());
            status.put("simulationCount", result.getSimulationCount());
            if ("COMPLETED".equals(result.getStatus())) {
                status.put("meanCombinedUncertainty", result.getMeanCombinedUncertainty());
                status.put("expandedUncertainty_k2", result.getExpandedUncertainty_k2());
            }
        }
        return status;
    }

    private double[] calculateHistogram(DescriptiveStatistics stats, int binCount) {
        double min = stats.getMin();
        double max = stats.getMax();
        double range = max - min;
        if (range <= 0) return new double[binCount];

        double binWidth = range / binCount;
        double[] histogram = new double[binCount];

        double[] values = stats.getValues();
        int total = values.length;

        for (double value : values) {
            int binIndex = (int) ((value - min) / binWidth);
            if (binIndex >= binCount) binIndex = binCount - 1;
            if (binIndex < 0) binIndex = 0;
            histogram[binIndex]++;
        }

        for (int i = 0; i < binCount; i++) {
            histogram[i] = histogram[i] / total;
        }

        return histogram;
    }

    private double[] calculatePercentiles(DescriptiveStatistics stats) {
        double[] percentiles = new double[101];
        double[] sorted = stats.getSortedValues();
        int n = sorted.length;

        for (int p = 0; p <= 100; p++) {
            if (n == 0) {
                percentiles[p] = 0.0;
            } else {
                double index = (p / 100.0) * (n - 1);
                int lower = (int) Math.floor(index);
                int upper = (int) Math.ceil(index);
                double weight = index - lower;
                if (lower == upper) {
                    percentiles[p] = sorted[lower];
                } else {
                    percentiles[p] = sorted[lower] * (1 - weight) + sorted[upper] * weight;
                }
            }
        }

        return percentiles;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MonteCarloResult {
        private Long balanceId;
        private Integer simulationCount;
        private String status;

        private Double meanCombinedUncertainty;
        private Double stdCombinedUncertainty;
        private Double expandedUncertainty_k2;
        private Double confidenceLevel95;
        private Double confidenceLevel99;
        private Double minCombinedUncertainty;
        private Double maxCombinedUncertainty;

        private double[] histogram;
        private Integer histogramBinCount;

        private Double frictionMean;
        private Double frictionStd;
        private Double armErrorMean;
        private Double armErrorStd;
        private Double knifeRadiusMean;
        private Double knifeRadiusStd;
    }
}
