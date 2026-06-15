package com.metrology.balance.modules.craft_inferrer;

import com.metrology.balance.entity.Balance;
import com.metrology.balance.entity.BalanceMeasurement;
import com.metrology.balance.modules.craft_inferrer.model.CraftMethodKnowledge;
import com.metrology.balance.modules.craft_inferrer.model.LiteratureReference;
import com.metrology.balance.modules.craft_inferrer.model.UncertaintyBudget;
import com.metrology.balance.modules.craft_inferrer.service.CraftInferrerService;
import com.metrology.balance.repository.BalanceMeasurementRepository;
import com.metrology.balance.repository.BalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("工艺推断服务测试")
class CraftInferrerServiceTest {

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private BalanceMeasurementRepository measurementRepository;

    @InjectMocks
    private CraftInferrerService craftInferrerService;

    private Balance bronzeBalance;
    private Balance jadeBalance;
    private Balance steelBalance;
    private Balance nullMaterialBalance;

    @BeforeEach
    void setUp() {
        bronzeBalance = createBalance(1L, "青铜-001", "青铜", "0.5", "180.0", "180.0");
        jadeBalance = createBalance(2L, "玉石-001", "玉石", "0.3", "180.0", "180.5");
        steelBalance = createBalance(3L, "钢铁-001", "钢", "0.6", "200.0", "200.0");
        nullMaterialBalance = createBalance(4L, "未知-001", null, "0.5", "180.0", "180.0");
    }

    private Balance createBalance(Long id, String code, String material,
                                   String knifeRadius, String leftArm, String rightArm) {
        Balance balance = new Balance();
        balance.setId(id);
        balance.setBalanceCode(code);
        balance.setName(code + "天平");
        balance.setBalanceType("EQUAL_ARM");
        balance.setMaterial(material);
        balance.setKnifeEdgeRadius(new BigDecimal(knifeRadius));
        balance.setLeftArmLength(new BigDecimal(leftArm));
        balance.setRightArmLength(new BigDecimal(rightArm));
        balance.setMaxCapacity(new BigDecimal("1000.00"));
        return balance;
    }

    private List<BalanceMeasurement> createMeasurements(int count, double baseFriction, double frictionVariation,
                                                        double baseWear, long balanceId) {
        List<BalanceMeasurement> measurements = new ArrayList<>();
        Random random = new Random(42);
        for (int i = 0; i < count; i++) {
            BalanceMeasurement m = new BalanceMeasurement();
            m.setId((long) i);
            m.setBalanceId(balanceId);
            m.setMeasurementTime(LocalDateTime.now().minusHours(count - i));
            double friction = baseFriction + (random.nextDouble() - 0.5) * frictionVariation;
            m.setKnifeEdgeFriction(BigDecimal.valueOf(friction));
            double wear = baseWear + (random.nextDouble() - 0.5) * 0.002;
            m.setKnifeEdgeWearDepth(BigDecimal.valueOf(Math.max(0, wear)));
            measurements.add(m);
        }
        return measurements;
    }

    @Nested
    @DisplayName("正常场景测试")
    class NormalScenarios {

        @Test
        @DisplayName("工艺推断 - 青铜天平充足数据")
        void testInferCraftMethod_Bronze_SufficientData() {
            List<BalanceMeasurement> measurements = createMeasurements(50, 0.0012, 0.0002, 0.03, 1L);

            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(1L))
                    .thenReturn(measurements);

            Map<String, Object> result = craftInferrerService.inferCraftMethod(1L);

            assertNotNull(result);
            assertEquals(1L, result.get("balanceId"));
            assertEquals("青铜", result.get("material"));

            String craftMethod = (String) result.get("inferredCraftMethod");
            assertNotNull(craftMethod);
            assertTrue(craftMethod.startsWith("青铜-"));

