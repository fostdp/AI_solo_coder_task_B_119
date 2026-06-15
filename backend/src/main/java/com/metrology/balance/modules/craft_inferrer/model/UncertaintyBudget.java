package com.metrology.balance.modules.craft_inferrer.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UncertaintyBudget {

    private String source;
    private String type;
    private Double value;
    private Double degreesOfFreedom;
    private Double sensitivity;
    private Double contribution;
}
