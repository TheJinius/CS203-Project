package com.ubs.tariffapp.services;

import org.springframework.stereotype.Service;

import com.ubs.tariffapp.exceptions.DutyNotFoundException;
import com.ubs.tariffapp.exceptions.InvalidRequestException;
import com.ubs.tariffapp.exceptions.TariffNotFoundException;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.duty.*;
import com.ubs.tariffapp.utils.DutyCalculationUtils;
import java.util.List;

@Service
public class DutyService {

    private final TariffScheduleService tariffScheduleService;

    public DutyService(TariffScheduleService tariffScheduleService) {
        this.tariffScheduleService = tariffScheduleService;
    }

    // NEW METHOD 1: Search for available tariffs
    public List<TariffSchedule> searchAvailableTariffs(String reporterCode, String partnerCode, String productCode, int year) {
        System.out.println("🔍 Searching for available tariffs...");
        List<TariffSchedule> tariffs = tariffScheduleService.searchTariffSchedules(reporterCode, partnerCode, productCode, year);
        
        if (tariffs.isEmpty()) {
            throw new TariffNotFoundException("No tariff schedules found for Reporter: " + reporterCode + 
                                            ", Partner: " + partnerCode + ", Product: " + productCode + ", Year: " + year);
        }
        
        System.out.println("Tariff found!");
        System.out.println(tariffs.toString());
        return tariffs;
    }

    // NEW METHOD 2: Calculate tariff using specific tariff ID
    public double calculateTariffById(Integer tariffId, double amountOfProduct) {
        
        if (amountOfProduct <= 0) {
            throw new InvalidRequestException("Amount of product must be greater than 0");
        }
        
        if (tariffId == null) {
            throw new InvalidRequestException("Tariff ID is required");
        }
        
        TariffSchedule tariffSchedule = tariffScheduleService.getTariffScheduleById(tariffId);
        
        if (tariffSchedule == null) {
            throw new TariffNotFoundException("No tariff schedule found for ID: " + tariffId);
        }
        
        // Get Duty directly from the one-to-one relationship
        Duty duty = tariffSchedule.getDuty();
        
        if (duty == null) {
            throw new DutyNotFoundException("No duty information found for TariffSchedule ID: " + tariffSchedule.getTariffId());
        }
        
        // Calculate tariff using the existing Duty entity
        return calculateTariffAmount(duty, amountOfProduct);
    }

    // Overloaded method for Combined Duty with separate product value and quantity
    public double calculateTariffById(Integer tariffId, double amountOfProduct, Double productValueDollars) {
        
        if (amountOfProduct <= 0) {
            throw new InvalidRequestException("Amount of product must be greater than 0");
        }
        
        if (tariffId == null) {
            throw new InvalidRequestException("Tariff ID is required");
        }
        
        TariffSchedule tariffSchedule = tariffScheduleService.getTariffScheduleById(tariffId);
        
        if (tariffSchedule == null) {
            throw new TariffNotFoundException("No tariff schedule found for ID: " + tariffId);
        }
        
        // Get Duty directly from the one-to-one relationship
        Duty duty = tariffSchedule.getDuty();
        
        if (duty == null) {
            throw new DutyNotFoundException("No duty information found for TariffSchedule ID: " + tariffSchedule.getTariffId());
        }
        
        // Calculate tariff - use productValueDollars if provided and it's Combined Duty
        if (duty instanceof CombinedDuty && productValueDollars != null) {
            return calculateTariffAmountForCombined((CombinedDuty) duty, productValueDollars, amountOfProduct);
        }
        
        return calculateTariffAmount(duty, amountOfProduct);
    }

