package com.ubs.tariffapp.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ubs.tariffapp.exceptions.DutyNotFoundException;
import com.ubs.tariffapp.exceptions.InvalidRequestException;
import com.ubs.tariffapp.exceptions.TariffNotFoundException;
import com.ubs.tariffapp.models.ExchangeRates;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.duty.AdValoremDuty;
import com.ubs.tariffapp.models.duty.CombinedDuty;
import com.ubs.tariffapp.models.duty.Duty;
import com.ubs.tariffapp.models.duty.SpecificDuty;
import com.ubs.tariffapp.models.request.TariffCalculationRequest;
import com.ubs.tariffapp.models.request.TariffSearchRequest;
import com.ubs.tariffapp.services.DutyService;
import com.ubs.tariffapp.services.ExchangeRateService;
import com.ubs.tariffapp.services.TariffScheduleService;

@RestController
@RequestMapping("/api/tariffs")
public class DutyController {

    private final DutyService dutyService;
    private final TariffScheduleService tariffScheduleService;
    private final ExchangeRateService exchangeRateService;

    public DutyController(DutyService dutyService, TariffScheduleService tariffScheduleService, ExchangeRateService exchangeRateService) {
        this.dutyService = dutyService;
        this.tariffScheduleService = tariffScheduleService;
        this.exchangeRateService = exchangeRateService;
    }

