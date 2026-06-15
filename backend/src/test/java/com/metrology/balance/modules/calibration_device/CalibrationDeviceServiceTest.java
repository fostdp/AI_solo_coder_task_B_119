package com.metrology.balance.modules.calibration_device;

import com.metrology.balance.entity.Balance;
import com.metrology.balance.entity.CalibrationDevice;
import com.metrology.balance.entity.CalibrationResult;
import com.metrology.balance.modules.calibration_device.model.CalibrationReport;
import com.metrology.balance.modules.calibration_device.service.CalibrationDeviceService;
import com.metrology.balance.repository.BalanceRepository;
import com.metrology.balance.repository.CalibrationDeviceRepository;
import com.metrology.balance.repository.CalibrationResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("校准装置服务测试")
class CalibrationDeviceServiceTest {

    @Mock
    private CalibrationDeviceRepository deviceRepository;

    @Mock
    private CalibrationResultRepository resultRepository;

    @Mock
    private BalanceRepository balanceRepository;

    @InjectMocks
    private CalibrationDeviceService service;

    private CalibrationDevice precisionDevice;
    private CalibrationDevice standardDevice;
    private CalibrationDevice workshopDevice;
    private CalibrationDevice unequalArmDevice;
    private Balance testBalance;

    @BeforeEach
    void setUp() {
        precisionDevice = new CalibrationDevice();
        precisionDevice.setId(1);
        precisionDevice.setDeviceCode("PREC-001");
        precisionDevice.setDeviceName("精密校准装置");
        precisionDevice.setDeviceType("PRECISION_CALIBRATOR");
        precisionDevice.setLeftArmLength(250.0);
        precisionDevice.setRightArmLength(249.99);
        precisionDevice.setKnifeEdgeRadius(0.02);
        precisionDevice.setMaxCapacity(200.0);
        precisionDevice.setMinReadability(0.00001);
        precisionDevice.setMaterial("玛瑙");

        standardDevice = new CalibrationDevice();
        standardDevice.setId(2);
        standardDevice.setDeviceCode("STD-001");
        standardDevice.setDeviceName("标准校准装置");
        standardDevice.setDeviceType("STANDARD_CALIBRATOR");
        standardDevice.setLeftArmLength(180.0);
        standardDevice.setRightArmLength(179.95);
        standardDevice.setKnifeEdgeRadius(0.1);
        standardDevice.setMaxCapacity(500.0);
        standardDevice.setMinReadability(0.0001);
        standardDevice.setMaterial("青铜");

        workshopDevice = new CalibrationDevice();
        workshopDevice.setId(3);
        workshopDevice.setDeviceCode("WORK-001");
        workshopDevice.setDeviceName("车间校准装置");
        workshopDevice.setDeviceType("WORKSHOP");
        workshopDevice.setLeftArmLength(150.0);
        workshopDevice.setRightArmLength(149.5);
        workshopDevice.setKnifeEdgeRadius(0.5);
        workshopDevice.setMaxCapacity(1000.0);
        workshopDevice.setMinReadability(0.01);
        workshopDevice.setMaterial("铸铁");

        unequalArmDevice = new CalibrationDevice();
        unequalArmDevice.setId(4);
        unequalArmDevice.setDeviceCode("UNEQ-001");
        unequalArmDevice.setDeviceName("不等臂校准装置");
        unequalArmDevice.setDeviceType("UNEQUAL_ARM");
        unequalArmDevice.setLeftArmLength(100.0);
        unequalArmDevice.setRightArmLength(500.0);
        unequalArmDevice.setKnifeEdgeRadius(0.2);
        unequalArmDevice.setMaxCapacity(3000.0);
        unequalArmDevice.setMinReadability(0.001);
        unequalArmDevice.setMaterial("钢");

        testBalance = new Balance();
        testBalance.setId(1L);
        testBalance.setBalanceCode("BAL-001");
        testBalance.setName("测试天平");
        testBalance.setBalanceType("EQUAL_ARM");
        testBalance.setLeftArmLength(new BigDecimal("180.0000"));
        testBalance.setRightArmLength(new BigDecimal("179.9500"));
        testBalance.setKnifeEdgeRadius(new BigDecimal("0.500000"));
        testBalance.setMaterial("青铜");
    }

