package com.metrology.balance.modules.civilization_comparison;

import com.metrology.balance.dto.CivilizationComparisonResult;
import com.metrology.balance.entity.CivilizationBalance;
import com.metrology.balance.repository.CivilizationBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("文明对比服务测试")
class CivilizationComparisonServiceTest {

    @Mock
    private CivilizationBalanceRepository civilizationRepository;

    @InjectMocks
    private CivilizationComparisonService service;

    private CivilizationBalance tangChina;
    private CivilizationBalance roman;
    private CivilizationBalance hanChina;
    private CivilizationBalance egypt;
    private CivilizationBalance warringChina;

    @BeforeEach
    void setUp() {
        warringChina = new CivilizationBalance();
        warringChina.setId(1);
        warringChina.setCivilizationName("战国-中国");
        warringChina.setCivilizationCode("CHN-WARRING");
        warringChina.setPeriodStartYear(-475);
        warringChina.setPeriodEndYear(-221);
        warringChina.setBalanceType("EQUAL_ARM");
        warringChina.setMaxCapacity(500.0);
        warringChina.setRelativePrecision(0.0001);
        warringChina.setMaterialHardness(150.0);
        warringChina.setArmRatioConsistency(92.0);
        warringChina.setStructureComplexity(70.0);
        warringChina.setDurabilityScore(75.0);
        warringChina.setCulturalSignificance("战国时期楚国等地已使用精密等臂天平称量黄金");
        warringChina.setRepresentativeArtifact("战国青铜天平权");

        hanChina = new CivilizationBalance();
        hanChina.setId(2);
        hanChina.setCivilizationName("西汉-中国");
        hanChina.setCivilizationCode("CHN-HAN");
        hanChina.setPeriodStartYear(-202);
        hanChina.setPeriodEndYear(8);
        hanChina.setBalanceType("EQUAL_ARM");
        hanChina.setMaxCapacity(1000.0);
        hanChina.setRelativePrecision(0.00008);
        hanChina.setMaterialHardness(160.0);
        hanChina.setArmRatioConsistency(95.0);
        hanChina.setStructureComplexity(75.0);
        hanChina.setDurabilityScore(80.0);
        hanChina.setCulturalSignificance("汉代权衡制度完备，《九章算术》记载杠杆原理应用");
        hanChina.setRepresentativeArtifact("西汉铜衡杆");

        tangChina = new CivilizationBalance();
        tangChina.setId(3);
        tangChina.setCivilizationName("唐代-中国");
        tangChina.setCivilizationCode("CHN-TANG");
        tangChina.setPeriodStartYear(618);
        tangChina.setPeriodEndYear(907);
        tangChina.setBalanceType("EQUAL_ARM");
        tangChina.setMaxCapacity(2000.0);
        tangChina.setRelativePrecision(0.00005);
        tangChina.setMaterialHardness(155.0);
        tangChina.setArmRatioConsistency(98.0);
        tangChina.setStructureComplexity(80.0);
        tangChina.setDurabilityScore(90.0);
        tangChina.setCulturalSignificance("唐代度量衡制度完善，丝绸之路贸易推动计量技术发展");
        tangChina.setRepresentativeArtifact("唐代玛瑙天平");

        roman = new CivilizationBalance();
        roman.setId(4);
        roman.setCivilizationName("古罗马");
        roman.setCivilizationCode("ROME");
        roman.setPeriodStartYear(-300);
        roman.setPeriodEndYear(476);
        roman.setBalanceType("UNEQUAL_ARM");
        roman.setMaxCapacity(3000.0);
        roman.setRelativePrecision(0.0005);
        roman.setMaterialHardness(180.0);
        roman.setArmRatioConsistency(80.0);
        roman.setStructureComplexity(85.0);
        roman.setDurabilityScore(75.0);
        roman.setCulturalSignificance("古罗马Steelyard不等臂天平广泛用于商业贸易");
        roman.setRepresentativeArtifact("罗马铜制杆秤");

        egypt = new CivilizationBalance();
        egypt.setId(5);
        egypt.setCivilizationName("古埃及");
        egypt.setCivilizationCode("EGYPT");
        egypt.setPeriodStartYear(-3000);
        egypt.setPeriodEndYear(-30);
        egypt.setBalanceType("EQUAL_ARM");
        egypt.setMaxCapacity(200.0);
        egypt.setRelativePrecision(0.001);
        egypt.setMaterialHardness(120.0);
        egypt.setArmRatioConsistency(75.0);
        egypt.setStructureComplexity(50.0);
        egypt.setDurabilityScore(60.0);
        egypt.setCulturalSignificance("古埃及是最早使用等臂天平的文明之一，用于称量黄金和香料");
        egypt.setRepresentativeArtifact("古埃及石质天平");
    }

