package com.ubs.tariffapp.models;

import java.util.Map;

public class ForexRate {
    private String baseCurrency;
    private Map<String, Object> rates;

    public ForexRate(String baseCurrency, Map<String, Object> rates) {
        this.baseCurrency = baseCurrency;
        this.rates = rates;
    }

    public double getRate(String targetCurrency) {
        Object rate = rates.get(targetCurrency);
        return rate == null ? -1 : Double.parseDouble(rate.toString());
    }

    public String getBaseCurrency() { return baseCurrency; }
    public Map<String, Object> getRates() { return rates; }
}
