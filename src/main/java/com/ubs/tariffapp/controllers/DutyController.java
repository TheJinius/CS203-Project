package com.ubs.tariffapp.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.FacesRequestAttributes;

import com.ubs.tariffapp.models.request.TariffCalculationRequest;
import com.ubs.tariffapp.models.request.TariffSearchRequest;
import com.ubs.tariffapp.models.ExchangeRates;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.services.DutyService;
import com.ubs.tariffapp.services.ExchangeRateService;
import com.ubs.tariffapp.services.TariffScheduleService;
import com.ubs.tariffapp.exceptions.*;
import com.ubs.tariffapp.models.dto.TariffOptionsResponse;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            // TODO: REMOVE THIS AND REPLACE WITH REAL DATA
            // FAKE tariff data for testing
            List<Map<String, Object>> fakeTariffs = new ArrayList<>();

            // Tariff 1
            Map<String, Object> tariff1 = new HashMap<>();
            tariff1.put("tariffId", 1001);
            tariff1.put("description", "Pure bred breeding horses - Standard Rate");
            tariff1.put("dutyType", "Ad Valorem");
            tariff1.put("tlsSuffix", "00");
            fakeTariffs.add(tariff1);

            // Tariff 2
            Map<String, Object> tariff2 = new HashMap<>();
            tariff2.put("tariffId", 1002);
            tariff2.put("description", "Pure bred breeding horses - Preferential Rate");
            tariff2.put("dutyType", "Specific");
            tariff2.put("tlsSuffix", "10");
            fakeTariffs.add(tariff2);

            // Tariff 3
            Map<String, Object> tariff3 = new HashMap<>();
            tariff3.put("tariffId", 1003);
            tariff3.put("description", "Pure bred breeding horses - Combined Rate");
            tariff3.put("dutyType", "Combined (Mixed)");
            tariff3.put("tlsSuffix", "20");
            fakeTariffs.add(tariff3);

            Map<String, Object> response = new HashMap<>();
            response.put("tariffs", fakeTariffs);
            response.put("count", fakeTariffs.size());
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
        System.out.println("💰 Calculating tariff:");
        System.out.println("TariffId = " + request.getTariffId());
        System.out.println("AmountofProduct = " + request.getAmountOfProduct());
        String requestedCurrency = request.getCurrency() != null ? request.getCurrency() : "USD";

        try {
            // Fake calculation for now
            double tariffAmount = request.getAmountOfProduct() * 0.1; // 10% duty rate

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

            System.out.println("SUCCESS: Calculated tariff = " + convertedAmount + " " + requestedCurrency);
            return ResponseEntity.ok(response);

        } catch (TariffNotFoundException e) {
            System.err.println("TariffNotFoundException: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "tariff_not_found");
            return ResponseEntity.status(404).body(errorResponse);

        } catch (DutyNotFoundException e) {
            System.err.println("DutyNotFoundException: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "duty_not_found");
            return ResponseEntity.status(404).body(errorResponse);

        } catch (InvalidRequestException e) {
            System.err.println("InvalidRequestException: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", "bad_request");
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error: " + e.getMessage());
            errorResponse.put("status", "internal_error");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // // Helper method to build user-friendly tariff description
    // private String buildTariffDescription(TariffSchedule ts) {
    // StringBuilder desc = new StringBuilder();

    // if (ts.getProduct() != null) {
    // desc.append(ts.getProduct().getDescription() != null ?
    // ts.getProduct().getDescription() : ts.getProduct().getTlCode());
    // }

    // if (ts.getTlsSuffix() != null && !ts.getTlsSuffix().isEmpty()) {
    // desc.append(" (").append(ts.getTlsSuffix()).append(")");
    // }

    // if (ts.getDutyType() != null) {
    // desc.append(" - ").append(ts.getDutyType().getDutyTypeDescription());
    // }

    // return desc.toString();
    // }
}
