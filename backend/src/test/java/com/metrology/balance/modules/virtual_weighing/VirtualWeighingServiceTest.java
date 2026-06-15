package com.metrology.balance.modules.virtual_weighing;

import com.metrology.balance.dto.VirtualWeighingResult;
import com.metrology.balance.entity.VirtualWeighingItem;
import com.metrology.balance.repository.VirtualWeighingItemRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("虚拟称量体验服务测试")
class VirtualWeighingServiceTest {

    @Mock
    private VirtualWeighingItemRepository itemRepository;

    @InjectMocks
    private VirtualWeighingService service;

    private VirtualWeighingItem weight100g;
    private VirtualWeighingItem weight50g;
    private VirtualWeighingItem weight20g;
    private VirtualWeighingItem weight10g;
    private VirtualWeighingItem hanGoldCoin;
    private VirtualWeighingItem romanCoin;
    private VirtualWeighingItem heavyItem;
    private VirtualWeighingItem lightItem;
    private VirtualWeighingItem legendaryItem;
    private VirtualWeighingItem rareItem;

    @BeforeEach
    void setUp() {
        weight100g = createItem(1, "weight_100g", "100g砝码", "weight", 100.0, "CHN-QIN", "common");
        weight50g = createItem(2, "weight_50g", "50g砝码", "weight", 50.0, "MODERN", "common");
        weight20g = createItem(3, "weight_20g", "20g砝码", "weight", 20.0, "MODERN", "common");
        weight10g = createItem(4, "weight_10g", "10g砝码", "weight", 10.0, "MODERN", "common");

        hanGoldCoin = createItem(10, "han_gold_coin", "汉金饼", "artifact",
                250.0, "CHN-HAN", "rare");
        hanGoldCoin.setHistoricalSignificance("汉代金饼，纯度极高，是研究汉代货币制度的重要实物");

        romanCoin = createItem(11, "roman_aureus", "罗马金币", "artifact",
                7.3, "ROME", "rare");

        heavyItem = createItem(20, "heavy_artifact", "大型青铜鼎", "artifact",
                5000.0, "CHN-ZHOU", "legendary");

        lightItem = createItem(21, "feather", "羽毛", "fun",
                0.01, "GENERAL", "common");

        legendaryItem = createItem(30, "legendary_item", "传国玉玺", "artifact",
                1000.0, "CHN-QIN", "legendary");

        rareItem = createItem(31, "rare_item", "汝窑天青釉", "artifact",
                300.0, "CHN-SONG", "rare");
    }

    private VirtualWeighingItem createItem(int id, String code, String name, String category,
                                            double mass, String civilization, String rarity) {
        VirtualWeighingItem item = new VirtualWeighingItem();
        item.setId(id);
        item.setItemCode(code);
        item.setItemName(name);
        item.setCategory(category);
        item.setNominalMass(mass);
        item.setActualMass(mass);
        item.setCivilization(civilization);
        item.setRarity(rarity);
        item.setActive(true);
        item.setDisplayOrder(id);
        return item;
    }

    @Nested
    @DisplayName("正常场景测试")
    class NormalScenarios {

        @Test
        @DisplayName("完美平衡 - 左右各100g砝码")
        void testPerfectBalanceWithEqualWeights() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(1), Arrays.asList(1), null, "EQUAL_ARM");

            assertNotNull(result);
            assertEquals("BALANCED", result.getBalanceStatus());
            assertEquals(100.0, result.getLeftTotalMass(), 0.001);
            assertEquals(100.0, result.getRightTotalMass(), 0.001);
            assertEquals(0.0, result.getMassDifference(), 0.01);
            assertEquals(0, result.getTorqueDifference(), 0.01);