    // Keep old methods for backward compatibility
    public double calculateTariff(String reporterCode, String partnerCode, String productCode, double amountOfProduct, int year) {
        
        if (amountOfProduct <= 0) {
            throw new InvalidRequestException("Amount of product must be greater than 0");
        }
        
        TariffSchedule tariffSchedule = tariffScheduleService.getTariffSchedule(reporterCode, partnerCode, productCode, year);
        
        if (tariffSchedule == null) {
            throw new TariffNotFoundException("No tariff schedule found for Reporter: " + reporterCode + 
                                            ", Partner: " + partnerCode + ", Product: " + productCode + ", Year: " + year);
        }
        
        Duty duty = tariffSchedule.getDuty();
        
        if (duty == null) {
            throw new DutyNotFoundException("No duty information found for TariffSchedule ID: " + tariffSchedule.getTariffId());
        }
        
        return calculateTariffAmount(duty, amountOfProduct);
    }

    public double calculateTariff(String reporterCode, String partnerCode, String productCode, double amountOfProduct) {
        return calculateTariff(reporterCode, partnerCode, productCode, amountOfProduct, 2023);
    }

    private double calculateTariffAmount(Duty duty, double amountOfProduct) {
        // Your existing calculation logic stays the same
        double tariffAmount = 0.0;
        
        if (duty instanceof AdValoremDuty) {
            AdValoremDuty adValoremDuty = (AdValoremDuty) duty;
            tariffAmount = DutyCalculationUtils.calculateAdValoremAmount(
                amountOfProduct, 
                adValoremDuty.getRatePercent().doubleValue()
            );
            
        } else if (duty instanceof SpecificDuty) {
            SpecificDuty specificDuty = (SpecificDuty) duty;
            tariffAmount = DutyCalculationUtils.calculateSpecificAmount(
                amountOfProduct,
                specificDuty.getMultiplier().doubleValue(),
                specificDuty.getAmount().doubleValue()
            );
            
        } else if (duty instanceof CombinedDuty) {
            // LEGACY: Single value used for both Ad Valorem and Specific calculations
            // This has semantic ambiguity and is inherently wrong - prefer using calculateTariffById with separate productValueDollars
            System.out.println("WARNING: Using legacy Combined Duty calculation with single value (semantically ambiguous)");
            
            CombinedDuty combinedDuty = (CombinedDuty) duty;
            double adValorem = DutyCalculationUtils.calculateAdValoremAmount(
                amountOfProduct,
                combinedDuty.getRatePercent().doubleValue()
            );
            double specific = DutyCalculationUtils.calculateSpecificAmount(
                amountOfProduct,
                combinedDuty.getMultiplier().doubleValue(),
                combinedDuty.getAmount().doubleValue()
            );
            
            tariffAmount = DutyCalculationUtils.calculateCombinedDutyResult(combinedDuty.getMixedOrCompound(), adValorem, specific);
        } else {
            tariffAmount = 0.0;
        }

        return DutyCalculationUtils.roundToTwoDecimals(tariffAmount);
    }

    // Calculate tariff for Combined Duty with separate product value and quantity
    private double calculateTariffAmountForCombined(CombinedDuty combinedDuty, double productValueDollars, double productQuantity) {
        // Ad Valorem component uses dollar value
        double adValorem = DutyCalculationUtils.calculateAdValoremAmount(
            productValueDollars,
            combinedDuty.getRatePercent().doubleValue()
        );
        
        // Specific component uses quantity in units
        double specific = DutyCalculationUtils.calculateSpecificAmount(
            productQuantity,
            combinedDuty.getMultiplier().doubleValue(),
            combinedDuty.getAmount().doubleValue()
        );
        
        double tariffAmount = DutyCalculationUtils.calculateCombinedDutyResult(combinedDuty.getMixedOrCompound(), adValorem, specific);

        return DutyCalculationUtils.roundToTwoDecimals(tariffAmount);
    }

    public double calculateTariff(TariffSchedule tariffSchedule, double amountOfProduct) {
        if (amountOfProduct <= 0) {
            throw new InvalidRequestException("Amount of product must be greater than 0");
        }
        
        if (tariffSchedule == null) {
            throw new TariffNotFoundException("TariffSchedule is null");
        }
        
        Duty duty = tariffSchedule.getDuty();
        
        if (duty == null) {
            throw new DutyNotFoundException("No duty information found for TariffSchedule ID: " + tariffSchedule.getTariffId());
        }
        
        return calculateTariffAmount(duty, amountOfProduct);
    }
}