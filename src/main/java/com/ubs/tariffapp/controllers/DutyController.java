package com.ubs.tariffapp.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ubs.tariffapp.models.ExchangeRates;
import com.ubs.tariffapp.models.TariffSchedule;
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
    }

    // NEW ENDPOINT 2: Calculate tariff using specific tariff ID
    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateTariff(@RequestBody TariffCalculationRequest request) {
        String requestedCurrency = request.getCurrency() != null ? request.getCurrency() : "USD";

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
        response.put("calculationDetails", getCalculationDetails(request.getTariffId()));
        response.put("status", "success");

        System.out.println("✅ SUCCESS: Calculated tariff = " + convertedAmount + " " + requestedCurrency);
        return ResponseEntity.ok(response);
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
    private String getCalculationDetails(Integer tariffId) {
        TariffSchedule tariff = tariffScheduleService.getTariffScheduleById(tariffId);
        if (tariff != null && tariff.getDuty() != null) {
            return "Applied " + tariff.getDutyType().getDutyTypeDescription() + 
                   " duty for " + tariff.getProduct().getDescription();
        }
        
        return "No calculation details available";
    }
}