    // NEW ENDPOINT 1: Search for available tariffs
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchTariffs(@RequestBody TariffSearchRequest request) {
        System.out.println("🔍 Searching tariffs:");
        System.out.println("ReporterCode = " + request.getReporterCode());
        System.out.println("PartnerCode = " + request.getPartnerCode());
        System.out.println("ProductCode = " + request.getProductCode());
        System.out.println("Year = " + request.getYear());

        try {
            // Use real DutyService method instead of fake data
            List<TariffSchedule> tariffs = dutyService.searchAvailableTariffs(
                request.getReporterCode(),
                request.getPartnerCode(),
                request.getProductCode(),
                request.getYear()
            );
            
            // Convert TariffSchedule entities to response format
            List<Map<String, Object>> tariffList = tariffs.stream()
                    .map(ts -> {
                        Map<String, Object> tariffMap = new HashMap<>();
                        tariffMap.put("tariffId", ts.getTariffId());
                        tariffMap.put("description", buildTariffDescription(ts));
                        tariffMap.put("dutyType", ts.getDutyType() != null ? ts.getDutyType().getDutyTypeDescription() : "Unknown");
                        tariffMap.put("tlsSuffix", ts.getTlsSuffix() != null ? ts.getTlsSuffix() : "");
                        tariffMap.put("year", ts.getTariffYear());
                        
                        // Add additional useful information
                        if (ts.getProduct() != null) {
                            tariffMap.put("productCode", ts.getProduct().getTlCode());
                            tariffMap.put("productDescription", ts.getProduct().getDescription());
                        }
                        
                        return tariffMap;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("tariffs", tariffList);
            response.put("count", tariffs.size());
            response.put("year", request.getYear());
            response.put("status", "success");
            
            System.out.println("✅ Found " + tariffs.size() + " tariff(s)");
            return ResponseEntity.ok(response);
            
        } catch (TariffNotFoundException e) {
            System.err.println("❌ TariffNotFoundException: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "not_found");
            errorResponse.put("tariffs", new ArrayList<>());
            errorResponse.put("count", 0);
            return ResponseEntity.status(404).body(errorResponse);
            
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error: " + e.getMessage());
            errorResponse.put("status", "error");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // NEW ENDPOINT 2: Calculate tariff using specific tariff ID
    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateTariff(@RequestBody TariffCalculationRequest request) {
        System.out.println("💰 Calculating tariff:");
        System.out.println("TariffId = " + request.getTariffId());
        System.out.println("AmountofProduct = " + request.getAmountOfProduct());
        String requestedCurrency = request.getCurrency() != null ? request.getCurrency() : "USD";

        try {
            // Use real DutyService calculation instead of fake calculation
            double tariffAmount = dutyService.calculateTariffById(
                request.getTariffId(),
                request.getAmountOfProduct()
            );
            
            System.out.println("🧮 Base tariff amount (USD): " + tariffAmount);

            // Fetch exchange rates using ExchangeRateService
            ExchangeRates rates = exchangeRateService.fetchRates();
            double rate = 1.0;
            if (rates != null && rates.getRates() != null) {
                //if input rate is not in usd, we convert to usd first
                if (!requestedCurrency.equalsIgnoreCase("USD")) {
                    Double otherRate = rates.getRates().get(requestedCurrency);
                    if (otherRate != null && otherRate != 0) {
                        rate = 1.0 * otherRate;
                    }
                }
            }

            //always return rate in usd
            double convertedAmount = tariffAmount * rate;

            Map<String, Object> response = new HashMap<>();
            response.put("tariffAmount", convertedAmount);
            response.put("currency", requestedCurrency);
            response.put("tariffId", request.getTariffId());
            response.put("calculationDetails", getCalculationDetails(request.getTariffId(), request.getAmountOfProduct()));
            response.put("status", "success");

            System.out.println("✅ SUCCESS: Calculated tariff = " + convertedAmount + " " + requestedCurrency);
            return ResponseEntity.ok(response);

        } catch (TariffNotFoundException e) {
            System.err.println("❌ TariffNotFoundException: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "tariff_not_found");
            return ResponseEntity.status(404).body(errorResponse);

        } catch (DutyNotFoundException e) {
            System.err.println("❌ DutyNotFoundException: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "duty_not_found");
            return ResponseEntity.status(404).body(errorResponse);

        } catch (InvalidRequestException e) {
            System.err.println("❌ InvalidRequestException: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "bad_request");
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error: " + e.getMessage());
            errorResponse.put("status", "internal_error");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Helper method to build user-friendly tariff description
    private String buildTariffDescription(TariffSchedule ts) {
        StringBuilder desc = new StringBuilder();

        if (ts.getProduct() != null) {
            desc.append(ts.getProduct().getDescription() != null ? 
                ts.getProduct().getDescription() : ts.getProduct().getTlCode());
        }

        if (ts.getTlsSuffix() != null && !ts.getTlsSuffix().isEmpty()) {
            desc.append(" (").append(ts.getTlsSuffix()).append(")");
        }

        if (ts.getDutyType() != null) {
            desc.append(" - ").append(ts.getDutyType().getDutyTypeDescription());
        }

        return desc.toString();
    }

    // Helper method to get calculation details for a specific tariff
    private Map<String, Object> getCalculationDetails(Integer tariffId, double amountOfProduct) {
        Map<String, Object> details = new HashMap<>();
        
        try {
            TariffSchedule tariff = tariffScheduleService.getTariffScheduleById(tariffId);
            if (tariff != null && tariff.getDuty() != null) {
                Duty duty = tariff.getDuty();
                String dutyType = tariff.getDutyType().getDutyTypeDescription();
                
                details.put("dutyType", dutyType);
                details.put("productDescription", tariff.getProduct().getDescription());
                details.put("productCode", tariff.getProduct().getTlCode());
                
                // Add specific calculation formulas based on duty type
                if (duty instanceof AdValoremDuty) {
                    AdValoremDuty adValoremDuty = (AdValoremDuty) duty;
                    double rate = adValoremDuty.getRatePercent().doubleValue();
                    details.put("formula", "Tariff = Product Value × Rate");
                    details.put("calculation", String.format("$%.2f × %.2f%% = $%.2f", 
                        amountOfProduct, rate, amountOfProduct * rate / 100.0));
                    details.put("rate", rate + "%");
                    
                } else if (duty instanceof SpecificDuty) {
                    SpecificDuty specificDuty = (SpecificDuty) duty;
                    double amount = specificDuty.getAmount().doubleValue();
                    double multiplier = specificDuty.getMultiplier().doubleValue();
                    details.put("formula", "Tariff = (Product Value / Multiplier) × Amount per Unit");
                    details.put("calculation", String.format("($%.2f / %.2f) × $%.2f = $%.2f",
                        amountOfProduct, multiplier, amount, 
                        (amountOfProduct / multiplier) * amount));
                    details.put("amountPerUnit", "$" + amount);
                    details.put("multiplier", String.valueOf(multiplier));
                    
                } else if (duty instanceof CombinedDuty) {
                    CombinedDuty combinedDuty = (CombinedDuty) duty;
                    double rate = combinedDuty.getRatePercent().doubleValue();
                    double amount = combinedDuty.getAmount().doubleValue();
                    double multiplier = combinedDuty.getMultiplier().doubleValue();
                    String mixedOrConditional = combinedDuty.getMixedOrConditional();
                    
                    double adValorem = amountOfProduct * rate / 100.0;
                    double specific = (amountOfProduct / multiplier) * amount;
                    
                    if ("M".equals(mixedOrConditional)) {
                        details.put("formula", "Tariff = Ad Valorem + Specific Duty");
                        details.put("calculation", String.format("($%.2f × %.2f%%) + (($%.2f / %.2f) × $%.2f) = $%.2f + $%.2f = $%.2f",
                            amountOfProduct, rate, amountOfProduct, multiplier, amount,
                            adValorem, specific, adValorem + specific));
                        details.put("combinationType", "Mixed (Sum of both)");
                    } else if ("C".equals(mixedOrConditional)) {
                        details.put("formula", "Tariff = MAX(Ad Valorem, Specific Duty)");
                        details.put("calculation", String.format("MAX(($%.2f × %.2f%%), (($%.2f / %.2f) × $%.2f)) = MAX($%.2f, $%.2f) = $%.2f",
                            amountOfProduct, rate, amountOfProduct, multiplier, amount,
                            adValorem, specific, Math.max(adValorem, specific)));
                        details.put("combinationType", "Conditional (Maximum of both)");
                    }
                    details.put("rate", rate + "%");
                    details.put("amountPerUnit", "$" + amount);
                    details.put("multiplier", String.valueOf(multiplier));
                }
            }
        } catch (Exception e) {
            System.err.println("Could not get calculation details: " + e.getMessage());
            details.put("error", "Could not retrieve calculation details");
        }
        
        return details;
    }
}
