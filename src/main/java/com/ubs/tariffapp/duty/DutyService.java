package com.ubs.tariffapp.duty;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.ubs.tariffapp.country.CountryService;
import com.ubs.tariffapp.product.ProductService;


@Service
public class DutyService {
    
    private final DutyRepository tariffRepository;
    private final CountryService countryService;
    private final ProductService productService;

    @Autowired
    public DutyService(
        DutyRepository tariffRepository,
        CountryService countryService,
        ProductService productService
    ) {
        this.tariffRepository = tariffRepository;
        this.countryService = countryService;
        this.productService = productService;
    }

    // Calculator calls this
    public TariffCalculationResponse calculateTariff(
        String productCode, 
        String sourceCountry,
        String destinationCountry,
        double tradeValue
    ) {
        // 1. Validate country and product existence
        var product = productService.getProductByCode(productCode);
        var source = countryService.getCountryByCode(sourceCountry);
        var destination = countryService.getCountryByCode(destinationCountry);

        // 2. Get applicable tariff rules
        String origin = sourceCountry + "#" + productCode;
        Duty tariff = tariffRepository.findById(origin)
            .orElseThrow(() -> new TariffNotFoundException(origin));

        // 3. Calculate base tariff
        double baseTariff = tradeValue * tariff.getTaxRate();

        // // 4. Apply surcharges
        // double totalSurcharges = tariff.getSurcharges().stream()
        //     .mapToDouble(surcharge -> surcharge.getAmount())
        //     .sum();

        return new TariffCalculationResponse(
            baseTariff
            // totalSurcharges,
            // baseTariff + totalSurcharges,
            // tariff.getBaseCurrency()
        );
    }
}