            assertTrue(result.getCulturalInsights().stream()
                            .anyMatch(i -> i.contains("平衡")),
                    "平衡状态应包含平衡相关的文化解读");
        }

        @Test
        @DisplayName("左重右轻 - 左盘100g，右盘50g")
        void testLeftHeavyScenario() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));
            when(itemRepository.findById(2)).thenReturn(Optional.of(weight50g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(1), Arrays.asList(2), null, "EQUAL_ARM");

            assertEquals("LEFT_HEAVY", result.getBalanceStatus());
            assertTrue(result.getMassDifference() > 0,
                    "左重时质量差应为正数");
            assertTrue(result.getTorqueDifference() > 0,
                    "左重时力矩差应为正数");
            assertTrue(result.getSwingAngle() < 0,
                    "左重时摆角应为负值（向左倾斜）");
        }

        @Test
        @DisplayName("右重左轻 - 左盘50g，右盘100g")
        void testRightHeavyScenario() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));
            when(itemRepository.findById(2)).thenReturn(Optional.of(weight50g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(2), Arrays.asList(1), null, "EQUAL_ARM");

            assertEquals("RIGHT_HEAVY", result.getBalanceStatus());
            assertTrue(result.getMassDifference() < 0,
                    "右重时质量差应为负数");
            assertTrue(result.getTorqueDifference() < 0,
                    "右重时力矩差应为负数");
            assertTrue(result.getSwingAngle() > 0,
                    "右重时摆角应为正值（向右倾斜）");
        }

        @Test
        @DisplayName("多物品累加 - 多个砝码组合")
        void testMultipleItemsAccumulation() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));
            when(itemRepository.findById(2)).thenReturn(Optional.of(weight50g));
            when(itemRepository.findById(3)).thenReturn(Optional.of(weight20g));
            when(itemRepository.findById(4)).thenReturn(Optional.of(weight10g));

            List<Integer> leftIds = Arrays.asList(1, 2, 3);
            List<Integer> rightIds = Arrays.asList(1, 2, 3, 4);

            VirtualWeighingResult result = service.performWeighing(
                    leftIds, rightIds, null, "EQUAL_ARM");

            assertEquals(170.0, result.getLeftTotalMass(), 0.001);
            assertEquals(180.0, result.getRightTotalMass(), 0.001);
            assertEquals(3, result.getLeftItems().size());
            assertEquals(4, result.getRightItems().size());
        }

        @Test
        @DisplayName("等臂天平 - 臂长相等验证")
        void testEqualArmBalanceArms() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(1), Arrays.asList(1), null, "EQUAL_ARM");

            Map<String, Object> physics = result.getPhysicsParameters();
            assertEquals(180.0, physics.get("leftArmLength_mm"));
            assertEquals(180.0, physics.get("rightArmLength_mm"));
        }

        @Test
        @DisplayName("不等臂天平 - 臂长比率验证")
        void testUnequalArmBalanceArms() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(1), Arrays.asList(1), null, "UNEQUAL_ARM");

            Map<String, Object> physics = result.getPhysicsParameters();
            double leftArm = (Double) physics.get("leftArmLength_mm");
            double rightArm = (Double) physics.get("rightArmLength_mm");

            assertNotEquals(leftArm, rightArm,
                    "不等臂天平台臂长应不相等");
            assertEquals(4.0, rightArm / leftArm, 0.001,
                    "不等臂天平台臂长比应为4:1");
        }

        @Test
        @DisplayName("物理参数完整性 - 包含所有必要字段")
        void testPhysicsParametersCompleteness() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(1), Collections.emptyList(), null, "EQUAL_ARM");

            Map<String, Object> physics = result.getPhysicsParameters();
            assertNotNull(physics);
            assertTrue(physics.containsKey("leftArmLength_mm"));
            assertTrue(physics.containsKey("rightArmLength_mm"));
            assertTrue(physics.containsKey("leftTorque_gmm"));
            assertTrue(physics.containsKey("rightTorque_gmm"));
            assertTrue(physics.containsKey("stiffness"));
            assertTrue(physics.containsKey("damping"));
            assertTrue(physics.containsKey("gravity"));
            assertTrue(physics.containsKey("oscillationFrequency"));
        }

        @Test
        @DisplayName("动画数据完整性 - 包含关键帧信息")
        void testAnimationDataCompleteness() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));
            when(itemRepository.findById(2)).thenReturn(Optional.of(weight50g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(1), Arrays.asList(2), null, "EQUAL_ARM");

            Map<String, Object> animation = result.getAnimationData();
            assertNotNull(animation);
            assertTrue(animation.containsKey("initialAngle"));
            assertTrue(animation.containsKey("decayTime"));
            assertTrue(animation.containsKey("oscillationCount"));
            assertTrue(animation.containsKey("finalAngle"));

            assertTrue((Double) animation.get("decayTime") > 0,
                    "衰减时间应为正数");
        }

        @Test
        @DisplayName("文化解读生成 - 平衡状态触发历史典故")
        void testCulturalInsightsGeneration() {
            when(itemRepository.findById(10)).thenReturn(Optional.of(hanGoldCoin));
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));
            when(itemRepository.findById(2)).thenReturn(Optional.of(weight50g));
            when(itemRepository.findById(3)).thenReturn(Optional.of(weight20g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(10), Arrays.asList(1, 2, 3), null, "EQUAL_ARM");

            assertNotNull(result.getCulturalInsights());
            assertFalse(result.getCulturalInsights().isEmpty(),
                    "应生成至少1条文化解读");
        }

        @Test
        @DisplayName("质量换算 - 包含所有古代度量衡单位")
        void testMassConversionUnits() {
            when(itemRepository.findById(10)).thenReturn(Optional.of(hanGoldCoin));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(10), Collections.emptyList(), null, "EQUAL_ARM");

            Map<String, Object> conversion = result.getMassConversion();
            assertNotNull(conversion);
            assertTrue(conversion.containsKey("grams"));
            assertTrue(conversion.containsKey("秦斤"));
            assertTrue(conversion.containsKey("西汉斤"));
            assertTrue(conversion.containsKey("唐斤"));
            assertTrue(conversion.containsKey("明斤"));
            assertTrue(conversion.containsKey("罗马磅"));
            assertTrue(conversion.containsKey("市斤"));
            assertTrue(conversion.containsKey("金衡盎司"));
            assertTrue(conversion.containsKey("克拉"));

            double hanJin = (Double) conversion.get("西汉斤");
            assertEquals(1.0, hanJin, 0.05,
                    "250g汉金饼约等于1西汉斤");
        }
    }

    @Nested
    @DisplayName("边界场景测试")
    class BoundaryScenarios {

        @Test
        @DisplayName("两盘皆空 - 平衡状态")
        void testBothPansEmpty() {
            VirtualWeighingResult result = service.performWeighing(
                    Collections.emptyList(), Collections.emptyList(), null, "EQUAL_ARM");

            assertEquals("BALANCED", result.getBalanceStatus());
            assertEquals(0.0, result.getLeftTotalMass(), 0.001);
            assertEquals(0.0, result.getRightTotalMass(), 0.001);
            assertEquals(0, result.getLeftItems().size());
            assertEquals(0, result.getRightItems().size());

            assertEquals(0.0, result.getSwingAngle(), 0.001,
                    "两盘空时摆角应为0");
        }

        @Test
        @DisplayName("仅左盘有物品 - 极端不平衡")
        void testOnlyLeftPanHasItems() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(1), Collections.emptyList(), null, "EQUAL_ARM");

            assertEquals("LEFT_HEAVY", result.getBalanceStatus());
            assertEquals(100.0, result.getLeftTotalMass(), 0.001);
            assertEquals(0.0, result.getRightTotalMass(), 0.001);
        }

        @Test
        @DisplayName("仅右盘有物品 - 极端不平衡")
        void testOnlyRightPanHasItems() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));

            VirtualWeighingResult result = service.performWeighing(
                    Collections.emptyList(), Arrays.asList(1), null, "EQUAL_ARM");

            assertEquals("RIGHT_HEAVY", result.getBalanceStatus());
            assertEquals(0.0, result.getLeftTotalMass(), 0.001);
            assertEquals(100.0, result.getRightTotalMass(), 0.001);
        }

        @Test
        @DisplayName("极轻物品 - 羽毛称量")
        void testVeryLightItem() {
            when(itemRepository.findById(21)).thenReturn(Optional.of(lightItem));
            when(itemRepository.findById(4)).thenReturn(Optional.of(weight10g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(21), Arrays.asList(4), null, "EQUAL_ARM");

            assertTrue(result.getMassDifference() < 0,
                    "羽毛比10g砝码轻");
            assertTrue(result.getRelativeError() > 0.9,
                    "极轻物品相对误差较大");
        }

        @Test
        @DisplayName("极重物品 - 大型文物称量")
        void testVeryHeavyItem() {
            when(itemRepository.findById(20)).thenReturn(Optional.of(heavyItem));
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(20), Arrays.asList(1), null, "EQUAL_ARM");

            assertEquals("LEFT_HEAVY", result.getBalanceStatus());
            assertTrue(result.getLeftTotalMass() > 1000,
                    "大型文物应重达数千克");
        }

        @Test
        @DisplayName("微差平衡 - 质量差小于0.01g时判定为平衡")
        void testVerySmallDifferenceBalanced() {
            VirtualWeighingItem itemA = createItem(100, "item_a", "物品A", "weight",
                    100.005, "MODERN", "common");
            VirtualWeighingItem itemB = createItem(101, "item_b", "物品B", "weight",
                    100.003, "MODERN", "common");

            when(itemRepository.findById(100)).thenReturn(Optional.of(itemA));
            when(itemRepository.findById(101)).thenReturn(Optional.of(itemB));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(100), Arrays.asList(101), null, "EQUAL_ARM");

            assertEquals(0.002, result.getMassDifference(), 0.0001);
        }

        @Test
        @DisplayName("大量物品 - 20件物品性能测试")
        void testLargeNumberOfItems() {
            List<Integer> leftIds = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                VirtualWeighingItem item = createItem(200 + i, "w_" + i, "砝码" + i,
                        "weight", 10.0 + i, "MODERN", "common");
                when(itemRepository.findById(200 + i)).thenReturn(Optional.of(item));
                leftIds.add(200 + i);
            }

            List<Integer> rightIds = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                VirtualWeighingItem item = createItem(300 + i, "rw_" + i, "右砝码" + i,
                        "weight", 10.0 + i + 0.5, "MODERN", "common");
                when(itemRepository.findById(300 + i)).thenReturn(Optional.of(item));
                rightIds.add(300 + i);
            }

            long startTime = System.currentTimeMillis();
            VirtualWeighingResult result = service.performWeighing(
                    leftIds, rightIds, null, "EQUAL_ARM");
            long endTime = System.currentTimeMillis();

            assertEquals(10, result.getLeftItems().size());
            assertEquals(10, result.getRightItems().size());
            assertTrue((endTime - startTime) < 1000,
                    "20件物品称量应在1秒内完成");
        }

        @Test
        @DisplayName("最大摆角限制 - 不会超过物理极限")
        void testMaxSwingAngleLimit() {
            when(itemRepository.findById(20)).thenReturn(Optional.of(heavyItem));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(20), Collections.emptyList(), null, "EQUAL_ARM");

            assertTrue(Math.abs(result.getSwingAngle()) <= 20.0,
                    "最大摆角不应超过物理极限（约15-20度）: " + result.getSwingAngle());
        }

        @Test
        @DisplayName("平衡时间边界 - 不会超过10秒")
        void testEquilibriumTimeLimit() {
            when(itemRepository.findById(20)).thenReturn(Optional.of(heavyItem));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(20), Arrays.asList(1), null, "EQUAL_ARM");

            assertTrue(result.getEquilibriumTime() <= 10.0,
                    "平衡时间不应超过10秒");
            assertTrue(result.getEquilibriumTime() > 0,
                    "平衡时间应为正数");
        }

        @Test
        @DisplayName("重复称量 - 相同输入结果一致（确定性）")
        void testDeterministicResults() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));
            when(itemRepository.findById(2)).thenReturn(Optional.of(weight50g));

            VirtualWeighingResult result1 = service.performWeighing(
                    Arrays.asList(1), Arrays.asList(2), null, "EQUAL_ARM");
            VirtualWeighingResult result2 = service.performWeighing(
                    Arrays.asList(1), Arrays.asList(2), null, "EQUAL_ARM");

            assertEquals(result1.getLeftTotalMass(), result2.getLeftTotalMass());
            assertEquals(result1.getRightTotalMass(), result2.getRightTotalMass());
            assertEquals(result1.getSwingAngle(), result2.getSwingAngle(), 0.001);
            assertEquals(result1.getBalanceStatus(), result2.getBalanceStatus());
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionScenarios {

        @Test
        @DisplayName("物品不存在 - 静默跳过不崩溃")
        void testNonexistentItemSkipped() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));
            when(itemRepository.findById(999)).thenReturn(Optional.empty());

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(1, 999), Arrays.asList(1), null, "EQUAL_ARM");

            assertEquals(1, result.getLeftItems().size(),
                    "只有存在的物品被计入");
            assertEquals(100.0, result.getLeftTotalMass(), 0.001);
        }

        @Test
        @DisplayName("null物品列表 - 当作空列表处理")
        void testNullItemListTreatedAsEmpty() {
            assertDoesNotThrow(() -> {
                VirtualWeighingResult result = service.performWeighing(
                        null, null, null, "EQUAL_ARM");
                assertEquals("BALANCED", result.getBalanceStatus());
            }, "null物品列表不应导致崩溃");
        }

        @Test
        @DisplayName("null天平类型 - 使用等臂默认值")
        void testNullBalanceTypeUsesDefault() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(1), Arrays.asList(1), null, null);

            assertNotNull(result.getBalanceType());
            assertEquals("EQUAL_ARM", result.getBalanceType());
        }

        @Test
        @DisplayName("无效天平类型 - 降级为等臂")
        void testInvalidBalanceTypeFallsBack() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));

            assertDoesNotThrow(() -> {
                VirtualWeighingResult result = service.performWeighing(
                        Arrays.asList(1), Arrays.asList(1), null, "INVALID_TYPE");
                assertNotNull(result);
            }, "无效天平类型不应崩溃");
        }

        @Test
        @DisplayName("质量为0的物品 - 不影响平衡")
        void testZeroMassItem() {
            VirtualWeighingItem zeroItem = createItem(50, "zero_item", "零质量物品",
                    "fun", 0.0, "FUN", "common");
            when(itemRepository.findById(50)).thenReturn(Optional.of(zeroItem));
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(1, 50), Arrays.asList(1), null, "EQUAL_ARM");

            assertEquals("BALANCED", result.getBalanceStatus());
        }

        @Test
        @DisplayName("负质量物品 - 绝对值处理或安全降级")
        void testNegativeMassItemDoesNotCrash() {
            VirtualWeighingItem negativeItem = createItem(51, "neg_item", "负质量物品",
                    "fun", -100.0, "FUN", "common");
            when(itemRepository.findById(51)).thenReturn(Optional.of(negativeItem));

            assertDoesNotThrow(() -> {
                VirtualWeighingResult result = service.performWeighing(
                        Arrays.asList(51), Collections.emptyList(), null, "EQUAL_ARM");
                assertNotNull(result);
            }, "负质量物品不应导致崩溃");
        }

        @Test
        @DisplayName("超大质量 - 不溢出、不崩溃")
        void testExtremelyLargeMass() {
            VirtualWeighingItem hugeItem = createItem(52, "huge_item", "超大质量",
                    "fun", 1e10, "FUN", "legendary");
            when(itemRepository.findById(52)).thenReturn(Optional.of(hugeItem));

            assertDoesNotThrow(() -> {
                VirtualWeighingResult result = service.performWeighing(
                        Arrays.asList(52), Collections.emptyList(), null, "EQUAL_ARM");
                assertNotNull(result);
                assertTrue(Double.isFinite(result.getLeftTotalMass()),
                        "质量计算不应溢出为Infinity");
            }, "超大质量不应导致崩溃或数值溢出");
        }
    }

    @Nested
    @DisplayName("交互体验测试")
    class InteractionExperienceTests {

        @Test
        @DisplayName("传说级稀有度 - 触发特殊文案")
        void testLegendaryRaritySpecialText() {
            when(itemRepository.findById(30)).thenReturn(Optional.of(legendaryItem));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(30), Collections.emptyList(), null, "EQUAL_ARM");

            assertTrue(result.getCulturalInsights().stream()
                            .anyMatch(i -> i.contains("传说") || i.contains("🌟")),
                    "传说级文物应触发特殊文案");
        }

        @Test
        @DisplayName("珍贵级稀有度 - 触发小心操作提示")
        void testRareRarityCarefulMessage() {
            when(itemRepository.findById(31)).thenReturn(Optional.of(rareItem));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(31), Collections.emptyList(), null, "EQUAL_ARM");

            assertTrue(result.getCulturalInsights().stream()
                            .anyMatch(i -> i.contains("珍贵") || i.contains("小心") || i.contains("✨")),
                    "珍贵文物应触发小心操作提示");
        }

        @Test
        @DisplayName("跨文明对比 - 触发文明对比文案")
        void testCrossCivilizationComparisonText() {
            when(itemRepository.findById(10)).thenReturn(Optional.of(hanGoldCoin));
            when(itemRepository.findById(11)).thenReturn(Optional.of(romanCoin));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(10), Arrays.asList(11), null, "EQUAL_ARM");

            assertTrue(result.getCulturalInsights().stream()
                            .anyMatch(i -> i.contains("跨文明") || i.contains("🌍")),
                    "跨文明称量应触发文明对比文案");
        }

        @Test
        @DisplayName("质量接近秦斤 - 触发历史单位联想")
        void testCloseToQinJinHistoricalReference() {
            VirtualWeighingItem qinItem = createItem(60, "qin_quan", "秦权",
                    "weight", 253.0, "CHN-QIN", "rare");
            when(itemRepository.findById(60)).thenReturn(Optional.of(qinItem));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(60), Arrays.asList(60), null, "EQUAL_ARM");

            assertTrue(result.getCulturalInsights().stream()
                            .anyMatch(i -> i.contains("秦代") || i.contains("秦斤")),
                    "253克应触发秦斤联想");
        }

        @Test
        @DisplayName("质量接近罗马磅 - 触发罗马单位联想")
        void testCloseToRomanPoundReference() {
            VirtualWeighingItem romanPound = createItem(61, "roman_libra", "罗马磅",
                    "weight", 327.4, "ROME", "rare");
            when(itemRepository.findById(61)).thenReturn(Optional.of(romanPound));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(61), Arrays.asList(61), null, "EQUAL_ARM");

            assertTrue(result.getCulturalInsights().stream()
                            .anyMatch(i -> i.contains("罗马") || i.contains("磅")),
                    "327.4克应触发罗马磅联想");
        }

        @Test
        @DisplayName("物品分类查询 - 按类别过滤")
        void testGetItemsByCategory() {
            List<VirtualWeighingItem> weightItems = Arrays.asList(weight100g, weight50g);
            when(itemRepository.findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc("weight"))
                    .thenReturn(weightItems);

            List<VirtualWeighingItem> result = service.getItemsByCategory("weight");

            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(i -> "weight".equals(i.getCategory())));
        }

        @Test
        @DisplayName("获取所有分类 - 去重正确")
        void testGetCategoriesDistinct() {
            List<VirtualWeighingItem> allItems = Arrays.asList(
                    weight100g, weight50g, hanGoldCoin, romanCoin, lightItem
            );
            when(itemRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(allItems);

            List<String> categories = service.getCategories();

            assertEquals(3, categories.size());
            assertTrue(categories.contains("weight"));
            assertTrue(categories.contains("artifact"));
            assertTrue(categories.contains("fun"));
        }

        @Test
        @DisplayName("历史文化背景查询 - 中国")
        void testGetHistoricalContextChina() {
            Map<String, Object> context = service.getHistoricalContext("中国");

            assertNotNull(context);
            assertTrue(context.containsKey("title"));
            assertTrue(context.containsKey("description"));
            assertTrue(context.containsKey("keyPoints"));
            assertTrue(context.containsKey("culturalReference"));
            assertTrue(((String) context.get("title")).contains("中国"));
        }

        @Test
        @DisplayName("历史文化背景查询 - 罗马")
        void testGetHistoricalContextRome() {
            Map<String, Object> context = service.getHistoricalContext("罗马");

            assertNotNull(context);
            assertTrue(((String) context.get("title")).contains("罗马"));
        }

        @Test
        @DisplayName("杠杆原理解释 - 包含历史和现代应用")
        void testGetLeverPrincipleExplanation() {
            Map<String, Object> explanation = service.getLeverPrincipleExplanation();

            assertNotNull(explanation);
            assertTrue(explanation.containsKey("title"));
            assertTrue(explanation.containsKey("principle"));
            assertTrue(explanation.containsKey("history"));
            assertTrue(explanation.containsKey("modernApplications"));
            assertTrue(explanation.containsKey("formulaExplanation"));

            List<?> history = (List<?>) explanation.get("history");
            assertTrue(history.size() >= 3,
                    "历史介绍应至少包含3个时期");

            List<?> apps = (List<?>) explanation.get("modernApplications");
            assertTrue(apps.size() >= 3,
                    "现代应用应至少包含3种");
        }
    }
}
