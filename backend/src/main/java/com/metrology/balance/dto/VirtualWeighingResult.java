package com.metrology.balance.dto;

import com.metrology.balance.entity.VirtualWeighingItem;

import java.util.List;
import java.util.Map;

public class VirtualWeighingResult {

    private List<VirtualWeighingItem> leftItems;
    private List<VirtualWeighingItem> rightItems;
    private Double leftTotalMass;
    private Double rightTotalMass;
    private Double massDifference;
    private Double torqueDifference;
    private Double swingAngle;
    private Double equilibriumTime;
    private String balanceType;
    private String balanceStatus;
    private Double relativeError;
    private Double precisionGrade;
    private Map<String, Object> physicsParameters;
    private Map<String, Object> animationData;
    private List<String> culturalInsights;
    private Map<String, Object> massConversion;

    public List<VirtualWeighingItem> getLeftItems() {
        return leftItems;
    }

    public void setLeftItems(List<VirtualWeighingItem> leftItems) {
        this.leftItems = leftItems;
    }

    public List<VirtualWeighingItem> getRightItems() {
        return rightItems;
    }

    public void setRightItems(List<VirtualWeighingItem> rightItems) {
        this.rightItems = rightItems;
    }

    public Double getLeftTotalMass() {
        return leftTotalMass;
    }

    public void setLeftTotalMass(Double leftTotalMass) {
        this.leftTotalMass = leftTotalMass;
    }

    public Double getRightTotalMass() {
        return rightTotalMass;
    }

    public void setRightTotalMass(Double rightTotalMass) {
        this.rightTotalMass = rightTotalMass;
    }

    public Double getMassDifference() {
        return massDifference;
    }

    public void setMassDifference(Double massDifference) {
        this.massDifference = massDifference;
    }

    public Double getTorqueDifference() {
        return torqueDifference;
    }

    public void setTorqueDifference(Double torqueDifference) {
        this.torqueDifference = torqueDifference;
    }

    public Double getSwingAngle() {
        return swingAngle;
    }

    public void setSwingAngle(Double swingAngle) {
        this.swingAngle = swingAngle;
    }

    public Double getEquilibriumTime() {
        return equilibriumTime;
    }

    public void setEquilibriumTime(Double equilibriumTime) {
        this.equilibriumTime = equilibriumTime;
    }

    public String getBalanceType() {
        return balanceType;
    }

    public void setBalanceType(String balanceType) {
        this.balanceType = balanceType;
    }

    public String getBalanceStatus() {
        return balanceStatus;
    }

    public void setBalanceStatus(String balanceStatus) {
        this.balanceStatus = balanceStatus;
    }

    public Double getRelativeError() {
        return relativeError;
    }

    public void setRelativeError(Double relativeError) {
        this.relativeError = relativeError;
    }

    public Double getPrecisionGrade() {
        return precisionGrade;
    }

    public void setPrecisionGrade(Double precisionGrade) {
        this.precisionGrade = precisionGrade;
    }

    public Map<String, Object> getPhysicsParameters() {
        return physicsParameters;
    }

    public void setPhysicsParameters(Map<String, Object> physicsParameters) {
        this.physicsParameters = physicsParameters;
    }

    public Map<String, Object> getAnimationData() {
        return animationData;
    }

    public void setAnimationData(Map<String, Object> animationData) {
        this.animationData = animationData;
    }

    public List<String> getCulturalInsights() {
        return culturalInsights;
    }

    public void setCulturalInsights(List<String> culturalInsights) {
        this.culturalInsights = culturalInsights;
    }

    public Map<String, Object> getMassConversion() {
        return massConversion;
    }

    public void setMassConversion(Map<String, Object> massConversion) {
        this.massConversion = massConversion;
    }
}
