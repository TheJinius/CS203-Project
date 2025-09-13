package com.ubs.tariffapp.services;

import org.springframework.stereotype.Service;
import com.ubs.tariffapp.repositories.DutyRepository;
import com.ubs.tariffapp.models.duty.AdValoremDuty;
import com.ubs.tariffapp.models.duty.CombinedDuty;
import com.ubs.tariffapp.models.duty.Duty;
import com.ubs.tariffapp.models.duty.SpecificDuty;
import java.math.BigDecimal;
import java.math.RoundingMode;


@Service
public class DutyService {

    private final DutyRepository dutyRepository;

    public DutyService(DutyRepository dutyRepository) {
        this.dutyRepository = dutyRepository;
    }

    
    public Duty getDuty(String importerCountry, String exporterCountry, String productCode) {
        return dutyRepository.findDutyByCountriesAndProduct(importerCountry, exporterCountry, productCode)
                .orElse(null); // or throw an exception if not found
    }

    // DutyController calls this to calculate
    public double calculateTariff(String importerCountry, String exporterCountry, String product, double amountOfProduct) {
        Duty duty = getDuty(importerCountry, exporterCountry, product);
        
        if (duty == null) {
            throw new RuntimeException("No duty found for the given countries and product.");
        }
        
        double tariffAmount = 0.0;
        
        if (duty instanceof AdValoremDuty) {
            AdValoremDuty adValoremDuty = (AdValoremDuty) duty;
            tariffAmount = amountOfProduct * adValoremDuty.getRatePercent().doubleValue() / 100.0;
        } else if (duty instanceof SpecificDuty) {
            SpecificDuty specificDuty = (SpecificDuty) duty;
            tariffAmount = (amountOfProduct / specificDuty.getMultiplier().doubleValue()) * specificDuty.getAmount().doubleValue();
        } else if (duty instanceof CombinedDuty) {
            CombinedDuty combinedDuty = (CombinedDuty) duty;
            double adValorem = amountOfProduct * combinedDuty.getRatePercent().doubleValue() / 100.0;
            double specific = (amountOfProduct / combinedDuty.getMultiplier().doubleValue()) * combinedDuty.getAmount().doubleValue();
            
            if (combinedDuty.getMixedOrConditional().equals("M")) {
                // Mixed: sum of ad valorem and specific duty
                tariffAmount = adValorem + specific;
            } else if (combinedDuty.getMixedOrConditional().equals("C")) {
                // Conditional: choose the higher of ad valorem or specific duty
                tariffAmount = Math.max(adValorem, specific);
            } else {
                throw new RuntimeException("Invalid mixed/conditional type for combined duty.");
            }
        } else {
            throw new RuntimeException("Unknown duty type.");
        }

        // Round to 2 decimal places using banker's rounding (HALF_EVEN)
        return BigDecimal.valueOf(tariffAmount)
                .setScale(2, RoundingMode.HALF_EVEN)
                .doubleValue();
    }
}
