package com.ubs.tariffapp.models;

import java.io.Serializable;
import java.util.Objects;import jakarta.persistence.Embeddable;  // Add this import

//@Embeddable  // Add this annotation - REQUIRED for JPA composite keys
// Composite key class for DutyType
public class DutyTypeId implements Serializable {
    private String dutyType;
    private String dutyCode;

    public DutyTypeId() {
    }

    public DutyTypeId(String dutyType, String dutyCode) {
        this.dutyType = dutyType;
        this.dutyCode = dutyCode;
    }

    public String getDutyType() {
        return dutyType;
    }

    public void setDutyType(String dutyType) {
        this.dutyType = dutyType;
    }

    public String getDutyCode() {
        return dutyCode;
    }

    public void setDutyCode(String dutyCode) {
        this.dutyCode = dutyCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DutyTypeId that = (DutyTypeId) o;
        return Objects.equals(dutyType, that.dutyType) && Objects.equals(dutyCode, that.dutyCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dutyType, dutyCode);
    }
}
