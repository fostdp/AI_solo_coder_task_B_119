package com.metrology.balance.modules.civilization_comparator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertValidationResult {

    private String civilizationCode;

    private String civilizationName;

    private ValidationStatus status;

    private Double overallConfidence;

    @Builder.Default
    private List<String> passItems = new ArrayList<>();

    @Builder.Default
    private List<String> warningItems = new ArrayList<>();

    @Builder.Default
    private List<String> failItems = new ArrayList<>();

    private String validator;

    @Builder.Default
    private LocalDateTime validationTime = LocalDateTime.now();

    private DataSourceClassification dataSourceClassification;

    private StandardizedCategory standardizedCategory;

    public enum ValidationStatus {
        PASS("通过", "数据完整可靠，各项指标均符合要求"),
        WARNING("警告", "数据基本可靠，但存在部分需要注意的问题"),
        FAIL("失败", "数据存在严重问题，不建议用于正式分析");

        private final String label;
        private final String description;

        ValidationStatus(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }
}
