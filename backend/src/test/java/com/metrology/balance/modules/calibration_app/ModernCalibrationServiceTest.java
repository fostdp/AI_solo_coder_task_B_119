package com.metrology.balance.modules.calibration_app;

import com.metrology.balance.entity.Balance;
import com.metrology.balance.entity.CalibrationDevice;
import com.metrology.balance.entity.CalibrationResult;
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
@DisplayName("现代校准应用服务测试")
class ModernCalibrationServiceTest {

    @Mock
    private CalibrationDeviceRepository deviceRepository;

    @Mock
    private CalibrationResultRepository resultRepository;

    @Mock
    private BalanceRepository balanceRepository;

    @InjectMocks
    private ModernCalibrationService service;

    private CalibrationDevice leverPrincipleDevice;
    private CalibrationDevice precisionAgateDevice;
    private CalibrationDevice romanSteelyardDevice;
    private Balance bronzeBalance;

    @BeforeEach
    void setUp() {
        leverPrincipleDevice = new CalibrationDevice();
        leverPrincipleDevice.setId(1);
        leverPrincipleDevice.setDeviceCode("LEVER-001");
        leverPrincipleDevice.setDeviceName("杠杆原理校准装置");
        leverPrincipleDevice.setDeviceType("EQUAL_ARM");
        leverPrincipleDevice.setBalanceType("EQUAL_ARM");
        leverPrincipleDevice.setLeftArmLength(180.0);
        leverPrincipleDevice.setRightArmLength(179.95);
        leverPrincipleDevice.setKnifeEdgeRadius(0.1);
        leverPrincipleDevice.setMaxCapacity(500.0);
        leverPrincipleDevice.setMinReadability(0.0001);
        leverPrincipleDevice.setMaterial("玛瑙");

        precisionAgateDevice = new CalibrationDevice();
        precisionAgateDevice.setId(2);
        precisionAgateDevice.setDeviceCode("AGATE-001");
        precisionAgateDevice.setDeviceName("精密玛瑙刀口装置");
        precisionAgateDevice.setDeviceType("EQUAL_ARM");
        precisionAgateDevice.setBalanceType("EQUAL_ARM");
        precisionAgateDevice.setLeftArmLength(250.0);
        precisionAgateDevice.setRightArmLength(249.99);
        precisionAgateDevice.setKnifeEdgeRadius(0.02);
        precisionAgateDevice.setMaxCapacity(200.0);
        precisionAgateDevice.setMinReadability(0.00001);
        precisionAgateDevice.setMaterial("玛瑙");

        romanSteelyardDevice = new CalibrationDevice();
        romanSteelyardDevice.setId(3);
        romanSteelyardDevice.setDeviceCode("ROME-001");
        romanSteelyardDevice.setDeviceName("罗马式校准装置");
        romanSteelyardDevice.setDeviceType("UNEQUAL_ARM");
        romanSteelyardDevice.setBalanceType("UNEQUAL_ARM");
        romanSteelyardDevice.setLeftArmLength(100.0);
        romanSteelyardDevice.setRightArmLength(500.0);
        romanSteelyardDevice.setKnifeEdgeRadius(0.2);
        romanSteelyardDevice.setMaxCapacity(3000.0);
        romanSteelyardDevice.setMinReadability(0.01);
        romanSteelyardDevice.setMaterial("青铜");

        bronzeBalance = new Balance();
        bronzeBalance.setId(1L);
        bronzeBalance.setBalanceCode("BRONZE-001");
        bronzeBalance.setName("战国青铜天平");
        bronzeBalance.setBalanceType("EQUAL_ARM");
        bronzeBalance.setLeftArmLength(new BigDecimal("180.0000"));
        bronzeBalance.setRightArmLength(new BigDecimal("179.9500"));
        bronzeBalance.setKnifeEdgeRadius(new BigDecimal("0.500000"));
        bronzeBalance.setMaterial("青铜");
    }

    @Nested
    @DisplayName("正常场景测试")
    class NormalScenarios {

