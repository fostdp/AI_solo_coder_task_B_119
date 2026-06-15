package com.metrology.balance.modules.virtual_weighing.physics;

import com.metrology.balance.modules.virtual_weighing.model.OscillationFrame;
import com.metrology.balance.modules.virtual_weighing.model.PhysicsParameters;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BalancePhysicsEngine {

    public static final double GRAVITY = 9.80665;
    public static final double AIR_VISCOSITY = 1.81e-5;
    public static final double AIR_DENSITY = 1.225;
    public static final double BEAM_WIDTH = 6.0;
    public static final double BEAM_THICKNESS = 3.0;

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

    private BalancePhysicsEngine() {
    }

    public static double getMaterialDamping(String material) {
        return MATERIAL_DAMPING.getOrDefault(material, 0.08);
    }

    public static double getMaterialStiffness(String material) {
        return MATERIAL_STIFFNESS.getOrDefault(material, TORSIONAL_STIFFNESS_BRONZE);
    }

    public static double calculateBeamMomentOfInertia(double beamMassG, double leftArmMm,
                                                       double rightArmMm, double widthMm, double thicknessMm) {
        double beamMassKg = beamMassG / 1000.0;
        double totalLengthM = (leftArmMm + rightArmMm) / 1000.0;
        double wM = widthMm / 1000.0;
        double tM = thicknessMm / 1000.0;
        double iRect = beamMassKg * (totalLengthM * totalLengthM + wM * wM) / 12.0;
        double iRod = beamMassKg * totalLengthM * totalLengthM / 12.0;
        return Math.max(iRect, iRod);
    }

    public static double calculateAirDamping(double leftArmMm, double rightArmMm, double widthMm, double thicknessMm) {
        double totalLengthM = (leftArmMm + rightArmMm) / 1000.0;
        double wM = widthMm / 1000.0;
        double characteristicLength = totalLengthM;
        double reynoldsNumber = AIR_DENSITY * 1.0 * characteristicLength / AIR_VISCOSITY;
        double dragCoefficient = reynoldsNumber < 1000 ? 10.0 / Math.sqrt(reynoldsNumber) : 1.2;
        double frontalArea = totalLengthM * wM;
        double damping = 0.5 * AIR_DENSITY * dragCoefficient * frontalArea * characteristicLength;
        return Math.max(0.001, damping / 10.0);
    }

    public static double calculateKnifeEdgeFrictionDamping(double totalMassG, double knifeRadiusMm) {
        double totalMassKg = totalMassG / 1000.0;
        double normalForce = totalMassKg * GRAVITY;
        double frictionCoeff = 0.001;
        double rM = knifeRadiusMm / 1000.0;
        return frictionCoeff * normalForce * rM;
    }

    public static double calculateTorsionalStiffness(String beamMaterial, double knifeEdgeRadiusMm,
                                                      double leftArmMm, double rightArmMm) {
        double baseStiffness = getMaterialStiffness(beamMaterial);
        double torsionalStiffness = baseStiffness * Math.pow(knifeEdgeRadiusMm, 4) / (leftArmMm + rightArmMm) * 1000.0;
        return Math.max(0.005, torsionalStiffness / 1000000.0);
    }

    public static double calculateTotalDampingCoefficient(String beamMaterial, double torsionalStiffness,
                                                           double totalMomentOfInertia, double airDamping,
                                                           double frictionDamping) {
        double materialDampingCoeff = getMaterialDamping(beamMaterial);
        double totalDampingCoeff = materialDampingCoeff * 2.0 * Math.sqrt(torsionalStiffness * totalMomentOfInertia);
        return Math.max(totalDampingCoeff, airDamping + frictionDamping);
    }

    public static double calculateNaturalFrequency(double torsionalStiffness, double totalMomentOfInertia) {
        return Math.sqrt(torsionalStiffness / totalMomentOfInertia);
    }

    public static double calculateDampingRatio(double totalDampingCoeff, double torsionalStiffness,
                                                double totalMomentOfInertia) {
        double dampingRatio = totalDampingCoeff / (2.0 * Math.sqrt(torsionalStiffness * totalMomentOfInertia));
        return Math.min(0.95, dampingRatio);
    }

    public static double calculateDampedFrequency(double naturalFrequency, double dampingRatio) {
        return naturalFrequency * Math.sqrt(1.0 - dampingRatio * dampingRatio);
    }

    public static double calculateSwingAngle(double torqueDiffNm, double torsionalStiffness, double dampingRatio) {
        double staticAngleRad = torqueDiffNm / Math.max(torsionalStiffness, 0.001);
        double maxAngleRad = 15.0 * Math.PI / 180.0;
        if (Math.abs(staticAngleRad) > maxAngleRad) {
            staticAngleRad = Math.signum(staticAngleRad) * maxAngleRad;
        }
        double dynamicOvershoot = Math.exp(-dampingRatio * Math.PI / Math.sqrt(Math.max(0.001, 1.0 - dampingRatio * dampingRatio)));
        double peakAngleRad = staticAngleRad * (1.0 + dynamicOvershoot);
        return peakAngleRad * 180.0 / Math.PI;
    }

    public static double calculateEquilibriumTime(double dampingRatio, double naturalFrequency) {
        if (dampingRatio >= 1.0) {
            return 4.0 / (dampingRatio * naturalFrequency);
        }
        double settlingTime = 4.0 / (dampingRatio * Math.max(naturalFrequency, 0.1));
        return Math.max(0.5, Math.min(15.0, settlingTime));
    }

    public static List<OscillationFrame> generateOscillationFrames(double initialAngleDeg, double dampingRatio,
                                                                    double naturalFrequency, double dampedFrequency,
                                                                    double totalTimeS) {
        List<OscillationFrame> frames = new ArrayList<>();
        int frameCount = 30;
        double dt = totalTimeS / frameCount;
        double initialAngleRad = initialAngleDeg * Math.PI / 180.0;

        for (int i = 0; i <= frameCount; i++) {
            double t = i * dt;
            double envelope = Math.exp(-dampingRatio * naturalFrequency * t);
            double angleRad;
            double angularVelocityRadS;

            if (dampingRatio >= 1.0) {
                double alpha1 = (-dampingRatio + Math.sqrt(dampingRatio * dampingRatio - 1.0)) * naturalFrequency;
                double alpha2 = (-dampingRatio - Math.sqrt(dampingRatio * dampingRatio - 1.0)) * naturalFrequency;
                angleRad = initialAngleRad * (alpha2 * Math.exp(alpha1 * t) - alpha1 * Math.exp(alpha2 * t)) / (alpha2 - alpha1);
                angularVelocityRadS = initialAngleRad * (alpha1 * alpha2 * Math.exp(alpha1 * t) - alpha1 * alpha2 * Math.exp(alpha2 * t)) / (alpha2 - alpha1);
            } else {
                angleRad = initialAngleRad * envelope * Math.cos(dampedFrequency * t);
                angularVelocityRadS = -initialAngleRad * envelope * (
                        dampingRatio * naturalFrequency * Math.cos(dampedFrequency * t)
                                + dampedFrequency * Math.sin(dampedFrequency * t));
            }

            OscillationFrame frame = OscillationFrame.builder()
                    .timeS(t)
                    .angleDeg(angleRad * 180.0 / Math.PI)
                    .angularVelocityDegPerS(angularVelocityRadS * 180.0 / Math.PI)
                    .envelope(envelope)
                    .build();
            frames.add(frame);
        }
        return frames;
    }

    public static PhysicsParameters buildPhysicsParameters(double leftArmMm, double rightArmMm,
                                                           double leftMassG, double rightMassG,
                                                           double beamMassG, double knifeEdgeRadiusMm,
                                                           String beamMaterial) {
        double leftTorqueGmm = leftMassG * leftArmMm;
        double rightTorqueGmm = rightMassG * rightArmMm;

        double beamMomentOfInertia = calculateBeamMomentOfInertia(beamMassG, leftArmMm, rightArmMm, BEAM_WIDTH, BEAM_THICKNESS);
        double leftMassMomentOfInertia = leftMassG / 1000.0 * leftArmMm * leftArmMm / 1000000.0;
        double rightMassMomentOfInertia = rightMassG / 1000.0 * rightArmMm * rightArmMm / 1000000.0;
        double totalMomentOfInertia = beamMomentOfInertia + leftMassMomentOfInertia + rightMassMomentOfInertia;

        double torsionalStiffness = calculateTorsionalStiffness(beamMaterial, knifeEdgeRadiusMm, leftArmMm, rightArmMm);

        double airDamping = calculateAirDamping(leftArmMm, rightArmMm, BEAM_WIDTH, BEAM_THICKNESS);
        double frictionDamping = calculateKnifeEdgeFrictionDamping(beamMassG + leftMassG + rightMassG, knifeEdgeRadiusMm);
        double totalDampingCoeff = calculateTotalDampingCoefficient(beamMaterial, torsionalStiffness, totalMomentOfInertia,
                airDamping, frictionDamping);

        double naturalFrequency = calculateNaturalFrequency(torsionalStiffness, totalMomentOfInertia);
        double dampingRatio = calculateDampingRatio(totalDampingCoeff, torsionalStiffness, totalMomentOfInertia);
        double dampedFrequency = calculateDampedFrequency(naturalFrequency, dampingRatio);

        return PhysicsParameters.builder()
                .leftArmLengthMm(leftArmMm)
                .rightArmLengthMm(rightArmMm)
                .leftTorqueGmm(leftTorqueGmm)
                .rightTorqueGmm(rightTorqueGmm)
                .beamMomentOfInertiaKgm2(beamMomentOfInertia)
                .totalMomentOfInertiaKgm2(totalMomentOfInertia)
                .torsionalStiffnessNmPerRad(torsionalStiffness)
                .materialDampingCoefficient(getMaterialDamping(beamMaterial))
                .airDampingCoefficient(airDamping)
                .frictionDampingCoefficient(frictionDamping)
                .totalDampingCoefficient(totalDampingCoeff)
                .naturalFrequencyRadPerS(naturalFrequency)
                .dampingRatioXi(dampingRatio)
                .dampedFrequencyRadPerS(dampedFrequency)
                .gravityMPerS2(GRAVITY)
                .oscillationFrequencyHz(naturalFrequency / (2 * Math.PI))
                .beamMassG(beamMassG)
                .knifeEdgeRadiusMm(knifeEdgeRadiusMm)
                .build();
    }

    public enum DampingType {
        UNDERDAMPED("欠阻尼"),
        CRITICALLY_DAMPED("临界阻尼"),
        OVERDAMPED("过阻尼");

        private final String description;

        DampingType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public static DampingType getDampingType(double dampingRatio) {
        if (dampingRatio < 1.0) {
            return DampingType.UNDERDAMPED;
        } else if (Math.abs(dampingRatio - 1.0) < 0.001) {
            return DampingType.CRITICALLY_DAMPED;
        } else {
            return DampingType.OVERDAMPED;
        }
    }
}