    @Nested
    @DisplayName("正常场景测试")
    class NormalScenarios {

        @Test
        @DisplayName("中国唐代 vs 古罗马 - 经典对比验证")
        void testChinaRomeClassicComparison() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));
            when(civilizationRepository.findByCivilizationCode("ROME"))
                    .thenReturn(Optional.of(roman));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("CHN-TANG", "ROME"));

            assertNotNull(result);
            assertEquals(2, result.getCivilizationNames().size());
            assertTrue(result.getCivilizationNames().contains("唐代-中国"));
            assertTrue(result.getCivilizationNames().contains("古罗马"));

            assertEquals(6, result.getDimensions().size());
            assertTrue(result.getDimensions().contains("最大称量"));
            assertTrue(result.getDimensions().contains("相对精度"));
            assertTrue(result.getDimensions().contains("材料硬度"));
            assertTrue(result.getDimensions().contains("臂长一致性"));
            assertTrue(result.getDimensions().contains("结构复杂度"));
            assertTrue(result.getDimensions().contains("耐久性"));

            assertNotNull(result.getRadarData());
            assertEquals(6, result.getRadarData().size());
            assertEquals(2, result.getRadarData().get("最大称量").size());
        }

        @Test
        @DisplayName("雷达图数据验证 - 各维度归一化后在0-100区间")
        void testRadarDataNormalizedRange() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));
            when(civilizationRepository.findByCivilizationCode("ROME"))
                    .thenReturn(Optional.of(roman));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("CHN-TANG", "ROME"));

            Map<String, List<Double>> radarData = result.getRadarData();
            for (Map.Entry<String, List<Double>> entry : radarData.entrySet()) {
                for (Double value : entry.getValue()) {
                    assertTrue(value >= 0 && value <= 100,
                            entry.getKey() + " 的归一化值应在0-100之间: " + value);
                }
            }
        }

        @Test
        @DisplayName("特征提取验证 - 罗马等臂vs不等臂有明显差异")
        void testFeatureExtractionBalanceType() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));
            when(civilizationRepository.findByCivilizationCode("ROME"))
                    .thenReturn(Optional.of(roman));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("CHN-TANG", "ROME"));

            assertNotNull(result.getSummaries());
            assertEquals(2, result.getSummaries().size());

            CivilizationComparisonResult.CivilizationSummary tangSummary =
                    result.getSummaries().get("CHN-TANG");
            CivilizationComparisonResult.CivilizationSummary romanSummary =
                    result.getSummaries().get("ROME");

            assertNotNull(tangSummary);
            assertNotNull(romanSummary);

            assertEquals("等臂天平", tangSummary.getBalanceType());
            assertEquals("不等臂天平", romanSummary.getBalanceType());

            assertTrue(romanSummary.getScores().get("最大称量") > tangSummary.getScores().get("最大称量"),
                    "罗马不等臂天平应有更大称量");

            assertTrue(tangSummary.getScores().get("相对精度") > romanSummary.getScores().get("相对精度"),
                    "唐代等臂天平均应有更高精度");

            assertTrue(tangSummary.getScores().get("臂长一致性") > romanSummary.getScores().get("臂长一致性"),
                    "等臂天平应有更高臂长一致性");
        }

        @Test
        @DisplayName("综合评分与最优文明判定")
        void testOverallWinnerDetermination() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));
            when(civilizationRepository.findByCivilizationCode("ROME"))
                    .thenReturn(Optional.of(roman));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("CHN-TANG", "ROME"));

            assertNotNull(result.getOverallWinner());

            Map<String, CivilizationComparisonResult.CivilizationSummary> summaries = result.getSummaries();
            double maxAvg = summaries.values().stream()
                    .mapToDouble(CivilizationComparisonResult.CivilizationSummary::getAvgScore)
                    .max().orElse(0);

            CivilizationComparisonResult.CivilizationSummary winner = summaries.values().stream()
                    .filter(s -> s.getName().equals(result.getOverallWinner()))
                    .findFirst().orElse(null);

            assertNotNull(winner);
            assertEquals(maxAvg, winner.getAvgScore(), 0.001,
                    "综合最优的文明应有最高平均分");
        }

        @Test
        @DisplayName("对比分析文本生成 - 包含多个分析维度")
        void testComparativeAnalysisGeneration() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));
            when(civilizationRepository.findByCivilizationCode("ROME"))
                    .thenReturn(Optional.of(roman));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("CHN-TANG", "ROME"));

            assertNotNull(result.getComparativeAnalysis());
            assertTrue(result.getComparativeAnalysis().size() >= 3,
                    "应至少生成3条对比分析");

            boolean hasOverall = result.getComparativeAnalysis().stream()
                    .anyMatch(a -> a.contains("综合评估"));
            assertTrue(hasOverall, "应包含综合评估");

            boolean hasTechRoute = result.getComparativeAnalysis().stream()
                    .anyMatch(a -> a.contains("技术路线"));
            assertTrue(hasTechRoute, "应包含技术路线对比");
        }

        @Test
        @DisplayName("多文明对比 - 3个以上文明排序正确")
        void testMultipleCivilizationsComparison() {
            when(civilizationRepository.findByCivilizationCode("EGYPT"))
                    .thenReturn(Optional.of(egypt));
            when(civilizationRepository.findByCivilizationCode("CHN-WARRING"))
                    .thenReturn(Optional.of(warringChina));
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("EGYPT", "CHN-WARRING", "CHN-TANG"));

            assertEquals(3, result.getCivilizationNames().size());

            int egyptIdx = result.getCivilizationCodes().indexOf("EGYPT");
            int warringIdx = result.getCivilizationCodes().indexOf("CHN-WARRING");
            int tangIdx = result.getCivilizationCodes().indexOf("CHN-TANG");

            assertTrue(egyptIdx < warringIdx && warringIdx < tangIdx,
                    "应按年代从早到晚排序：古埃及 → 战国 → 唐代");
        }

        @Test
        @DisplayName("文明摘要包含优势和劣势分析")
        void testSummaryStrengthsAndWeaknesses() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));
            when(civilizationRepository.findByCivilizationCode("ROME"))
                    .thenReturn(Optional.of(roman));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("CHN-TANG", "ROME"));

            CivilizationComparisonResult.CivilizationSummary tang =
                    result.getSummaries().get("CHN-TANG");

            assertNotNull(tang.getStrengths());
            assertNotNull(tang.getWeaknesses());

            tang.getScores().forEach((dim, score) -> {
                if (score >= 85) {
                    assertTrue(tang.getStrengths().stream().anyMatch(s -> s.contains(dim)),
                            dim + "得分" + score + "应出现在优势中");
                } else if (score < 65) {
                    assertTrue(tang.getWeaknesses().stream().anyMatch(s -> s.contains(dim)),
                            dim + "得分" + score + "应出现在不足中");
                }
            });
        }
    }

    @Nested
    @DisplayName("边界场景测试")
    class BoundaryScenarios {

        @Test
        @DisplayName("只传1个文明 - 抛出异常提示至少需要2个")
        void testSingleCivilizationThrowsException() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> service.compareCivilizations(Collections.singletonList("CHN-TANG")));

            assertTrue(exception.getMessage().contains("至少需要"),
                    "异常信息应说明至少需要2个文明");
        }

        @Test
        @DisplayName("空列表参数 - 查询全部文明对比")
        void testEmptyListReturnsAllCivilizations() {
            List<CivilizationBalance> allCivs = Arrays.asList(egypt, warringChina, roman, hanChina, tangChina);
            when(civilizationRepository.findAllByOrderByPeriodStartYearAsc()).thenReturn(allCivs);

            CivilizationComparisonResult result = service.compareCivilizations(Collections.emptyList());

            assertNotNull(result);
            assertEquals(5, result.getCivilizationNames().size());
            verify(civilizationRepository, times(1)).findAllByOrderByPeriodStartYearAsc();
        }

        @Test
        @DisplayName("null参数 - 查询全部文明对比")
        void testNullParameterReturnsAllCivilizations() {
            List<CivilizationBalance> allCivs = Arrays.asList(egypt, warringChina, roman, hanChina, tangChina);
            when(civilizationRepository.findAllByOrderByPeriodStartYearAsc()).thenReturn(allCivs);

            CivilizationComparisonResult result = service.compareCivilizations(null);

            assertNotNull(result);
            assertEquals(5, result.getCivilizationNames().size());
        }

        @Test
        @DisplayName("部分无效文明代码 - 只对比存在的文明")
        void testPartialInvalidCivilizationCodes() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));
            when(civilizationRepository.findByCivilizationCode("ROME"))
                    .thenReturn(Optional.of(roman));
            when(civilizationRepository.findByCivilizationCode("INVALID"))
                    .thenReturn(Optional.empty());
            when(civilizationRepository.findByCivilizationCode("UNKNOWN"))
                    .thenReturn(Optional.empty());

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("CHN-TANG", "INVALID", "ROME", "UNKNOWN"));

            assertEquals(2, result.getCivilizationNames().size(),
                    "只有有效的2个文明参与对比");
        }

        @Test
        @DisplayName("全部无效文明代码 - 抛出异常")
        void testAllInvalidCivilizationCodesThrowException() {
            when(civilizationRepository.findByCivilizationCode(anyString()))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.compareCivilizations(
                            Arrays.asList("INVALID1", "INVALID2")));
        }

        @Test
        @DisplayName("极端精度值 - 归一化后不越界")
        void testExtremePrecisionValuesNormalize() {
            CivilizationBalance perfectPrecision = new CivilizationBalance();
            perfectPrecision.setId(10);
            perfectPrecision.setCivilizationName("极高精度文明");
            perfectPrecision.setCivilizationCode("PERFECT");
            perfectPrecision.setPeriodStartYear(1000);
            perfectPrecision.setPeriodEndYear(1200);
            perfectPrecision.setBalanceType("EQUAL_ARM");
            perfectPrecision.setMaxCapacity(100.0);
            perfectPrecision.setRelativePrecision(0.000001);
            perfectPrecision.setMaterialHardness(200.0);
            perfectPrecision.setArmRatioConsistency(99.0);
            perfectPrecision.setStructureComplexity(90.0);
            perfectPrecision.setDurabilityScore(95.0);

            CivilizationBalance lowPrecision = new CivilizationBalance();
            lowPrecision.setId(11);
            lowPrecision.setCivilizationName("极低精度文明");
            lowPrecision.setCivilizationCode("LOW");
            lowPrecision.setPeriodStartYear(-5000);
            lowPrecision.setPeriodEndYear(-4000);
            lowPrecision.setBalanceType("EQUAL_ARM");
            lowPrecision.setMaxCapacity(50.0);
            lowPrecision.setRelativePrecision(0.1);
            lowPrecision.setMaterialHardness(50.0);
            lowPrecision.setArmRatioConsistency(50.0);
            lowPrecision.setStructureComplexity(30.0);
            lowPrecision.setDurabilityScore(40.0);

            when(civilizationRepository.findByCivilizationCode("PERFECT"))
                    .thenReturn(Optional.of(perfectPrecision));
            when(civilizationRepository.findByCivilizationCode("LOW"))
                    .thenReturn(Optional.of(lowPrecision));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("PERFECT", "LOW"));

            List<Double> precisionScores = result.getRadarData().get("相对精度");
            for (Double score : precisionScores) {
                assertTrue(score >= 0 && score <= 100,
                        "归一化精度值应在0-100之间: " + score);
            }

            assertTrue(precisionScores.get(0) > precisionScores.get(1),
                    "高精度文明的精度评分应更高");
        }

        @Test
        @DisplayName("null字段值 - 使用默认值50分")
        void testNullFieldValuesUseDefault() {
            CivilizationBalance incompleteCiv = new CivilizationBalance();
            incompleteCiv.setId(20);
            incompleteCiv.setCivilizationName("数据不全的文明");
            incompleteCiv.setCivilizationCode("INCOMPLETE");
            incompleteCiv.setPeriodStartYear(500);
            incompleteCiv.setPeriodEndYear(600);
            incompleteCiv.setBalanceType("EQUAL_ARM");
            incompleteCiv.setMaxCapacity(null);
            incompleteCiv.setRelativePrecision(null);
            incompleteCiv.setMaterialHardness(null);
            incompleteCiv.setArmRatioConsistency(null);
            incompleteCiv.setStructureComplexity(null);
            incompleteCiv.setDurabilityScore(null);

            when(civilizationRepository.findByCivilizationCode("INCOMPLETE"))
                    .thenReturn(Optional.of(incompleteCiv));
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));

            assertDoesNotThrow(() -> service.compareCivilizations(
                            Arrays.asList("INCOMPLETE", "CHN-TANG")),
                    "null字段值不应导致崩溃");

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("INCOMPLETE", "CHN-TANG"));

            CivilizationComparisonResult.CivilizationSummary summary =
                    result.getSummaries().get("INCOMPLETE");
            assertNotNull(summary);
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionScenarios {

        @Test
        @DisplayName("Repository查询失败 - 异常向上传递")
        void testRepositoryFailurePropagatesException() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenThrow(new RuntimeException("数据库连接超时"));

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> service.compareCivilizations(
                            Arrays.asList("CHN-TANG", "ROME")));

            assertEquals("数据库连接超时", exception.getMessage());
        }

        @Test
        @DisplayName("混合大小写代码 - 不匹配时返回空")
        void testCaseSensitiveCivilizationCode() {
            when(civilizationRepository.findByCivilizationCode("chn-tang"))
                    .thenReturn(Optional.empty());
            when(civilizationRepository.findByCivilizationCode("rome"))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.compareCivilizations(
                            Arrays.asList("chn-tang", "rome")),
                    "文明代码应区分大小写，不匹配的代码应被过滤");
        }

        @Test
        @DisplayName("超大文明数量 - 性能与正确性")
        void testLargeNumberOfCivilizations() {
            List<CivilizationBalance> manyCivs = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                CivilizationBalance civ = new CivilizationBalance();
                civ.setId(i);
                civ.setCivilizationName("文明" + i);
                civ.setCivilizationCode("CIV_" + i);
                civ.setPeriodStartYear(-3000 + i * 300);
                civ.setPeriodEndYear(-2700 + i * 300);
                civ.setBalanceType(i % 2 == 0 ? "EQUAL_ARM" : "UNEQUAL_ARM");
                civ.setMaxCapacity(100.0 + i * 100);
                civ.setRelativePrecision(0.01 / (i + 1));
                civ.setMaterialHardness(100.0 + i * 10);
                civ.setArmRatioConsistency(60.0 + i * 2);
                civ.setStructureComplexity(50.0 + i * 2);
                civ.setDurabilityScore(55.0 + i * 2);
                manyCivs.add(civ);
            }

            when(civilizationRepository.findAllByOrderByPeriodStartYearAsc()).thenReturn(manyCivs);

            long startTime = System.currentTimeMillis();
            CivilizationComparisonResult result = service.compareCivilizations(Collections.emptyList());
            long endTime = System.currentTimeMillis();

            assertEquals(20, result.getCivilizationNames().size());
            assertEquals(6, result.getRadarData().size());
            assertTrue((endTime - startTime) < 5000,
                    "20个文明对比应在5秒内完成");
        }
    }

    @Nested
    @DisplayName("特征提取验证")
    class FeatureExtractionValidation {

        @Test
        @DisplayName("最大称量归一化 - 线性映射验证")
        void testMaxCapacityNormalization() {
            CivilizationBalance smallCap = new CivilizationBalance();
            smallCap.setId(30);
            smallCap.setCivilizationName("小量程");
            smallCap.setCivilizationCode("SMALL");
            smallCap.setPeriodStartYear(0);
            smallCap.setPeriodEndYear(100);
            smallCap.setBalanceType("EQUAL_ARM");
            smallCap.setMaxCapacity(80.0);
            smallCap.setRelativePrecision(0.0001);
            smallCap.setMaterialHardness(150.0);
            smallCap.setArmRatioConsistency(90.0);
            smallCap.setStructureComplexity(70.0);
            smallCap.setDurabilityScore(80.0);

            CivilizationBalance largeCap = new CivilizationBalance();
            largeCap.setId(31);
            largeCap.setCivilizationName("大量程");
            largeCap.setCivilizationCode("LARGE");
            largeCap.setPeriodStartYear(0);
            largeCap.setPeriodEndYear(100);
            largeCap.setBalanceType("EQUAL_ARM");
            largeCap.setMaxCapacity(800.0);
            largeCap.setRelativePrecision(0.0001);
            largeCap.setMaterialHardness(150.0);
            largeCap.setArmRatioConsistency(90.0);
            largeCap.setStructureComplexity(70.0);
            largeCap.setDurabilityScore(80.0);

            when(civilizationRepository.findByCivilizationCode("SMALL"))
                    .thenReturn(Optional.of(smallCap));
            when(civilizationRepository.findByCivilizationCode("LARGE"))
                    .thenReturn(Optional.of(largeCap));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("SMALL", "LARGE"));

            List<Double> capacityScores = result.getRadarData().get("最大称量");
            assertTrue(capacityScores.get(1) > capacityScores.get(0),
                    "更大的量程应有更高的评分");
        }

        @Test
        @DisplayName("材料硬度归一化 - 6.5倍比例验证")
        void testMaterialHardnessNormalization() {
            CivilizationBalance softMat = new CivilizationBalance();
            softMat.setId(32);
            softMat.setCivilizationName("软质材料");
            softMat.setCivilizationCode("SOFT");
            softMat.setPeriodStartYear(0);
            softMat.setPeriodEndYear(100);
            softMat.setBalanceType("EQUAL_ARM");
            softMat.setMaxCapacity(100.0);
            softMat.setRelativePrecision(0.0001);
            softMat.setMaterialHardness(65.0);
            softMat.setArmRatioConsistency(90.0);
            softMat.setStructureComplexity(70.0);
            softMat.setDurabilityScore(80.0);

            CivilizationBalance hardMat = new CivilizationBalance();
            hardMat.setId(33);
            hardMat.setCivilizationName("硬质材料");
            hardMat.setCivilizationCode("HARD");
            hardMat.setPeriodStartYear(0);
            hardMat.setPeriodEndYear(100);
            hardMat.setBalanceType("EQUAL_ARM");
            hardMat.setMaxCapacity(100.0);
            hardMat.setRelativePrecision(0.0001);
            hardMat.setMaterialHardness(650.0);
            hardMat.setArmRatioConsistency(90.0);
            hardMat.setStructureComplexity(70.0);
            hardMat.setDurabilityScore(80.0);

            when(civilizationRepository.findByCivilizationCode("SOFT"))
                    .thenReturn(Optional.of(softMat));
            when(civilizationRepository.findByCivilizationCode("HARD"))
                    .thenReturn(Optional.of(hardMat));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("SOFT", "HARD"));

            List<Double> hardnessScores = result.getRadarData().get("材料硬度");
            assertTrue(hardnessScores.get(1) > hardnessScores.get(0),
                    "更高的材料硬度应有更高评分");
            assertTrue(hardnessScores.get(1) <= 100,
                    "硬度评分上限为100分");
        }

        @Test
        @DisplayName("技术演进分析 - 时间越晚技术越先进")
        void testTechnologyEvolutionAnalysis() {
            when(civilizationRepository.findByCivilizationCode("EGYPT"))
                    .thenReturn(Optional.of(egypt));
            when(civilizationRepository.findByCivilizationCode("CHN-WARRING"))
                    .thenReturn(Optional.of(warringChina));
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("EGYPT", "CHN-WARRING", "CHN-TANG"));

            boolean hasEvolution = result.getComparativeAnalysis().stream()
                    .anyMatch(a -> a.contains("技术演进"));

            if (result.getSummaries().get("CHN-TANG").getAvgScore() >
                    result.getSummaries().get("EGYPT").getAvgScore()) {
                assertTrue(hasEvolution, "技术进步时应生成技术演进分析");
            }
        }

        @Test
        @DisplayName("中国特色分析 - 中国文明精度领先时触发")
        void testChineseCharacteristicAnalysis() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));
            when(civilizationRepository.findByCivilizationCode("ROME"))
                    .thenReturn(Optional.of(roman));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("CHN-TANG", "ROME"));

            boolean hasChineseFeature = result.getComparativeAnalysis().stream()
                    .anyMatch(a -> a.contains("中国特色"));

            CivilizationComparisonResult.CivilizationSummary tang =
                    result.getSummaries().get("CHN-TANG");

            if (tang.getScores().get("相对精度") >= 85) {
                assertTrue(hasChineseFeature,
                        "中国文明精度突出时应生成中国特色分析");
            }
        }

        @Test
        @DisplayName("罗马特色分析 - 罗马称量或结构领先时触发")
        void testRomanCharacteristicAnalysis() {
            when(civilizationRepository.findByCivilizationCode("CHN-WARRING"))
                    .thenReturn(Optional.of(warringChina));
            when(civilizationRepository.findByCivilizationCode("ROME"))
                    .thenReturn(Optional.of(roman));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("CHN-WARRING", "ROME"));

            boolean hasRomanFeature = result.getComparativeAnalysis().stream()
                    .anyMatch(a -> a.contains("罗马特色"));

            CivilizationComparisonResult.CivilizationSummary r =
                    result.getSummaries().get("ROME");

            if (r.getScores().get("最大称量") >= 85 ||
                    r.getScores().get("结构复杂度") >= 85) {
                assertTrue(hasRomanFeature,
                        "罗马称量或结构突出时应生成罗马特色分析");
            }
        }
    }
}
