package com.ubs.tariffapp.Services;

import org.springframework.stereotype.Service;
import com.ubs.tariffapp.repositories.DutyRepository;
import com.ubs.tariffapp.models.Duty;


@Service
public class DutyService {

    private final DutyRepository dutyRepository;

    public DutyService(DutyRepository dutyRepository) {
        this.dutyRepository = dutyRepository;
    }

    
    public Duty getDuty(String importerCountry, String exporterCountry, String productCode) {
        return dutyRepository.findDutyByCountriesAndProduct(importerCountry, exporterCountry, productCode)
                .orElse(null); // or throw an exception if not found
    }

    // DutyController calls this to calculate
    public String calculateTariff(String importerCountry, String exporterCountry, String product) {
        Duty duty = getDuty(importerCountry, exporterCountry, product);
        // Add your calculation logic here using the Duty object
        
        return null;
    }
}
