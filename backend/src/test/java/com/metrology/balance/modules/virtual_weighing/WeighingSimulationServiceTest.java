package com.metrology.balance.modules.virtual_weighing;

import com.metrology.balance.dto.VirtualWeighingResult;
import com.metrology.balance.entity.VirtualWeighingItem;
import com.metrology.balance.modules.virtual_weighing.model.OscillationFrame;
import com.metrology.balance.modules.virtual_weighing.model.PhysicsParameters;
import com.metrology.balance.modules.virtual_weighing.physics.BalancePhysicsEngine;
import com.metrology.balance.modules.virtual_weighing.service.WeighingSimulationService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("称量模拟服务回归测试")
class WeighingSimulationServiceTest {

    @Mock
    private VirtualWeighingItemRepository itemRepository;

    @InjectMocks
    private WeighingSimulationService service;

    private VirtualWeighingItem weight100g;
    private VirtualWeighingItem weight50g;
    private VirtualWeighingItem weight20g;
    private VirtualWeighingItem weight10g;
    private VirtualWeighingItem weight5g;
    private VirtualWeighingItem hanGoldCoin;
    private VirtualWeighingItem romanCoin;
    private VirtualWeighingItem heavyItem;
    private VirtualWeighingItem lightItem;
    private VirtualWeighingItem legendaryItem;
    private VirtualWeighingItem rareItem;
    private VirtualWeighingItem zeroMassItem;
    private VirtualWeighingItem negativeMassItem;

    @BeforeEach
    void setUp() {
        weight100g = createItem(1, "weight_100g", "100g砝码", "weight", 100.0, "CHN-QIN", "common");
        weight50g = createItem(2, "weight_50g", "50g砝码", "weight", 50.0, "MODERN", "common");
        weight20g = createItem(3, "weight_20g", "20g砝码", "weight", 20.0, "MODERN", "common");
        weight10g = createItem(4, "weight_10g", "10g砝码", "weight", 10.0, "MODERN", "common");
        weight5g = createItem(5, "weight_5g", "5g砝码", "weight", 5.0, "MODERN", "common");

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

        zeroMassItem = createItem(40, "zero_mass", "零质量物品", "fun",
                0.0, "FUN", "common");

        negativeMassItem = createItem(41, "negative_mass", "负质量物品", "fun",
                -50.0, "FUN", "common");
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
        @DisplayName("等臂天平完美平衡 - 左右各100g砝码")
        void testEqualArmPerfectBalance() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(1), Arrays.asList(1), null, "EQUAL_ARM");

            assertNotNull(result);
            assertEquals("BALANCED", result.getBalanceStatus());
            assertEquals(100.0, result.getLeftTotalMass(), 0.001);
            assertEquals(100.0, result.getRightTotalMass(), 0.001);
            assertEquals(0.0, result.getMassDifference(), 0.01);
            assertEquals(0.0, result.getTorqueDifference(), 0.01);

