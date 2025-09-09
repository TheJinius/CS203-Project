package com.ubs.tariffapp.duty;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;


@RestController
@RequestMapping("/api/tariffs")
public class DutyController {
    
    private final DutyService tariffService;

    @Autowired
    public DutyController(DutyService tariffService) {
        this.tariffService = tariffService;
    }

    @PostMapping("/calculate")
    public TariffCalculationResponse calculateTariff(@RequestBody TariffCalculationRequest request) {
        return tariffService.calculateTariff(
            request.getProductCode(), 
            request.getSourceCountry(),
            request.getDestinationCountry(),
            request.getTradeValue()
        );
    }
}
