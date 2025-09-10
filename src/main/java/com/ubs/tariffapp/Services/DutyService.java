package com.ubs.tariffapp.Services;

import org.springframework.stereotype.Service;
import com.ubs.tariffapp.repositories.DutyRepository;
import com.ubs.tariffapp.models.AdValoremDuty;
import com.ubs.tariffapp.models.CombinedDuty;
import com.ubs.tariffapp.models.Duty;
import com.ubs.tariffapp.models.SpecificDuty;


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
    public String calculateTariff(String importerCountry, String exporterCountry, String product) {
        Duty duty = getDuty(importerCountry, exporterCountry, product);
        // Add your calculation logic here using the Duty object
        if (duty == null) {
            return "No applicable duty found."; // or should i return null or throw exception?
        }
        
        if (duty instanceof AdValoremDuty) {
            AdValoremDuty adValoremDuty = (AdValoremDuty) duty;
            return "Ad Valorem Duty Rate: " + adValoremDuty.getRatePercent() + "%";
        } else if (duty instanceof SpecificDuty) {
            SpecificDuty specificDuty = (SpecificDuty) duty;
            return "Specific Duty Amount: " + specificDuty.getAmount() + " per " + specificDuty.getMultiplier() + " " + specificDuty.getUnit();
            
        } else if (duty instanceof CombinedDuty) {
            CombinedDuty combinedDuty = (CombinedDuty) duty;
            if (combinedDuty.getMixedOrConditional().equals("M")) {
                return "Combined Duty (Mixed): " +
                        "Ad Valorem Rate: " + combinedDuty.getRatePercent() + "%, " +
                        "Specific Amount: " + combinedDuty.getAmount() + " per " + combinedDuty.getMultiplier() + " " + combinedDuty.getUnit();
            } else if (combinedDuty.getMixedOrConditional().equals("C")) {
                return "Combined Duty (Conditional): " +
                        "Ad Valorem Rate: " + combinedDuty.getRatePercent() + "%, " +
                        "Specific Amount: " + combinedDuty.getAmount() + " per " + combinedDuty.getMultiplier() + " " + combinedDuty.getUnit();
                
            }
            
        }

        return null;
    }
}
