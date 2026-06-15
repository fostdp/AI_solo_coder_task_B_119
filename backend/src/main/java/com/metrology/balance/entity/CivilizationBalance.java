package com.metrology.balance.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "civilization_balances")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class CivilizationBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "civilization_name", nullable = false, length = 50)
    private String civilizationName;

    @Column(name = "civilization_code", nullable = false, unique = true, length = 20)
    private String civilizationCode;

    @Column(name = "period_start_year")
    private Integer periodStartYear;

    @Column(name = "period_end_year")
    private Integer periodEndYear;

    @Column(name = "balance_type", length = 50)
    private String balanceType;

    @Column(name = "max_capacity", precision = 10, scale = 2)
    private Double maxCapacity;

    @Column(name = "relative_precision", precision = 12, scale = 8)
    private Double relativePrecision;

    @Column(name = "material_hardness", precision = 8, scale = 2)
    private Double materialHardness;

    @Column(name = "arm_ratio_consistency", precision = 5, scale = 2)
    private Double armRatioConsistency;

    @Column(name = "structure_complexity", precision = 5, scale = 2)
    private Double structureComplexity;

    @Column(name = "durability_score", precision = 5, scale = 2)
    private Double durabilityScore;

    @Column(name = "typical_arm_length", precision = 8, scale = 2)
    private Double typicalArmLength;

    @Column(name = "typical_material", length = 50)
    private String typicalMaterial;

    @Column(name = "cultural_significance", columnDefinition = "TEXT")
    private String culturalSignificance;

    @Column(name = "representative_artifact", length = 200)
    private String representativeArtifact;

    @Column(name = "discovery_location", length = 200)
    private String discoveryLocation;

    @Column(name = "reference_source", columnDefinition = "TEXT")
    private String referenceSource;

    @Type(type = "jsonb")
    @Column(name = "radar_data", columnDefinition = "jsonb")
    private Map<String, Object> radarData;

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

    public String getCivilizationName() {
        return civilizationName;
    }

    public void setCivilizationName(String civilizationName) {
        this.civilizationName = civilizationName;
    }

    public String getCivilizationCode() {
        return civilizationCode;
    }

    public void setCivilizationCode(String civilizationCode) {
        this.civilizationCode = civilizationCode;
    }

    public Integer getPeriodStartYear() {
        return periodStartYear;
    }

    public void setPeriodStartYear(Integer periodStartYear) {
        this.periodStartYear = periodStartYear;
    }

    public Integer getPeriodEndYear() {
        return periodEndYear;
    }

    public void setPeriodEndYear(Integer periodEndYear) {
        this.periodEndYear = periodEndYear;
    }

    public String getBalanceType() {
        return balanceType;
    }

    public void setBalanceType(String balanceType) {
        this.balanceType = balanceType;
    }

    public Double getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Double maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Double getRelativePrecision() {
        return relativePrecision;
    }

    public void setRelativePrecision(Double relativePrecision) {
        this.relativePrecision = relativePrecision;
    }

    public Double getMaterialHardness() {
        return materialHardness;
    }

    public void setMaterialHardness(Double materialHardness) {
        this.materialHardness = materialHardness;
    }

    public Double getArmRatioConsistency() {
        return armRatioConsistency;
    }

    public void setArmRatioConsistency(Double armRatioConsistency) {
        this.armRatioConsistency = armRatioConsistency;
    }

    public Double getStructureComplexity() {
        return structureComplexity;
    }

    public void setStructureComplexity(Double structureComplexity) {
        this.structureComplexity = structureComplexity;
    }

    public Double getDurabilityScore() {
        return durabilityScore;
    }

    public void setDurabilityScore(Double durabilityScore) {
        this.durabilityScore = durabilityScore;
    }

    public Double getTypicalArmLength() {
        return typicalArmLength;
    }

    public void setTypicalArmLength(Double typicalArmLength) {
        this.typicalArmLength = typicalArmLength;
    }

    public String getTypicalMaterial() {
        return typicalMaterial;
    }

    public void setTypicalMaterial(String typicalMaterial) {
        this.typicalMaterial = typicalMaterial;
    }

    public String getCulturalSignificance() {
        return culturalSignificance;
    }

    public void setCulturalSignificance(String culturalSignificance) {
        this.culturalSignificance = culturalSignificance;
    }

    public String getRepresentativeArtifact() {
        return representativeArtifact;
    }

    public void setRepresentativeArtifact(String representativeArtifact) {
        this.representativeArtifact = representativeArtifact;
    }

    public String getDiscoveryLocation() {
        return discoveryLocation;
    }

    public void setDiscoveryLocation(String discoveryLocation) {
        this.discoveryLocation = discoveryLocation;
    }

    public String getReferenceSource() {
        return referenceSource;
    }

    public void setReferenceSource(String referenceSource) {
        this.referenceSource = referenceSource;
    }

    public Map<String, Object> getRadarData() {
        return radarData;
    }

    public void setRadarData(Map<String, Object> radarData) {
        this.radarData = radarData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
