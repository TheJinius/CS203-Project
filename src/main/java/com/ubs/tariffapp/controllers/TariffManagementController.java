package com.ubs.tariffapp.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ubs.tariffapp.models.Country;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.dto.TariffRequest;
import com.ubs.tariffapp.models.dto.TariffResponse;
import com.ubs.tariffapp.models.duty.AdValoremDuty;
import com.ubs.tariffapp.models.duty.CombinedDuty;
import com.ubs.tariffapp.models.duty.OtherDuty;
import com.ubs.tariffapp.models.duty.SpecificDuty;
import com.ubs.tariffapp.models.request.TariffSearchRequest;
import com.ubs.tariffapp.repositories.CountryRepository;
import com.ubs.tariffapp.repositories.TariffScheduleRepository;
import com.ubs.tariffapp.services.DutyService;
import com.ubs.tariffapp.services.TariffManagementService;

@RestController
@RequestMapping("/api/admin/tariffs")
@CrossOrigin(origins = "*")
public class TariffManagementController {

    private final TariffManagementService tariffManagementService;
    private final DutyService dutyService;
    private final CountryRepository countryRepository;
    private final TariffScheduleRepository tariffScheduleRepository;

    public TariffManagementController(TariffManagementService tariffManagementService, DutyService dutyService, CountryRepository countryRepository, TariffScheduleRepository tariffScheduleRepository) {
        this.tariffManagementService = tariffManagementService;
        this.dutyService = dutyService;
        this.countryRepository = countryRepository;
        this.tariffScheduleRepository = tariffScheduleRepository;
    }

    // Get all tariffs (for management view)
    @GetMapping
    @PreAuthorize("hasAuthority('Admins')")
    public ResponseEntity<Map<String, Object>> getAllTariffs() {
        System.out.println("📋 Fetching all tariffs for management");
        List<TariffResponse> allTariffs = tariffManagementService.getAllTariffs();
        
        Map<String, Object> response = new HashMap<>();
        response.put("tariffs", allTariffs);
        response.put("count", allTariffs.size());
        response.put("status", "success");
        
        System.out.println("✅ Retrieved " + allTariffs.size() + " tariff(s)");
        return ResponseEntity.ok(response);
    }

