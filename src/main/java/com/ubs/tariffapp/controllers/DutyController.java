package com.ubs.tariffapp.controllers;

import org.springframework.web.bind.annotation.*;

import com.ubs.tariffapp.Services.DutyService;
import com.ubs.tariffapp.models.TariffCalculationRequest;

@RestController
@RequestMapping("/api/tariffs")
public class DutyController {

    private final DutyService dutyService;

    public DutyController(DutyService dutyService) {
        this.dutyService = dutyService;
    }

    @PostMapping("/calculate")
    public String calculateTariff(@RequestBody TariffCalculationRequest request) {
        return dutyService.calculateTariff(
                request.getImporterCountry(),
                request.getExporterCountry(),
                request.getProduct());
    }
}
