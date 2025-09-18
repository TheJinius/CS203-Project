package com.ubs.tariffapp.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ubs.tariffapp.models.request.TariffCalculationRequest;
import com.ubs.tariffapp.services.DutyService;

import java.util.Map;

@RestController
@RequestMapping("/api/tariffs")
@CrossOrigin(origins = "http://localhost:3000") // Allow frontend to connect
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

        //do not delete. uncomment after db is set up, this is the actual code for calling evertyh else!!!
        // try {
        //     double tariffAmount = dutyService.calculateTariff(
        //             request.getReporterCode(),
        //             request.getPartnerCode(),
        //             request.getProductCode(),
        //             request.getAmountOfProduct()
        //     );
            
        //     Map<String, Object> response = Map.of(
        //         "tariffAmount", tariffAmount,
        //         "currency", request.getCurrency() != null ? request.getCurrency() : "SGD",
        //         "status", "success"
        //     );
            
        //     return ResponseEntity.ok(response);
        // } catch (RuntimeException e) {
        //     Map<String, Object> errorResponse = Map.of(
        //         "error", e.getMessage(),
        //         "status", "error"
        //     );
        //     return ResponseEntity.badRequest().body(errorResponse);
        // }


        // fake data to test backend and fronntend integration
        Map<String, Object> response = Map.of(
                "tariffAmount", 500,
                "currency", request.getCurrency() != null ? request.getCurrency() : "SGD",
                "status", "success"
            );
            return ResponseEntity.ok(response);
    }
}
