package com.metrology.balance.modules.monte_carlo.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationParameter {

    private String name;
    private String distribution;
    private double mean;
    private double stdDev;
    private double minValue;
    private double maxValue;
    private String unit;
    private double sensitivityCoefficient;
}
