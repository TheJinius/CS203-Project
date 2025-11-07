package com.ubs.tariffapp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ubs.tariffapp.services.ScheduledScrapingService;

@RestController
@RequestMapping("/api/scraping")
public class ScrapingController {
    
    @Autowired
    private ScheduledScrapingService scheduledScrapingService;
    
    /**
     * Trigger manual scraping for all configured countries
     * POST /api/scraping/trigger
     */
    @PostMapping("/trigger")
    public ResponseEntity<String> triggerManualScraping() {
        try {
            scheduledScrapingService.triggerManualScraping();
            return ResponseEntity.ok("Manual scraping triggered successfully for all countries");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error triggering scraping: " + e.getMessage());
        }
    }
    
    /**
     * Trigger scraping for a specific country
     * POST /api/scraping/trigger/{countryCode}
     */
    @PostMapping("/trigger/{countryCode}")
    public ResponseEntity<String> triggerCountrySpecificScraping(@PathVariable String countryCode) {
        try {
            boolean success = scheduledScrapingService.scrapeSpecificCountry(countryCode);
            if (success) {
                return ResponseEntity.ok("Scraping completed successfully for " + countryCode);
            } else {
                return ResponseEntity.badRequest()
                    .body("Scraping failed for " + countryCode);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error scraping " + countryCode + ": " + e.getMessage());
        }
    }
    
    /**
     * Health check endpoint to verify scraping service is available
     * GET /api/scraping/status
     */
    @GetMapping("/status")
    public ResponseEntity<String> getScrapingServiceStatus() {
        return ResponseEntity.ok("Scraping service is operational");
    }
}