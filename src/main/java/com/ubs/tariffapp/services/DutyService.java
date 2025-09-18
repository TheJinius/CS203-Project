package com.ubs.tariffapp.services;

import org.springframework.stereotype.Service;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.duty.*;
import com.ubs.tariffapp.services.TariffScheduleService;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DutyService {

    private final TariffScheduleService tariffScheduleService;

    public DutyService(TariffScheduleService tariffScheduleService) {
        this.tariffScheduleService = tariffScheduleService;
    }

    public double calculateTariff(String reporterCode, String partnerCode, String productCode, double amountOfProduct) {
        // Get TariffSchedule first
        TariffSchedule tariffSchedule = tariffScheduleService.getTariffSchedule(reporterCode, partnerCode, productCode);
        
        if (tariffSchedule == null) {
            throw new RuntimeException("No tariff schedule found for the given countries and product.");
        }
        
        // Get Duty directly from the one-to-one relationship
        Duty duty = tariffSchedule.getDuty();
        
        if (duty == null) {
            throw new RuntimeException("No duty found for tariff schedule.");
        }
        
        // Calculate tariff using the existing Duty entity
        return calculateTariffAmount(duty, amountOfProduct);
    }

    private double calculateTariffAmount(Duty duty, double amountOfProduct) {
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
            
            if ("M".equals(combinedDuty.getMixedOrConditional())) {
                tariffAmount = adValorem + specific;
            } else if ("C".equals(combinedDuty.getMixedOrConditional())) {
                tariffAmount = Math.max(adValorem, specific);
            }
        } else {
            // Handle other duty types or default case
            tariffAmount = 0.0;
        }

        return BigDecimal.valueOf(tariffAmount)
                .setScale(2, RoundingMode.HALF_EVEN)
                .doubleValue();
    }
}