    @Nested
    @DisplayName("正常场景测试")
    class NormalScenarios {

        @Test
        @DisplayName("7点校准流程 - 验证校准点数量和数据完整性")
        void testSevenPointCalibrationProcess() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(precisionDevice));
            when(balanceRepository.findById(1L)).thenReturn(Optional.of(testBalance));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(1, 1, "MULTI_POSITION_LEVER");

            assertNotNull(result);
            assertEquals(1, result.getDeviceId());
            assertEquals(1, result.getBalanceId());
            assertEquals("MULTI_POSITION_LEVER", result.getCalibrationMethod());
            assertNotNull(result.getCalibrationTime());

            assertNotNull(result.getPositionsData());
            assertEquals(7, result.getPositionsData().size(),
                    "7点校准应有7个校准点位");

            for (int i = 0; i < 7; i++) {
                Map<String, Object> posData = (Map<String, Object>) result.getPositionsData().get("pos_" + i);
                assertNotNull(posData);
                assertTrue(posData.containsKey("nominal"));
                assertTrue(posData.containsKey("leftReading"));
                assertTrue(posData.containsKey("rightReading"));
                assertTrue(posData.containsKey("leftError"));
                assertTrue(posData.containsKey("rightError"));
                assertTrue(posData.containsKey("correction"));
            }

