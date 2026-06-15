package com.metrology.balance.modules.calibration_device.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalibrationReport {

    private String reportTitle;

    private String reportNumber;

    private LocalDateTime calibrationDate;

    private String calibrationMethod;

    private DeviceInfo deviceInfo;

    private CalibrationResults calibrationResults;

    private List<String> conclusions;

    private List<UncertaintySource> uncertaintyBudget;

    private VibrationAnalysis vibrationAnalysis;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeviceInfo {
        private String deviceName;
        private String deviceType;
        private Double leftArmLength;
        private Double rightArmLength;
        private Double armRatio;
        private Double knifeEdgeRadius;
        private Double maxCapacity;
        private Double minReadability;
        private String material;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CalibrationResults {
        private String calibrationGrade;
        private Double expandedUncertainty;
        private Double kFactor;
        private String confidenceLevel;
        private Double leftArmCorrection;
        private Double rightArmCorrection;
        private Double armRatioCorrection;
        private Double zeroPointDrift;
        private Double linearityError;
        private Double repeatabilityStd;
        private Double hysteresisError;
        private Map<String, Object> correctionTable;
        private Map<String, Object> positionsData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UncertaintySource {
        private String source;
        private Double value;
        private String type;
        private Integer degreesOfFreedom;
        private Double sensitivity;
        private Double contribution;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VibrationAnalysis {
        private String environmentLevel;
        private String environmentLabel;
        private String isolationSystem;
        private String isolationLabel;
        private Double isolationEfficiencyDb;
        private Double inputVibrationXUm;
        private Double inputVibrationYUm;
        private Double inputVibrationZUm;
        private Double residualVibrationRMSUm;
        private Double vibrationInducedErrorMm;
        private Double vibrationNoiseScale;
        private Double vibrationUncertaintyComponent;
    }
}
