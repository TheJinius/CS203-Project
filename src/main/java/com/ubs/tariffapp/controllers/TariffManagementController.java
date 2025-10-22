package com.ubs.tariffapp.controllers;

import com.ubs.tariffapp.models.dto.TariffRequest;
import com.ubs.tariffapp.models.dto.TariffResponse;
import com.ubs.tariffapp.services.TariffManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tariffs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TariffManagementController {

    private final TariffManagementService tariffManagementService;

    @PostMapping
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<TariffResponse> createTariff(@Valid @RequestBody TariffRequest request) {
        System.out.println("📝 Creating new tariff");
        TariffResponse response = tariffManagementService.createTariff(request);
        System.out.println("✅ Tariff created with ID: " + response.getTariffId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<TariffResponse> updateTariff(
            @PathVariable Integer id,
            @Valid @RequestBody TariffRequest request) {
        System.out.println("✏️ Updating tariff ID: " + id);
        TariffResponse response = tariffManagementService.updateTariff(id, request);
        System.out.println("✅ Tariff updated successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<TariffResponse> getTariff(@PathVariable Integer id) {
        System.out.println("🔍 Fetching tariff ID: " + id);
        TariffResponse response = tariffManagementService.getTariffById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<List<TariffResponse>> getAllTariffs() {
        System.out.println("📋 Fetching all tariffs");
        List<TariffResponse> tariffs = tariffManagementService.getAllTariffs();
        System.out.println("✅ Found " + tariffs.size() + " tariff(s)");
        return ResponseEntity.ok(tariffs);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Admin')")
    public ResponseEntity<Void> deleteTariff(@PathVariable Integer id) {
        System.out.println("🗑️ Deleting tariff ID: " + id);
        tariffManagementService.deleteTariff(id);
        System.out.println("✅ Tariff deleted successfully");
        return ResponseEntity.noContent().build();
    }
}
