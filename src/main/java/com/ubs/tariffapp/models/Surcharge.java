package com.ubs.tariffapp.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

// Surcharge needs to have a separate table as it is multivalued
// To convert into 1st normal form (recall data management concepts)

@Embeddable
public class Surcharge {
    @Column(name = "type")
    private String type;

    @Column(name = "amount")
    private double amount;

    @Column(name = "currency")
    private String currency;


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}