package com.ubs.tariffapp.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ubs.tariffapp.models.ExchangeRates;
import com.ubs.tariffapp.services.ExchangeRateService;

@RestController
public class ExchangeRateController {

    private final ExchangeRateService service;

    public ExchangeRateController(ExchangeRateService service) {
        this.service = service;
    }

    @GetMapping("/exchange-rates")
    public ExchangeRates getExchangeRates(@RequestParam(defaultValue = "USD") String base) {
        return service.fetchRates(base);
    }
}
