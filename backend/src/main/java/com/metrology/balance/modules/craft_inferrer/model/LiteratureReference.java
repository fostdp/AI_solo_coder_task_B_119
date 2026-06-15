package com.metrology.balance.modules.craft_inferrer.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiteratureReference {

    private String material;
    private String source;
    private double[] frictionRange;
    private double[] wearRange;
    private double[] knifeRadiusRange;
    private double baseUncertainty;
}
