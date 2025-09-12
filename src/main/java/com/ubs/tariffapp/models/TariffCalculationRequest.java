package com.ubs.tariffapp.models;

public class TariffCalculationRequest {
    private String importerCountry;
    private String exporterCountry;
    private String product;
    private double amountOfProduct;
    private String currency;
    public String getImporterCountry() {
        return importerCountry;
    }
    public void setImporterCountry(String importerCountry) {
        this.importerCountry = importerCountry;
    }
    public String getExporterCountry() {
        return exporterCountry;
    }
    public void setExporterCountry(String exporterCountry) {
        this.exporterCountry = exporterCountry;
    }
    public String getProduct() {
        return product;
    }
    public void setProduct(String product) {
        this.product = product;
    }
    public double getAmountOfProduct() {
        return amountOfProduct;
    }
    public void setAmountOfProduct(double amountOfProduct) {
        this.amountOfProduct = amountOfProduct;
    }
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
