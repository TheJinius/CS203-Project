package com.ubs.tariffapp.models;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

@Entity
public class TariffSchedule {
    @Id
    private Integer tariffId;

    @ManyToOne
    @JoinColumn(name = "reporter_id")
    private Country reporter;

    @ManyToOne
    @JoinColumn(name = "partner_id")
    private Country partner;

    @ManyToOne
    @JoinColumn(name = "tl_code")
    private Product product;

    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "duty_type", referencedColumnName = "dutyType"),
        @JoinColumn(name = "duty_code", referencedColumnName = "dutyCode")
    })
    private DutyType dutyType;

    private String tlsSuffix;
    private String note;

    @OneToOne(mappedBy = "tariffSchedule")
    private Duty duty;

    @OneToMany(mappedBy = "tariffSchedule")
    private List<AuditLog> auditLogs;

    // No-argument constructor
    public TariffSchedule() {
    }

    // All-argument constructor
    public TariffSchedule(Integer tariffId, Country reporter, Country partner, Product product, String tariffLineSuffix,
            DutyType dutyType, String note, Duty duty, List<AuditLog> auditLogs) {
        this.tariffId = tariffId;
        this.reporter = reporter;
        this.partner = partner;
        this.product = product;
        this.tlsSuffix = tariffLineSuffix;
        this.dutyType = dutyType;
        this.note = note;
        this.duty = duty;
        this.auditLogs = auditLogs;
    }

    // Getters and setters
    public Integer getTariffId() {
        return tariffId;
    }

    public void setTariffId(Integer tariffId) {
        this.tariffId = tariffId;
    }

    public Country getReporter() {
        return reporter;
    }

    public void setReporter(Country reporter) {
        this.reporter = reporter;
    }

    public Country getPartner() {
        return partner;
    }

    public void setPartner(Country partner) {
        this.partner = partner;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getTlsSuffix() {
        return tlsSuffix;
    }

    public void setTlsSuffix(String tlsSuffix) {
        this.tlsSuffix = tlsSuffix;
    }

    public DutyType getDutyType() {
        return dutyType;
    }

    public void setDutyType(DutyType dutyType) {
        this.dutyType = dutyType;
    }


    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogs;
    }

    public void setAuditLogs(List<AuditLog> auditLogs) {
        this.auditLogs = auditLogs;
    }

    public Duty getDuty() {
        return duty;
    }

    public void setDuty(Duty duty) {
        this.duty = duty;
    }
}
