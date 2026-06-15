package com.metrology.balance.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "calibration_results")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class CalibrationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "device_id")
    private Integer deviceId;

    @Column(name = "balance_id")
    private Integer balanceId;

    @Column(name = "calibration_time", nullable = false)
    private LocalDateTime calibrationTime;

    @Column(name = "calibration_method", length = 50)
    private String calibrationMethod;

    @Column(name = "left_arm_correction", precision = 12, scale = 8)
    private Double leftArmCorrection;

    @Column(name = "right_arm_correction", precision = 12, scale = 8)
    private Double rightArmCorrection;

    @Column(name = "arm_length_ratio_correction", precision = 12, scale = 8)
    private Double armLengthRatioCorrection;

    @Column(name = "zero_point_drift", precision = 12, scale = 8)
    private Double zeroPointDrift;

    @Column(name = "linearity_error", precision = 12, scale = 8)
    private Double linearityError;

    @Column(name = "repeatability_std", precision = 12, scale = 8)
    private Double repeatabilityStd;

    @Column(name = "hysteresis_error", precision = 12, scale = 8)
    private Double hysteresisError;

    @Column(name = "corrected_uncertainty", precision = 12, scale = 8)
    private Double correctedUncertainty;

    @Column(name = "calibration_grade", length = 20)
    private String calibrationGrade;

    @Type(type = "jsonb")
    @Column(name = "positions_data", columnDefinition = "jsonb")
    private Map<String, Object> positionsData;

    @Type(type = "jsonb")
    @Column(name = "correction_table", columnDefinition = "jsonb")
    private Map<String, Object> correctionTable;

    @Type(type = "jsonb")
    @Column(name = "raw_measurements", columnDefinition = "jsonb")
    private Map<String, Object> rawMeasurements;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (calibrationTime == null) {
            calibrationTime = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getBalanceId() {
        return balanceId;
    }

    public void setBalanceId(Integer balanceId) {
        this.balanceId = balanceId;
    }

    public LocalDateTime getCalibrationTime() {
        return calibrationTime;
    }

    public void setCalibrationTime(LocalDateTime calibrationTime) {
        this.calibrationTime = calibrationTime;
    }

    public String getCalibrationMethod() {
        return calibrationMethod;
    }

    public void setCalibrationMethod(String calibrationMethod) {
        this.calibrationMethod = calibrationMethod;
    }

    public Double getLeftArmCorrection() {
        return leftArmCorrection;
    }

    public void setLeftArmCorrection(Double leftArmCorrection) {
        this.leftArmCorrection = leftArmCorrection;
    }

    public Double getRightArmCorrection() {
        return rightArmCorrection;
    }

    public void setRightArmCorrection(Double rightArmCorrection) {
        this.rightArmCorrection = rightArmCorrection;
    }

    public Double getArmLengthRatioCorrection() {
        return armLengthRatioCorrection;
    }

    public void setArmLengthRatioCorrection(Double armLengthRatioCorrection) {
        this.armLengthRatioCorrection = armLengthRatioCorrection;
    }

    public Double getZeroPointDrift() {
        return zeroPointDrift;
    }

    public void setZeroPointDrift(Double zeroPointDrift) {
        this.zeroPointDrift = zeroPointDrift;
    }

    public Double getLinearityError() {
        return linearityError;
    }

    public void setLinearityError(Double linearityError) {
        this.linearityError = linearityError;
    }

    public Double getRepeatabilityStd() {
        return repeatabilityStd;
    }

    public void setRepeatabilityStd(Double repeatabilityStd) {
        this.repeatabilityStd = repeatabilityStd;
    }

    public Double getHysteresisError() {
        return hysteresisError;
    }

    public void setHysteresisError(Double hysteresisError) {
        this.hysteresisError = hysteresisError;
    }

    public Double getCorrectedUncertainty() {
        return correctedUncertainty;
    }

    public void setCorrectedUncertainty(Double correctedUncertainty) {
        this.correctedUncertainty = correctedUncertainty;
    }

    public String getCalibrationGrade() {
        return calibrationGrade;
    }

    public void setCalibrationGrade(String calibrationGrade) {
        this.calibrationGrade = calibrationGrade;
    }

    public Map<String, Object> getPositionsData() {
        return positionsData;
    }

    public void setPositionsData(Map<String, Object> positionsData) {
        this.positionsData = positionsData;
    }

    public Map<String, Object> getCorrectionTable() {
        return correctionTable;
    }

    public void setCorrectionTable(Map<String, Object> correctionTable) {
        this.correctionTable = correctionTable;
    }

    public Map<String, Object> getRawMeasurements() {
        return rawMeasurements;
    }

    public void setRawMeasurements(Map<String, Object> rawMeasurements) {
        this.rawMeasurements = rawMeasurements;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
