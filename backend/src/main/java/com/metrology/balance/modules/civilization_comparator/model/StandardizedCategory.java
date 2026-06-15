package com.metrology.balance.modules.civilization_comparator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardizedCategory {

    private String code;

    private String label;

    private String description;

    private Double typicalPrecision;

    private Double typicalCapacity;

    public enum CategoryType {
        PRECISION_LEGAL("PRECISION_LEGAL", "精密法定衡器",
                "用于官方计量、法律认证的精密衡器，要求高精度和稳定性",
                1e-3, 5.0),
        COMMERCIAL_TRADE("COMMERCIAL_TRADE", "商业贸易衡器",
                "用于商品交易、市场贸易的衡器，兼顾精度与量程",
                1e-2, 50.0),
        PRECIOUS_METAL("PRECIOUS_METAL", "金银珠宝衡器",
                "用于称量黄金、白银、珠宝等贵重物品的高精度小量程衡器",
                1e-4, 1.0),
        RITUAL_CEREMONIAL("RITUAL_CEREMONIAL", "礼仪祭祀衡器",
                "用于祭祀、礼仪活动的衡器，具有象征意义和文化价值",
                1e-2, 10.0),
        HOUSEHOLD_DAILY("HOUSEHOLD_DAILY", "民间日用衡器",
                "民间日常生活使用的衡器，结构简单，使用广泛",
                1e-1, 20.0),
        SCIENTIFIC_METROLOGY("SCIENTIFIC_METROLOGY", "科学计量衡器",
                "用于科学研究、精密计量的高端衡器，代表最高技术水平",
                1e-5, 0.5);

        private final String code;
        private final String label;
        private final String description;
        private final double typicalPrecision;
        private final double typicalCapacity;

        CategoryType(String code, String label, String description,
                     double typicalPrecision, double typicalCapacity) {
            this.code = code;
            this.label = label;
            this.description = description;
            this.typicalPrecision = typicalPrecision;
            this.typicalCapacity = typicalCapacity;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }

        public double getTypicalPrecision() {
            return typicalPrecision;
        }

        public double getTypicalCapacity() {
            return typicalCapacity;
        }

        public static StandardizedCategory fromCode(String code) {
            for (CategoryType type : values()) {
                if (type.code.equals(code)) {
                    return StandardizedCategory.builder()
                            .code(type.code)
                            .label(type.label)
                            .description(type.description)
                            .typicalPrecision(type.typicalPrecision)
                            .typicalCapacity(type.typicalCapacity)
                            .build();
                }
            }
            return StandardizedCategory.builder()
                    .code("UNCLASSIFIED")
                    .label("未分类")
                    .description("无法归类的衡器类型")
                    .typicalPrecision(null)
                    .typicalCapacity(null)
                    .build();
        }
    }
}
