package com.metrology.balance.modules.civilization_comparator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceClassification {

    private String code;

    private String label;

    private Double reliabilityScore;

    private String description;

    public enum DataSourceType {
        ARCHAEOLOGICAL("ARCHAEOLOGICAL", "考古实物测量", 0.95, "通过对出土文物进行实际测量获得的数据"),
        LITERARY("LITERARY", "文献记载考证", 0.70, "通过古代文献、典籍记载考据获得的数据"),
        EXPERT_ESTIMATE("EXPERT_ESTIMATE", "专家估算推断", 0.60, "由领域专家基于经验估算和推断的数据"),
        EXPERIMENTAL_RECONSTRUCTION("EXPERIMENTAL_RECONSTRUCTION", "实验复原测量", 0.85, "通过复原实验进行测量获得的数据"),
        MIXED("MIXED", "多源综合数据", 0.80, "综合多种来源的数据进行交叉验证");

        private final String code;
        private final String label;
        private final double reliability;
        private final String description;

        DataSourceType(String code, String label, double reliability, String description) {
            this.code = code;
            this.label = label;
            this.reliability = reliability;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public double getReliability() {
            return reliability;
        }

        public String getDescription() {
            return description;
        }

        public static DataSourceClassification fromCode(String code) {
            for (DataSourceType type : values()) {
                if (type.code.equals(code)) {
                    return DataSourceClassification.builder()
                            .code(type.code)
                            .label(type.label)
                            .reliabilityScore(type.reliability)
                            .description(type.description)
                            .build();
                }
            }
            return DataSourceClassification.builder()
                    .code("UNKNOWN")
                    .label("未知来源")
                    .reliabilityScore(0.50)
                    .description("数据来源不明确")
                    .build();
        }
    }
}
