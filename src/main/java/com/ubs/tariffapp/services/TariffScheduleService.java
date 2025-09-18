package com.ubs.tariffapp.services;

import org.springframework.stereotype.Service;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.repositories.TariffScheduleRepository;

@Service
public class TariffScheduleService {
    private final TariffScheduleRepository tariffScheduleRepository;

    public TariffScheduleService(TariffScheduleRepository tariffScheduleRepository) {
        this.tariffScheduleRepository = tariffScheduleRepository;
    }

    // Parameters match your CSV data: Reporter="702", Partner="000", TL="1012100"
    public TariffSchedule getTariffSchedule(String reporterCode, String partnerCode, String tlCode) {
        return tariffScheduleRepository.findByReporterAndPartnerAndTl(reporterCode, partnerCode, tlCode)
                .orElse(null);
    }
}
