package com.ubs.tariffapp.services;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledScrapingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledScrapingService.class);
    
    @Autowired
    private PythonScraperService pythonScraperService;
    
    // List of countries to scrape automatically (using ISO codes or country identifiers)
    private final List<String> COUNTRIES_TO_SCRAPE = Arrays.asList("USA", "CHN", "SGP", "JPN");
    
    /**
     * Scheduled method that runs on the 1st day of every month at 2 AM
     * Cron expression: "0 0 2 1 * ?" = second minute hour day month year
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void performMonthlyDataScraping() {
        logger.info("Starting monthly data scraping process");
        
        int successCount = 0;
        int failureCount = 0;
        
        for (String countryCode : COUNTRIES_TO_SCRAPE) {
            try {
                logger.info("Processing country: {}", countryCode);
                boolean success = pythonScraperService.scrapeAndProcessCountryData(countryCode);
                
                if (success) {
                    successCount++;
                    logger.info("Successfully processed data for country: {}", countryCode);
                } else {
                    failureCount++;
                    logger.error("Failed to process data for country: {}", countryCode);
                }
                
                // Add delay between countries to avoid overwhelming the data source
                Thread.sleep(10000); // 10 seconds
                
            } catch (Exception e) {
                failureCount++;
                logger.error("Error processing country {}: {}", countryCode, e.getMessage(), e);
            }
        }
        
        logger.info("Monthly scraping completed. Success: {}, Failures: {}", successCount, failureCount);
    }
    
    /**
     * Manual trigger for testing purposes - processes all configured countries
     */
    public void triggerManualScraping() {
        logger.info("Manual scraping triggered for all countries");
        performMonthlyDataScraping();
    }
    
    /**
     * Scrape data for a specific country manually
     * @param countryCode Country code to scrape
     * @return true if successful, false otherwise
     */
    public boolean scrapeSpecificCountry(String countryCode) {
        logger.info("Manual scraping triggered for country: {}", countryCode);
        return pythonScraperService.scrapeAndProcessCountryData(countryCode);
    }
}