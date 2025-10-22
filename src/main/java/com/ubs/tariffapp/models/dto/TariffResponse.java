package com.ubs.tariffapp.models.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TariffResponse {
    private Integer tariffId;
    private Integer tariffYear;
    
    // Country information
    private String reporterCode;
    private String reporterName;
    private String partnerCode;
    private String partnerName;
    
    // Product information
    private String tlCode;
    private String productDescription;
    
    // Duty type information
    private String dutyType;
    private String dutyCode;
    private String dutyTypeDescription;
    
    private String tlsSuffix;
    private String note;
    
    // Duty details
    private String dutyCategory; // AD_VALOREM, SPECIFIC, COMBINED, etc.
    private Double adValoremRate;
    private Double specificRate;
    private String specificRateUnit;
    private Double compoundRate1;
    private Double compoundRate2;
}
