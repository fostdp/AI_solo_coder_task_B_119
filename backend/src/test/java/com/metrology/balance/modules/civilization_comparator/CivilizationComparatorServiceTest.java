package com.metrology.balance.modules.civilization_comparator;

import com.metrology.balance.dto.CivilizationComparisonResult;
import com.metrology.balance.entity.CivilizationBalance;
import com.metrology.balance.modules.civilization_comparator.model.DataSourceClassification;
import com.metrology.balance.modules.civilization_comparator.model.ExpertValidationResult;
import com.metrology.balance.modules.civilization_comparator.model.StandardizedCategory;
import com.metrology.balance.modules.civilization_comparator.service.CivilizationComparatorService;
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
@DisplayName("文明对比器服务回归测试")
class CivilizationComparatorServiceTest {

    @Mock
    private CivilizationBalanceRepository civilizationRepository;

    @InjectMocks
    private CivilizationComparatorService service;

    private CivilizationBalance hanChina;
    private CivilizationBalance rome;
    private CivilizationBalance tangChina;
    private CivilizationBalance egypt;
    private CivilizationBalance warringChina;
    private CivilizationBalance mingChina;

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
        warringChina.setDiscoveryLocation("湖北荆州");
        warringChina.setReferenceSource("考工记与考古实物测量");

        hanChina = new CivilizationBalance();
        hanChina.setId(2);
        hanChina.setCivilizationName("西汉-中国");
        hanChina.setCivilizationCode("CHN-WEST-HAN");
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
        hanChina.setDiscoveryLocation("");
        hanChina.setReferenceSource("史书记载");

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
        tangChina.setDiscoveryLocation("陕西西安");
        tangChina.setReferenceSource("考工记文献与考古实物综合");

        rome = new CivilizationBalance();
        rome.setId(4);
        rome.setCivilizationName("古罗马");
        rome.setCivilizationCode("ROME");
        rome.setPeriodStartYear(-300);
        rome.setPeriodEndYear(476);
        rome.setBalanceType("UNEQUAL_ARM");
        rome.setMaxCapacity(3000.0);
        rome.setRelativePrecision(0.0005);
        rome.setMaterialHardness(180.0);
        rome.setArmRatioConsistency(80.0);
        rome.setStructureComplexity(85.0);
        rome.setDurabilityScore(75.0);
        rome.setCulturalSignificance("古罗马Steelyard不等臂天平广泛用于商业贸易");
        rome.setRepresentativeArtifact("罗马铜制杆秤");
        rome.setDiscoveryLocation("庞贝遗址");
        rome.setReferenceSource("考古实物测量与实验复原");

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
        egypt.setDiscoveryLocation("卢克索神庙");
        egypt.setReferenceSource("考古发现");