            Map<String, Object> physics = result.getPhysicsParameters();
            assertEquals(180.0, physics.get("leftArmLength_mm"));
            assertEquals(180.0, physics.get("rightArmLength_mm"));
        }

        @Test
        @DisplayName("左重右轻 - 左盘100g，右盘50g")
        void testLeftHeavyScenario() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));
            when(itemRepository.findById(2)).thenReturn(Optional.of(weight50g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(1), Arrays.asList(2), null, "EQUAL_ARM");

            assertEquals("LEFT_HEAVY", result.getBalanceStatus());
            assertTrue(result.getMassDifference() > 0);
            assertTrue(result.getTorqueDifference() > 0);
        }

        @Test
        @DisplayName("右重左轻 - 左盘50g，右盘100g")
        void testRightHeavyScenario() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));
            when(itemRepository.findById(2)).thenReturn(Optional.of(weight50g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(2), Arrays.asList(1), null, "EQUAL_ARM");

            assertEquals("RIGHT_HEAVY", result.getBalanceStatus());
            assertTrue(result.getMassDifference() < 0);
            assertTrue(result.getTorqueDifference() < 0);
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
        @DisplayName("不等臂天平 - 臂长比率验证")
        void testUnequalArmBalanceArms() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(1), Arrays.asList(1), null, "UNEQUAL_ARM");

            Map<String, Object> physics = result.getPhysicsParameters();
            double leftArm = (Double) physics.get("leftArmLength_mm");
            double rightArm = (Double) physics.get("rightArmLength_mm");

            assertNotEquals(leftArm, rightArm);
            assertEquals(4.0, rightArm / leftArm, 0.001);
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
            assertTrue(physics.containsKey("beamMomentOfInertia_kgm2"));
            assertTrue(physics.containsKey("totalMomentOfInertia_kgm2"));
            assertTrue(physics.containsKey("torsionalStiffness_Nm_per_rad"));
            assertTrue(physics.containsKey("materialDampingCoefficient"));
            assertTrue(physics.containsKey("airDampingCoefficient"));
            assertTrue(physics.containsKey("frictionDampingCoefficient"));
            assertTrue(physics.containsKey("totalDampingCoefficient"));
            assertTrue(physics.containsKey("naturalFrequency_rad_per_s"));
            assertTrue(physics.containsKey("dampingRatio_xi"));
            assertTrue(physics.containsKey("dampedFrequency_rad_per_s"));
            assertTrue(physics.containsKey("gravity_m_per_s2"));
            assertTrue(physics.containsKey("oscillationFrequency_Hz"));
            assertTrue(physics.containsKey("beamMass_g"));
            assertTrue(physics.containsKey("knifeEdgeRadius_mm"));
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
            assertFalse(result.getCulturalInsights().isEmpty());
            assertTrue(result.getCulturalInsights().stream()
                    .anyMatch(i -> i.contains("平衡")));
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
            assertTrue(conversion.containsKey("kilograms"));
            assertTrue(conversion.containsKey("秦斤"));
            assertTrue(conversion.containsKey("秦两"));
            assertTrue(conversion.containsKey("西汉斤"));
            assertTrue(conversion.containsKey("西汉两"));
            assertTrue(conversion.containsKey("唐斤"));
            assertTrue(conversion.containsKey("唐两"));
            assertTrue(conversion.containsKey("明斤"));
            assertTrue(conversion.containsKey("明两"));
            assertTrue(conversion.containsKey("罗马磅"));
            assertTrue(conversion.containsKey("市斤"));
            assertTrue(conversion.containsKey("市两"));
            assertTrue(conversion.containsKey("金衡盎司"));
            assertTrue(conversion.containsKey("常衡盎司"));
            assertTrue(conversion.containsKey("磅"));
            assertTrue(conversion.containsKey("克拉"));
        }

        @Test
        @DisplayName("振动帧生成 - 动画数据完整性")
        void testOscillationFramesGeneration() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));
            when(itemRepository.findById(2)).thenReturn(Optional.of(weight50g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(1), Arrays.asList(2), null, "EQUAL_ARM");

            Map<String, Object> animation = result.getAnimationData();
            assertNotNull(animation);
            assertTrue(animation.containsKey("initialAngle"));
            assertTrue(animation.containsKey("finalAngle_deg"));
            assertTrue(animation.containsKey("decayTime_s"));
            assertTrue(animation.containsKey("oscillationCount"));
            assertTrue(animation.containsKey("dampedFrequency_Hz"));
            assertTrue(animation.containsKey("oscillationFrames"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> frames = (List<Map<String, Object>>) animation.get("oscillationFrames");
            assertNotNull(frames);
            assertFalse(frames.isEmpty());
            assertTrue(frames.size() >= 30);

            Map<String, Object> firstFrame = frames.get(0);
            assertTrue(firstFrame.containsKey("time_s"));
            assertTrue(firstFrame.containsKey("angle_deg"));
            assertTrue(firstFrame.containsKey("angularVelocity_deg_per_s"));
            assertTrue(firstFrame.containsKey("envelope"));
            assertEquals(0.0, (Double) firstFrame.get("time_s"), 0.001);
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
            assertEquals(0.0, result.getSwingAngle(), 0.001);
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

            assertTrue(result.getMassDifference() < 0);
            assertTrue(result.getRelativeError() > 0.9);
        }

        @Test
        @DisplayName("极重物品 - 大型文物称量")
        void testVeryHeavyItem() {
            when(itemRepository.findById(20)).thenReturn(Optional.of(heavyItem));
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(20), Arrays.asList(1), null, "EQUAL_ARM");

            assertEquals("LEFT_HEAVY", result.getBalanceStatus());
            assertTrue(result.getLeftTotalMass() > 1000);
        }

        @Test
        @DisplayName("微差平衡 - 质量差极小时判定为平衡")
        void testVerySmallDifferenceBalanced() {
            VirtualWeighingItem itemA = createItem(100, "item_a", "物品A", "weight",
                    100.001, "MODERN", "common");
            VirtualWeighingItem itemB = createItem(101, "item_b", "物品B", "weight",
                    100.002, "MODERN", "common");

            when(itemRepository.findById(100)).thenReturn(Optional.of(itemA));
            when(itemRepository.findById(101)).thenReturn(Optional.of(itemB));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(100), Arrays.asList(101), null, "EQUAL_ARM");

            double massDiff = Math.abs(result.getMassDifference());
            assertTrue(massDiff < 0.01);
        }

        @Test
        @DisplayName("大量物品性能 - 20件物品性能测试")
        void testLargeNumberOfItemsPerformance() {
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
            assertTrue((endTime - startTime) < 1000);
        }

        @Test
        @DisplayName("最大摆角限制 - 不会超过物理极限")
        void testMaxSwingAngleLimit() {
            when(itemRepository.findById(20)).thenReturn(Optional.of(heavyItem));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(20), Collections.emptyList(), null, "EQUAL_ARM");

            assertTrue(Math.abs(result.getSwingAngle()) <= 20.0,
                    "最大摆角不应超过物理极限: " + result.getSwingAngle());
        }

        @Test
        @DisplayName("平衡时间边界 - 在合理范围内")
        void testEquilibriumTimeBoundary() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(1), Collections.emptyList(), null, "EQUAL_ARM");

            assertTrue(result.getEquilibriumTime() >= 0.5,
                    "平衡时间不应小于0.5秒");
            assertTrue(result.getEquilibriumTime() <= 15.0,
                    "平衡时间不应超过15秒");
        }
    }

    @Nested
    @DisplayName("异常场景测试")
    class ExceptionScenarios {

        @Test
        @DisplayName("物品ID不存在 - 静默跳过不崩溃")
        void testNonexistentItemSkipped() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));
            when(itemRepository.findById(999)).thenReturn(Optional.empty());

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(1, 999), Arrays.asList(1), null, "EQUAL_ARM");

            assertEquals(1, result.getLeftItems().size());
            assertEquals(100.0, result.getLeftTotalMass(), 0.001);
            verify(itemRepository, times(2)).findById(anyInt());
        }

        @Test
        @DisplayName("null物品列表 - 当作空列表处理")
        void testNullItemListTreatedAsEmpty() {
            assertDoesNotThrow(() -> {
                VirtualWeighingResult result = service.performWeighing(
                        null, null, null, "EQUAL_ARM");
                assertEquals("BALANCED", result.getBalanceStatus());
                assertEquals(0.0, result.getLeftTotalMass(), 0.001);
                assertEquals(0.0, result.getRightTotalMass(), 0.001);
            });
        }

        @Test
        @DisplayName("无效天平类型 - 降级为等臂")
        void testInvalidBalanceTypeFallsBack() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));

            assertDoesNotThrow(() -> {
                VirtualWeighingResult result = service.performWeighing(
                        Arrays.asList(1), Arrays.asList(1), null, "INVALID_TYPE");
                assertNotNull(result);
                assertNotNull(result.getBalanceType());
            });
        }

        @Test
        @DisplayName("null天平类型 - 使用等臂默认值")
        void testNullBalanceTypeUsesDefault() {
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(1), Arrays.asList(1), null, null);

            assertEquals("EQUAL_ARM", result.getBalanceType());
        }

        @Test
        @DisplayName("零质量物品 - 不影响平衡")
        void testZeroMassItem() {
            when(itemRepository.findById(40)).thenReturn(Optional.of(zeroMassItem));
            when(itemRepository.findById(1)).thenReturn(Optional.of(weight100g));

            VirtualWeighingResult result = service.performWeighing(
                    Arrays.asList(1, 40), Arrays.asList(1), null, "EQUAL_ARM");

            assertEquals("BALANCED", result.getBalanceStatus());
            assertEquals(100.0, result.getLeftTotalMass(), 0.001);
        }

        @Test
        @DisplayName("负质量物品 - 不崩溃")
        void testNegativeMassItemDoesNotCrash() {
            when(itemRepository.findById(41)).thenReturn(Optional.of(negativeMassItem));

            assertDoesNotThrow(() -> {
                VirtualWeighingResult result = service.performWeighing(
                        Arrays.asList(41), Collections.emptyList(), null, "EQUAL_ARM");
                assertNotNull(result);
                assertTrue(Double.isFinite(result.getLeftTotalMass()));
            });
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
                assertTrue(Double.isFinite(result.getLeftTotalMass()));
            });
        }
    }

    @Nested
    @DisplayName("物理引擎专项验证")
    class PhysicsEngineVerification {

        @Test
        @DisplayName("转动惯量计算正确性 - 梁的转动惯量")
        void testBeamMomentOfInertiaCalculation() {
            double beamMassG = 45.0;
            double leftArmMm = 180.0;
            double rightArmMm = 180.0;
            double widthMm = 6.0;
            double thicknessMm = 3.0;

            double momentOfInertia = BalancePhysicsEngine.calculateBeamMomentOfInertia(
                    beamMassG, leftArmMm, rightArmMm, widthMm, thicknessMm);

            assertTrue(momentOfInertia > 0);
            assertTrue(Double.isFinite(momentOfInertia));

            double beamMassKg = beamMassG / 1000.0;
            double totalLengthM = (leftArmMm + rightArmMm) / 1000.0;
            double expectedRodI = beamMassKg * totalLengthM * totalLengthM / 12.0;
            assertEquals(expectedRodI, momentOfInertia, expectedRodI * 0.1);
        }

        @Test
        @DisplayName("扭转刚度计算 - 不同材料刚度不同")
        void testTorsionalStiffnessCalculation() {
            double bronzeStiffness = BalancePhysicsEngine.calculateTorsionalStiffness(
                    "青铜", 0.3, 180.0, 180.0);
            double steelStiffness = BalancePhysicsEngine.calculateTorsionalStiffness(
                    "钢", 0.3, 180.0, 180.0);
            double agateStiffness = BalancePhysicsEngine.calculateTorsionalStiffness(
                    "玛瑙", 0.3, 180.0, 180.0);

            assertTrue(bronzeStiffness > 0);
            assertTrue(steelStiffness > 0);
            assertTrue(agateStiffness > 0);
            assertTrue(steelStiffness > bronzeStiffness);
            assertTrue(agateStiffness > steelStiffness);
        }

        @Test
        @DisplayName("空气阻尼计算 - 雷诺数和阻力系数")
        void testAirDampingCalculation() {
            double airDamping = BalancePhysicsEngine.calculateAirDamping(
                    180.0, 180.0, 6.0, 3.0);

            assertTrue(airDamping > 0);
            assertTrue(Double.isFinite(airDamping));
            assertTrue(airDamping >= 0.001);
        }

        @Test
        @DisplayName("刀口摩擦阻尼计算 - 与质量正相关")
        void testKnifeEdgeFrictionDampingCalculation() {
            double lightFriction = BalancePhysicsEngine.calculateKnifeEdgeFrictionDamping(
                    100.0, 0.3);
            double heavyFriction = BalancePhysicsEngine.calculateKnifeEdgeFrictionDamping(
                    1000.0, 0.3);

            assertTrue(lightFriction > 0);
            assertTrue(heavyFriction > 0);
            assertTrue(heavyFriction > lightFriction);

            double smallKnifeFriction = BalancePhysicsEngine.calculateKnifeEdgeFrictionDamping(
                    100.0, 0.1);
            double largeKnifeFriction = BalancePhysicsEngine.calculateKnifeEdgeFrictionDamping(
                    100.0, 0.5);
            assertTrue(largeKnifeFriction > smallKnifeFriction);
        }

        @Test
        @DisplayName("二阶振荡系统 - 欠阻尼状态验证")
        void testUnderdampedSystem() {
            PhysicsParameters params = BalancePhysicsEngine.buildPhysicsParameters(
                    180.0, 180.0, 100.0, 50.0, 45.0, 0.3, "青铜");

            assertTrue(params.getDampingRatioXi() < 1.0);
            assertTrue(params.getDampedFrequencyRadPerS() > 0);
            assertTrue(params.getDampedFrequencyRadPerS() < params.getNaturalFrequencyRadPerS());

            BalancePhysicsEngine.DampingType dampingType = BalancePhysicsEngine.getDampingType(
                    params.getDampingRatioXi());
            assertEquals(BalancePhysicsEngine.DampingType.UNDERDAMPED, dampingType);
        }

        @Test
        @DisplayName("二阶振荡系统 - 阻尼类型判断")
        void testDampingTypeClassification() {
            assertEquals(BalancePhysicsEngine.DampingType.UNDERDAMPED,
                    BalancePhysicsEngine.getDampingType(0.5));
            assertEquals(BalancePhysicsEngine.DampingType.UNDERDAMPED,
                    BalancePhysicsEngine.getDampingType(0.99));
            assertEquals(BalancePhysicsEngine.DampingType.CRITICALLY_DAMPED,
                    BalancePhysicsEngine.getDampingType(1.0));
            assertEquals(BalancePhysicsEngine.DampingType.OVERDAMPED,
                    BalancePhysicsEngine.getDampingType(2.0));
        }

        @Test
        @DisplayName("振动帧生成 - 欠阻尼振荡特性")
        void testUnderdampedOscillationFrames() {
            double initialAngle = 5.0;
            double dampingRatio = 0.1;
            double naturalFrequency = 10.0;
            double dampedFrequency = naturalFrequency * Math.sqrt(1 - dampingRatio * dampingRatio);
            double totalTime = 5.0;

            List<OscillationFrame> frames = BalancePhysicsEngine.generateOscillationFrames(
                    initialAngle, dampingRatio, naturalFrequency, dampedFrequency, totalTime);

            assertNotNull(frames);
            assertTrue(frames.size() > 10);

            OscillationFrame firstFrame = frames.get(0);
            assertEquals(0.0, firstFrame.getTimeS(), 0.001);
            assertEquals(initialAngle, firstFrame.getAngleDeg(), 0.001);
            assertEquals(1.0, firstFrame.getEnvelope(), 0.001);

            OscillationFrame lastFrame = frames.get(frames.size() - 1);
            assertTrue(lastFrame.getTimeS() > 0);
            assertTrue(Math.abs(lastFrame.getAngleDeg()) < Math.abs(initialAngle));
            assertTrue(lastFrame.getEnvelope() < 1.0);
            assertTrue(lastFrame.getEnvelope() > 0);
        }

        @Test
        @DisplayName("振动帧生成 - 过阻尼无振荡")
        void testOverdampedOscillationFrames() {
            double initialAngle = 5.0;
            double dampingRatio = 2.0;
            double naturalFrequency = 10.0;
            double dampedFrequency = 0;
            double totalTime = 2.0;

            List<OscillationFrame> frames = BalancePhysicsEngine.generateOscillationFrames(
                    initialAngle, dampingRatio, naturalFrequency, dampedFrequency, totalTime);

            assertNotNull(frames);
            assertFalse(frames.isEmpty());

            boolean monotonicallyDecreasing = true;
            double prevAngle = Math.abs(frames.get(0).getAngleDeg());
            for (int i = 1; i < frames.size(); i++) {
                double currentAngle = Math.abs(frames.get(i).getAngleDeg());
                if (currentAngle > prevAngle + 0.01) {
                    monotonicallyDecreasing = false;
                    break;
                }
                prevAngle = currentAngle;
            }
            assertTrue(monotonicallyDecreasing,
                    "过阻尼系统角度幅值应单调衰减，不应振荡");
        }

        @Test
        @DisplayName("物理参数构建 - 完整性验证")
        void testPhysicsParametersBuildCompleteness() {
            PhysicsParameters params = BalancePhysicsEngine.buildPhysicsParameters(
                    180.0, 180.0, 100.0, 100.0, 45.0, 0.3, "青铜");

            assertNotNull(params);
            assertEquals(180.0, params.getLeftArmLengthMm());
            assertEquals(180.0, params.getRightArmLengthMm());
            assertEquals(18000.0, params.getLeftTorqueGmm(), 0.01);
            assertEquals(18000.0, params.getRightTorqueGmm(), 0.01);
            assertTrue(params.getBeamMomentOfInertiaKgm2() > 0);
            assertTrue(params.getTotalMomentOfInertiaKgm2() > 0);
            assertTrue(params.getTorsionalStiffnessNmPerRad() > 0);
            assertTrue(params.getMaterialDampingCoefficient() > 0);
            assertTrue(params.getAirDampingCoefficient() > 0);
            assertTrue(params.getFrictionDampingCoefficient() >= 0);
            assertTrue(params.getTotalDampingCoefficient() > 0);
            assertTrue(params.getNaturalFrequencyRadPerS() > 0);
            assertTrue(params.getDampingRatioXi() > 0);
            assertTrue(params.getDampedFrequencyRadPerS() > 0);
            assertEquals(BalancePhysicsEngine.GRAVITY, params.getGravityMPerS2());
            assertTrue(params.getOscillationFrequencyHz() > 0);
            assertEquals(45.0, params.getBeamMassG());
            assertEquals(0.3, params.getKnifeEdgeRadiusMm());
        }

        @Test
        @DisplayName("自然频率与转动惯量关系 - 质量越大频率越低")
        void testNaturalFrequencyVsMomentOfInertia() {
            PhysicsParameters lightParams = BalancePhysicsEngine.buildPhysicsParameters(
                    180.0, 180.0, 10.0, 10.0, 45.0, 0.3, "青铜");
            PhysicsParameters heavyParams = BalancePhysicsEngine.buildPhysicsParameters(
                    180.0, 180.0, 1000.0, 1000.0, 45.0, 0.3, "青铜");

            assertTrue(heavyParams.getTotalMomentOfInertiaKgm2() > lightParams.getTotalMomentOfInertiaKgm2());
            assertTrue(heavyParams.getNaturalFrequencyRadPerS() < lightParams.getNaturalFrequencyRadPerS());
        }

        @Test
        @DisplayName("摆角计算 - 与扭矩差正相关")
        void testSwingAngleProportionalToTorque() {
            double torsionalStiffness = 0.01;
            double dampingRatio = 0.1;

            double smallAngle = BalancePhysicsEngine.calculateSwingAngle(
                    0.001, torsionalStiffness, dampingRatio);
            double largeAngle = BalancePhysicsEngine.calculateSwingAngle(
                    0.01, torsionalStiffness, dampingRatio);

            assertTrue(Math.abs(largeAngle) > Math.abs(smallAngle));
        }

        @Test
        @DisplayName("平衡时间计算 - 阻尼越小时间越长")
        void testEquilibriumTimeVsDamping() {
            double naturalFrequency = 10.0;

            double lowDampingTime = BalancePhysicsEngine.calculateEquilibriumTime(
                    0.1, naturalFrequency);
            double highDampingTime = BalancePhysicsEngine.calculateEquilibriumTime(
                    0.5, naturalFrequency);

            assertTrue(lowDampingTime > highDampingTime);
        }

        @Test
        @DisplayName("材料阻尼系数 - 不同材料取值正确")
        void testMaterialDampingCoefficients() {
            assertEquals(0.08, BalancePhysicsEngine.getMaterialDamping("青铜"), 0.001);
            assertEquals(0.05, BalancePhysicsEngine.getMaterialDamping("钢"), 0.001);
            assertEquals(0.015, BalancePhysicsEngine.getMaterialDamping("玛瑙"), 0.001);
            assertEquals(0.15, BalancePhysicsEngine.getMaterialDamping("木"), 0.001);
            assertEquals(0.08, BalancePhysicsEngine.getMaterialDamping("未知材料"), 0.001);
        }

        @Test
        @DisplayName("重力加速度常量 - 标准值")
        void testGravityConstant() {
            assertEquals(9.80665, BalancePhysicsEngine.GRAVITY, 0.0001);
        }
    }
}
