package com.example.sd009thuan.dto;

import java.util.List;

public class SoSanhRequestDTO {
    private String compareMode;
    private Integer year;
    private Integer startYear;
    private Integer endYear;
    private Long brandId;
    private String productKeyword;
    private String fromDateA;
    private String toDateA;
    private String fromDateB;
    private String toDateB;
    private List<PeriodRequestDTO> periods;

    public SoSanhRequestDTO() {
    }

    public String getCompareMode() {
        return compareMode;
    }

    public void setCompareMode(String compareMode) {
        this.compareMode = compareMode;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getStartYear() {
        return startYear;
    }

    public void setStartYear(Integer startYear) {
        this.startYear = startYear;
    }

    public Integer getEndYear() {
        return endYear;
    }

    public void setEndYear(Integer endYear) {
        this.endYear = endYear;
    }

    public Long getBrandId() {
        return brandId;
    }

    public void setBrandId(Long brandId) {
        this.brandId = brandId;
    }

    public String getProductKeyword() {
        return productKeyword;
    }

    public void setProductKeyword(String productKeyword) {
        this.productKeyword = productKeyword;
    }

    public String getFromDateA() {
        return fromDateA;
    }

    public void setFromDateA(String fromDateA) {
        this.fromDateA = fromDateA;
    }

    public String getToDateA() {
        return toDateA;
    }

    public void setToDateA(String toDateA) {
        this.toDateA = toDateA;
    }

    public String getFromDateB() {
        return fromDateB;
    }

    public void setFromDateB(String fromDateB) {
        this.fromDateB = fromDateB;
    }

    public String getToDateB() {
        return toDateB;
    }

    public void setToDateB(String toDateB) {
        this.toDateB = toDateB;
    }

    public List<PeriodRequestDTO> getPeriods() {
        return periods;
    }

    public void setPeriods(List<PeriodRequestDTO> periods) {
        this.periods = periods;
    }
}

