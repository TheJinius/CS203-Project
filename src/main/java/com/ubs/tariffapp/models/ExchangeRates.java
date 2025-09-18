package com.ubs.tariffapp.models;

import java.util.Map;

public class ExchangeRates {
    private String base;
    private long timestamp;
    private Map<String, Double> rates;

    // getters & setters
    public String getBase() { return base; }
    public void setBase(String base) { this.base = base; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Map<String, Double> getRates() { return rates; }
    public void setRates(Map<String, Double> rates) { this.rates = rates; }
}

