package com.ubs.tariffapp.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ubs.tariffapp.services.DutyService;
import com.ubs.tariffapp.models.TariffCalculationRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/tariffs")
public class DutyController {

    private final DutyService dutyService;

    public DutyController(DutyService dutyService) {
        this.dutyService = dutyService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateTariff(@RequestBody TariffCalculationRequest request) {
        try {
            double tariffAmount = dutyService.calculateTariff(
                    request.getImporterCountry(),
                    request.getExporterCountry(),
                    request.getProduct(),
                    request.getAmountOfProduct()
            );
            
            Map<String, Object> response = Map.of(
                "tariffAmount", tariffAmount,
                "currency", request.getCurrency() != null ? request.getCurrency() : "SGD",
                "status", "success"
            );
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = Map.of(
                "error", e.getMessage(),
                "status", "error"
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