            CraftMethodKnowledge craftDetails = (CraftMethodKnowledge) result.get("craftMethodDetails");
            assertNotNull(craftDetails);
            assertNotNull(craftDetails.getMethodName());

            assertEquals("SUFFICIENT", result.get("dataSufficiency"));
            assertEquals(Boolean.FALSE, result.get("usesLiteratureEstimate"));

            assertNotNull(result.get("overallScore"));
            assertNotNull(result.get("overallTechnologyGrade"));
            assertNotNull(result.get("sixSigmaScore"));

            assertNotNull(result.get("uncertainty"));
            verify(balanceRepository, times(1)).findById(1L);
            verify(measurementRepository, times(1)).findTop100ByBalanceIdOrderByMeasurementTimeDesc(1L);
        }

        @Test
        @DisplayName("工艺推断 - 玉石天平平精度")
        void testInferCraftMethod_Jade_PartialData() {
            List<BalanceMeasurement> measurements = createMeasurements(15, 0.0005, 0.0001, 0.01, 2L);

            when(balanceRepository.findById(2L)).thenReturn(Optional.of(jadeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(2L))
                    .thenReturn(measurements);

            Map<String, Object> result = craftInferrerService.inferCraftMethod(2L);

            assertNotNull(result);
            assertEquals("玉石", result.get("material"));
            assertEquals("玉石-琢磨", result.get("inferredCraftMethod"));
            assertEquals("PARTIAL", result.get("dataSufficiency"));
            assertEquals(Boolean.FALSE, result.get("usesLiteratureEstimate"));

            double sixSigmaScore = (Double) result.get("sixSigmaScore");
            assertTrue(sixSigmaScore >= 0 && sixSigmaScore <= 100);

            String grade = (String) result.get("overallTechnologyGrade");
            assertNotNull(grade);
            assertTrue(grade.length() > 0);
        }

        @Test
        @DisplayName("不确定度评定 - A类B类计算正确")
        void testUncertaintyEvaluation_TypeAB() {
            List<BalanceMeasurement> measurements = createMeasurements(30, 0.0010, 0.00015, 0.02, 1L);

            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(1L))
                    .thenReturn(measurements);

            Map<String, Object> result = craftInferrerService.inferCraftMethod(1L);
            Map<String, Object> uncertainty = (Map<String, Object>) result.get("uncertainty");

            assertNotNull(uncertainty);

            double typeA = (Double) uncertainty.get("typeAUncertainty");
            double typeB = (Double) uncertainty.get("typeBUncertainty");
            double combined = (Double) uncertainty.get("combinedUncertainty");
            double expanded = (Double) uncertainty.get("expandedUncertainty_k2");

            assertTrue(typeA > 0, "A类不确定度应为正数");
            assertTrue(typeB > 0, "B类不确定度应为正数");
            assertTrue(combined > 0, "合成不确定度应为正数");
            assertEquals(expanded, combined * 2.0, 0.00001,
                    "扩展不确定度应为合成不确定度乘以2");

            assertEquals("95%", uncertainty.get("confidenceLevel"));
            assertEquals(2.0, uncertainty.get("coverageFactor"));

            List<UncertaintyBudget> budgets = (List<UncertaintyBudget>) uncertainty.get("budgets");
            assertNotNull(budgets);
            assertTrue(budgets.size() >= 3);

            Optional<UncertaintyBudget> typeABudget = budgets.stream()
                    .filter(b -> "A".equals(b.getType()))
                    .findFirst();
            assertTrue(typeABudget.isPresent(), "应包含A类不确定度分项");

            Optional<UncertaintyBudget> typeBBudget = budgets.stream()
                    .filter(b -> "B".equals(b.getType()))
                    .findFirst();
            assertTrue(typeBBudget.isPresent(), "应包含B类不确定度分项");
        }

        @Test
        @DisplayName("六西格玛评分 - 分值在合理范围")
        void testSixSigmaScore_ValidRange() {
            List<BalanceMeasurement> measurements = createMeasurements(30, 0.0008, 0.0001, 0.015, 1L);

            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(1L))
                    .thenReturn(measurements);

