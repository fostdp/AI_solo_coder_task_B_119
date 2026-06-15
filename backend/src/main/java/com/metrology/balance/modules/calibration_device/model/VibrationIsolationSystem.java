package com.metrology.balance.modules.calibration_device.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VibrationIsolationSystem {

    private String code;

    private String name;

    private double transmissibilityX;

    private double transmissibilityY;

    private double transmissibilityZ;

    private double isolationEfficiencyDb;

    public double getAverageTransmissibility() {
        return (transmissibilityX + transmissibilityY + transmissibilityZ) / 3.0;
    }

    public double getIsolationEfficiencyPercent() {
        return (1.0 - getAverageTransmissibility()) * 100.0;
    }

    public VibrationLevel applyIsolation(VibrationLevel input) {
        return VibrationLevel.builder()
                .code(input.getCode() + "_isolated")
                .label(input.getLabel() + " (经" + name + ")")
                .displacementX(input.getDisplacementX() * transmissibilityX)
                .displacementY(input.getDisplacementY() * transmissibilityY)
                .displacementZ(input.getDisplacementZ() * transmissibilityZ)
                .build();
    }
}
