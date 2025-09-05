package com.ubs.tariffapp.tariff;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.ubs.tariffapp.models.Tariff;

@RestController
@RequestMapping("/api/tariffs")
public class TariffController {
    
    private final TariffService tariffService;

    @Autowired
    public TariffController(TariffService tariffService) {
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
