package com.ubs.tariffapp.models;

public class TariffCalculationRequest {
    private String importerCountry;
    private String exporterCountry;
    private String product;
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

}
