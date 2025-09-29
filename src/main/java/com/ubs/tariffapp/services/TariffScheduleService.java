package com.ubs.tariffapp.services;

import java.util.Optional;
import java.util.List;
import org.springframework.stereotype.Service;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.dto.TariffOptionsResponse;
import com.ubs.tariffapp.models.dto.TariffSearchResult;
import com.ubs.tariffapp.repositories.TariffScheduleRepository;

@Service
public class TariffScheduleService {
    
    private final TariffScheduleRepository tariffScheduleRepository;

    public TariffScheduleService(TariffScheduleRepository tariffScheduleRepository) {
        this.tariffScheduleRepository = tariffScheduleRepository;
    }

    // NEW: Search for multiple tariffs
    public List<TariffSchedule> searchTariffSchedules(String reporterCode, String partnerCode, String productCode, int year) {
        System.out.println("🔍 Searching for tariffs - Reporter: " + reporterCode + ", Partner: " + partnerCode + ", Product: " + productCode + ", Year: " + year);
        List<TariffSchedule> results = tariffScheduleRepository.findAllByReporterAndPartnerAndTlAndYear(reporterCode, partnerCode, productCode, year);
        System.out.println("📋 Found " + results.size() + " tariff schedules");
        return results;
    }

    // NEW: Get specific tariff by ID
    public TariffSchedule getTariffScheduleById(Integer tariffId) {
        System.out.println("🔍 Getting tariff by ID: " + tariffId);
        Optional<TariffSchedule> result = tariffScheduleRepository.findById(tariffId);
        return result.orElse(null);
    }

    // Keep existing methods for backward compatibility
    public TariffSchedule getTariffSchedule(String reporterCode, String partnerCode, String tlCode) {
        System.out.println("🔍 Searching without year (defaulting to 2023)");
        Optional<TariffSchedule> result = tariffScheduleRepository.findByReporterAndPartnerAndTl(reporterCode, partnerCode, tlCode);
        return result.orElse(null);
    }

    public TariffSchedule getTariffSchedule(String reporterCode, String partnerCode, String tlCode, int year) {
        System.out.println("🔍 Searching with year: " + year);
        Optional<TariffSchedule> result = tariffScheduleRepository.findByReporterAndPartnerAndTlAndYear(reporterCode, partnerCode, tlCode, year);
        return result.orElse(null);
    }

    public long getTotalCount() {
        return tariffScheduleRepository.count();
    }

    public TariffSchedule getFirstTariffSchedule() {
        return tariffScheduleRepository.findAll().stream().findFirst().orElse(null);
    }

    public TariffOptionsResponse searchOptions(String reporter, String partner, String tlCode, Integer year) {
        // Pad TL code to 8 digits to match CSV format
        String paddedTlCode;
        try {
            paddedTlCode = String.format("%08d", Integer.parseInt(tlCode));
        } catch (NumberFormatException e) {
            // If tlCode is already a string, ensure 8 digits
            paddedTlCode = tlCode.length() < 8 ? String.format("%8s", tlCode).replace(' ', '0') : tlCode;
        }
        
        // Try exact match first
        List<TariffSearchResult> list = tariffScheduleRepository.findOptions(reporter, partner, paddedTlCode, year);
        if (!list.isEmpty()) {
            return new TariffOptionsResponse(list, false, null);
        }

        // Fallback: Try World partner ("000")
        List<TariffSearchResult> worldOptions = tariffScheduleRepository.findWorldOptions(reporter, paddedTlCode, year);
        if (!worldOptions.isEmpty()) {
            return new TariffOptionsResponse(worldOptions, true, "Using World partner fallback");
        }

        // No options found
        return new TariffOptionsResponse(List.of(), true, "No tariff data available");
    }
}
