package com.ubs.tariffapp.models.duty;

import com.ubs.tariffapp.models.TariffSchedule;

import jakarta.persistence.Entity;

@Entity
public class OtherDuty extends Duty {
    /*
     * Taken from the 'Specific Duty Rate' column in the CSV, but is not
     * always a rate e.g. "See 9822.04.01-9822.04.03"
     */
    private String rawText;
    private Boolean isComputable; // Whether this duty can be computed or not

    public OtherDuty() {
    }

    public OtherDuty(Integer tariffId, TariffSchedule tariffSchedule,
            String dutyNature, String mathExpression, String rawText,
            Boolean isComputable) {
        super(tariffId, tariffSchedule, dutyNature, mathExpression);
        this.rawText = rawText;
        this.isComputable = isComputable;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public Boolean getIsComputable() {
        return isComputable;
    }

    public void setIsComputable(Boolean isComputable) {
        this.isComputable = isComputable;
    }
}
