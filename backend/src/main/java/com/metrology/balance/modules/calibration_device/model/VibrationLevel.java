package com.metrology.balance.modules.calibration_device.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VibrationLevel {

    private String code;

    private String label;

    private double displacementX;

    private double displacementY;

    private double displacementZ;

    public double getRmsDisplacement() {
        return Math.sqrt(displacementX * displacementX
                + displacementY * displacementY
                + displacementZ * displacementZ) / Math.sqrt(3.0);
    }

    public double getDisplacementXUm() {
        return displacementX * 1e6;
    }

    public double getDisplacementYUm() {
        return displacementY * 1e6;
    }

    public double getDisplacementZUm() {
        return displacementZ * 1e6;
    }

    public double getRmsDisplacementUm() {
        return getRmsDisplacement() * 1e6;
    }
}
