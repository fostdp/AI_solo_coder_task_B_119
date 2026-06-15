package com.metrology.balance.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "calibration_devices")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class CalibrationDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "device_code", nullable = false, unique = true, length = 50)
    private String deviceCode;

    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName;

    @Column(name = "device_type", nullable = false, length = 50)
    private String deviceType;

    @Column(name = "balance_type", length = 20)
    private String balanceType = "EQUAL_ARM";

    @Column(name = "left_arm_length", precision = 10, scale = 4)
    private Double leftArmLength;

    @Column(name = "right_arm_length", precision = 10, scale = 4)
    private Double rightArmLength;

    @Column(name = "fulcrum_position", precision = 10, scale = 4)
    private Double fulcrumPosition;

    @Column(name = "knife_edge_radius", precision = 10, scale = 6)
    private Double knifeEdgeRadius;

    @Column(name = "max_capacity", precision = 10, scale = 2)
    private Double maxCapacity;

    @Column(name = "min_readability", precision = 12, scale = 8)
    private Double minReadability;

    @Column(name = "material", length = 50)
    private String material;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Type(type = "jsonb")
    @Column(name = "calibration_protocol", columnDefinition = "jsonb")
    private Map<String, Object> calibrationProtocol;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getBalanceType() {
        return balanceType;
    }

    public void setBalanceType(String balanceType) {
        this.balanceType = balanceType;
    }

    public Double getLeftArmLength() {
        return leftArmLength;
    }

    public void setLeftArmLength(Double leftArmLength) {
        this.leftArmLength = leftArmLength;
    }

    public Double getRightArmLength() {
        return rightArmLength;
    }

    public void setRightArmLength(Double rightArmLength) {
        this.rightArmLength = rightArmLength;
    }

    public Double getFulcrumPosition() {
        return fulcrumPosition;
    }

    public void setFulcrumPosition(Double fulcrumPosition) {
        this.fulcrumPosition = fulcrumPosition;
    }

    public Double getKnifeEdgeRadius() {
        return knifeEdgeRadius;
    }

    public void setKnifeEdgeRadius(Double knifeEdgeRadius) {
        this.knifeEdgeRadius = knifeEdgeRadius;
    }

    public Double getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Double maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Double getMinReadability() {
        return minReadability;
    }

    public void setMinReadability(Double minReadability) {
        this.minReadability = minReadability;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getCalibrationProtocol() {
        return calibrationProtocol;
    }

    public void setCalibrationProtocol(Map<String, Object> calibrationProtocol) {
        this.calibrationProtocol = calibrationProtocol;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