        mingChina = new CivilizationBalance();
        mingChina.setId(6);
        mingChina.setCivilizationName("明代-中国");
        mingChina.setCivilizationCode("CHN-MING");
        mingChina.setPeriodStartYear(1368);
        mingChina.setPeriodEndYear(1644);
        mingChina.setBalanceType("UNEQUAL_ARM");
        mingChina.setMaxCapacity(5000.0);
        mingChina.setRelativePrecision(0.001);
        mingChina.setMaterialHardness(200.0);
        mingChina.setArmRatioConsistency(85.0);
        mingChina.setStructureComplexity(78.0);
        mingChina.setDurabilityScore(88.0);
        mingChina.setCulturalSignificance("明代戥秤技术成熟，广泛用于商业和民间");
        mingChina.setRepresentativeArtifact("明代象牙戥秤");
        mingChina.setDiscoveryLocation("");
        mingChina.setReferenceSource("实验复原测量");
    }

    @Nested
    @DisplayName("正常场景测试")
    class NormalScenarios {

        @Test
        @DisplayName("两文明对比 - 中罗经典对比验证")
        void testChinaRomeClassicComparison() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));
            when(civilizationRepository.findByCivilizationCode("ROME"))
                    .thenReturn(Optional.of(rome));

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
        @DisplayName("多文明对比排序 - 按年代从早到晚")
        void testMultipleCivilizationsSortedByPeriod() {
            when(civilizationRepository.findByCivilizationCode("EGYPT"))
                    .thenReturn(Optional.of(egypt));
            when(civilizationRepository.findByCivilizationCode("CHN-WARRING"))
                    .thenReturn(Optional.of(warringChina));
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));
            when(civilizationRepository.findByCivilizationCode("CHN-MING"))
                    .thenReturn(Optional.of(mingChina));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("CHN-MING", "EGYPT", "CHN-TANG", "CHN-WARRING"));

            assertEquals(4, result.getCivilizationNames().size());

            int egyptIdx = result.getCivilizationCodes().indexOf("EGYPT");
            int warringIdx = result.getCivilizationCodes().indexOf("CHN-WARRING");
            int tangIdx = result.getCivilizationCodes().indexOf("CHN-TANG");
            int mingIdx = result.getCivilizationCodes().indexOf("CHN-MING");

            assertTrue(egyptIdx < warringIdx, "古埃及应早于战国");
            assertTrue(warringIdx < tangIdx, "战国应早于唐代");
            assertTrue(tangIdx < mingIdx, "唐代应早于明代");
        }

        @Test
        @DisplayName("六维雷达图归一化 - 所有维度值在0-100区间")
        void testSixDimensionRadarNormalization() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));
            when(civilizationRepository.findByCivilizationCode("ROME"))
                    .thenReturn(Optional.of(rome));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("CHN-TANG", "ROME"));

            Map<String, List<Double>> radarData = result.getRadarData();
            assertEquals(6, radarData.size());

            for (Map.Entry<String, List<Double>> entry : radarData.entrySet()) {
                for (Double value : entry.getValue()) {
                    assertTrue(value >= 0 && value <= 100,
                            entry.getKey() + " 的归一化值应在0-100之间: " + value);
                }
            }

            assertTrue(radarData.containsKey("最大称量"));
            assertTrue(radarData.containsKey("相对精度"));
            assertTrue(radarData.containsKey("材料硬度"));
            assertTrue(radarData.containsKey("臂长一致性"));
            assertTrue(radarData.containsKey("结构复杂度"));
            assertTrue(radarData.containsKey("耐久性"));
        }

        @Test
        @DisplayName("特征提取验证 - 等臂vs不等臂特征差异明显")
        void testFeatureExtractionBalanceTypeDifference() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));
            when(civilizationRepository.findByCivilizationCode("ROME"))
                    .thenReturn(Optional.of(rome));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("CHN-TANG", "ROME"));

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
        @DisplayName("优势劣势分析 - 高分维度为优势、低分维度为劣势")
        void testStrengthsAndWeaknessesAnalysis() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));
            when(civilizationRepository.findByCivilizationCode("EGYPT"))
                    .thenReturn(Optional.of(egypt));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("CHN-TANG", "EGYPT"));

            CivilizationComparisonResult.CivilizationSummary tangSummary =
                    result.getSummaries().get("CHN-TANG");
            CivilizationComparisonResult.CivilizationSummary egyptSummary =
                    result.getSummaries().get("EGYPT");

            assertNotNull(tangSummary.getStrengths());
            assertNotNull(tangSummary.getWeaknesses());
            assertNotNull(egyptSummary.getStrengths());
            assertNotNull(egyptSummary.getWeaknesses());

            tangSummary.getScores().forEach((dim, score) -> {
                if (score >= 85) {
                    assertTrue(tangSummary.getStrengths().stream().anyMatch(s -> s.contains(dim)),
                            dim + "得分" + score + "应出现在优势中");
                } else if (score < 65) {
                    assertTrue(tangSummary.getWeaknesses().stream().anyMatch(s -> s.contains(dim)),
                            dim + "得分" + score + "应出现在劣势中");
                }
            });
        }
    }

    @Nested
    @DisplayName("数据源与标准化分类测试")
    class DataSourceAndStandardizationTests {

        @Test
        @DisplayName("数据源分类 - 考古实物类型验证")
        void testDataSourceClassificationArchaeological() {
            when(civilizationRepository.findByCivilizationCode("EGYPT"))
                    .thenReturn(Optional.of(egypt));
            when(civilizationRepository.findByCivilizationCode("ROME"))
                    .thenReturn(Optional.of(rome));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("EGYPT", "ROME"));

            Map<String, Object> egyptStd = result.getSummaries().get("EGYPT").getStandardizationInfo();
            assertNotNull(egyptStd);
            assertNotNull(egyptStd.get("dataSource"));
            assertNotNull(egyptStd.get("dataReliabilityScore"));

            Double reliability = (Double) egyptStd.get("dataReliabilityScore");
            assertTrue(reliability > 0 && reliability <= 1.0,
                    "可靠性分数应在0-1之间: " + reliability);
        }

        @Test
        @DisplayName("数据源分类 - 5种类型及可靠性验证")
        void testFiveDataSourceTypesAndReliability() {
            Map<String, Object> metadata = service.getStandardizationMetadata();
            assertNotNull(metadata);

            Map<String, Map<String, Object>> dataSources =
                    (Map<String, Map<String, Object>>) metadata.get("dataSourceClassifications");
            assertNotNull(dataSources);
            assertTrue(dataSources.size() >= 5, "至少应有5种数据源分类");

            assertTrue(dataSources.containsKey("ARCHAEOLOGICAL"));
            assertTrue(dataSources.containsKey("LITERARY"));
            assertTrue(dataSources.containsKey("EXPERT_ESTIMATE"));
            assertTrue(dataSources.containsKey("EXPERIMENTAL_RECONSTRUCTION"));
            assertTrue(dataSources.containsKey("MIXED"));

            Map<String, Object> archaeological = dataSources.get("ARCHAEOLOGICAL");
            assertEquals(0.95, (Double) archaeological.get("reliability"), 0.001);

            Map<String, Object> literary = dataSources.get("LITERARY");
            assertEquals(0.70, (Double) literary.get("reliability"), 0.001);

            Map<String, Object> expertEstimate = dataSources.get("EXPERT_ESTIMATE");
            assertEquals(0.60, (Double) expertEstimate.get("reliability"), 0.001);
        }

        @Test
        @DisplayName("标准化用途分类 - 6类衡器验证")
        void testSixStandardizedCategories() {
            Map<String, Object> metadata = service.getStandardizationMetadata();
            assertNotNull(metadata);

            Map<String, Map<String, Object>> categories =
                    (Map<String, Map<String, Object>>) metadata.get("standardizedCategories");
            assertNotNull(categories);
            assertEquals(6, categories.size());

            assertTrue(categories.containsKey("PRECISION_LEGAL"));
            assertTrue(categories.containsKey("COMMERCIAL_TRADE"));
            assertTrue(categories.containsKey("PRECIOUS_METAL"));
            assertTrue(categories.containsKey("RITUAL_CEREMONIAL"));
            assertTrue(categories.containsKey("HOUSEHOLD_DAILY"));
            assertTrue(categories.containsKey("SCIENTIFIC_METROLOGY"));

            Map<String, Object> scientific = categories.get("SCIENTIFIC_METROLOGY");
            assertNotNull(scientific.get("typicalPrecision"));
            assertNotNull(scientific.get("typicalCapacity"));
            assertNotNull(scientific.get("label"));
            assertNotNull(scientific.get("description"));
        }

        @Test
        @DisplayName("专家置信度加权 - 各维度权重不同")
        void testExpertConfidenceWeighting() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));
            when(civilizationRepository.findByCivilizationCode("ROME"))
                    .thenReturn(Optional.of(rome));

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("CHN-TANG", "ROME"));

            CivilizationComparisonResult.CivilizationSummary tangSummary =
                    result.getSummaries().get("CHN-TANG");

            Map<String, Object> stdInfo = tangSummary.getStandardizationInfo();
            assertNotNull(stdInfo.get("expertConfidenceByDimension"));

            Map<String, Double> confidenceByDim =
                    (Map<String, Double>) stdInfo.get("expertConfidenceByDimension");
            assertEquals(6, confidenceByDim.size());

            assertNotNull(stdInfo.get("weightedAverageScore"));
            Double weightedAvg = (Double) stdInfo.get("weightedAverageScore");
            assertNotNull(weightedAvg);
            assertTrue(weightedAvg >= 0 && weightedAvg <= 100,
                    "加权平均分应在0-100之间: " + weightedAvg);
        }
    }

    @Nested
    @DisplayName("专家校验方法测试")
    class ExpertValidationTests {

        @Test
        @DisplayName("validateCivilizationData - 正常数据校验通过")
        void testValidateCivilizationDataPass() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));

            ExpertValidationResult result = service.validateCivilizationData("CHN-TANG");

            assertNotNull(result);
            assertEquals("CHN-TANG", result.getCivilizationCode());
            assertEquals("唐代-中国", result.getCivilizationName());
            assertNotNull(result.getStatus());
            assertNotNull(result.getOverallConfidence());
            assertNotNull(result.getPassItems());
            assertNotNull(result.getWarningItems());
            assertNotNull(result.getFailItems());

            assertTrue(result.getOverallConfidence() >= 0 && result.getOverallConfidence() <= 1.0,
                    "置信度应在0-1之间: " + result.getOverallConfidence());

            assertNotNull(result.getDataSourceClassification());
            assertNotNull(result.getStandardizedCategory());
        }

        @Test
        @DisplayName("validateCivilizationData - 文明代码不存在返回FAIL")
        void testValidateCivilizationDataNotFound() {
            when(civilizationRepository.findByCivilizationCode("INVALID"))
                    .thenReturn(Optional.empty());

            ExpertValidationResult result = service.validateCivilizationData("INVALID");

            assertNotNull(result);
            assertEquals("INVALID", result.getCivilizationCode());
            assertEquals("未知文明", result.getCivilizationName());
            assertEquals(ExpertValidationResult.ValidationStatus.FAIL, result.getStatus());
            assertEquals(0.0, result.getOverallConfidence(), 0.001);
            assertFalse(result.getFailItems().isEmpty());
            assertTrue(result.getFailItems().get(0).contains("文明代码不存在"));
        }

        @Test
        @DisplayName("validateCivilizationData - 数据异常时校验失败")
        void testValidateCivilizationDataWithInvalidValues() {
            CivilizationBalance invalidCiv = new CivilizationBalance();
            invalidCiv.setId(100);
            invalidCiv.setCivilizationName("异常文明");
            invalidCiv.setCivilizationCode("INVALID-CIV");
            invalidCiv.setPeriodStartYear(1000);
            invalidCiv.setPeriodEndYear(500);
            invalidCiv.setBalanceType("EQUAL_ARM");
            invalidCiv.setMaxCapacity(20000.0);
            invalidCiv.setRelativePrecision(-0.001);
            invalidCiv.setMaterialHardness(2000.0);
            invalidCiv.setArmRatioConsistency(150.0);
            invalidCiv.setStructureComplexity(-10.0);
            invalidCiv.setDurabilityScore(120.0);
            invalidCiv.setCulturalSignificance("");
            invalidCiv.setRepresentativeArtifact("");
            invalidCiv.setReferenceSource("");

            when(civilizationRepository.findByCivilizationCode("INVALID-CIV"))
                    .thenReturn(Optional.of(invalidCiv));

            ExpertValidationResult result = service.validateCivilizationData("INVALID-CIV");

            assertNotNull(result);
            assertEquals(ExpertValidationResult.ValidationStatus.FAIL, result.getStatus());
            assertFalse(result.getFailItems().isEmpty());
            assertTrue(result.getFailItems().size() >= 3,
                    "应至少有3项失败: " + result.getFailItems().size());
        }
    }

    @Nested
    @DisplayName("边界场景测试")
    class BoundaryScenarios {

        @Test
        @DisplayName("单文明对比 - 抛出异常提示至少需要2个")
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
            List<CivilizationBalance> allCivs = Arrays.asList(egypt, warringChina, hanChina, tangChina, rome);
            when(civilizationRepository.findAllByOrderByPeriodStartYearAsc()).thenReturn(allCivs);

            CivilizationComparisonResult result = service.compareCivilizations(Collections.emptyList());

            assertNotNull(result);
            assertEquals(5, result.getCivilizationNames().size());
            verify(civilizationRepository, times(1)).findAllByOrderByPeriodStartYearAsc();
        }

        @Test
        @DisplayName("null参数 - 查询全部文明对比")
        void testNullParameterReturnsAllCivilizations() {
            List<CivilizationBalance> allCivs = Arrays.asList(egypt, warringChina, hanChina, tangChina, rome);
            when(civilizationRepository.findAllByOrderByPeriodStartYearAsc()).thenReturn(allCivs);

            CivilizationComparisonResult result = service.compareCivilizations(null);

            assertNotNull(result);
            assertEquals(5, result.getCivilizationNames().size());
        }

        @Test
        @DisplayName("部分文明代码无效 - 只对比存在的文明")
        void testPartialInvalidCivilizationCodes() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));
            when(civilizationRepository.findByCivilizationCode("ROME"))
                    .thenReturn(Optional.of(rome));
            when(civilizationRepository.findByCivilizationCode("INVALID1"))
                    .thenReturn(Optional.empty());
            when(civilizationRepository.findByCivilizationCode("INVALID2"))
                    .thenReturn(Optional.empty());

            CivilizationComparisonResult result = service.compareCivilizations(
                    Arrays.asList("CHN-TANG", "INVALID1", "ROME", "INVALID2"));

            assertEquals(2, result.getCivilizationNames().size(),
                    "只有有效的2个文明参与对比");
        }

        @Test
        @DisplayName("全部文明代码无效 - 抛出异常")
        void testAllInvalidCivilizationCodesThrowException() {
            when(civilizationRepository.findByCivilizationCode(anyString()))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.compareCivilizations(
                            Arrays.asList("INVALID1", "INVALID2")));
        }

        @Test
        @DisplayName("极端精度值 - 归一化后不越界")
        void testExtremePrecisionValuesNormalized() {
            CivilizationBalance perfectPrecision = new CivilizationBalance();
            perfectPrecision.setId(10);
            perfectPrecision.setCivilizationName("极高精度文明");
            perfectPrecision.setCivilizationCode("PERFECT");
            perfectPrecision.setPeriodStartYear(1000);
            perfectPrecision.setPeriodEndYear(1200);
            perfectPrecision.setBalanceType("EQUAL_ARM");
            perfectPrecision.setMaxCapacity(100.0);
            perfectPrecision.setRelativePrecision(1e-6);
            perfectPrecision.setMaterialHardness(200.0);
            perfectPrecision.setArmRatioConsistency(99.0);
            perfectPrecision.setStructureComplexity(90.0);
            perfectPrecision.setDurabilityScore(95.0);
            perfectPrecision.setReferenceSource("考古实物");

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
            lowPrecision.setReferenceSource("专家估算");

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
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionScenarios {

        @Test
        @DisplayName("Repository抛出异常 - 异常向上传递")
        void testRepositoryThrowsExceptionPropagates() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenThrow(new RuntimeException("数据库连接失败"));

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> service.compareCivilizations(
                            Arrays.asList("CHN-TANG", "ROME")));

            assertEquals("数据库连接失败", exception.getMessage());
        }

        @Test
        @DisplayName("文明代码不存在 - validate返回FAIL状态")
        void testCivilizationCodeNotExistValidation() {
            String unknownCode = "UNKNOWN-CIV";
            when(civilizationRepository.findByCivilizationCode(unknownCode))
                    .thenReturn(Optional.empty());

            ExpertValidationResult result = service.validateCivilizationData(unknownCode);

            assertEquals(ExpertValidationResult.ValidationStatus.FAIL, result.getStatus());
            assertTrue(result.getFailItems().stream()
                    .anyMatch(item -> item.contains("文明代码不存在")));
        }

        @Test
        @DisplayName("null代码 - 校验方法处理null参数")
        void testNullCodeValidation() {
            when(civilizationRepository.findByCivilizationCode(null))
                    .thenReturn(Optional.empty());

            ExpertValidationResult result = service.validateCivilizationData(null);

            assertNotNull(result);
            assertEquals(ExpertValidationResult.ValidationStatus.FAIL, result.getStatus());
        }
    }

    @Nested
    @DisplayName("辅助方法与元数据测试")
    class UtilityAndMetadataTests {

        @Test
        @DisplayName("getAllCivilizations - 返回全部文明列表")
        void testGetAllCivilizations() {
            List<CivilizationBalance> allCivs = Arrays.asList(egypt, warringChina, tangChina, rome);
            when(civilizationRepository.findAllByOrderByPeriodStartYearAsc()).thenReturn(allCivs);

            List<CivilizationBalance> result = service.getAllCivilizations();

            assertNotNull(result);
            assertEquals(4, result.size());
            verify(civilizationRepository, times(1)).findAllByOrderByPeriodStartYearAsc();
        }

        @Test
        @DisplayName("getCivilization - 根据代码查询单个文明")
        void testGetCivilizationByCode() {
            when(civilizationRepository.findByCivilizationCode("CHN-TANG"))
                    .thenReturn(Optional.of(tangChina));

            Optional<CivilizationBalance> result = service.getCivilization("CHN-TANG");

            assertTrue(result.isPresent());
            assertEquals("CHN-TANG", result.get().getCivilizationCode());
            assertEquals("唐代-中国", result.get().getCivilizationName());
        }

        @Test
        @DisplayName("getDimensionLabels - 返回六维标签映射")
        void testGetDimensionLabels() {
            Map<String, String> labels = service.getDimensionLabels();

            assertNotNull(labels);
            assertEquals(6, labels.size());
            assertEquals("最大称量", labels.get("maxCapacity"));
            assertEquals("相对精度", labels.get("relativePrecision"));
            assertEquals("材料硬度", labels.get("materialHardness"));
            assertEquals("臂长一致性", labels.get("armRatioConsistency"));
            assertEquals("结构复杂度", labels.get("structureComplexity"));
            assertEquals("耐久性", labels.get("durabilityScore"));
        }

        @Test
        @DisplayName("getEasternWesternCivilizationCodes - 东西方文明分组")
        void testEasternWesternCivilizationGroups() {
            List<String> eastern = service.getEasternCivilizationCodes();
            List<String> western = service.getWesternCivilizationCodes();

            assertNotNull(eastern);
            assertNotNull(western);
            assertTrue(eastern.size() >= 3);
            assertTrue(western.size() >= 3);

            assertTrue(eastern.contains("CHN-WARRING"));
            assertTrue(eastern.contains("CHN-TANG"));
            assertTrue(western.contains("EGYPT"));
            assertTrue(western.contains("ROME"));
        }
    }
}
