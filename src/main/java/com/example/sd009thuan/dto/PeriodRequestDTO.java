package com.example.sd009thuan.dto;

public class PeriodRequestDTO {
    private String key;
    private String name;
    private String startDate; // "YYYY-MM-DD"
    private String endDate;   // "YYYY-MM-DD"

    public PeriodRequestDTO() {
    }

    public PeriodRequestDTO(String key, String name, String startDate, String endDate) {
        this.key = key;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
}

