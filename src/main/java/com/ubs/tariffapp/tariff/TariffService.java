package com.ubs.tariffapp.tariff;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.ubs.tariffapp.country.CountryService;
import com.ubs.tariffapp.product.ProductService;
import com.ubs.tariffapp.models.Tariff;
import com.ubs.tariffapp.repositories.TariffRepository;

@Service
public class TariffService {
    
    private final TariffRepository tariffRepository;
    private final CountryService countryService;
    private final ProductService productService;

    @Autowired
    public TariffService(
        TariffRepository tariffRepository,
        CountryService countryService,
        ProductService productService
    ) {
        this.tariffRepository = tariffRepository;
        this.countryService = countryService;
        this.productService = productService;
    }

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
        Tariff tariff = tariffRepository.findById(origin)
            .orElseThrow(() -> new TariffNotFoundException(origin));

        // 3. Calculate base tariff
        double baseTariff = tradeValue * tariff.getTaxRate();

        // 4. Apply surcharges
        double totalSurcharges = tariff.getSurcharges().stream()
            .mapToDouble(surcharge -> surcharge.getAmount())
            .sum();

        return new TariffCalculationResponse(
            baseTariff,
            totalSurcharges,
            baseTariff + totalSurcharges,
            tariff.getBaseCurrency()
        );
    }
}
