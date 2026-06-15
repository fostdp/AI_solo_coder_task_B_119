package com.metrology.balance.modules.craft_inferrer.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CraftMethodKnowledge {

    private String methodKey;
    private String methodName;
    private String period;
    private double[] knifeRadiusRange;
    private double[] scoreRange;
    private double[] frictionRange;
    private List<String> processSteps;
    private String description;
}
