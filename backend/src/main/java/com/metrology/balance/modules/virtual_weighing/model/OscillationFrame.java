package com.metrology.balance.modules.virtual_weighing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OscillationFrame {

    private double timeS;
    private double angleDeg;
    private double angularVelocityDegPerS;
    private double envelope;
}