    // NEW: Search for tariffs to edit (same as CalculateTab logic)
    @PostMapping("/search")
    @PreAuthorize("hasAnyAuthority('Users', 'Admins')")
    public ResponseEntity<Map<String, Object>> searchTariffs(@RequestBody TariffSearchRequest request) {
        System.out.println("🔍 Admin searching tariffs for editing:");
        System.out.println("ReporterCode = " + request.getReporterCode());
        System.out.println("PartnerCode = " + request.getPartnerCode());
        System.out.println("ProductCode = " + request.getProductCode());
        System.out.println("Year = " + request.getYear());

        List<TariffSchedule> tariffs = dutyService.searchAvailableTariffs(
            request.getReporterCode(),
            request.getPartnerCode(),
            request.getProductCode(),
            request.getYear()
        );
        
        // Convert to admin-friendly format with FULL duty info
        List<Map<String, Object>> tariffList = tariffs.stream()
                .map(ts -> {
                    Map<String, Object> tariffMap = new HashMap<>();
                    tariffMap.put("tariffId", ts.getTariffId());
                    // ✅ CRITICAL FIX: Use "tariffYear" to match frontend interface
                    tariffMap.put("tariffYear", ts.getTariffYear());  // Changed from "year"
                    tariffMap.put("reporterCode", ts.getReporter().getCountryId());
                    tariffMap.put("reporterName", ts.getReporter().getCountryName());
                    tariffMap.put("partnerCode", ts.getPartner().getCountryId());
                    tariffMap.put("partnerName", ts.getPartner().getCountryName());
                    tariffMap.put("tlCode", ts.getProduct().getTlCode());
                    tariffMap.put("productDescription", ts.getProduct().getDescription());
                    tariffMap.put("dutyType", ts.getDutyType().getId().getDutyType());
                    tariffMap.put("dutyCode", ts.getDutyType().getId().getDutyCode());
                    tariffMap.put("dutyTypeDescription", ts.getDutyType().getDutyTypeDescription());
                    tariffMap.put("tlsSuffix", ts.getTlsSuffix());
                    tariffMap.put("note", ts.getNote());
                    
                    // ✅ ADD FULL DUTY DETAILS
                    if (ts.getDuty() != null) {
                        tariffMap.put("dutyCategory", ts.getDuty().getDutyNature());
                        
                        System.out.println("🔍 DUTY TYPE CHECK - TariffID: " + ts.getTariffId() + ", Duty Nature: " + ts.getDuty().getDutyNature() + ", Class: " + ts.getDuty().getClass().getSimpleName());
                        
                        // Extract specific duty rates based on type
                        if (ts.getDuty() instanceof AdValoremDuty) {
                            AdValoremDuty avDuty = (AdValoremDuty) ts.getDuty();
                            System.out.println("   → Ad Valorem: " + avDuty.getRatePercent() + "%");
                            // ✅ Convert BigDecimal to Double
                            tariffMap.put("adValoremRate", avDuty.getRatePercent().doubleValue());
                            
                        } else if (ts.getDuty() instanceof SpecificDuty) {
                            SpecificDuty specDuty = (SpecificDuty) ts.getDuty();
                            System.out.println("   → Specific: " + specDuty.getAmount() + " " + specDuty.getUnit());
                            tariffMap.put("specificRate", specDuty.getAmount().doubleValue());
                            tariffMap.put("specificRateUnit", specDuty.getUnit());
                            
                        } else if (ts.getDuty() instanceof CombinedDuty) {
                            CombinedDuty combDuty = (CombinedDuty) ts.getDuty();
                            System.out.println("   → Combined: " + combDuty.getRatePercent() + "% + " + combDuty.getAmount() + " " + combDuty.getUnit());
                            if (combDuty.getRatePercent() != null) {
                                tariffMap.put("adValoremRate", combDuty.getRatePercent().doubleValue());
                                tariffMap.put("compoundRate1", combDuty.getRatePercent().doubleValue());
                            }
                            if (combDuty.getAmount() != null) {
                                tariffMap.put("specificRate", combDuty.getAmount().doubleValue());
                                tariffMap.put("compoundRate2", combDuty.getAmount().doubleValue());
                            }
                            tariffMap.put("specificRateUnit", combDuty.getUnit());
                            
                        } else if (ts.getDuty() instanceof OtherDuty) {
                            OtherDuty otherDuty = (OtherDuty) ts.getDuty();
                            System.out.println("   → Other: rawText=" + otherDuty.getRawText() + ", isComputable=" + otherDuty.getIsComputable());
                            tariffMap.put("rawText", otherDuty.getRawText());
                            tariffMap.put("isComputable", otherDuty.getIsComputable());
                        }
                    }
                    
                    return tariffMap;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("tariffs", tariffList);
        response.put("count", tariffs.size());
        response.put("year", request.getYear());
        response.put("status", "success");
        
        System.out.println("✅ Found " + tariffs.size() + " tariff(s) for editing with full duty details");
        return ResponseEntity.ok(response);
    }


    // Get specific tariff details for editing
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('Admins')")
    public ResponseEntity<TariffResponse> getTariffForEdit(@PathVariable Integer id) {
        System.out.println("🔍 Fetching tariff for edit - ID: " + id);
        TariffResponse response = tariffManagementService.getTariffById(id);
        return ResponseEntity.ok(response);
    }

    // Create new tariff
    @PostMapping
    @PreAuthorize("hasAuthority('Admins')")
    public ResponseEntity<TariffResponse> createTariff(
            @Validated(TariffRequest.Create.class) @RequestBody TariffRequest request) {
        System.out.println("📝 Creating new tariff");
        TariffResponse response = tariffManagementService.createTariff(request);
        System.out.println("✅ Tariff created with ID: " + response.getTariffId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Update existing tariff
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Admins')")
    public ResponseEntity<TariffResponse> updateTariff(
        @PathVariable Integer id,
        @RequestBody Map<String, Object> updates  // ✅ Change from TariffRequest to Map
    ) {
        TariffResponse updated = tariffManagementService.updateTariff(id, updates);
        return ResponseEntity.ok(updated);
    }

    // Delete tariff
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Admins')")
    public ResponseEntity<Void> deleteTariff(@PathVariable Integer id) {
        System.out.println("🗑️ Deleting tariff ID: " + id);
        tariffManagementService.deleteTariff(id);
        System.out.println("✅ Tariff deleted successfully");
        return ResponseEntity.noContent().build();
    }

    // Get all countries
    @GetMapping("/countries")
    @PreAuthorize("hasAnyAuthority('Users', 'Admins')")
    public ResponseEntity<Map<String, Object>> getAllCountries() {
        System.out.println("🌍 Fetching all countries");
        List<Country> countries = countryRepository.findAll();
        
        // Convert to simple map format
        List<Map<String, String>> countryList = countries.stream()
                .map(country -> {
                    Map<String, String> countryMap = new HashMap<>();
                    countryMap.put("code", country.getCountryId());
                    countryMap.put("name", country.getCountryName());
                    return countryMap;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("countries", countryList);
        response.put("count", countryList.size());
        response.put("status", "success");
        
        System.out.println("✅ Retrieved " + countryList.size() + " countries");
        return ResponseEntity.ok(response);
    }
    
    // Get available years from tariff_schedule table
    @GetMapping("/years")
    @PreAuthorize("hasAnyAuthority('Users', 'Admins')")
    public ResponseEntity<Map<String, Object>> getAvailableYears() {
        System.out.println("📅 Fetching available tariff years");
        List<Integer> years = tariffScheduleRepository.findDistinctYears();
        
        Map<String, Object> response = new HashMap<>();
        response.put("years", years);
        response.put("count", years.size());
        response.put("status", "success");
        
        System.out.println("✅ Retrieved " + years.size() + " distinct years: " + years);
        return ResponseEntity.ok(response);
    }
}