            verify(resultRepository, times(1)).save(any(CalibrationResult.class));
        }

        @Test
        @DisplayName("线性误差计算 - 验证线性回归结果")
        void testLinearityErrorCalculation() {
            when(deviceRepository.findById(2)).thenReturn(Optional.of(standardDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(2, null, "SEVEN_POINT");

            assertNotNull(result.getLinearityError());
            assertTrue(result.getLinearityError() >= 0,
                    "线性误差应为非负数");
            assertTrue(result.getLinearityError() < 100.0,
                    "线性误差百分比应在合理范围内");
        }

        @Test
        @DisplayName("滞后误差计算 - 验证滞后误差值")
        void testHysteresisErrorCalculation() {
            when(deviceRepository.findById(2)).thenReturn(Optional.of(standardDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(2, null, "SEVEN_POINT");

            assertNotNull(result.getHysteresisError());
            assertTrue(result.getHysteresisError() >= 0,
                    "滞后误差应为非负数");
        }

        @Test
        @DisplayName("重复性测量 - 10次测量统计验证")
        void testRepeatabilityMeasurement() {
            when(deviceRepository.findById(2)).thenReturn(Optional.of(standardDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(2, null, "SEVEN_POINT");

            assertNotNull(result.getRawMeasurements());
            assertNotNull(result.getRepeatabilityStd());
            assertTrue(result.getRepeatabilityStd() >= 0,
                    "重复性标准偏差应为非负数");

            int repCount = 0;
            for (String key : result.getRawMeasurements().keySet()) {
                if (key.startsWith("rep_")) {
                    repCount++;
                }
            }
            assertEquals(10, repCount, "应有10次重复性测量");
        }

        @Test
        @DisplayName("校正系数与校正表 - 验证校正表数据")
        void testCorrectionTable() {
            when(deviceRepository.findById(2)).thenReturn(Optional.of(standardDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(2, null, "SEVEN_POINT");

            assertNotNull(result.getCorrectionTable());
            assertEquals(7, result.getCorrectionTable().size(),
                    "校正表应有7个条目");

            for (Object entry : result.getCorrectionTable().values()) {
                Map<String, Object> correctionEntry = (Map<String, Object>) entry;
                assertTrue(correctionEntry.containsKey("nominal"));
                assertTrue(correctionEntry.containsKey("correctionValue"));
                assertTrue(correctionEntry.containsKey("uncertainty"));
                assertTrue(correctionEntry.containsKey("kFactor"));
                assertEquals(2.0, correctionEntry.get("kFactor"),
                        "包含因子k应为2");
            }
        }

        @Test
        @DisplayName("校准等级判定 - E1~M2等级验证")
        void testCalibrationGradeDetermination() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(precisionDevice));
            when(deviceRepository.findById(3)).thenReturn(Optional.of(workshopDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult precisionResult = service.calibrateBalance(1, null, "SEVEN_POINT");
            CalibrationResult workshopResult = service.calibrateBalance(3, null, "SEVEN_POINT");

            List<String> validGrades = Arrays.asList("E1", "E2", "F1", "F2", "M1", "M2");
            assertTrue(validGrades.contains(precisionResult.getCalibrationGrade()),
                    "精密装置校准等级应为OIML标准之一");
            assertTrue(validGrades.contains(workshopResult.getCalibrationGrade()),
                    "车间装置校准等级应为OIML标准之一");

            List<String> precisionOrder = Arrays.asList("E1", "E2", "F1", "F2", "M1", "M2");
            int precisionIdx = precisionOrder.indexOf(precisionResult.getCalibrationGrade());
            int workshopIdx = precisionOrder.indexOf(workshopResult.getCalibrationGrade());
            assertTrue(precisionIdx <= workshopIdx,
                    "精密装置的校准等级应高于或等于车间装置");
        }

        @Test
        @DisplayName("不确定度预算 - 8项来源验证")
        void testUncertaintyBudget() {
            CalibrationResult mockResult = new CalibrationResult();
            mockResult.setId(1);
            mockResult.setDeviceId(1);
            mockResult.setCalibrationMethod("SEVEN_POINT");
            mockResult.setCalibrationGrade("F1");
            mockResult.setCorrectedUncertainty(0.0005);
            mockResult.setArmLengthRatioCorrection(0.001);
            mockResult.setLeftArmCorrection(0.05);
            mockResult.setRightArmCorrection(0.02);
            mockResult.setZeroPointDrift(0.001);
            mockResult.setLinearityError(0.03);
            mockResult.setRepeatabilityStd(0.005);
            mockResult.setHysteresisError(0.008);
            mockResult.setCorrectionTable(new LinkedHashMap<>());
            mockResult.setPositionsData(new LinkedHashMap<>());
            mockResult.setRawMeasurements(new LinkedHashMap<>());
            mockResult.setCalibrationTime(java.time.LocalDateTime.now());

            when(resultRepository.findById(1)).thenReturn(Optional.of(mockResult));
            when(deviceRepository.findById(1)).thenReturn(Optional.of(precisionDevice));

            CalibrationReport report = service.generateCalibrationReport(1);

            assertNotNull(report.getUncertaintyBudget());
            assertEquals(8, report.getUncertaintyBudget().size(),
                    "不确定度预算应有8项来源");

            List<String> expectedSources = Arrays.asList(
                    "重复性测量", "线性误差", "零点漂移", "滞后误差",
                    "环境振动影响", "标准砝码不确定度", "温度影响", "湿度影响"
            );
            for (String source : expectedSources) {
                assertTrue(report.getUncertaintyBudget().stream()
                                .anyMatch(us -> source.equals(us.getSource())),
                        "不确定度来源应包含: " + source);
            }
        }
    }

    @Nested
    @DisplayName("边界场景测试")
    class BoundaryScenarios {

        @Test
        @DisplayName("零点校准 - 0g测量点验证")
        void testZeroPointCalibration() {
            when(deviceRepository.findById(2)).thenReturn(Optional.of(standardDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(2, null, "SEVEN_POINT");

            Map<String, Object> zeroPos = (Map<String, Object>) result.getPositionsData().get("pos_0");
            assertNotNull(zeroPos);
            assertEquals(0.0, zeroPos.get("nominal"),
                    "第一个校准点应为0g");

            assertNotNull(result.getZeroPointDrift(),
                    "应有零点漂移数据");
        }

        @Test
        @DisplayName("满量程校准 - 最大载荷验证")
        void testFullScaleCalibration() {
            when(deviceRepository.findById(2)).thenReturn(Optional.of(standardDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(2, null, "SEVEN_POINT");

            Map<String, Object> lastPos = (Map<String, Object>) result.getPositionsData().get("pos_6");
            assertNotNull(lastPos);

            double maxNominal = (Double) lastPos.get("nominal");
            assertEquals(500.0, maxNominal, 0.001,
                    "最后一个校准点应为500g");
        }

        @Test
        @DisplayName("最小读数精度边界 - 高精度装置")
        void testMinReadabilityBoundary() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(precisionDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(1, null, "SEVEN_POINT");

            assertTrue(result.getRepeatabilityStd() > 0,
                    "重复性标准偏差应为正数");
            assertNotNull(result.getCorrectedUncertainty());
            assertTrue(result.getCorrectedUncertainty() > 0,
                    "扩展不确定度应为正数");
        }

        @Test
        @DisplayName("不等臂大比率 - 5:1臂长比")
        void testUnequalArmLargeRatio() {
            when(deviceRepository.findById(4)).thenReturn(Optional.of(unequalArmDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(4, null, "SEVEN_POINT");

            double ratio = unequalArmDevice.getRightArmLength() / unequalArmDevice.getLeftArmLength();
            assertEquals(5.0, ratio, 0.001,
                    "不等臂装置的臂长比应为5:1");

            double expectedRatioCorrection = 1.0 - ratio;
            assertEquals(expectedRatioCorrection, result.getArmLengthRatioCorrection(), 0.000001,
                    "臂长比校正系数计算应正确");
        }

        @Test
        @DisplayName("振动等级边界 - VC-A极安静环境")
        void testVibrationLevelBoundaryVC_A() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(precisionDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(1, null, "SEVEN_POINT");

            Map<String, Object> rawMeasurements = result.getRawMeasurements();
            assertNotNull(rawMeasurements);
            assertTrue(rawMeasurements.containsKey("vibrationAnalysis"),
                    "应包含振动分析数据");

            Map<String, Object> vibrationAnalysis = (Map<String, Object>) rawMeasurements.get("vibrationAnalysis");
            assertEquals("VC_A", vibrationAnalysis.get("environmentLevel"),
                    "高精度装置应对应VC-A振动等级");
        }

        @Test
        @DisplayName("减振系统边界 - 磁悬浮主动减振")
        void testIsolationSystemBoundaryActiveMagnetic() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(precisionDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(1, null, "SEVEN_POINT");

            Map<String, Object> vibrationAnalysis = (Map<String, Object>)
                    result.getRawMeasurements().get("vibrationAnalysis");
            assertEquals("ACTIVE_MAGNETIC", vibrationAnalysis.get("isolationSystem"),
                    "高精度装置应使用磁悬浮主动减振系统");
        }

        @Test
        @DisplayName("振动等级边界 - WORKSHOP车间环境")
        void testVibrationLevelBoundaryWorkshop() {
            when(deviceRepository.findById(3)).thenReturn(Optional.of(workshopDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(3, null, "SEVEN_POINT");

            Map<String, Object> vibrationAnalysis = (Map<String, Object>)
                    result.getRawMeasurements().get("vibrationAnalysis");
            assertEquals("VC_E", vibrationAnalysis.get("environmentLevel"),
                    "车间装置应对应VC-E振动等级");
        }

        @Test
        @DisplayName("减振系统边界 - 无减振系统")
        void testIsolationSystemBoundaryNone() {
            when(deviceRepository.findById(3)).thenReturn(Optional.of(workshopDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(3, null, "SEVEN_POINT");

            Map<String, Object> vibrationAnalysis = (Map<String, Object>)
                    result.getRawMeasurements().get("vibrationAnalysis");
            assertEquals("NONE", vibrationAnalysis.get("isolationSystem"),
                    "低精度装置应使用无减振系统");
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionScenarios {

        @Test
        @DisplayName("装置不存在 - 抛出IllegalArgumentException")
        void testDeviceNotFoundThrowsException() {
            when(deviceRepository.findById(999)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> service.calibrateBalance(999, 1, "SEVEN_POINT"));

            assertTrue(exception.getMessage().contains("不存在"));
            assertTrue(exception.getMessage().contains("999"));
            verify(resultRepository, never()).save(any());
        }

        @Test
        @DisplayName("天平不存在 - 抛出IllegalArgumentException")
        void testBalanceNotFoundThrowsException() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(precisionDevice));
            when(balanceRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> service.calibrateBalance(1, 999, "SEVEN_POINT"));

            assertTrue(exception.getMessage().contains("不存在"));
            assertTrue(exception.getMessage().contains("999"));
            verify(resultRepository, never()).save(any());
        }

        @Test
        @DisplayName("结果不存在 - 报告生成失败")
        void testCalibrationResultNotFoundForReport() {
            when(resultRepository.findById(999)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> service.generateCalibrationReport(999));

            assertTrue(exception.getMessage().contains("不存在"));
            assertTrue(exception.getMessage().contains("999"));
        }

        @Test
        @DisplayName("无效振动等级 - 使用默认VC_C")
        void testInvalidVibrationLevelUsesDefault() {
            Map<String, Object> result = service.simulateVibrationImpact("INVALID_LEVEL", "NONE", 0.5);

            assertNotNull(result);
            Map<String, Object> inputVibration = (Map<String, Object>) result.get("inputVibration");
            assertEquals("VC-C", inputVibration.get("code"),
                    "无效振动等级应使用默认VC-C");
        }

        @Test
        @DisplayName("无效减振类型 - 使用默认NONE")
        void testInvalidIsolationTypeUsesDefault() {
            Map<String, Object> result = service.simulateVibrationImpact("VC_C", "INVALID_TYPE", 0.5);

            assertNotNull(result);
            Map<String, Object> isolationSystem = (Map<String, Object>) result.get("isolationSystem");
            assertEquals("NONE", isolationSystem.get("type"),
                    "无效减振类型应使用默认NONE");
        }
    }

    @Nested
    @DisplayName("振动模拟专项测试")
    class VibrationSimulationTests {

        @Test
        @DisplayName("simulateVibrationImpact - 正常振动影响模拟")
        void testSimulateVibrationImpactNormal() {
            Map<String, Object> result = service.simulateVibrationImpact("VC_C", "PASSIVE_AIR", 0.1);

            assertNotNull(result);
            assertTrue(result.containsKey("inputVibration"));
            assertTrue(result.containsKey("isolationSystem"));
            assertTrue(result.containsKey("residualVibration"));
            assertTrue(result.containsKey("vibrationInducedError_mm"));
            assertTrue(result.containsKey("achievableGrade"));
            assertTrue(result.containsKey("simulationData"));
            assertTrue(result.containsKey("assessment"));
        }

        @Test
        @DisplayName("6级环境振动标准 - VC-A到WORKSHOP")
        void testSixVibrationLevels() {
            Map<String, Object> metadata = service.getVibrationMetadata();
            Map<String, Object> vibrationLevels = (Map<String, Object>) metadata.get("vibrationLevels");

            assertEquals(6, vibrationLevels.size(),
                    "应有6级环境振动标准");

            List<String> expectedLevels = Arrays.asList("VC_A", "VC_B", "VC_C", "VC_D", "VC_E", "WORKSHOP");
            for (String level : expectedLevels) {
                assertTrue(vibrationLevels.containsKey(level),
                        "应包含振动等级: " + level);
            }
        }

        @Test
        @DisplayName("5级减振系统 - NONE到ACTIVE_MAGNETIC")
        void testFiveIsolationSystems() {
            Map<String, Object> metadata = service.getVibrationMetadata();
            Map<String, Object> isolationSystems = (Map<String, Object>) metadata.get("isolationSystems");

            assertEquals(5, isolationSystems.size(),
                    "应有5级减振系统");

            List<String> expectedSystems = Arrays.asList(
                    "NONE", "PASSIVE_RUBBER", "PASSIVE_AIR", "ACTIVE_PIEZO", "ACTIVE_MAGNETIC"
            );
            for (String system : expectedSystems) {
                assertTrue(isolationSystems.containsKey(system),
                        "应包含减振系统: " + system);
            }
        }

        @Test
        @DisplayName("振动误差模拟 - 残余振动计算")
        void testVibrationErrorSimulationResidual() {
            Map<String, Object> result = service.simulateVibrationImpact("VC_C", "PASSIVE_AIR", 0.5);

            Map<String, Object> inputVib = (Map<String, Object>) result.get("inputVibration");
            Map<String, Object> residualVib = (Map<String, Object>) result.get("residualVibration");
            Map<String, Object> isolation = (Map<String, Object>) result.get("isolationSystem");

            double inputX = (Double) inputVib.get("displacementX_um");
            double residualX = (Double) residualVib.get("displacementX_um");
            double transX = (Double) isolation.get("transmissibilityX");

            assertEquals(inputX * transX, residualX, 0.0001,
                    "残余振动位移应等于输入振动乘以传递率");
        }

        @Test
        @DisplayName("振动误差模拟 - 可达校准等级")
        void testVibrationErrorSimulationAchievableGrade() {
            Map<String, Object> resultVC_A = service.simulateVibrationImpact("VC_A", "ACTIVE_MAGNETIC", 0.5);
            Map<String, Object> resultWorkshop = service.simulateVibrationImpact("WORKSHOP", "NONE", 0.5);

            String gradeVC_A = (String) resultVC_A.get("achievableGrade");
            String gradeWorkshop = (String) resultWorkshop.get("achievableGrade");

            List<String> gradeOrder = Arrays.asList("E1", "E2", "F1", "F2", "M1", "M2");
            int idxVC_A = gradeOrder.indexOf(gradeVC_A);
            int idxWorkshop = gradeOrder.indexOf(gradeWorkshop);

            assertTrue(idxVC_A <= idxWorkshop,
                    "VC-A环境可达等级应高于或等于WORKSHOP环境");
        }

        @Test
        @DisplayName("振动误差模拟 - 模拟数据点数")
        void testVibrationErrorSimulationDataPoints() {
            Map<String, Object> result = service.simulateVibrationImpact("VC_C", "NONE", 0.5);

            List<Map<String, Object>> simulationData = (List<Map<String, Object>>) result.get("simulationData");
            assertEquals(5, simulationData.size(),
                    "应有5个测试质量点");

            for (Map<String, Object> point : simulationData) {
                assertTrue(point.containsKey("nominalMass"));
                assertTrue(point.containsKey("readings"));
                assertTrue(point.containsKey("meanError"));
                assertTrue(point.containsKey("maxError"));

                double[] readings = (double[]) point.get("readings");
                assertEquals(10, readings.length,
                        "每个质量点应有10次读数");
            }
        }
    }

    @Nested
    @DisplayName("校准报告专项测试")
    class CalibrationReportTests {

        @Test
        @DisplayName("校准报告生成 - 包含所有必要信息")
        void testCalibrationReportGeneration() {
            CalibrationResult mockResult = new CalibrationResult();
            mockResult.setId(1);
            mockResult.setDeviceId(1);
            mockResult.setCalibrationMethod("SEVEN_POINT");
            mockResult.setCalibrationGrade("F1");
            mockResult.setCorrectedUncertainty(0.0005);
            mockResult.setArmLengthRatioCorrection(0.001);
            mockResult.setLeftArmCorrection(0.05);
            mockResult.setRightArmCorrection(0.02);
            mockResult.setZeroPointDrift(0.001);
            mockResult.setLinearityError(0.03);
            mockResult.setRepeatabilityStd(0.005);
            mockResult.setHysteresisError(0.008);
            mockResult.setCorrectionTable(new LinkedHashMap<>());
            mockResult.setPositionsData(new LinkedHashMap<>());
            mockResult.setRawMeasurements(new LinkedHashMap<>());
            mockResult.setCalibrationTime(java.time.LocalDateTime.now());

            when(resultRepository.findById(1)).thenReturn(Optional.of(mockResult));
            when(deviceRepository.findById(1)).thenReturn(Optional.of(precisionDevice));

            CalibrationReport report = service.generateCalibrationReport(1);

            assertNotNull(report);
            assertNotNull(report.getReportTitle());
            assertNotNull(report.getReportNumber());
            assertNotNull(report.getCalibrationDate());
            assertNotNull(report.getCalibrationMethod());
            assertNotNull(report.getDeviceInfo());
            assertNotNull(report.getCalibrationResults());
            assertNotNull(report.getConclusions());
            assertNotNull(report.getUncertaintyBudget());
        }

        @Test
        @DisplayName("校准报告 - 装置信息完整")
        void testCalibrationReportDeviceInfo() {
            CalibrationResult mockResult = new CalibrationResult();
            mockResult.setId(1);
            mockResult.setDeviceId(1);
            mockResult.setCalibrationMethod("SEVEN_POINT");
            mockResult.setCalibrationGrade("F1");
            mockResult.setCorrectedUncertainty(0.0005);
            mockResult.setArmLengthRatioCorrection(0.001);
            mockResult.setLeftArmCorrection(0.05);
            mockResult.setRightArmCorrection(0.02);
            mockResult.setZeroPointDrift(0.001);
            mockResult.setLinearityError(0.03);
            mockResult.setRepeatabilityStd(0.005);
            mockResult.setHysteresisError(0.008);
            mockResult.setCorrectionTable(new LinkedHashMap<>());
            mockResult.setPositionsData(new LinkedHashMap<>());
            mockResult.setRawMeasurements(new LinkedHashMap<>());
            mockResult.setCalibrationTime(java.time.LocalDateTime.now());

            when(resultRepository.findById(1)).thenReturn(Optional.of(mockResult));
            when(deviceRepository.findById(1)).thenReturn(Optional.of(precisionDevice));

            CalibrationReport report = service.generateCalibrationReport(1);
            CalibrationReport.DeviceInfo deviceInfo = report.getDeviceInfo();

            assertNotNull(deviceInfo);
            assertEquals(precisionDevice.getDeviceName(), deviceInfo.getDeviceName());
            assertEquals(precisionDevice.getDeviceType(), deviceInfo.getDeviceType());
            assertEquals(precisionDevice.getLeftArmLength(), deviceInfo.getLeftArmLength());
            assertEquals(precisionDevice.getRightArmLength(), deviceInfo.getRightArmLength());
            assertEquals(precisionDevice.getKnifeEdgeRadius(), deviceInfo.getKnifeEdgeRadius());
            assertEquals(precisionDevice.getMaxCapacity(), deviceInfo.getMaxCapacity());
            assertEquals(precisionDevice.getMinReadability(), deviceInfo.getMinReadability());
            assertEquals(precisionDevice.getMaterial(), deviceInfo.getMaterial());
        }

        @Test
        @DisplayName("校准报告 - 结论数量")
        void testCalibrationReportConclusions() {
            CalibrationResult mockResult = new CalibrationResult();
            mockResult.setId(1);
            mockResult.setDeviceId(1);
            mockResult.setCalibrationMethod("SEVEN_POINT");
            mockResult.setCalibrationGrade("E1");
            mockResult.setCorrectedUncertainty(0.000005);
            mockResult.setArmLengthRatioCorrection(0.00005);
            mockResult.setLeftArmCorrection(0.005);
            mockResult.setRightArmCorrection(0.002);
            mockResult.setZeroPointDrift(0.0001);
            mockResult.setLinearityError(0.003);
            mockResult.setRepeatabilityStd(0.0005);
            mockResult.setHysteresisError(0.0008);
            mockResult.setCorrectionTable(new LinkedHashMap<>());
            mockResult.setPositionsData(new LinkedHashMap<>());
            mockResult.setRawMeasurements(new LinkedHashMap<>());
            mockResult.setCalibrationTime(java.time.LocalDateTime.now());

            when(resultRepository.findById(1)).thenReturn(Optional.of(mockResult));
            when(deviceRepository.findById(1)).thenReturn(Optional.of(precisionDevice));

            CalibrationReport report = service.generateCalibrationReport(1);
            List<String> conclusions = report.getConclusions();

            assertNotNull(conclusions);
            assertTrue(conclusions.size() >= 3,
                    "报告结论应至少3条");
        }

        @Test
        @DisplayName("校准等级元数据 - 6个等级")
        void testCalibrationGradesMetadata() {
            Map<String, Object> grades = service.getCalibrationGrades();

            assertNotNull(grades);
            List<Map<String, Object>> gradeList = (List<Map<String, Object>>) grades.get("grades");
            assertEquals(6, gradeList.size(),
                    "应有6个校准等级");

            List<String> expectedCodes = Arrays.asList("E1", "E2", "F1", "F2", "M1", "M2");
            for (int i = 0; i < expectedCodes.size(); i++) {
                assertEquals(expectedCodes.get(i), gradeList.get(i).get("code"),
                        "第" + i + "个等级应为" + expectedCodes.get(i));
            }
        }

        @Test
        @DisplayName("设备查询 - getAllDevices")
        void testGetAllDevices() {
            List<CalibrationDevice> devices = Arrays.asList(precisionDevice, standardDevice, workshopDevice);
            when(deviceRepository.findAll()).thenReturn(devices);

            List<CalibrationDevice> result = service.getAllDevices();

            assertEquals(3, result.size());
            verify(deviceRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("设备查询 - getDevice")
        void testGetDevice() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(precisionDevice));

            Optional<CalibrationDevice> result = service.getDevice(1);

            assertTrue(result.isPresent());
            assertEquals("PREC-001", result.get().getDeviceCode());
        }

        @Test
        @DisplayName("校准历史 - getCalibrationHistory")
        void testGetCalibrationHistory() {
            List<CalibrationResult> history = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                CalibrationResult r = new CalibrationResult();
                r.setId(i);
                r.setCalibrationTime(java.time.LocalDateTime.now().minusDays(i));
                history.add(r);
            }

            when(resultRepository.findByBalanceIdOrderByCalibrationTimeDesc(anyInt()))
                    .thenReturn(history);

            List<CalibrationResult> result = service.getCalibrationHistory(1);

            assertEquals(5, result.size());
            assertTrue(result.get(0).getCalibrationTime().isAfter(result.get(4).getCalibrationTime()),
                    "校准历史应按时间倒序排列");
        }

        @Test
        @DisplayName("最新校准 - getLatestCalibration")
        void testGetLatestCalibration() {
            CalibrationResult latest = new CalibrationResult();
            latest.setId(1);
            latest.setCalibrationTime(java.time.LocalDateTime.now());

            when(resultRepository.findTopByBalanceIdOrderByCalibrationTimeDesc(anyInt()))
                    .thenReturn(Optional.of(latest));

            Optional<CalibrationResult> result = service.getLatestCalibration(1);

            assertTrue(result.isPresent());
            assertEquals(1, result.get().getId());
        }
    }
}
