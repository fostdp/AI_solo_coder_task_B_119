package com.metrology.balance.dto;

import java.util.List;
import java.util.Map;

public class CivilizationComparisonResult {

    private List<String> civilizationNames;
    private List<String> civilizationCodes;
    private List<String> dimensions;
    private Map<String, List<Double>> radarData;
    private Map<String, CivilizationSummary> summaries;
    private String overallWinner;
    private List<String> comparativeAnalysis;

    public static class CivilizationSummary {
        private String name;
        private String code;
        private Integer periodStart;
        private Integer periodEnd;
        private String balanceType;
        private Double avgScore;
        private Map<String, Double> scores;
        private List<String> strengths;
        private List<String> weaknesses;
        private String culturalSignificance;
        private String representativeArtifact;
        private Map<String, Object> standardizationInfo;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public Integer getPeriodStart() {
            return periodStart;
        }

        public void setPeriodStart(Integer periodStart) {
            this.periodStart = periodStart;
        }

        public Integer getPeriodEnd() {
            return periodEnd;
        }

        public void setPeriodEnd(Integer periodEnd) {
            this.periodEnd = periodEnd;
        }

        public String getBalanceType() {
            return balanceType;
        }

        public void setBalanceType(String balanceType) {
            this.balanceType = balanceType;
        }

        public Double getAvgScore() {
            return avgScore;
        }

        public void setAvgScore(Double avgScore) {
            this.avgScore = avgScore;
        }

        public Map<String, Double> getScores() {
            return scores;
        }

        public void setScores(Map<String, Double> scores) {
            this.scores = scores;
        }

        public List<String> getStrengths() {
            return strengths;
        }

        public void setStrengths(List<String> strengths) {
            this.strengths = strengths;
        }

        public List<String> getWeaknesses() {
            return weaknesses;
        }

        public void setWeaknesses(List<String> weaknesses) {
            this.weaknesses = weaknesses;
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

        public Map<String, Object> getStandardizationInfo() {
            return standardizationInfo;
        }

        public void setStandardizationInfo(Map<String, Object> standardizationInfo) {
            this.standardizationInfo = standardizationInfo;
        }
    }

    public List<String> getCivilizationNames() {
        return civilizationNames;
    }

    public void setCivilizationNames(List<String> civilizationNames) {
        this.civilizationNames = civilizationNames;
    }

    public List<String> getCivilizationCodes() {
        return civilizationCodes;
    }

    public void setCivilizationCodes(List<String> civilizationCodes) {
        this.civilizationCodes = civilizationCodes;
    }

    public List<String> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<String> dimensions) {
        this.dimensions = dimensions;
    }

    public Map<String, List<Double>> getRadarData() {
        return radarData;
    }

    public void setRadarData(Map<String, List<Double>> radarData) {
        this.radarData = radarData;
    }

    public Map<String, CivilizationSummary> getSummaries() {
        return summaries;
    }

    public void setSummaries(Map<String, CivilizationSummary> summaries) {
        this.summaries = summaries;
    }

    public String getOverallWinner() {
        return overallWinner;
    }

    public void setOverallWinner(String overallWinner) {
        this.overallWinner = overallWinner;
    }

    public List<String> getComparativeAnalysis() {
        return comparativeAnalysis;
    }

    public void setComparativeAnalysis(List<String> comparativeAnalysis) {
        this.comparativeAnalysis = comparativeAnalysis;
    }
}
