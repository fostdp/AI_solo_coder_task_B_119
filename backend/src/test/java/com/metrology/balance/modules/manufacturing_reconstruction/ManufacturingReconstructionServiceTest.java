package com.metrology.balance.modules.manufacturing_reconstruction;

import com.metrology.balance.entity.Balance;
import com.metrology.balance.entity.BalanceMeasurement;
import com.metrology.balance.entity.ManufacturingAnalysis;
import com.metrology.balance.repository.BalanceMeasurementRepository;
import com.metrology.balance.repository.BalanceRepository;
import com.metrology.balance.repository.ManufacturingAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("制造工艺反演服务测试")
class ManufacturingReconstructionServiceTest {

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private BalanceMeasurementRepository measurementRepository;

    @Mock
    private ManufacturingAnalysisRepository analysisRepository;

    @InjectMocks
    private ManufacturingReconstructionService service;

    private Balance bronzeBalance;
    private Balance agateBalance;
    private Balance ironBalance;
    private List<BalanceMeasurement> measurements;

    @BeforeEach
    void setUp() {
        bronzeBalance = new Balance();
        bronzeBalance.setId(1L);
        bronzeBalance.setName("战国青铜天平");
        bronzeBalance.setBalanceType("EQUAL_ARM");
        bronzeBalance.setLeftArmLength(new BigDecimal("180.0000"));
        bronzeBalance.setRightArmLength(new BigDecimal("179.9500"));
        bronzeBalance.setKnifeEdgeRadius(new BigDecimal("0.500000"));
        bronzeBalance.setMaterial("青铜");

        agateBalance = new Balance();
        agateBalance.setId(2L);
        agateBalance.setName("唐代玛瑙天平");
        agateBalance.setBalanceType("EQUAL_ARM");
        agateBalance.setLeftArmLength(new BigDecimal("200.0000"));
        agateBalance.setRightArmLength(new BigDecimal("199.9900"));
        agateBalance.setKnifeEdgeRadius(new BigDecimal("0.150000"));
        agateBalance.setMaterial("玛瑙");

        ironBalance = new Balance();
        ironBalance.setId(3L);
        ironBalance.setName("汉代铁权天平");
        ironBalance.setBalanceType("UNEQUAL_ARM");
        ironBalance.setLeftArmLength(new BigDecimal("150.0000"));
        ironBalance.setRightArmLength(new BigDecimal("149.8000"));
        ironBalance.setKnifeEdgeRadius(new BigDecimal("1.200000"));
        ironBalance.setMaterial("铁");

        measurements = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            BalanceMeasurement m = new BalanceMeasurement();
            m.setId((long) i);
            m.setKnifeEdgeWearDepth(new BigDecimal("0.000" + (10 + i)));
            m.setKnifeEdgeFriction(new BigDecimal("0.0008" + i));
            measurements.add(m);
        }
    }

    @Nested
    @DisplayName("正常场景测试")
    class NormalScenarios {

        @Test
        @DisplayName("青铜天平工艺反演 - 验证范铸工艺推断")
        void testBronzeBalanceCraftInference() {
            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(
                    eq(1L), any(Pageable.class))).thenReturn(measurements);
            when(analysisRepository.save(any(ManufacturingAnalysis.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ManufacturingAnalysis result = service.analyzeManufacturingTechnology(1);

            assertNotNull(result);
            assertEquals(1, result.getBalanceId());
            assertNotNull(result.getOverallTechnologyGrade());
            assertNotNull(result.getInferredCraftMethod());

            assertTrue(result.getKnifeEdgeGeometryScore() > 60 && result.getKnifeEdgeGeometryScore() < 95,
                    "青铜刀口几何评分应在60-95之间");

            assertTrue(result.getSurfaceRoughnessScore() > 50 && result.getSurfaceRoughnessScore() < 95,
                    "表面粗糙度评分应在50-95之间");

            assertTrue(result.getMaterialQualityScore() > 60,
                    "材料质量评分应大于60");

            assertTrue(result.getAssemblyPrecisionScore() > 50,
                    "装配精度评分应大于50");

            double overall = (result.getKnifeEdgeGeometryScore() + result.getSurfaceRoughnessScore()
                    + result.getMaterialQualityScore() + result.getAssemblyPrecisionScore()) / 4.0;
            assertTrue(overall > 50 && overall < 100,
                    "综合评分应在50-100之间: " + overall);

            verify(analysisRepository, times(1)).save(any(ManufacturingAnalysis.class));
        }

        @Test
        @DisplayName("玛瑙天平工艺反演 - 验证高精度琢磨工艺")
        void testAgateBalanceHighPrecision() {
            when(balanceRepository.findById(2L)).thenReturn(Optional.of(agateBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(
                    eq(2L), any(Pageable.class))).thenReturn(Collections.emptyList());
            when(analysisRepository.save(any(ManufacturingAnalysis.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ManufacturingAnalysis result = service.analyzeManufacturingTechnology(2);

            assertNotNull(result);
            assertTrue(result.getKnifeEdgeGeometryScore() > 90,
                    "玛瑙小刀口应有高几何评分: " + result.getKnifeEdgeGeometryScore());

            double expectedRatioError = Math.abs(agateBalance.getLeftArmLength().subtract(agateBalance.getRightArmLength()).doubleValue())
                    / ((agateBalance.getLeftArmLength().doubleValue() + agateBalance.getRightArmLength().doubleValue()) / 2.0);
            assertEquals(expectedRatioError, result.getArmLengthRatioError(), 0.001,
                    "臂长比误差应正确计算");
        }

        @Test
        @DisplayName("六级评分等级验证 - 妙品/能品/佳品区间")
        void testSixGradeRatingSystem() {
            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(
                    eq(1L), any(Pageable.class))).thenReturn(measurements);
            when(analysisRepository.save(any(ManufacturingAnalysis.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ManufacturingAnalysis result = service.analyzeManufacturingTechnology(1);

            String grade = result.getOverallTechnologyGrade();
            assertNotNull(grade);

            List<String> validGrades = Arrays.asList("神品", "妙品", "能品", "佳品", "常品", "残品");
            assertTrue(validGrades.contains(grade),
                    "评分等级必须是六级之一: " + grade);

            double overall = (result.getKnifeEdgeGeometryScore() + result.getSurfaceRoughnessScore()
                    + result.getMaterialQualityScore() + result.getAssemblyPrecisionScore()) / 4.0;

            if (grade.equals("神品")) assertTrue(overall >= 95);
            else if (grade.equals("妙品")) assertTrue(overall >= 85 && overall < 95);
            else if (grade.equals("能品")) assertTrue(overall >= 75 && overall < 85);
            else if (grade.equals("佳品")) assertTrue(overall >= 65 && overall < 75);
            else if (grade.equals("常品")) assertTrue(overall >= 50 && overall < 65);
            else if (grade.equals("残品")) assertTrue(overall < 50);
        }

        @Test
        @DisplayName("原始数据完整性验证 - 包含所有必要字段")
        void testRawDataCompleteness() {
            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(
                    eq(1L), any(Pageable.class))).thenReturn(measurements);
            when(analysisRepository.save(any(ManufacturingAnalysis.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ManufacturingAnalysis result = service.analyzeManufacturingTechnology(1);

            Map<String, Object> rawData = result.getRawData();
            assertNotNull(rawData);
            assertTrue(rawData.containsKey("knifeEdgeRadius"));
            assertTrue(rawData.containsKey("material"));
            assertTrue(rawData.containsKey("avgWearDepth"));
            assertTrue(rawData.containsKey("avgFrictionCoefficient"));
            assertTrue(rawData.containsKey("overallScore"));
            assertTrue(rawData.containsKey("materialHardness"));
            assertTrue(rawData.containsKey("craftMethodDetails"));
            assertTrue(rawData.containsKey("measurementCount"));
            assertEquals(10, rawData.get("measurementCount"));
        }

        @Test
        @DisplayName("工艺推断与材质匹配验证 - 青铜对应范铸")
        void testCraftMethodMaterialMatching() {
            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(
                    eq(1L), any(Pageable.class))).thenReturn(measurements);
            when(analysisRepository.save(any(ManufacturingAnalysis.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ManufacturingAnalysis result = service.analyzeManufacturingTechnology(1);

            String craft = result.getInferredCraftMethod();
            assertNotNull(craft);
            assertTrue(craft.contains("范铸") || craft.contains("失蜡") || craft.contains("锻打") ||
                            craft.contains("琢磨") || craft.contains("钻孔") || craft.contains("切削"),
                    "工艺推断应在已知工艺列表中: " + craft);

            Map<String, Object> rawData = result.getRawData();
            assertNotNull(rawData.get("craftMethodDetails"));
        }

        @Test
        @DisplayName("材料质量评分 - 不同材质有明显区分度")
        void testMaterialQualityScoreDifference() {
            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(balanceRepository.findById(2L)).thenReturn(Optional.of(agateBalance));
            when(balanceRepository.findById(3L)).thenReturn(Optional.of(ironBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(
                    anyLong(), any(Pageable.class))).thenReturn(Collections.emptyList());
            when(analysisRepository.save(any(ManufacturingAnalysis.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ManufacturingAnalysis bronzeResult = service.analyzeManufacturingTechnology(1);
            ManufacturingAnalysis agateResult = service.analyzeManufacturingTechnology(2);
            ManufacturingAnalysis ironResult = service.analyzeManufacturingTechnology(3);

            assertTrue(agateResult.getMaterialQualityScore() > bronzeResult.getMaterialQualityScore(),
                    "玛瑙材料质量评分应高于青铜");
        }
    }

    @Nested
    @DisplayName("边界场景测试")
    class BoundaryScenarios {

        @Test
        @DisplayName("完美等臂天平 - 臂长差为0时装配精度满分")
        void testPerfectEqualArmBalance() {
            Balance perfectBalance = new Balance();
            perfectBalance.setId(10L);
            perfectBalance.setName("完美等臂天平");
            perfectBalance.setBalanceType("EQUAL_ARM");
            perfectBalance.setLeftArmLength(new BigDecimal("200.0000"));
            perfectBalance.setRightArmLength(new BigDecimal("200.0000"));
            perfectBalance.setKnifeEdgeRadius(new BigDecimal("0.200000"));
            perfectBalance.setMaterial("玛瑙");

            when(balanceRepository.findById(10L)).thenReturn(Optional.of(perfectBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(
                    eq(10L), any(Pageable.class))).thenReturn(Collections.emptyList());
            when(analysisRepository.save(any(ManufacturingAnalysis.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ManufacturingAnalysis result = service.analyzeManufacturingTechnology(10);

            assertTrue(result.getAssemblyPrecisionScore() > 90,
                    "完美等臂应有高装配精度评分: " + result.getAssemblyPrecisionScore());
            assertEquals(0.0, result.getArmLengthRatioError(), 0.00001,
                    "完美等臂的臂长比误差应为0");
        }

        @Test
        @DisplayName("极小刀口半径 - 几何精度最高档")
        void testVerySmallKnifeRadius() {
            Balance precisionBalance = new Balance();
            precisionBalance.setId(11L);
            precisionBalance.setName("精密玛瑙天平");
            precisionBalance.setBalanceType("EQUAL_ARM");
            precisionBalance.setLeftArmLength(new BigDecimal("200.0000"));
            precisionBalance.setRightArmLength(new BigDecimal("199.9950"));
            precisionBalance.setKnifeEdgeRadius(new BigDecimal("0.050000"));
            precisionBalance.setMaterial("玛瑙");

            when(balanceRepository.findById(11L)).thenReturn(Optional.of(precisionBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(
                    eq(11L), any(Pageable.class))).thenReturn(Collections.emptyList());
            when(analysisRepository.save(any(ManufacturingAnalysis.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ManufacturingAnalysis result = service.analyzeManufacturingTechnology(11);

            assertTrue(result.getKnifeEdgeGeometryScore() >= 95,
                    "极小刀口半径应有高几何评分: " + result.getKnifeEdgeGeometryScore());
        }

        @Test
        @DisplayName("极大刀口半径 - 几何精度最低档")
        void testVeryLargeKnifeRadius() {
            Balance roughBalance = new Balance();
            roughBalance.setId(12L);
            roughBalance.setName("粗制铁天平");
            roughBalance.setBalanceType("EQUAL_ARM");
            roughBalance.setLeftArmLength(new BigDecimal("150.0000"));
            roughBalance.setRightArmLength(new BigDecimal("148.0000"));
            roughBalance.setKnifeEdgeRadius(new BigDecimal("3.000000"));
            roughBalance.setMaterial("铁");

            when(balanceRepository.findById(12L)).thenReturn(Optional.of(roughBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(
                    eq(12L), any(Pageable.class))).thenReturn(Collections.emptyList());
            when(analysisRepository.save(any(ManufacturingAnalysis.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ManufacturingAnalysis result = service.analyzeManufacturingTechnology(12);

            assertTrue(result.getKnifeEdgeGeometryScore() < 60,
                    "大刀口半径应有较低几何评分: " + result.getKnifeEdgeGeometryScore());
        }

        @Test
        @DisplayName("零测量数据 - 使用默认摩擦系数计算")
        void testNoMeasurementData() {
            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(
                    eq(1L), any(Pageable.class))).thenReturn(Collections.emptyList());
            when(analysisRepository.save(any(ManufacturingAnalysis.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ManufacturingAnalysis result = service.analyzeManufacturingTechnology(1);

            assertNotNull(result);
            assertNotNull(result.getSurfaceRoughnessScore());
            assertTrue(result.getSurfaceRoughnessScore() > 0,
                    "无测量数据时也应计算表面粗糙度评分");

            Map<String, Object> rawData = result.getRawData();
            assertEquals(0, rawData.get("measurementCount"));
        }

        @Test
        @DisplayName("大量测量数据 - 100条数据统计稳定性")
        void testLargeMeasurementDataset() {
            List<BalanceMeasurement> manyMeasurements = new ArrayList<>();
            Random random = new Random(42);
            for (int i = 0; i < 100; i++) {
                BalanceMeasurement m = new BalanceMeasurement();
                m.setId((long) i);
                m.setKnifeEdgeWearDepth(BigDecimal.valueOf(0.0001 + random.nextGaussian() * 0.00002));
                m.setKnifeEdgeFriction(BigDecimal.valueOf(0.0008 + random.nextGaussian() * 0.0001));
                manyMeasurements.add(m);
            }

            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(
                    eq(1L), any(Pageable.class))).thenReturn(manyMeasurements);
            when(analysisRepository.save(any(ManufacturingAnalysis.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ManufacturingAnalysis result = service.analyzeManufacturingTechnology(1);

            assertNotNull(result);
            assertTrue(result.getMaterialHomogeneity() > 50,
                    "材料均匀度应大于50");

            Map<String, Object> rawData = result.getRawData();
            assertTrue((Double) rawData.get("avgFrictionCoefficient") > 0,
                    "平均摩擦系数应大于0");
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionScenarios {

        @Test
        @DisplayName("天平不存在 - 抛出IllegalArgumentException")
        void testBalanceNotFound() {
            when(balanceRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> service.analyzeManufacturingTechnology(999));

            assertTrue(exception.getMessage().contains("不存在"));
            assertTrue(exception.getMessage().contains("999"));

            verify(analysisRepository, never()).save(any());
        }

        @Test
        @DisplayName("null材料 - 自动降级为青铜默认值")
        void testNullMaterialFallback() {
            Balance nullMaterialBalance = new Balance();
            nullMaterialBalance.setId(20L);
            nullMaterialBalance.setName("未知材质天平");
            nullMaterialBalance.setBalanceType("EQUAL_ARM");
            nullMaterialBalance.setLeftArmLength(new BigDecimal("180.0000"));
            nullMaterialBalance.setRightArmLength(new BigDecimal("179.9000"));
            nullMaterialBalance.setKnifeEdgeRadius(new BigDecimal("0.500000"));
            nullMaterialBalance.setMaterial(null);

            when(balanceRepository.findById(20L)).thenReturn(Optional.of(nullMaterialBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(
                    eq(20L), any(Pageable.class))).thenReturn(Collections.emptyList());
            when(analysisRepository.save(any(ManufacturingAnalysis.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ManufacturingAnalysis result = service.analyzeManufacturingTechnology(20);

            assertNotNull(result);
            assertNotNull(result.getInferredCraftMethod(),
                    "null材质不应崩溃，应使用默认材质");

            Map<String, Object> rawData = result.getRawData();
            assertEquals("青铜", rawData.get("material"));
        }

        @Test
        @DisplayName("臂长为0 - 防止除零异常")
        void testZeroArmLength() {
            Balance zeroArmBalance = new Balance();
            zeroArmBalance.setId(21L);
            zeroArmBalance.setName("异常天平");
            zeroArmBalance.setBalanceType("EQUAL_ARM");
            zeroArmBalance.setLeftArmLength(new BigDecimal("0.0000"));
            zeroArmBalance.setRightArmLength(new BigDecimal("0.0000"));
            zeroArmBalance.setKnifeEdgeRadius(new BigDecimal("0.500000"));
            zeroArmBalance.setMaterial("青铜");

            when(balanceRepository.findById(21L)).thenReturn(Optional.of(zeroArmBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(
                    eq(21L), any(Pageable.class))).thenReturn(Collections.emptyList());
            when(analysisRepository.save(any(ManufacturingAnalysis.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            assertDoesNotThrow(() -> service.analyzeManufacturingTechnology(21),
                    "臂长为0时不应抛出除零异常");
        }

        @Test
        @DisplayName("负刀口半径 - 验证评分处理")
        void testNegativeKnifeRadius() {
            Balance negativeRadiusBalance = new Balance();
            negativeRadiusBalance.setId(22L);
            negativeRadiusBalance.setName("异常数据天平");
            negativeRadiusBalance.setBalanceType("EQUAL_ARM");
            negativeRadiusBalance.setLeftArmLength(new BigDecimal("180.0000"));
            negativeRadiusBalance.setRightArmLength(new BigDecimal("179.9000"));
            negativeRadiusBalance.setKnifeEdgeRadius(new BigDecimal("-0.500000"));
            negativeRadiusBalance.setMaterial("青铜");

            when(balanceRepository.findById(22L)).thenReturn(Optional.of(negativeRadiusBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(
                    eq(22L), any(Pageable.class))).thenReturn(Collections.emptyList());
            when(analysisRepository.save(any(ManufacturingAnalysis.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            assertDoesNotThrow(() -> service.analyzeManufacturingTechnology(22),
                    "负刀口半径不应导致崩溃");
        }

        @Test
        @DisplayName("Repository保存失败 - 异常向上传递")
        void testRepositorySaveFailure() {
            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(
                    eq(1L), any(Pageable.class))).thenReturn(measurements);
            when(analysisRepository.save(any(ManufacturingAnalysis.class)))
                    .thenThrow(new RuntimeException("数据库连接失败"));

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> service.analyzeManufacturingTechnology(1));

            assertEquals("数据库连接失败", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("考古证据一致性验证")
    class ArchaeologicalEvidenceValidation {

        @Test
        @DisplayName("战国青铜天平 - 工艺推断与考古记录一致")
        void testWarringStatesBronzeConsistency() {
            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(
                    eq(1L), any(Pageable.class))).thenReturn(measurements);
            when(analysisRepository.save(any(ManufacturingAnalysis.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ManufacturingAnalysis result = service.analyzeManufacturingTechnology(1);

            Map<String, Object> rawData = result.getRawData();
            double hardness = (Double) rawData.get("materialHardness");

            assertTrue(hardness > 50 && hardness < 300,
                    "青铜硬度应在合理范围内(50-300HB): " + hardness);

            String era = result.getEstimatedManufacturingEra();
            assertNotNull(era);
            assertTrue(era.contains("战国") || era.contains("春秋") || era.contains("先秦") ||
                            era.contains("汉代") || era.contains("唐代") || era.contains("宋代"),
                    "推断年代应为中国历史朝代之一: " + era);
        }

        @Test
        @DisplayName("唐代玛瑙天平 - 精度等级与历史记载匹配")
        void testTangDynastyAgateConsistency() {
            when(balanceRepository.findById(2L)).thenReturn(Optional.of(agateBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(
                    eq(2L), any(Pageable.class))).thenReturn(Collections.emptyList());
            when(analysisRepository.save(any(ManufacturingAnalysis.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ManufacturingAnalysis result = service.analyzeManufacturingTechnology(2);

            double overall = (result.getKnifeEdgeGeometryScore() + result.getSurfaceRoughnessScore()
                    + result.getMaterialQualityScore() + result.getAssemblyPrecisionScore()) / 4.0;

            assertTrue(overall > 70,
                    "唐代玛瑙天平应有较高工艺评分: " + overall);

            assertTrue(result.getGeometryToleranceMicrom() > 0,
                    "几何公差应为正数");
        }

        @Test
        @DisplayName("表面粗糙度Ra推断 - 与摩擦系数正相关")
        void testSurfaceRoughnessCorrelation() {
            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(
                    eq(1L), any(Pageable.class))).thenReturn(measurements);
            when(analysisRepository.save(any(ManufacturingAnalysis.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ManufacturingAnalysis result = service.analyzeManufacturingTechnology(1);

            Double ra = result.getInferredSurfaceRoughnessRa();
            assertNotNull(ra);
            assertTrue(ra > 0, "表面粗糙度Ra应为正数: " + ra);
            assertTrue(ra < 100, "表面粗糙度Ra应在合理范围内: " + ra);
        }
    }
}