            Map<String, Object> result = craftInferrerService.inferCraftMethod(1L);

            double sixSigmaScore = (Double) result.get("sixSigmaScore");
            assertTrue(sixSigmaScore >= 0 && sixSigmaScore <= 100,
                    "六西格玛评分应在0-100之间: " + sixSigmaScore);

            double overallScore = (Double) result.get("overallScore");
            assertTrue(overallScore >= 0 && overallScore <= 100,
                    "综合评分应在0-100之间: " + overallScore);
        }

        @Test
        @DisplayName("工艺方法知识库 - 获取所有工艺方法")
        void testGetAllCraftMethods() {
            List<CraftMethodKnowledge> methods = craftInferrerService.getAllCraftMethods();

            assertNotNull(methods);
            assertFalse(methods.isEmpty());
            assertTrue(methods.size() >= 5);

            assertTrue(methods.stream().anyMatch(m -> "青铜-范铸".equals(m.getMethodKey())));
            assertTrue(methods.stream().anyMatch(m -> "青铜-失蜡".equals(m.getMethodKey())));
            assertTrue(methods.stream().anyMatch(m -> "玉石-琢磨".equals(m.getMethodKey())));
            assertTrue(methods.stream().anyMatch(m -> "钢铁-锻打".equals(m.getMethodKey())));

            CraftMethodKnowledge method = methods.get(0);
            assertNotNull(method.getMethodName());
            assertNotNull(method.getPeriod());
            assertNotNull(method.getDescription());
            assertNotNull(method.getKnifeRadiusRange());
            assertEquals(2, method.getKnifeRadiusRange().length);
            assertNotNull(method.getFrictionRange());
            assertEquals(2, method.getFrictionRange().length);
            assertNotNull(method.getProcessSteps());
            assertFalse(method.getProcessSteps().isEmpty());
        }

        @Test
        @DisplayName("文献数据获取 - 获取所有文献参考")
        void testGetAllLiteratureReferences() {
            List<LiteratureReference> references = craftInferrerService.getAllLiteratureReferences();

            assertNotNull(references);
            assertFalse(references.isEmpty());
            assertTrue(references.size() >= 5);

            assertTrue(references.stream().anyMatch(r -> "青铜".equals(r.getMaterial())));
            assertTrue(references.stream().anyMatch(r -> "玉石".equals(r.getMaterial())));
            assertTrue(references.stream().anyMatch(r -> "玛瑙".equals(r.getMaterial())));

            LiteratureReference ref = references.get(0);
            assertNotNull(ref.getSource());
            assertNotNull(ref.getFrictionRange());
            assertEquals(2, ref.getFrictionRange().length);
            assertNotNull(ref.getWearRange());
            assertEquals(2, ref.getWearRange().length);
            assertNotNull(ref.getKnifeRadiusRange());
            assertEquals(2, ref.getKnifeRadiusRange().length);
            assertTrue(ref.getBaseUncertainty() > 0);
        }
    }

    @Nested
    @DisplayName("边界场景测试")
    class BoundaryScenarios {

        @Test
        @DisplayName("零测量数据 - 纯文献估算 LITERATURE_ONLY")
        void testZeroMeasurements_PureLiterature() {
            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(1L))
                    .thenReturn(Collections.emptyList());

            Map<String, Object> result = craftInferrerService.inferCraftMethod(1L);

            assertNotNull(result);
            assertEquals("LITERATURE_ONLY", result.get("dataSufficiency"));
            assertEquals(Boolean.TRUE, result.get("usesLiteratureEstimate"));
            assertEquals(0, result.get("validMeasurementCount"));

            Map<String, Object> uncertainty = (Map<String, Object>) result.get("uncertainty");
            double typeA = (Double) uncertainty.get("typeAUncertainty");
            assertEquals(0.0, typeA, "零数据时A类不确定度应为0");

            double typeB = (Double) uncertainty.get("typeBUncertainty");
            assertTrue(typeB > 0, "零数据时B类不确定度应来自文献");

            assertNotNull(result.get("literatureSource"));
            assertNotNull(result.get("literatureFrictionRange"));
        }

        @Test
        @DisplayName("刚好10个样本 - PARTIAL边界")
        void testExactly10Samples_PartialBoundary() {
            List<BalanceMeasurement> measurements = createMeasurements(10, 0.0012, 0.0002, 0.03, 1L);

            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(1L))
                    .thenReturn(measurements);

            Map<String, Object> result = craftInferrerService.inferCraftMethod(1L);

            assertNotNull(result);
            assertEquals("PARTIAL", result.get("dataSufficiency"));
            assertEquals(10, result.get("validMeasurementCount"));
            assertEquals(Boolean.FALSE, result.get("usesLiteratureEstimate"));

            Map<String, Object> uncertainty = (Map<String, Object>) result.get("uncertainty");
            double typeA = (Double) uncertainty.get("typeAUncertainty");
            assertTrue(typeA > 0, "10个样本时应有A类不确定度");
        }

        @Test
        @DisplayName("刚好30个样本 - SUFFICIENT边界")
        void testExactly30Samples_SufficientBoundary() {
            List<BalanceMeasurement> measurements = createMeasurements(30, 0.0012, 0.0002, 0.03, 1L);

            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(1L))
                    .thenReturn(measurements);

            Map<String, Object> result = craftInferrerService.inferCraftMethod(1L);

            assertNotNull(result);
            assertEquals("SUFFICIENT", result.get("dataSufficiency"));
            assertEquals(30, result.get("validMeasurementCount"));
            assertEquals(Boolean.FALSE, result.get("usesLiteratureEstimate"));

            double overallScore = (Double) result.get("overallScore");
            assertTrue(overallScore > 0);
        }

        @Test
        @DisplayName("9个样本 - LITERATURE_SUPPLEMENTED 边界")
        void test9Samples_LiteratureSupplemented() {
            List<BalanceMeasurement> measurements = createMeasurements(9, 0.0012, 0.0002, 0.03, 1L);

            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(1L))
                    .thenReturn(measurements);

            Map<String, Object> result = craftInferrerService.inferCraftMethod(1L);

            assertNotNull(result);
            assertEquals("LITERATURE_SUPPLEMENTED", result.get("dataSufficiency"));
            assertEquals(9, result.get("validMeasurementCount"));
            assertEquals(Boolean.TRUE, result.get("usesLiteratureEstimate"));
        }

        @Test
        @DisplayName("空材料 - 默认青铜")
        void testNullMaterial_DefaultBronze() {
            List<BalanceMeasurement> measurements = createMeasurements(20, 0.0012, 0.0002, 0.03, 4L);

            when(balanceRepository.findById(4L)).thenReturn(Optional.of(nullMaterialBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(4L))
                    .thenReturn(measurements);

            Map<String, Object> result = craftInferrerService.inferCraftMethod(4L);

            assertNotNull(result);
            assertEquals("青铜", result.get("material"),
                    "null材料应默认使用青铜");

            String craftMethod = (String) result.get("inferredCraftMethod");
            assertTrue(craftMethod.startsWith("青铜-"),
                    "默认材料应为青铜工艺");
        }

        @Test
        @DisplayName("测量数据中混合null摩擦值")
        void testMixedNullFrictionMeasurements() {
            List<BalanceMeasurement> measurements = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                BalanceMeasurement m = new BalanceMeasurement();
                m.setId((long) i);
                m.setBalanceId(1L);
                m.setMeasurementTime(LocalDateTime.now().minusHours(15 - i));
                if (i < 12) {
                    m.setKnifeEdgeFriction(BigDecimal.valueOf(0.001 + i * 0.00005));
                } else {
                    m.setKnifeEdgeFriction(null);
                }
                m.setKnifeEdgeWearDepth(BigDecimal.valueOf(0.02));
                measurements.add(m);
            }

            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(1L))
                    .thenReturn(measurements);

            Map<String, Object> result = craftInferrerService.inferCraftMethod(1L);

            assertNotNull(result);
            assertEquals(12, result.get("validMeasurementCount"));
            assertEquals(15, result.get("totalMeasurementCount"));
            assertEquals("PARTIAL", result.get("dataSufficiency"));
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionScenarios {

        @Test
        @DisplayName("天平ID不存在 - 抛出异常")
        void testBalanceNotFound_ThrowsException() {
            when(balanceRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> craftInferrerService.inferCraftMethod(999L));

            assertTrue(exception.getMessage().contains("天平不存在"));
            assertTrue(exception.getMessage().contains("999"));

            verify(balanceRepository, times(1)).findById(999L);
            verify(measurementRepository, never()).findTop100ByBalanceIdOrderByMeasurementTimeDesc(anyLong());
        }

        @Test
        @DisplayName("蒙特卡洛模拟 - 天平不存在抛出异常")
        void testMonteCarlo_BalanceNotFound() {
            when(balanceRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> craftInferrerService.runMonteCarloSimulation(999L, 1000));

            assertTrue(exception.getMessage().contains("天平不存在"));
        }

        @Test
        @DisplayName("蒙特卡洛状态查询 - 未执行时返回NOT_STARTED")
        void testMonteCarloStatus_NotStarted() {
            Map<String, Object> status = craftInferrerService.getMonteCarloStatus(999L);

            assertNotNull(status);
            assertEquals("NOT_STARTED", status.get("status"));
            assertNotNull(status.get("message"));
        }

        @Test
        @DisplayName("蒙特卡洛结果查询 - 未执行时返回null")
        void testGetMonteCarloResult_NotStarted() {
            CraftInferrerService.MonteCarloResult result = craftInferrerService.getMonteCarloResult(999L);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("专项验证测试")
    class SpecialVerificationTests {

        @Test
        @DisplayName("数据充分性分级 - 四种状态全覆盖验证")
        void testDataSufficiencyAllStates() {
            assertAll("数据充分性分级验证",
                    () -> {
                        when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
                        when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(1L))
                                .thenReturn(Collections.emptyList());

                        Map<String, Object> result = craftInferrerService.inferCraftMethod(1L);
                        assertEquals("LITERATURE_ONLY", result.get("dataSufficiency"),
                                "0个样本应为LITERATURE_ONLY");
                    },
                    () -> {
                        List<BalanceMeasurement> measurements = createMeasurements(5, 0.0012, 0.0002, 0.03, 1L);
                        when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(1L))
                                .thenReturn(measurements);

                        Map<String, Object> result = craftInferrerService.inferCraftMethod(1L);
                        assertEquals("LITERATURE_SUPPLEMENTED", result.get("dataSufficiency"),
                                "5个样本应为LITERATURE_SUPPLEMENTED");
                    },
                    () -> {
                        List<BalanceMeasurement> measurements = createMeasurements(15, 0.0012, 0.0002, 0.03, 1L);
                        when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(1L))
                                .thenReturn(measurements);

                        Map<String, Object> result = craftInferrerService.inferCraftMethod(1L);
                        assertEquals("PARTIAL", result.get("dataSufficiency"),
                                "15个样本应为PARTIAL");
                    },
                    () -> {
                        List<BalanceMeasurement> measurements = createMeasurements(40, 0.0012, 0.0002, 0.03, 1L);
                        when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(1L))
                                .thenReturn(measurements);

                        Map<String, Object> result = craftInferrerService.inferCraftMethod(1L);
                        assertEquals("SUFFICIENT", result.get("dataSufficiency"),
                                "40个样本应为SUFFICIENT");
                    }
            );
        }

        @Test
        @DisplayName("工艺推断 - 不同材料对应不同工艺")
        void testInferCraftMethod_DifferentMaterials() {
            List<BalanceMeasurement> measurements = createMeasurements(20, 0.0010, 0.00015, 0.02, 1L);

            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(balanceRepository.findById(2L)).thenReturn(Optional.of(jadeBalance));
            when(balanceRepository.findById(3L)).thenReturn(Optional.of(steelBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(anyLong()))
                    .thenReturn(measurements);

            Map<String, Object> bronzeResult = craftInferrerService.inferCraftMethod(1L);
            String bronzeCraft = (String) bronzeResult.get("inferredCraftMethod");
            assertTrue(bronzeCraft.startsWith("青铜-"));

            Map<String, Object> jadeResult = craftInferrerService.inferCraftMethod(2L);
            assertEquals("玉石-琢磨", jadeResult.get("inferredCraftMethod"));

            Map<String, Object> steelResult = craftInferrerService.inferCraftMethod(3L);
            assertEquals("钢铁-锻打", steelResult.get("inferredCraftMethod"));
        }

        @Test
        @DisplayName("扩展不确定度验证 - k=2 置信度95%")
        void testExpandedUncertainty_k2() {
            List<BalanceMeasurement> measurements = createMeasurements(30, 0.0010, 0.0002, 0.02, 1L);

            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(1L))
                    .thenReturn(measurements);

            Map<String, Object> result = craftInferrerService.inferCraftMethod(1L);
            Map<String, Object> uncertainty = (Map<String, Object>) result.get("uncertainty");

            double combined = (Double) uncertainty.get("combinedUncertainty");
            double expanded = (Double) uncertainty.get("expandedUncertainty_k2");
            double relative = (Double) uncertainty.get("relativeUncertainty");

            assertEquals(combined * 2.0, expanded, 0.00001,
                    "扩展不确定度应为合成标准不确定度乘以包含因子k=2");
            assertEquals("95%", uncertainty.get("confidenceLevel"),
                    "k=2对应95%置信水平");
            assertEquals(2.0, uncertainty.get("coverageFactor"),
                    "包含因子应为2");

            assertTrue(relative > 0 && relative < 1,
                    "相对不确定度应在0-1之间");
            assertEquals(combined / ((Double) result.get("avgFrictionCoefficient")), relative, 0.00001,
                    "相对不确定度应为合成不确定度除以平均值");
        }

        @Test
        @DisplayName("蒙特卡洛模拟 - 同步执行验证结果结构")
        void testMonteCarloSimulation_ResultStructure() {
            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));

            craftInferrerService.runMonteCarloSimulation(1L, 100);

            CraftInferrerService.MonteCarloResult result = craftInferrerService.getMonteCarloResult(1L);

            assertNotNull(result);
            assertEquals(1L, result.getBalanceId());
            assertEquals(100, result.getSimulationCount());
            assertEquals("COMPLETED", result.getStatus());

            assertNotNull(result.getMeanCombinedUncertainty());
            assertTrue(result.getMeanCombinedUncertainty() > 0);

            assertNotNull(result.getStdCombinedUncertainty());
            assertNotNull(result.getExpandedUncertainty_k2());
            assertEquals(result.getMeanCombinedUncertainty() * 2.0,
                    result.getExpandedUncertainty_k2(), 0.00001);

            assertNotNull(result.getConfidenceLevel95());
            assertNotNull(result.getConfidenceLevel99());
            assertNotNull(result.getMinCombinedUncertainty());
            assertNotNull(result.getMaxCombinedUncertainty());

            assertNotNull(result.getHistogram());
            assertEquals(50, result.getHistogram().length);
            assertEquals(50, result.getHistogramBinCount());

            assertNotNull(result.getFrictionMean());
            assertNotNull(result.getFrictionStd());
            assertNotNull(result.getArmErrorMean());
            assertNotNull(result.getArmErrorStd());
            assertNotNull(result.getKnifeRadiusMean());
            assertNotNull(result.getKnifeRadiusStd());
        }

        @Test
        @DisplayName("蒙特卡洛状态查询 - 完成后返回正确状态")
        void testMonteCarloStatus_Completed() {
            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));

            craftInferrerService.runMonteCarloSimulation(1L, 50);

            Map<String, Object> status = craftInferrerService.getMonteCarloStatus(1L);

            assertNotNull(status);
            assertEquals("COMPLETED", status.get("status"));
            assertEquals(50, status.get("simulationCount"));
            assertNotNull(status.get("meanCombinedUncertainty"));
            assertNotNull(status.get("expandedUncertainty_k2"));
        }

        @Test
        @DisplayName("不确定度分项预算 - 完整性验证")
        void testUncertaintyBudgets_Completeness() {
            List<BalanceMeasurement> measurements = createMeasurements(20, 0.0010, 0.00015, 0.02, 1L);

            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(1L))
                    .thenReturn(measurements);

            Map<String, Object> result = craftInferrerService.inferCraftMethod(1L);
            Map<String, Object> uncertainty = (Map<String, Object>) result.get("uncertainty");
            List<UncertaintyBudget> budgets = (List<UncertaintyBudget>) uncertainty.get("budgets");

            assertNotNull(budgets);
            assertTrue(budgets.size() >= 4, "应至少包含4个不确定度分项");

            for (UncertaintyBudget budget : budgets) {
                assertNotNull(budget.getSource(), "每个分项应有来源描述");
                assertNotNull(budget.getType(), "每个分项应有类型(A/B)");
                assertNotNull(budget.getValue(), "每个分项应有数值");
                assertNotNull(budget.getDegreesOfFreedom(), "每个分项应有自由度");
                assertNotNull(budget.getSensitivity(), "每个分项应有灵敏系数");
                assertNotNull(budget.getContribution(), "每个分项应有贡献量");
            }

            assertTrue(budgets.stream().anyMatch(b -> b.getSource().contains("A类")),
                    "应包含A类测量重复性不确定度");
            assertTrue(budgets.stream().anyMatch(b -> b.getSource().contains("B类") || b.getSource().contains("文献")),
                    "应包含B类文献数据不确定度");
            assertTrue(budgets.stream().anyMatch(b -> b.getSource().contains("臂长")),
                    "应包含臂长误差不确定度");
            assertTrue(budgets.stream().anyMatch(b -> b.getSource().contains("刀口半径")),
                    "应包含刀口半径误差不确定度");
        }

        @Test
        @DisplayName("评分等级判定 - 各等级阈值验证")
        void testOverallGradeThresholds() {
            assertAll("评分等级验证",
                    () -> {
                        List<BalanceMeasurement> measurements = createMeasurements(30, 0.0002, 0.00002, 0.005, 2L);
                        when(balanceRepository.findById(2L)).thenReturn(Optional.of(jadeBalance));
                        when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(2L))
                                .thenReturn(measurements);
                        Map<String, Object> result = craftInferrerService.inferCraftMethod(2L);
                        String grade = (String) result.get("overallTechnologyGrade");
                        assertNotNull(grade);
                    },
                    () -> {
                        List<BalanceMeasurement> measurements = createMeasurements(30, 0.0015, 0.0005, 0.10, 1L);
                        when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
                        when(measurementRepository.findTop100ByBalanceIdOrderByMeasurementTimeDesc(1L))
                                .thenReturn(measurements);
                        Map<String, Object> result = craftInferrerService.inferCraftMethod(1L);
                        String grade = (String) result.get("overallTechnologyGrade");
                        assertNotNull(grade);
                    }
            );
        }
    }
}