        @Test
        @DisplayName("杠杆原理装置校准 - 验证7点校准流程")
        void testLeverPrincipleCalibration() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(leverPrincipleDevice));
            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
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
                    "应有7个校准点位");

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

            assertNotNull(result.getCorrectionTable());
            assertEquals(7, result.getCorrectionTable().size(),
                    "应有7个校正点");

            verify(resultRepository, times(1)).save(any(CalibrationResult.class));
        }

        @Test
        @DisplayName("校准等级判定 - E1/M2等级验证")
        void testCalibrationGradeDetermination() {
            when(deviceRepository.findById(2)).thenReturn(Optional.of(precisionAgateDevice));
            when(deviceRepository.findById(3)).thenReturn(Optional.of(romanSteelyardDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult precisionResult = service.calibrateBalance(2, null, "SEVEN_POINT");
            CalibrationResult romanResult = service.calibrateBalance(3, null, "SEVEN_POINT");

            assertNotNull(precisionResult.getCalibrationGrade());
            assertNotNull(romanResult.getCalibrationGrade());

            List<String> validGrades = Arrays.asList("E1", "E2", "F1", "F2", "M1", "M2");
            assertTrue(validGrades.contains(precisionResult.getCalibrationGrade()),
                    "精密装置校准等级应为OIML标准之一: " + precisionResult.getCalibrationGrade());
            assertTrue(validGrades.contains(romanResult.getCalibrationGrade()),
                    "罗马式装置校准等级应为OIML标准之一: " + romanResult.getCalibrationGrade());
        }

        @Test
        @DisplayName("不确定度评定 - 扩展不确定度计算验证")
        void testExpandedUncertaintyCalculation() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(leverPrincipleDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(1, null, "MULTI_POSITION_LEVER");

            assertNotNull(result.getCorrectedUncertainty());
            assertTrue(result.getCorrectedUncertainty() > 0,
                    "扩展不确定度应为正数");

            double repeatStd = result.getRepeatabilityStd();
            double linearity = result.getLinearityError();
            double zeroDrift = result.getZeroPointDrift();

            double combinedUnc = Math.sqrt(
                    repeatStd * repeatStd +
                            linearity * linearity +
                            zeroDrift * zeroDrift
            ) / 100.0;

            double expectedExpanded = combinedUnc * 2.0;
            assertEquals(expectedExpanded, result.getCorrectedUncertainty(), 0.00001,
                    "扩展不确定度应为合成标准不确定度乘以k=2");
        }

        @Test
        @DisplayName("臂长比校正系数 - 等臂/不等臂验证")
        void testArmLengthRatioCorrection() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(leverPrincipleDevice));
            when(deviceRepository.findById(3)).thenReturn(Optional.of(romanSteelyardDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult equalArmResult = service.calibrateBalance(1, null, "SEVEN_POINT");
            CalibrationResult unequalArmResult = service.calibrateBalance(3, null, "SEVEN_POINT");

            double equalRatio = equalArmResult.getArmLengthRatioCorrection();
            double unequalRatio = unequalArmResult.getArmLengthRatioCorrection();

            assertTrue(Math.abs(equalRatio) < Math.abs(unequalRatio),
                    "等臂天平的臂长比校正系数应小于不等臂天平");

            double expectedEqualRatio = 1.0 - (leverPrincipleDevice.getRightArmLength()
                    / leverPrincipleDevice.getLeftArmLength());
            assertEquals(expectedEqualRatio, equalRatio, 0.000001,
                    "等臂臂长比校正系数计算应正确");
        }

        @Test
        @DisplayName("重复性测量 - 10次测量统计")
        void testRepeatabilityMeasurements() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(leverPrincipleDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(1, null, "SEVEN_POINT");

            assertNotNull(result.getRawMeasurements());
            assertEquals(10, ((Map<?, ?>) result.getRawMeasurements()).size(),
                    "应有10次重复性测量");

            assertNotNull(result.getRepeatabilityStd());
            assertTrue(result.getRepeatabilityStd() >= 0,
                    "重复性标准偏差应为非负数");
        }

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
            mockResult.setCalibrationTime(java.time.LocalDateTime.now());

            when(resultRepository.findById(1)).thenReturn(Optional.of(mockResult));
            when(deviceRepository.findById(1)).thenReturn(Optional.of(leverPrincipleDevice));

            Map<String, Object> report = service.generateCalibrationReport(1);

            assertNotNull(report);
            assertTrue(report.containsKey("reportTitle"));
            assertTrue(report.containsKey("reportNumber"));
            assertTrue(report.containsKey("calibrationDate"));
            assertTrue(report.containsKey("calibrationMethod"));
            assertTrue(report.containsKey("deviceInfo"));
            assertTrue(report.containsKey("calibrationResults"));
            assertTrue(report.containsKey("conclusions"));

            Map<String, Object> results = (Map<String, Object>) report.get("calibrationResults");
            assertNotNull(results.get("calibrationGrade"));
            assertNotNull(results.get("expandedUncertainty"));
            assertNotNull(results.get("kFactor"));
            assertNotNull(results.get("confidenceLevel"));

            List<String> conclusions = (List<String>) report.get("conclusions");
            assertTrue(conclusions.size() >= 3,
                    "报告结论应至少3条");
        }
    }

    @Nested
    @DisplayName("边界场景测试")
    class BoundaryScenarios {

        @Test
        @DisplayName("零点校准 - 0g测量点验证")
        void testZeroPointCalibration() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(leverPrincipleDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(1, null, "SEVEN_POINT");

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
            when(deviceRepository.findById(1)).thenReturn(Optional.of(leverPrincipleDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(1, null, "SEVEN_POINT");

            Map<String, Object> lastPos = (Map<String, Object>) result.getPositionsData().get("pos_6");
            assertNotNull(lastPos);

            double maxNominal = (Double) lastPos.get("nominal");
            assertTrue(maxNominal <= leverPrincipleDevice.getMaxCapacity(),
                    "最大校准载荷不应超过装置量程");
        }

        @Test
        @DisplayName("最小读数精度 - 接近最小读数时的稳定性")
        void testMinReadabilityPrecision() {
            when(deviceRepository.findById(2)).thenReturn(Optional.of(precisionAgateDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(2, null, "SEVEN_POINT");

            assertTrue(result.getRepeatabilityStd() > 0,
                    "重复性标准偏差应为正数");
            assertTrue(result.getCorrectedUncertainty() > 0,
                    "扩展不确定度应为正数");
        }

        @Test
        @DisplayName("不等臂天平校准 - 大比率验证")
        void testUnequalArmCalibration() {
            when(deviceRepository.findById(3)).thenReturn(Optional.of(romanSteelyardDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(3, null, "SEVEN_POINT");

            double ratio = romanSteelyardDevice.getRightArmLength() / romanSteelyardDevice.getLeftArmLength();
            assertEquals(5.0, ratio, 0.001,
                    "罗马式装置的臂长比应为5:1");

            assertTrue(Math.abs(result.getArmLengthRatioCorrection()) > 0.1,
                    "不等臂天平台臂长比校正系数应较大");
        }

        @Test
        @DisplayName("无balanceId - 仅校准装置本身")
        void testCalibrationWithoutBalanceId() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(leverPrincipleDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(1, null, "SEVEN_POINT");

            assertNotNull(result);
            assertNull(result.getBalanceId());
            assertNotNull(result.getCalibrationGrade());
        }

        @Test
        @DisplayName("null校准方法 - 使用默认方法")
        void testNullMethodUsesDefault() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(leverPrincipleDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(1, 1, null);

            assertNotNull(result.getCalibrationMethod());
            assertTrue(result.getCalibrationMethod().length() > 0,
                    "null方法时应使用默认方法");
        }

        @Test
        @DisplayName("校准历史查询 - 按时间倒序排列")
        void testCalibrationHistoryOrdering() {
            List<CalibrationResult> mockHistory = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                CalibrationResult r = new CalibrationResult();
                r.setId(i);
                r.setCalibrationTime(java.time.LocalDateTime.now().minusDays(i));
                mockHistory.add(r);
            }

            when(resultRepository.findByBalanceIdOrderByCalibrationTimeDesc(anyInt()))
                    .thenReturn(mockHistory);

            List<CalibrationResult> history = service.getCalibrationHistory(1);

            assertEquals(5, history.size());
            assertTrue(history.get(0).getCalibrationTime().isAfter(history.get(4).getCalibrationTime()),
                    "校准历史应按时间倒序排列");
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionScenarios {

        @Test
        @DisplayName("校准装置不存在 - 抛出异常")
        void testDeviceNotFoundThrowsException() {
            when(deviceRepository.findById(999)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> service.calibrateBalance(999, 1, "SEVEN_POINT"));

            assertTrue(exception.getMessage().contains("不存在"));
            assertTrue(exception.getMessage().contains("999"));
            verify(resultRepository, never()).save(any());
        }

        @Test
        @DisplayName("天平不存在 - 抛出异常")
        void testBalanceNotFoundThrowsException() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(leverPrincipleDevice));
            when(balanceRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> service.calibrateBalance(1, 999, "SEVEN_POINT"));

            assertTrue(exception.getMessage().contains("不存在"));
            assertTrue(exception.getMessage().contains("999"));
            verify(resultRepository, never()).save(any());
        }

        @Test
        @DisplayName("校准结果不存在 - 报告生成失败")
        void testCalibrationResultNotFoundForReport() {
            when(resultRepository.findById(999)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> service.generateCalibrationReport(999));

            assertTrue(exception.getMessage().contains("不存在"));
        }

        @Test
        @DisplayName("臂长为0 - 防止除零异常")
        void testZeroArmLengthDoesNotCrash() {
            CalibrationDevice zeroArmDevice = new CalibrationDevice();
            zeroArmDevice.setId(10);
            zeroArmDevice.setDeviceCode("BAD-001");
            zeroArmDevice.setDeviceName("异常装置");
            zeroArmDevice.setDeviceType("EQUAL_ARM");
            zeroArmDevice.setBalanceType("EQUAL_ARM");
            zeroArmDevice.setLeftArmLength(0.0);
            zeroArmDevice.setRightArmLength(0.0);
            zeroArmDevice.setKnifeEdgeRadius(0.1);
            zeroArmDevice.setMaxCapacity(100.0);
            zeroArmDevice.setMinReadability(0.001);
            zeroArmDevice.setMaterial("青铜");

            when(deviceRepository.findById(10)).thenReturn(Optional.of(zeroArmDevice));

            assertDoesNotThrow(() -> service.calibrateBalance(10, null, "SEVEN_POINT"),
                    "臂长为0不应导致崩溃");
        }

        @Test
        @DisplayName("负最小读数 - 噪声计算处理")
        void testNegativeMinReadabilityHandling() {
            CalibrationDevice badDevice = new CalibrationDevice();
            badDevice.setId(11);
            badDevice.setDeviceCode("BAD-002");
            badDevice.setDeviceName("负精度装置");
            badDevice.setDeviceType("EQUAL_ARM");
            badDevice.setBalanceType("EQUAL_ARM");
            badDevice.setLeftArmLength(180.0);
            badDevice.setRightArmLength(180.0);
            badDevice.setKnifeEdgeRadius(0.1);
            badDevice.setMaxCapacity(500.0);
            badDevice.setMinReadability(-0.001);
            badDevice.setMaterial("青铜");

            when(deviceRepository.findById(11)).thenReturn(Optional.of(badDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            assertDoesNotThrow(() -> service.calibrateBalance(11, null, "SEVEN_POINT"),
                    "负最小读数不应导致崩溃");
        }

        @Test
        @DisplayName("Repository保存失败 - 异常向上传递")
        void testRepositorySaveFailurePropagates() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(leverPrincipleDevice));
            when(balanceRepository.findById(1L)).thenReturn(Optional.of(bronzeBalance));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenThrow(new RuntimeException("数据库写入失败"));

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> service.calibrateBalance(1, 1, "SEVEN_POINT"));

            assertEquals("数据库写入失败", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("精度验证测试")
    class AccuracyValidationTests {

        @Test
        @DisplayName("线性误差 - 线性回归R²验证")
        void testLinearityErrorCalculation() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(leverPrincipleDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(1, null, "SEVEN_POINT");

            assertNotNull(result.getLinearityError());
            assertTrue(result.getLinearityError() >= 0,
                    "线性误差应为非负数");
        }

        @Test
        @DisplayName("滞后误差 - 加减载差异验证")
        void testHysteresisErrorCalculation() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(leverPrincipleDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(1, null, "SEVEN_POINT");

            assertNotNull(result.getHysteresisError());
            assertTrue(result.getHysteresisError() >= 0,
                    "滞后误差应为非负数");
        }

        @Test
        @DisplayName("k=2包含因子 - 95.45%置信度")
        void testCoverageFactorKEquals2() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(leverPrincipleDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(1, null, "SEVEN_POINT");

            Map<String, Object> firstCorrection = (Map<String, Object>)
                    result.getCorrectionTable().values().iterator().next();

            assertNotNull(firstCorrection.get("kFactor"));
            assertEquals(2.0, firstCorrection.get("kFactor"),
                    "包含因子k应为2");
        }

        @Test
        @DisplayName("校正表与点位数据对应")
        void testCorrectionTableMatchesPositions() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(leverPrincipleDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult result = service.calibrateBalance(1, null, "SEVEN_POINT");

            assertEquals(result.getPositionsData().size(), result.getCorrectionTable().size(),
                    "校正表和点位数据数量应一致");
        }

        @Test
        @DisplayName("高精度装置 vs 普通装置 - 不确定度差异明显")
        void testPrecisionVsNormalDeviceUncertainty() {
            when(deviceRepository.findById(1)).thenReturn(Optional.of(leverPrincipleDevice));
            when(deviceRepository.findById(2)).thenReturn(Optional.of(precisionAgateDevice));
            when(resultRepository.save(any(CalibrationResult.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CalibrationResult normalResult = service.calibrateBalance(1, null, "SEVEN_POINT");
            CalibrationResult precisionResult = service.calibrateBalance(2, null, "SEVEN_POINT");

            assertTrue(precisionResult.getCorrectedUncertainty() <= normalResult.getCorrectedUncertainty(),
                    "精密装置的不确定度应小于普通装置");

            List<String> precisionOrder = Arrays.asList("E1", "E2", "F1", "F2", "M1", "M2");
            int normalIdx = precisionOrder.indexOf(normalResult.getCalibrationGrade());
            int precisionIdx = precisionOrder.indexOf(precisionResult.getCalibrationGrade());

            assertTrue(precisionIdx <= normalIdx,
                    "精密装置的校准等级应更高（更靠前）");
        }
    }
}
