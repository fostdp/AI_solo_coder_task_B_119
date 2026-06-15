package com.metrology.balance.modules.virtual_weighing;

import com.metrology.balance.dto.VirtualWeighingResult;
import com.metrology.balance.entity.VirtualWeighingItem;
import com.metrology.balance.model.KnifeEdgeWearModel;
import com.metrology.balance.repository.VirtualWeighingItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VirtualWeighingService {

    private static final Logger logger = LoggerFactory.getLogger(VirtualWeighingService.class);

    @Autowired
    private VirtualWeighingItemRepository itemRepository;

    private static final double GRAVITY = 9.80665;
    private static final double ARM_LENGTH_EQUAL = 180.0;
    private static final double BEAM_MASS_EQUAL = 45.0;
    private static final double BEAM_CENTER_OF_GRAVITY_OFFSET = 0.15;
    private static final double KNIFE_EDGE_RADIUS_EQUAL = 0.3;
    private static final double AIR_VISCOSITY = 1.81e-5;
    private static final double BEAM_WIDTH = 6.0;
    private static final double BEAM_THICKNESS = 3.0;
    private static final double TORSIONAL_STIFFNESS_BRONZE = 85.0;
    private static final double TORSIONAL_STIFFNESS_STEEL = 200.0;
    private static final double TORSIONAL_STIFFNESS_AGATE = 350.0;
    private static final Map<String, Double> MATERIAL_DAMPING = new HashMap<>();
    private static final Map<String, Double> MATERIAL_STIFFNESS = new HashMap<>();

    static {
        MATERIAL_DAMPING.put("青铜", 0.08);
        MATERIAL_DAMPING.put("钢", 0.05);
        MATERIAL_DAMPING.put("铁", 0.06);
        MATERIAL_DAMPING.put("玉石", 0.02);
        MATERIAL_DAMPING.put("玛瑙", 0.015);
        MATERIAL_DAMPING.put("木", 0.15);

        MATERIAL_STIFFNESS.put("青铜", TORSIONAL_STIFFNESS_BRONZE);
        MATERIAL_STIFFNESS.put("钢", TORSIONAL_STIFFNESS_STEEL);
        MATERIAL_STIFFNESS.put("铁", 160.0);
        MATERIAL_STIFFNESS.put("玉石", 280.0);
        MATERIAL_STIFFNESS.put("玛瑙", TORSIONAL_STIFFNESS_AGATE);
        MATERIAL_STIFFNESS.put("木", 12.0);
    }

    public List<VirtualWeighingItem> getAllItems() {
        return itemRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    public List<VirtualWeighingItem> getItemsByCategory(String category) {
        return itemRepository.findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(category);
    }

    public List<String> getCategories() {
        List<VirtualWeighingItem> items = getAllItems();
        return items.stream()
                .map(VirtualWeighingItem::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    public VirtualWeighingResult performWeighing(List<Integer> leftItemIds, List<Integer> rightItemIds,
                                                 Integer balanceId, String balanceType) {
        double leftMass = 0.0;
        double rightMass = 0.0;

        List<VirtualWeighingItem> leftItems = new ArrayList<>();
        List<VirtualWeighingItem> rightItems = new ArrayList<>();

        if (leftItemIds != null) {
            for (Integer id : leftItemIds) {
                itemRepository.findById(id).ifPresent(item -> {
                    leftItems.add(item);
                    leftMass += item.getActualMass();
                });
            }
        }

        if (rightItemIds != null) {
            for (Integer id : rightItemIds) {
                itemRepository.findById(id).ifPresent(item -> {
                    rightItems.add(item);
                    rightMass += item.getActualMass();
                });
            }
        }

        String type = balanceType != null ? balanceType : "EQUAL_ARM";

        VirtualWeighingResult result = new VirtualWeighingResult();
        result.setLeftItems(leftItems);
        result.setRightItems(rightItems);
        result.setLeftTotalMass(leftMass);
        result.setRightTotalMass(rightMass);
        result.setMassDifference(leftMass - rightMass);
        result.setBalanceType(type);

        double leftArm = ARM_LENGTH_EQUAL;
        double rightArm = ARM_LENGTH_EQUAL;
        double beamMass = BEAM_MASS_EQUAL;
        double knifeEdgeRadius = KNIFE_EDGE_RADIUS_EQUAL;
        String beamMaterial = "青铜";
        if ("UNEQUAL_ARM".equals(type)) {
            leftArm = 50.0;
            rightArm = 200.0;
            beamMass = 60.0;
            knifeEdgeRadius = 0.5;
        }

        double leftTorque_Nm = leftMass / 1000.0 * leftArm / 1000.0 * GRAVITY;
        double rightTorque_Nm = rightMass / 1000.0 * rightArm / 1000.0 * GRAVITY;
        double torqueDifference_Nm = leftTorque_Nm - rightTorque_Nm;
        double torqueDifference_gmm = leftMass * leftArm - rightMass * rightArm;
        result.setTorqueDifference(torqueDifference_gmm);

        double beamMomentOfInertia = calculateBeamMomentOfInertia(beamMass, leftArm, rightArm, BEAM_WIDTH, BEAM_THICKNESS);
        double leftMassMomentOfInertia = leftMass / 1000.0 * leftArm * leftArm / 1000000.0;
        double rightMassMomentOfInertia = rightMass / 1000.0 * rightArm * rightArm / 1000000.0;
        double totalMomentOfInertia = beamMomentOfInertia + leftMassMomentOfInertia + rightMassMomentOfInertia;

        double baseStiffness = MATERIAL_STIFFNESS.getOrDefault(beamMaterial, TORSIONAL_STIFFNESS_BRONZE);
        double torsionalStiffness = baseStiffness * Math.pow(knifeEdgeRadius, 4) / (leftArm + rightArm) * 1000.0;
        torsionalStiffness = Math.max(0.005, torsionalStiffness / 1000000.0);

        double materialDampingCoeff = MATERIAL_DAMPING.getOrDefault(beamMaterial, 0.08);
        double airDamping = calculateAirDamping(leftArm, rightArm, BEAM_WIDTH, BEAM_THICKNESS);
        double frictionDamping = calculateKnifeEdgeFrictionDamping(beamMass + leftMass + rightMass, knifeEdgeRadius);
        double totalDampingCoeff = materialDampingCoeff * 2.0 * Math.sqrt(torsionalStiffness * totalMomentOfInertia);
        totalDampingCoeff = Math.max(totalDampingCoeff, airDamping + frictionDamping);

        double naturalFrequency = Math.sqrt(torsionalStiffness / totalMomentOfInertia);
        double dampingRatio = totalDampingCoeff / (2.0 * Math.sqrt(torsionalStiffness * totalMomentOfInertia));
        dampingRatio = Math.min(0.95, dampingRatio);

        double dampedFrequency = naturalFrequency * Math.sqrt(1.0 - dampingRatio * dampingRatio);

        double swingAngle = calculateRealSwingAngle(torqueDifference_Nm, torsionalStiffness, dampingRatio);
        result.setSwingAngle(swingAngle);

        double equilibriumTime = calculateRealEquilibriumTime(dampingRatio, naturalFrequency);
        result.setEquilibriumTime(equilibriumTime);

        String balanceStatus;
        double tolerance = 0.5;
        if (Math.abs(torqueDifference_gmm) < tolerance) {
            balanceStatus = "BALANCED";
        } else if (torqueDifference_gmm > 0) {
            balanceStatus = "LEFT_HEAVY";
        } else {
            balanceStatus = "RIGHT_HEAVY";
        }
        result.setBalanceStatus(balanceStatus);

        double maxTorque = Math.max(Math.abs(leftMass * leftArm), Math.abs(rightMass * rightArm));
        double relativeError = Math.abs(torqueDifference_gmm) / Math.max(maxTorque, 0.01);
        double precisionGrade = Math.max(0, 100 - relativeError * 100000);
        result.setRelativeError(relativeError);
        result.setPrecisionGrade(precisionGrade);

        Map<String, Object> physicsParams = new LinkedHashMap<>();
        physicsParams.put("leftArmLength_mm", leftArm);
        physicsParams.put("rightArmLength_mm", rightArm);
        physicsParams.put("leftTorque_gmm", leftMass * leftArm);
        physicsParams.put("rightTorque_gmm", rightMass * rightArm);
        physicsParams.put("beamMomentOfInertia_kgm2", beamMomentOfInertia);
        physicsParams.put("totalMomentOfInertia_kgm2", totalMomentOfInertia);
        physicsParams.put("torsionalStiffness_Nm_per_rad", torsionalStiffness);
        physicsParams.put("materialDampingCoefficient", materialDampingCoeff);
        physicsParams.put("airDampingCoefficient", airDamping);
        physicsParams.put("frictionDampingCoefficient", frictionDamping);
        physicsParams.put("totalDampingCoefficient", totalDampingCoeff);
        physicsParams.put("naturalFrequency_rad_per_s", naturalFrequency);
        physicsParams.put("dampingRatio_xi", dampingRatio);
        physicsParams.put("dampedFrequency_rad_per_s", dampedFrequency);
        physicsParams.put("gravity_m_per_s2", GRAVITY);
        physicsParams.put("oscillationFrequency_Hz", naturalFrequency / (2 * Math.PI));
        physicsParams.put("beamMass_g", beamMass);
        physicsParams.put("knifeEdgeRadius_mm", knifeEdgeRadius);
        result.setPhysicsParameters(physicsParams);

        Map<String, Object> animationData = new LinkedHashMap<>();
        List<Map<String, Object>> oscillationFrames = generateOscillationFrames(
                swingAngle, dampingRatio, naturalFrequency, dampedFrequency, equilibriumTime);
        animationData.put("initialAngle", swingAngle);
        animationData.put("finalAngle_deg", swingAngle * Math.exp(-dampingRatio * naturalFrequency * equilibriumTime));
        animationData.put("decayTime_s", equilibriumTime);
        animationData.put("oscillationCount", (int) (equilibriumTime * dampedFrequency / (2 * Math.PI)));
        animationData.put("dampedFrequency_Hz", dampedFrequency / (2 * Math.PI));
        animationData.put("oscillationFrames", oscillationFrames);
        result.setAnimationData(animationData);

        List<String> insights = generateInsights(leftItems, rightItems, leftMass, rightMass, torqueDifference_gmm, balanceStatus);
        result.setCulturalInsights(insights);

        Map<String, Object> conversion = generateMassConversion(leftMass, rightMass);
        result.setMassConversion(conversion);

        logger.info("虚拟称量完成: 左盘{}件({}g), 右盘{}件({}g), 状态={}",
                leftItems.size(), String.format("%.2f", leftMass),
                rightItems.size(), String.format("%.2f", rightMass),
                balanceStatus);

        return result;
    }

    private double calculateBeamMomentOfInertia(double beamMass_g, double leftArm_mm,
                                                  double rightArm_mm, double width_mm, double thickness_mm) {
        double beamMass_kg = beamMass_g / 1000.0;
        double totalLength_m = (leftArm_mm + rightArm_mm) / 1000.0;
        double w_m = width_mm / 1000.0;
        double t_m = thickness_mm / 1000.0;
        double I_rect = beamMass_kg * (totalLength_m * totalLength_m + w_m * w_m) / 12.0;
        double I_rod = beamMass_kg * totalLength_m * totalLength_m / 12.0;
        return Math.max(I_rect, I_rod);
    }

    private double calculateAirDamping(double leftArm_mm, double rightArm_mm, double width_mm, double thickness_mm) {
        double totalLength_m = (leftArm_mm + rightArm_mm) / 1000.0;
        double w_m = width_mm / 1000.0;
        double characteristicLength = totalLength_m;
        double reynoldsNumber = 1.225 * 1.0 * characteristicLength / AIR_VISCOSITY;
        double dragCoefficient = reynoldsNumber < 1000 ? 10.0 / Math.sqrt(reynoldsNumber) : 1.2;
        double frontalArea = totalLength_m * w_m;
        double damping = 0.5 * 1.225 * dragCoefficient * frontalArea * characteristicLength;
        return Math.max(0.001, damping / 10.0);
    }

    private double calculateKnifeEdgeFrictionDamping(double totalMass_g, double knifeRadius_mm) {
        double totalMass_kg = totalMass_g / 1000.0;
        double normalForce = totalMass_kg * GRAVITY;
        double frictionCoeff = 0.001;
        double r_m = knifeRadius_mm / 1000.0;
        return frictionCoeff * normalForce * r_m;
    }

    private double calculateRealSwingAngle(double torqueDiff_Nm, double torsionalStiffness,
                                            double dampingRatio) {
        double staticAngle_rad = torqueDiff_Nm / Math.max(torsionalStiffness, 0.001);
        double maxAngle_rad = 15.0 * Math.PI / 180.0;
        if (Math.abs(staticAngle_rad) > maxAngle_rad) {
            staticAngle_rad = Math.signum(staticAngle_rad) * maxAngle_rad;
        }
        double dynamicOvershoot = Math.exp(-dampingRatio * Math.PI / Math.sqrt(Math.max(0.001, 1.0 - dampingRatio * dampingRatio)));
        double peakAngle_rad = staticAngle_rad * (1.0 + dynamicOvershoot);
        return peakAngle_rad * 180.0 / Math.PI;
    }

    private double calculateRealEquilibriumTime(double dampingRatio, double naturalFrequency) {
        if (dampingRatio >= 1.0) {
            return 4.0 / (dampingRatio * naturalFrequency);
        }
        double settlingTime = 4.0 / (dampingRatio * Math.max(naturalFrequency, 0.1));
        return Math.max(0.5, Math.min(15.0, settlingTime));
    }

    private List<Map<String, Object>> generateOscillationFrames(double initialAngle_deg, double dampingRatio,
                                                                  double naturalFrequency, double dampedFrequency,
                                                                  double totalTime_s) {
        List<Map<String, Object>> frames = new ArrayList<>();
        int frameCount = 30;
        double dt = totalTime_s / frameCount;
        double initialAngle_rad = initialAngle_deg * Math.PI / 180.0;

        for (int i = 0; i <= frameCount; i++) {
            double t = i * dt;
            double envelope = Math.exp(-dampingRatio * naturalFrequency * t);
            double angle_rad;
            if (dampingRatio >= 1.0) {
                double alpha1 = (-dampingRatio + Math.sqrt(dampingRatio * dampingRatio - 1.0)) * naturalFrequency;
                double alpha2 = (-dampingRatio - Math.sqrt(dampingRatio * dampingRatio - 1.0)) * naturalFrequency;
                angle_rad = initialAngle_rad * (alpha2 * Math.exp(alpha1 * t) - alpha1 * Math.exp(alpha2 * t)) / (alpha2 - alpha1);
            } else {
                angle_rad = initialAngle_rad * envelope * Math.cos(dampedFrequency * t);
            }
            double angle_deg = angle_rad * 180.0 / Math.PI;
            double angularVelocity_rad_s;
            if (dampingRatio >= 1.0) {
                double alpha1 = (-dampingRatio + Math.sqrt(dampingRatio * dampingRatio - 1.0)) * naturalFrequency;
                double alpha2 = (-dampingRatio - Math.sqrt(dampingRatio * dampingRatio - 1.0)) * naturalFrequency;
                angularVelocity_rad_s = initialAngle_rad * (alpha1 * alpha2 * Math.exp(alpha1 * t) - alpha1 * alpha2 * Math.exp(alpha2 * t)) / (alpha2 - alpha1);
            } else {
                angularVelocity_rad_s = -initialAngle_rad * envelope * (
                        dampingRatio * naturalFrequency * Math.cos(dampedFrequency * t)
                                + dampedFrequency * Math.sin(dampedFrequency * t));
            }
            Map<String, Object> frame = new LinkedHashMap<>();
            frame.put("time_s", t);
            frame.put("angle_deg", angle_deg);
            frame.put("angularVelocity_deg_per_s", angularVelocity_rad_s * 180.0 / Math.PI);
            frame.put("envelope", envelope);
            frames.add(frame);
        }
        return frames;
    }

    private List<String> generateInsights(List<VirtualWeighingItem> leftItems,
                                           List<VirtualWeighingItem> rightItems,
                                           double leftMass, double rightMass,
                                           double torqueDiff, String status) {
        List<String> insights = new ArrayList<>();

        if ("BALANCED".equals(status)) {
            insights.add("⚖️ 天平平衡！左右两盘力矩相等");
            insights.add(String.format("两侧质量均为 %.2f 克，杠杆原理完美体现", leftMass));
            insights.add(String.format("力矩差值仅为 %.2f g·mm，精度达到 %.1f 级",
                    torqueDiff, Math.max(0, 100 - Math.abs(torqueDiff) * 100)));

            if (leftMass >= 245 && leftMass <= 255) {
                insights.add("💡 这个重量接近秦代1斤标准(约253g)");
            } else if (leftMass >= 240 && leftMass <= 250) {
                insights.add("💡 这个重量接近西汉1斤标准(约248g)");
            } else if (leftMass >= 580 && leftMass <= 590) {
                insights.add("💡 这个重量接近明代1斤标准(约585g)");
            } else if (leftMass >= 15 && leftMass <= 16) {
                insights.add("💡 这个重量接近秦代1两标准(约15.625g)");
            } else if (leftMass >= 41 && leftMass <= 42) {
                insights.add("💡 这个重量接近唐代1两标准(约41.375g)");
            } else if (leftMass >= 320 && leftMass <= 330) {
                insights.add("💡 这个重量接近罗马1磅标准(约327.4g)");
            }
        } else {
            String heavySide = "LEFT_HEAVY".equals(status) ? "左盘" : "右盘";
            String lightSide = "LEFT_HEAVY".equals(status) ? "右盘" : "左盘";
            double diff = Math.abs(leftMass - rightMass);

            insights.add("⚖️ " + heavySide + "较重，天平向" + heavySide + "倾斜");
            insights.add(String.format("%s重 %.2f g，%s重 %.2f g，相差 %.2f g",
                    heavySide, "LEFT_HEAVY".equals(status) ? leftMass : rightMass,
                    lightSide, "LEFT_HEAVY".equals(status) ? rightMass : leftMass,
                    diff));

            if (diff > 200) {
                insights.add("💡 质量差异较大，需要添加更多砝码使天平平衡");
            } else if (diff > 50) {
                insights.add("💡 差异中等，可以通过调整砝码配置达到平衡");
            } else {
                insights.add("💡 差异较小，只需微调即可平衡");
            }
        }

        if (!leftItems.isEmpty() && !rightItems.isEmpty()) {
            Set<String> leftCiv = leftItems.stream()
                    .map(VirtualWeighingItem::getCivilization)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Set<String> rightCiv = rightItems.stream()
                    .map(VirtualWeighingItem::getCivilization)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (!leftCiv.equals(rightCiv) && !leftCiv.isEmpty() && !rightCiv.isEmpty()) {
                insights.add(String.format("🌍 跨文明称量对比：%s文物 vs %s文物",
                        String.join("、", leftCiv), String.join("、", rightCiv)));
            }
        }

        boolean hasLegendary = leftItems.stream().anyMatch(i -> "legendary".equals(i.getRarity()))
                || rightItems.stream().anyMatch(i -> "legendary".equals(i.getRarity()));
        if (hasLegendary) {
            insights.add("🌟 正在使用传说级文物进行称量，这是难得一见的文化体验！");
        }

        boolean hasRare = leftItems.stream().anyMatch(i -> "rare".equals(i.getRarity()))
                || rightItems.stream().anyMatch(i -> "rare".equals(i.getRarity()));
        if (hasRare && !hasLegendary) {
            insights.add("✨ 正在使用珍贵文物进行称量，请小心操作！");
        }

        return insights;
    }

    private Map<String, Object> generateMassConversion(double leftMass, double rightMass) {
        Map<String, Object> conversion = new LinkedHashMap<>();

        double showMass = Math.max(leftMass, rightMass);

        conversion.put("grams", showMass);
        conversion.put("kilograms", showMass / 1000.0);

        conversion.put("秦斤", showMass / 253.0);
        conversion.put("秦两", showMass / 15.8125);
        conversion.put("西汉斤", showMass / 248.0);
        conversion.put("西汉两", showMass / 15.5);
        conversion.put("唐斤", showMass / 662.0);
        conversion.put("唐两", showMass / 41.375);
        conversion.put("明斤", showMass / 585.0);
        conversion.put("明两", showMass / 36.5625);
        conversion.put("罗马磅", showMass / 327.4);

        conversion.put("市斤", showMass / 500.0);
        conversion.put("市两", showMass / 50.0);

        conversion.put("金衡盎司", showMass / 31.1035);
        conversion.put("常衡盎司", showMass / 28.3495);
        conversion.put("磅", showMass / 453.5924);
        conversion.put("克拉", showMass / 0.2);

        return conversion;
    }

    public Map<String, Object> getHistoricalContext(String civilization) {
        Map<String, Object> context = new LinkedHashMap<>();

        if ("中国".equals(civilization)) {
            context.put("title", "中国古代权衡文化");
            context.put("description", "中国古代权衡制度源远流长，从商代雏形到明清完备，形成了独特的度量衡体系。");
            context.put("keyPoints", Arrays.asList(
                    "商代已有权衡器具雏形",
                    "战国时期各国度量衡不统一，秦楚齐等国已有等臂天平",
                    "秦始皇统一度量衡，确立1斤=16两=253克的标准",
                    "唐代发明戥秤，专门用于金银珠宝的精密称量",
                    "明清时期戥秤工艺达到顶峰"
            ));
            context.put("culturalReference", "《墨经》中已有对杠杆原理的科学论述，比阿基米德早约200年");
        } else if ("罗马".equals(civilization)) {
            context.put("title", "古罗马权衡文化");
            context.put("description", "古罗马天平(Steelyard)采用不等臂设计，通过滑动砝码实现大称量，广泛应用于商业贸易。");
            context.put("keyPoints", Arrays.asList(
                    "罗马天平为不等臂设计，又称杆秤",
                    "使用游标原理，通过滑动游砣实现不同量程",
                    "1罗马磅约等于327.4克",
                    "庞贝遗址出土了大量天平实物",
                    "罗马法对度量衡有严格规定"
            ));
            context.put("culturalReference", "老普林尼《自然史》中记载了古罗马的度量衡制度");
        } else if ("埃及".equals(civilization)) {
            context.put("title", "古埃及权衡文化");
            context.put("description", "古埃及是世界上最早使用天平的文明之一，天平在宗教和经济生活中占据重要地位。");
            context.put("keyPoints", Arrays.asList(
                    "古王国时期已有天平用于称量香料和贵金属",
                    "天平在亡灵审判中扮演重要角色——称量心脏",
                    "1埃及德本约等于91克",
                    "图坦卡蒙墓中出土了黄金天平",
                    "莎草纸壁画中有大量天平称量场景"
            ));
            context.put("culturalReference", "《亡灵书》中记载了奥西里斯审判中用天平称量死者心脏的仪式");
        } else {
            context.put("title", "古代权衡文化");
            context.put("description", "不同文明独立发明了权衡器具，体现了人类对公平计量的共同追求。");
            context.put("keyPoints", Arrays.asList(
                    "古代天平最早出现在公元前5000年的美索不达米亚",
                    "等臂天平是各文明早期的共同选择",
                    "不等臂天平(杆秤)在中国、罗马、印度等地独立发展",
                    "玛瑙和玉石是古代精密天平刀口的首选材料",
                    "权衡制度的统一是国家治理的重要标志"
            ));
            context.put("culturalReference", "世界各地古代神话中都有天平象征正义和公平的意象");
        }

        return context;
    }

    public Map<String, Object> getLeverPrincipleExplanation() {
        Map<String, Object> explanation = new LinkedHashMap<>();

        explanation.put("title", "杠杆原理：从古代到现代");
        explanation.put("principle", "F₁ × L₁ = F₂ × L₂");
        explanation.put("principleCn", "动力 × 动力臂 = 阻力 × 阻力臂");

        List<Map<String, Object>> history = new ArrayList<>();
        history.add(createHistoryEntry("公元前4世纪", "墨子",
                "《墨经》中记载了杠杆原理的完整论述，包括等臂和不等臂两种情况"));
        history.add(createHistoryEntry("公元前3世纪", "阿基米德",
                "古希腊数学家阿基米德系统阐述了杠杆原理，提出\"给我一个支点，我能撬动地球\""));
        history.add(createHistoryEntry("公元1世纪", "罗马人",
                "广泛使用不等臂天平(Steelyard)进行商业贸易"));
        history.add(createHistoryEntry("公元7世纪", "唐代工匠",
                "发明戥秤，将等臂天平的精度推向新高度"));
        history.add(createHistoryEntry("公元17世纪", "现代科学",
                "伽利略、牛顿等科学家将杠杆原理纳入经典力学体系"));
        explanation.put("history", history);

        List<Map<String, Object>> modernApplications = new ArrayList<>();
        modernApplications.add(createAppEntry("精密电子天平", "实验室高精度分析，分辨率可达0.1微克",
                "https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=precision%20electronic%20balance&image_size=square"));
        modernApplications.add(createAppEntry("地磅", "卡车、集装箱等大质量称量，量程可达百吨",
                "https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=truck%20weighbridge&image_size=square"));
        modernApplications.add(createAppEntry("实验室分析天平", "化学分析、质量计量标准",
                "https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=analytical%20balance%20laboratory&image_size=square"));
        modernApplications.add(createAppEntry("珠宝秤", "贵金属、宝石精密称量",
                "https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=jewelry%20precision%20scale&image_size=square"));
        modernApplications.add(createAppEntry("杆秤", "传统市场仍在使用的不等臂杠杆",
                "https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=chinese%20traditional%20steelyard&image_size=square"));
        explanation.put("modernApplications", modernApplications);

        Map<String, Object> formulaExplanation = new LinkedHashMap<>();
        formulaExplanation.put("equalArm", "等臂天平：L₁=L₂，故F₁=F₂，直接比较质量");
        formulaExplanation.put("unequalArm", "不等臂天平：L₁≠L₂，通过力臂比换算质量");
        formulaExplanation.put("sensitivity", "灵敏度 = 偏转角 / 质量差，与臂长成正比，与摩擦成反比");
        formulaExplanation.put("precision", "相对精度 = 最小分度值 / 最大称量");
        explanation.put("formulaExplanation", formulaExplanation);

        return explanation;
    }

    private Map<String, Object> createHistoryEntry(String period, String figure, String description) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("period", period);
        entry.put("figure", figure);
        entry.put("description", description);
        return entry;
    }

    private Map<String, Object> createAppEntry(String name, String description, String imageUrl) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", name);
        entry.put("description", description);
        entry.put("imageUrl", imageUrl);
        return entry;
    }
}
