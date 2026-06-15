package com.metrology.balance.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "manufacturing_analyses")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class ManufacturingAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "balance_id", nullable = false)
    private Integer balanceId;

    @Column(name = "analysis_time", nullable = false)
    private LocalDateTime analysisTime;

    @Column(name = "knife_edge_geometry_score", precision = 5, scale = 2)
    private Double knifeEdgeGeometryScore;

    @Column(name = "surface_roughness_score", precision = 5, scale = 2)
    private Double surfaceRoughnessScore;

    @Column(name = "material_quality_score", precision = 5, scale = 2)
    private Double materialQualityScore;

    @Column(name = "assembly_precision_score", precision = 5, scale = 2)
    private Double assemblyPrecisionScore;

    @Column(name = "overall_technology_grade", length = 20)
    private String overallTechnologyGrade;

    @Column(name = "estimated_manufacturing_era", length = 50)
    private String estimatedManufacturingEra;

    @Column(name = "inferred_craft_method", length = 100)
    private String inferredCraftMethod;

    @Column(name = "geometry_tolerance_microm", precision = 10, scale = 4)
    private Double geometryToleranceMicrom;

    @Column(name = "inferred_surface_roughness_ra", precision = 10, scale = 4)
    private Double inferredSurfaceRoughnessRa;

    @Column(name = "material_homogeneity", precision = 5, scale = 2)
    private Double materialHomogeneity;

    @Column(name = "arm_length_ratio_error", precision = 10, scale = 6)
    private Double armLengthRatioError;

    @Type(type = "jsonb")
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private Map<String, Object> rawData;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (analysisTime == null) {
            analysisTime = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getBalanceId() {
        return balanceId;
    }

    public void setBalanceId(Integer balanceId) {
        this.balanceId = balanceId;
    }

    public LocalDateTime getAnalysisTime() {
        return analysisTime;
    }

    public void setAnalysisTime(LocalDateTime analysisTime) {
        this.analysisTime = analysisTime;
    }

    public Double getKnifeEdgeGeometryScore() {
        return knifeEdgeGeometryScore;
    }

    public void setKnifeEdgeGeometryScore(Double knifeEdgeGeometryScore) {
        this.knifeEdgeGeometryScore = knifeEdgeGeometryScore;
    }

    public Double getSurfaceRoughnessScore() {
        return surfaceRoughnessScore;
    }

    public void setSurfaceRoughnessScore(Double surfaceRoughnessScore) {
        this.surfaceRoughnessScore = surfaceRoughnessScore;
    }

    public Double getMaterialQualityScore() {
        return materialQualityScore;
    }

    public void setMaterialQualityScore(Double materialQualityScore) {
        this.materialQualityScore = materialQualityScore;
    }

    public Double getAssemblyPrecisionScore() {
        return assemblyPrecisionScore;
    }

    public void setAssemblyPrecisionScore(Double assemblyPrecisionScore) {
        this.assemblyPrecisionScore = assemblyPrecisionScore;
    }

    public String getOverallTechnologyGrade() {
        return overallTechnologyGrade;
    }

    public void setOverallTechnologyGrade(String overallTechnologyGrade) {
        this.overallTechnologyGrade = overallTechnologyGrade;
    }

    public String getEstimatedManufacturingEra() {
        return estimatedManufacturingEra;
    }

    public void setEstimatedManufacturingEra(String estimatedManufacturingEra) {
        this.estimatedManufacturingEra = estimatedManufacturingEra;
    }

    public String getInferredCraftMethod() {
        return inferredCraftMethod;
    }

    public void setInferredCraftMethod(String inferredCraftMethod) {
        this.inferredCraftMethod = inferredCraftMethod;
    }

    public Double getGeometryToleranceMicrom() {
        return geometryToleranceMicrom;
    }

    public void setGeometryToleranceMicrom(Double geometryToleranceMicrom) {
        this.geometryToleranceMicrom = geometryToleranceMicrom;
    }

    public Double getInferredSurfaceRoughnessRa() {
        return inferredSurfaceRoughnessRa;
    }

    public void setInferredSurfaceRoughnessRa(Double inferredSurfaceRoughnessRa) {
        this.inferredSurfaceRoughnessRa = inferredSurfaceRoughnessRa;
    }

    public Double getMaterialHomogeneity() {
        return materialHomogeneity;
    }

    public void setMaterialHomogeneity(Double materialHomogeneity) {
        this.materialHomogeneity = materialHomogeneity;
    }

    public Double getArmLengthRatioError() {
        return armLengthRatioError;
    }

    public void setArmLengthRatioError(Double armLengthRatioError) {
        this.armLengthRatioError = armLengthRatioError;
    }

    public Map<String, Object> getRawData() {
        return rawData;
    }

    public void setRawData(Map<String, Object> rawData) {
        this.rawData = rawData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
