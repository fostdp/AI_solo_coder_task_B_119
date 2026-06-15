package com.metrology.balance.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "virtual_weighing_items")
public class VirtualWeighingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "item_code", nullable = false, unique = true, length = 50)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "era", length = 50)
    private String era;

    @Column(name = "civilization", length = 50)
    private String civilization;

    @Column(name = "nominal_mass", precision = 10, scale = 4)
    private Double nominalMass;

    @Column(name = "actual_mass", precision = 10, scale = 6)
    private Double actualMass;

    @Column(name = "volume_cm3", precision = 8, scale = 2)
    private Double volumeCm3;

    @Column(name = "material", length = 50)
    private String material;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "shape", length = 20)
    private String shape;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "historical_significance", columnDefinition = "TEXT")
    private String historicalSignificance;

    @Column(name = "rarity", length = 20)
    private String rarity;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

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

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getEra() {
        return era;
    }

    public void setEra(String era) {
        this.era = era;
    }

    public String getCivilization() {
        return civilization;
    }

    public void setCivilization(String civilization) {
        this.civilization = civilization;
    }

    public Double getNominalMass() {
        return nominalMass;
    }

    public void setNominalMass(Double nominalMass) {
        this.nominalMass = nominalMass;
    }

    public Double getActualMass() {
        return actualMass;
    }

    public void setActualMass(Double actualMass) {
        this.actualMass = actualMass;
    }

    public Double getVolumeCm3() {
        return volumeCm3;
    }

    public void setVolumeCm3(Double volumeCm3) {
        this.volumeCm3 = volumeCm3;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getHistoricalSignificance() {
        return historicalSignificance;
    }

    public void setHistoricalSignificance(String historicalSignificance) {
        this.historicalSignificance = historicalSignificance;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
