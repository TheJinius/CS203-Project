package com.ubs.tariffapp.models.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TariffRequest {
    
    @NotNull(message = "Tariff year is required")
    @Min(value = 2000, message = "Tariff year must be 2000 or later")
    @Max(value = 2100, message = "Tariff year must be before 2100")
    private Integer tariffYear;

    @NotBlank(message = "Reporter country code is required")
    @Size(min = 3, max = 3, message = "Reporter country code must be 3 characters")
    private String reporterCode;

    @NotBlank(message = "Partner country code is required")
    @Size(min = 3, max = 3, message = "Partner country code must be 3 characters")
    private String partnerCode;

    @NotBlank(message = "Product TL code is required")
    private String tlCode;

    @NotBlank(message = "Duty type is required")
    private String dutyType;

    @NotBlank(message = "Duty code is required")
    private String dutyCode;

    private String tlsSuffix;

    @Size(max = 1000, message = "Note cannot exceed 1000 characters")
    private String note;

    // Duty details
    private Double adValoremRate;
    private Double specificRate;
    private String specificRateUnit;
    private Double compoundRate1;
    private Double compoundRate2;
}
