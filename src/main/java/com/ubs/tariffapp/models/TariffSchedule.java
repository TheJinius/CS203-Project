package com.ubs.tariffapp.models;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

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

    private Integer year;

    @ManyToOne
    @JoinColumn(name = "tl_code")
    private Product product;

    // No-argument constructor
    public TariffSchedule() {
    }

    // All-argument constructor
    public TariffSchedule(Integer tariffId, Country reporter, Country partner, Integer year, Product product,
            String tlsSuffix, DutyType dutyType, String dutyCode, String dutyTypeDescription, String dutyNature,
            String avMethod, String note, List<DutyComponent> dutyComponents, List<AuditLog> auditLogs) {
        this.tariffId = tariffId;
        this.reporter = reporter;
        this.partner = partner;
        this.year = year;
        this.product = product;
        this.tlsSuffix = tlsSuffix;
        this.dutyType = dutyType;
        this.dutyCode = dutyCode;
        this.dutyTypeDescription = dutyTypeDescription;
        this.dutyNature = dutyNature;
        this.avMethod = avMethod;
        this.note = note;
        this.dutyComponents = dutyComponents;
        this.auditLogs = auditLogs;
    }

    private String tlsSuffix;

    @ManyToOne
    @JoinColumn(name = "duty_type_code")
    private DutyType dutyType;

    private String dutyCode;
    private String dutyTypeDescription;
    private String dutyNature;
    private String avMethod;
    private String note;

    @OneToMany(mappedBy = "tariffSchedule")
    private List<DutyComponent> dutyComponents;

    @OneToMany(mappedBy = "tariffSchedule")
    private List<AuditLog> auditLogs;

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

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
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

    public String getDutyCode() {
        return dutyCode;
    }

    public void setDutyCode(String dutyCode) {
        this.dutyCode = dutyCode;
    }

    public String getDutyTypeDescription() {
        return dutyTypeDescription;
    }

    public void setDutyTypeDescription(String dutyTypeDescription) {
        this.dutyTypeDescription = dutyTypeDescription;
    }

    public String getDutyNature() {
        return dutyNature;
    }

    public void setDutyNature(String dutyNature) {
        this.dutyNature = dutyNature;
    }

    public String getAvMethod() {
        return avMethod;
    }

    public void setAvMethod(String avMethod) {
        this.avMethod = avMethod;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<DutyComponent> getDutyComponents() {
        return dutyComponents;
    }

    public void setDutyComponents(List<DutyComponent> dutyComponents) {
        this.dutyComponents = dutyComponents;
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogs;
    }

    public void setAuditLogs(List<AuditLog> auditLogs) {
        this.auditLogs = auditLogs;
    }
}
