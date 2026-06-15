package com.metrology.balance.modules.virtual_weighing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhysicsParameters {

    private double leftArmLengthMm;
    private double rightArmLengthMm;
    private double leftTorqueGmm;
    private double rightTorqueGmm;
    private double beamMomentOfInertiaKgm2;
    private double totalMomentOfInertiaKgm2;
    private double torsionalStiffnessNmPerRad;
    private double materialDampingCoefficient;
    private double airDampingCoefficient;
    private double frictionDampingCoefficient;
    private double totalDampingCoefficient;
    private double naturalFrequencyRadPerS;
    private double dampingRatioXi;
    private double dampedFrequencyRadPerS;
    private double gravityMPerS2;
    private double oscillationFrequencyHz;
    private double beamMassG;
    private double knifeEdgeRadiusMm;
}
