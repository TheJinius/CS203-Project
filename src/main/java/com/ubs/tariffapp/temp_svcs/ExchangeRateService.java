package com.ubs.tariffapp.services;

import org.springframework.stereotype.Service;
import com.ubs.tariffapp.repositories.ExchangeRateRepository;
import com.ubs.tariffapp.models.ExchangeRates;

@Service
public class ExchangeRateService {

    private final ExchangeRateRepository repository;

    public ExchangeRateService(ExchangeRateRepository repository) {
        this.repository = repository;
    }

    public ExchangeRates fetchRates(String baseCurrency) {
        return repository.getExchangeRates(baseCurrency);
    }
}
