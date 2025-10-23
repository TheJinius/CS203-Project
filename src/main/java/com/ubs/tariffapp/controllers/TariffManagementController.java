package com.ubs.tariffapp.controllers;

import com.ubs.tariffapp.models.dto.TariffRequest;
import com.ubs.tariffapp.models.dto.TariffResponse;
import com.ubs.tariffapp.models.request.TariffSearchRequest;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.services.TariffManagementService;
import com.ubs.tariffapp.services.DutyService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/tariffs")
@CrossOrigin(origins = "*")
public class TariffManagementController {

    private final TariffManagementService tariffManagementService;
    private final DutyService dutyService;

    public TariffManagementController(TariffManagementService tariffManagementService, DutyService dutyService) {
        this.tariffManagementService = tariffManagementService;
        this.dutyService = dutyService;
    }

    // NEW: Search for tariffs to edit (same as CalculateTab logic)
    @PostMapping("/search")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<Map<String, Object>> searchTariffsForEdit(@RequestBody TariffSearchRequest request) {
        System.out.println("🔍 Admin searching tariffs for editing:");
        System.out.println("ReporterCode = " + request.getReporterCode());
        System.out.println("PartnerCode = " + request.getPartnerCode());
        System.out.println("ProductCode = " + request.getProductCode());
        System.out.println("Year = " + request.getYear());

        try {
            // Use the same DutyService method that works in CalculateTab
            List<TariffSchedule> tariffs = dutyService.searchAvailableTariffs(
                request.getReporterCode(),
                request.getPartnerCode(),
                request.getProductCode(),
                request.getYear()
            );
            
            // Convert to admin-friendly format with edit info
            List<Map<String, Object>> tariffList = tariffs.stream()
                    .map(ts -> {
                        Map<String, Object> tariffMap = new HashMap<>();
                        tariffMap.put("tariffId", ts.getTariffId());
                        tariffMap.put("year", ts.getTariffYear());
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
                        
                        // Add duty details for editing
                        if (ts.getDuty() != null) {
                            tariffMap.put("dutyCategory", ts.getDuty().getDutyNature());
                            // Add specific duty rates based on type (will be populated by service)
                        }
                        
                        return tariffMap;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("tariffs", tariffList);
            response.put("count", tariffs.size());
            response.put("year", request.getYear());
            response.put("status", "success");
            
            System.out.println("✅ Found " + tariffs.size() + " tariff(s) for editing");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Search error: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to search tariffs: " + e.getMessage());
            errorResponse.put("status", "error");
            errorResponse.put("tariffs", List.of());
            errorResponse.put("count", 0);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Get specific tariff details for editing
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<TariffResponse> getTariffForEdit(@PathVariable Integer id) {
        System.out.println("🔍 Fetching tariff for edit - ID: " + id);
        try {
            TariffResponse response = tariffManagementService.getTariffById(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Error fetching tariff: " + e.getMessage());
            return ResponseEntity.status(404).build();
        }
    }

    // Create new tariff
    @PostMapping
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<TariffResponse> createTariff(@Valid @RequestBody TariffRequest request) {
        System.out.println("📝 Creating new tariff");
        try {
            TariffResponse response = tariffManagementService.createTariff(request);
            System.out.println("✅ Tariff created with ID: " + response.getTariffId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            System.err.println("❌ Error creating tariff: " + e.getMessage());
            return ResponseEntity.status(400).build();
        }
    }

    // Update existing tariff
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<TariffResponse> updateTariff(
            @PathVariable Integer id,
            @Valid @RequestBody TariffRequest request) {
        System.out.println("✏️ Updating tariff ID: " + id);
        try {
            TariffResponse response = tariffManagementService.updateTariff(id, request);
            System.out.println("✅ Tariff updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Error updating tariff: " + e.getMessage());
            return ResponseEntity.status(400).build();
        }
    }

    // Delete tariff
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<Void> deleteTariff(@PathVariable Integer id) {
        System.out.println("🗑️ Deleting tariff ID: " + id);
        try {
            tariffManagementService.deleteTariff(id);
            System.out.println("✅ Tariff deleted successfully");
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            System.err.println("❌ Error deleting tariff: " + e.getMessage());
            return ResponseEntity.status(404).build();
        }
    }
}
