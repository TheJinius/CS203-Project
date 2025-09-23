package com.ubs.tariffapp.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ubs.tariffapp.models.request.TariffCalculationRequest;
import com.ubs.tariffapp.services.DutyService;
import com.ubs.tariffapp.exceptions.*;

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
        System.out.println("ReporterCode = " + request.getReporterCode());
        System.out.println("PartnerCode = " + request.getPartnerCode());
        System.out.println("ProductCode = " + request.getProductCode());
        System.out.println("AmountofProduct = " + request.getAmountOfProduct());

        try {
            double tariffAmount = dutyService.calculateTariff(
                    request.getReporterCode(),
                    request.getPartnerCode(),
                    request.getProductCode(),
                    request.getAmountOfProduct()
            );
            
            Map<String, Object> response = Map.of(
                "tariffAmount", tariffAmount,
                "currency", request.getCurrency() != null ? request.getCurrency() : "SGD",
                "status", "success"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (TariffNotFoundException e) {
            // 404 - Tariff schedule not found
            Map<String, Object> errorResponse = Map.of(
                "error", e.getMessage(),
                "status", "tariff_not_found",
                "httpStatus", 404
            );
            return ResponseEntity.status(404).body(errorResponse);
            
        } catch (DutyNotFoundException e) {
            // 404 - Duty not found (data integrity issue)
            Map<String, Object> errorResponse = Map.of(
                "error", e.getMessage(),
                "status", "duty_not_found",
                "httpStatus", 404
            );
            return ResponseEntity.status(404).body(errorResponse);
            
        } catch (InvalidRequestException e) {
            // 400 - Bad request (invalid input)
            Map<String, Object> errorResponse = Map.of(
                "error", e.getMessage(),
                "status", "bad_request",
                "httpStatus", 400
            );
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            // 500 - Unexpected error
            Map<String, Object> errorResponse = Map.of(
                "error", "Internal server error: " + e.getMessage(),
                "status", "internal_error",
                "httpStatus", 500
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }


        // fake data to test backend and fronntend integration
    //     Map<String, Object> response = Map.of(
    //             "tariffAmount", 500,
    //             "currency", request.getCurrency() != null ? request.getCurrency() : "SGD",
    //             "status", "success"
    //         );
    //         return ResponseEntity.ok(response);
    // }
}
