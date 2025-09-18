package com.ubs.tariffapp.models;

public class ExchangeRate {
    private double rate;
    private String countryCode;
    private String currencyCode;

    public ExchangeRate(double rate, String countryCode, String currencyCode) {
        this.rate = rate;
        this.countryCode = countryCode;
        this.currencyCode = currencyCode;
    }

    public double getRate() {
        return rate;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }
}
