package com.ubs.tariffapp.models.dto;

public class TariffSearchResult {
    private Integer tariffId;
    private String dutyTypeDescription;
    private String tariffLineSuffix;
    private String dutyCode;
    private String dutyType;
    private Integer year;
    
    // Constructor for JPQL query
    public TariffSearchResult(Integer tariffId, String dutyTypeDescription, String tariffLineSuffix,
                             String dutyCode, String dutyType, Integer year) {
        this.tariffId = tariffId;
        this.dutyTypeDescription = dutyTypeDescription;
        this.tariffLineSuffix = tariffLineSuffix;
        this.dutyCode = dutyCode;
        this.dutyType = dutyType;
        this.year = year;
    }

    public Integer getTariffId() { return tariffId; }
    public String getDutyTypeDescription() { return dutyTypeDescription; }
    public String getTariffLineSuffix() { return tariffLineSuffix; }
    public String getDutyCode() { return dutyCode; }
    public String getDutyType() { return dutyType; }
    public Integer getYear() { return year; }
}
