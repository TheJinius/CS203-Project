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
                        
                        // Add duty class name and unit information
                        if (ts.getDuty() != null) {
                            Duty duty = ts.getDuty();
                            // Add the Java class name (e.g., "AdValoremDuty", "SpecificDuty", "CombinedDuty")
                            tariffMap.put("dutyClass", duty.getClass().getSimpleName());
                            
                            if (duty instanceof SpecificDuty) {
                                SpecificDuty specificDuty = (SpecificDuty) duty;
                                tariffMap.put("unit", specificDuty.getUnit());
                            } else if (duty instanceof CombinedDuty) {
                                CombinedDuty combinedDuty = (CombinedDuty) duty;
                                tariffMap.put("unit", combinedDuty.getUnit());
                            }
                        }
                        
                        return tariffMap;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("tariffs", tariffList);
            response.put("count", tariffs.size());
            response.put("year", request.getYear());
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (TariffNotFoundException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "not_found");
            errorResponse.put("tariffs", new ArrayList<>());
            errorResponse.put("count", 0);
            return ResponseEntity.status(404).body(errorResponse);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error: " + e.getMessage());
            errorResponse.put("status", "error");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // NEW ENDPOINT 2: Calculate tariff using specific tariff ID
    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateTariff(@RequestBody TariffCalculationRequest request) {
        String requestedCurrency = request.getCurrency() != null ? request.getCurrency() : "USD";

        try {
            // Use real DutyService calculation - handle Combined Duty with separate values
            double tariffAmount;
            if (request.getProductValueDollars() != null) {
                // Combined Duty: use separate product value and quantity
                tariffAmount = dutyService.calculateTariffById(
                    request.getTariffId(),
                    request.getAmountOfProduct(),  // quantity in units for Specific component
                    request.getProductValueDollars() // dollar value for Ad Valorem component
                );
            } else {
                // Other duty types: use single amountOfProduct value
                tariffAmount = dutyService.calculateTariffById(
                    request.getTariffId(),
                    request.getAmountOfProduct()
                );
            }
            
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
            response.put("status", "success");
            
            // Add calculation details directly to response (flattened, not nested)
            Map<String, Object> calcDetails = getCalculationDetails(request.getTariffId(), request.getAmountOfProduct(), request);
            response.putAll(calcDetails); // Merge all calculation details into main response

            return ResponseEntity.ok()
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(response);

        } catch (TariffNotFoundException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "tariff_not_found");
            return ResponseEntity.status(404).body(errorResponse);

        } catch (DutyNotFoundException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "duty_not_found");
            return ResponseEntity.status(404).body(errorResponse);

        } catch (InvalidRequestException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "bad_request");
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
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
    private Map<String, Object> getCalculationDetails(Integer tariffId, double amountOfProduct, TariffCalculationRequest request) {
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
                    double tariffResult = amountOfProduct * rate / 100.0;
                    
                    details.put("dutyTypeCode", "AD_VALOREM");
                    details.put("formula", "Tariff = Product Value × (Ad Valorem Rate / 100)");
                    details.put("rate", rate);
                    details.put("rateDisplay", rate + "%");
                    details.put("productValue", amountOfProduct);
                    details.put("tariffResult", tariffResult);
                    
                    // Step-by-step breakdown
                    List<Map<String, String>> steps = new ArrayList<>();
                    steps.add(Map.of(
                        "step", "1",
                        "description", "Product Value",
                        "value", String.format("$%.2f", amountOfProduct)
                    ));
                    steps.add(Map.of(
                        "step", "2",
                        "description", "Ad Valorem Rate",
                        "value", String.format("%.2f%%", rate)
                    ));
                    steps.add(Map.of(
                        "step", "3",
                        "description", "Calculate Tariff",
                        "value", String.format("$%.2f × %.2f%% = $%.2f", amountOfProduct, rate, tariffResult)
                    ));
                    details.put("steps", steps);
                    details.put("calculation", String.format("$%.2f × (%.2f / 100) = $%.2f", 
                        amountOfProduct, rate, tariffResult));
                    
                } else if (duty instanceof SpecificDuty) {
                    SpecificDuty specificDuty = (SpecificDuty) duty;
                    double amount = specificDuty.getAmount().doubleValue();
                    double multiplier = specificDuty.getMultiplier().doubleValue();
                    String unit = specificDuty.getUnit();
                    double units = amountOfProduct / multiplier;
                    double tariffResult = units * amount;
                    
                    details.put("dutyTypeCode", "SPECIFIC");
                    details.put("formula", "Tariff = (Product Quantity / Multiplier) × Amount per Unit");
                    details.put("amountPerUnit", amount);
                    details.put("amountPerUnitDisplay", "$" + String.format("%.2f", amount));
                    details.put("multiplier", multiplier);
                    details.put("productValue", amountOfProduct);
                    details.put("calculatedUnits", units);
                    details.put("tariffResult", tariffResult);
                    details.put("specificDutyRateRaw", specificDuty.getSpecificDutyRateRaw());
                    
                    // Step-by-step breakdown with clarified terminology
                    List<Map<String, String>> steps = new ArrayList<>();
                    steps.add(Map.of(
                        "step", "1",
                        "description", "Product Quantity (in " + (unit != null ? unit : "units") + ")",
                        "value", String.format("%.2f %s", amountOfProduct, unit != null ? unit : "units")
                    ));
                    steps.add(Map.of(
                        "step", "2",
                        "description", "Multiplier (unit conversion)",
                        "value", String.format("%.2f", multiplier)
                    ));
                    steps.add(Map.of(
                        "step", "3",
                        "description", "Calculate Billing Units",
                        "value", String.format("%.2f %s ÷ %.2f = %.2f billing units", amountOfProduct, unit != null ? unit : "", multiplier, units)
                    ));
                    steps.add(Map.of(
                        "step", "4",
                        "description", "Tariff Rate per Billing Unit",
                        "value", String.format("$%.2f per billing unit", amount)
                    ));
                    steps.add(Map.of(
                        "step", "5",
                        "description", "Calculate Tariff",
                        "value", String.format("%.2f billing units × $%.2f = $%.2f", units, amount, tariffResult)
                    ));
                    details.put("steps", steps);
                    details.put("calculation", String.format("(%.2f %s / %.2f) × $%.2f = %.2f × $%.2f = $%.2f",
                        amountOfProduct, unit != null ? unit : "", multiplier, amount, units, amount, tariffResult));
                    
                } else if (duty instanceof CombinedDuty) {
                    CombinedDuty combinedDuty = (CombinedDuty) duty;
                    double rate = combinedDuty.getRatePercent().doubleValue();
                    double amount = combinedDuty.getAmount().doubleValue();
                    double multiplier = combinedDuty.getMultiplier().doubleValue();
                    String unit = combinedDuty.getUnit();
                    String mixedOrConditional = combinedDuty.getMixedOrCompound();
                    
                    // Check if separate values provided for Combined Duty
                    Double productValueDollars = request.getProductValueDollars();
                    boolean hasSeparateValues = productValueDollars != null;
                    
                    double adValoremAmount, specificAmount, units, tariffResult;
                    
                    if (hasSeparateValues) {
                        // Use separate values: productValueDollars for Ad Valorem, amountOfProduct for Specific
                        adValoremAmount = productValueDollars * rate / 100.0;
                        units = amountOfProduct / multiplier;
                        specificAmount = units * amount;
                    } else {
                        // Legacy: use single value for both (old behavior for backward compatibility)
                        adValoremAmount = amountOfProduct * rate / 100.0;
                        units = amountOfProduct / multiplier;
                        specificAmount = units * amount;
                    }
                    
                    tariffResult = "M".equals(mixedOrConditional) ? 
                        (adValoremAmount + specificAmount) : Math.max(adValoremAmount, specificAmount);
                    
                    details.put("dutyTypeCode", "COMBINED");
                    details.put("rate", rate);
                    details.put("rateDisplay", rate + "%");
                    details.put("amountPerUnit", amount);
                    details.put("amountPerUnitDisplay", "$" + String.format("%.2f", amount));
                    details.put("multiplier", multiplier);
                    
                    if (hasSeparateValues) {
                        details.put("productValueDollars", productValueDollars);
                        details.put("productQuantity", amountOfProduct);
                    } else {
                        details.put("productValue", amountOfProduct);
                    }
                    
                    details.put("adValoremAmount", adValoremAmount);
                    details.put("specificAmount", specificAmount);
                    details.put("calculatedUnits", units);
                    details.put("tariffResult", tariffResult);
                    details.put("specificDutyRateRaw", combinedDuty.getSpecificDutyRateRaw());
                    
                    List<Map<String, String>> steps = new ArrayList<>();
                    
                    if ("M".equals(mixedOrConditional)) {
                        details.put("combinationType", "Mixed (Sum of both)");
                        
                        if (hasSeparateValues) {
                            details.put("formula", "Tariff = Ad Valorem + Specific Duty");
                            
                            steps.add(Map.of(
                                "step", "1a",
                                "description", "Product Value for Ad Valorem",
                                "value", String.format("$%.2f", productValueDollars)
                            ));
                            steps.add(Map.of(
                                "step", "1b",
                                "description", "Product Quantity for Specific Duty",
                                "value", String.format("%.2f %s", amountOfProduct, unit != null ? unit : "units")
                            ));
                            steps.add(Map.of(
                                "step", "2",
                                "description", "Calculate Ad Valorem Component",
                                "value", String.format("$%.2f × %.2f%% = $%.2f", productValueDollars, rate, adValoremAmount)
                            ));
                            steps.add(Map.of(
                                "step", "3",
                                "description", "Calculate Billing Units for Specific",
                                "value", String.format("%.2f %s ÷ %.2f = %.2f billing units", amountOfProduct, unit != null ? unit : "", multiplier, units)
                            ));
                            steps.add(Map.of(
                                "step", "4",
                                "description", "Calculate Specific Component",
                                "value", String.format("%.2f billing units × $%.2f = $%.2f", units, amount, specificAmount)
                            ));
                            steps.add(Map.of(
                                "step", "5",
                                "description", "Sum Both Components",
                                "value", String.format("$%.2f + $%.2f = $%.2f", adValoremAmount, specificAmount, tariffResult)
                            ));
                            
                            details.put("calculation", String.format("($%.2f × %.2f%%) + ((%.2f %s / %.2f) × $%.2f) = $%.2f + $%.2f = $%.2f",
                                productValueDollars, rate, amountOfProduct, unit != null ? unit : "", multiplier, amount,
                                adValoremAmount, specificAmount, tariffResult));
                        } else {
                            details.put("formula", "Tariff = Ad Valorem + Specific Duty (NOTE: Input has mixed semantics)");
                            
                            steps.add(Map.of(
                                "step", "1",
                                "description", "Input Value (treated as $ for Ad Valorem, " + (unit != null ? unit : "units") + " for Specific)",
                                "value", String.format("%.2f", amountOfProduct)
                            ));
                            steps.add(Map.of(
                                "step", "2",
                                "description", "Calculate Ad Valorem Component (as dollar value)",
                                "value", String.format("$%.2f × %.2f%% = $%.2f", amountOfProduct, rate, adValoremAmount)
                            ));
                            steps.add(Map.of(
                                "step", "3",
                                "description", "Calculate Billing Units for Specific (as quantity in " + (unit != null ? unit : "units") + ")",
                                "value", String.format("%.2f %s ÷ %.2f = %.2f billing units", amountOfProduct, unit != null ? unit : "", multiplier, units)
                            ));
                            steps.add(Map.of(
                                "step", "4",
                                "description", "Calculate Specific Component",
                                "value", String.format("%.2f billing units × $%.2f = $%.2f", units, amount, specificAmount)
                            ));
                            steps.add(Map.of(
                                "step", "5",
                                "description", "Sum Both Components",
                                "value", String.format("$%.2f + $%.2f = $%.2f", adValoremAmount, specificAmount, tariffResult)
                            ));
                            
                            details.put("calculation", String.format("($%.2f × %.2f%%) + ((%.2f %s / %.2f) × $%.2f) = $%.2f + $%.2f = $%.2f",
                                amountOfProduct, rate, amountOfProduct, unit != null ? unit : "", multiplier, amount,
                                adValoremAmount, specificAmount, tariffResult));
                        }
                            
                    } else if ("C".equals(mixedOrConditional)) {
                        details.put("combinationType", "Conditional (Maximum of both)");
                        
                        if (hasSeparateValues) {
                            details.put("formula", "Tariff = MAX(Ad Valorem, Specific Duty)");
                            
                            steps.add(Map.of(
                                "step", "1a",
                                "description", "Product Value for Ad Valorem",
                                "value", String.format("$%.2f", productValueDollars)
                            ));
                            steps.add(Map.of(
                                "step", "1b",
                                "description", "Product Quantity for Specific Duty",
                                "value", String.format("%.2f %s", amountOfProduct, unit != null ? unit : "units")
                            ));
                            steps.add(Map.of(
                                "step", "2",
                                "description", "Calculate Ad Valorem Component",
                                "value", String.format("$%.2f × %.2f%% = $%.2f", productValueDollars, rate, adValoremAmount)
                            ));
                            steps.add(Map.of(
                                "step", "3",
                                "description", "Calculate Billing Units for Specific",
                                "value", String.format("%.2f %s ÷ %.2f = %.2f billing units", amountOfProduct, unit != null ? unit : "", multiplier, units)
                            ));
                            steps.add(Map.of(
                                "step", "4",
                                "description", "Calculate Specific Component",
                                "value", String.format("%.2f billing units × $%.2f = $%.2f", units, amount, specificAmount)
                            ));
                            steps.add(Map.of(
                                "step", "5",
                                "description", "Choose Maximum",
                                "value", String.format("MAX($%.2f, $%.2f) = $%.2f", adValoremAmount, specificAmount, tariffResult)
                            ));
                            
                            details.put("calculation", String.format("MAX(($%.2f × %.2f%%), ((%.2f %s / %.2f) × $%.2f)) = MAX($%.2f, $%.2f) = $%.2f",
                                productValueDollars, rate, amountOfProduct, unit != null ? unit : "", multiplier, amount,
                                adValoremAmount, specificAmount, tariffResult));
                        } else {
                            details.put("formula", "Tariff = MAX(Ad Valorem, Specific Duty) (NOTE: Input has mixed semantics)");
                            
                            steps.add(Map.of(
                                "step", "1",
                                "description", "Input Value (treated as $ for Ad Valorem, " + (unit != null ? unit : "units") + " for Specific)",
                                "value", String.format("%.2f", amountOfProduct)
                            ));
                            steps.add(Map.of(
                                "step", "2",
                                "description", "Calculate Ad Valorem Component (as dollar value)",
                                "value", String.format("$%.2f × %.2f%% = $%.2f", amountOfProduct, rate, adValoremAmount)
                            ));
                            steps.add(Map.of(
                                "step", "3",
                                "description", "Calculate Billing Units for Specific (as quantity in " + (unit != null ? unit : "units") + ")",
                                "value", String.format("%.2f %s ÷ %.2f = %.2f billing units", amountOfProduct, unit != null ? unit : "", multiplier, units)
                            ));
                            steps.add(Map.of(
                                "step", "4",
                                "description", "Calculate Specific Component",
                                "value", String.format("%.2f billing units × $%.2f = $%.2f", units, amount, specificAmount)
                            ));
                            steps.add(Map.of(
                                "step", "5",
                                "description", "Choose Maximum",
                                "value", String.format("MAX($%.2f, $%.2f) = $%.2f", adValoremAmount, specificAmount, tariffResult)
                            ));
                            
                            details.put("calculation", String.format("MAX(($%.2f × %.2f%%), ((%.2f %s / %.2f) × $%.2f)) = MAX($%.2f, $%.2f) = $%.2f",
                                amountOfProduct, rate, amountOfProduct, unit != null ? unit : "", multiplier, amount,
                                adValoremAmount, specificAmount, tariffResult));
                        }
                    }
                    
                    details.put("steps", steps);
                }
            }
        } catch (Exception e) {
            details.put("error", "Could not retrieve calculation details");
        }
        
        return details;
    }
}