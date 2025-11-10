package com.ubs.tariffapp.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.ubs.tariffapp.App;
import com.ubs.tariffapp.services.PythonScraperService;

/**
 * Utility class to manually execute the scraper→cleaner→loader pipeline
 * 
 * Usage (single country):
 * mvn exec:java -Dexec.mainClass="com.ubs.tariffapp.utils.PipelineExecutor" -Dexec.args="USA 2023"
 * 
 * Usage (multiple countries):
 * mvn exec:java -Dexec.mainClass="com.ubs.tariffapp.utils.PipelineExecutor" -Dexec.args="USA,CHN,SGP,JPN 2023"
 * 
 * Usage (all default countries):
 * mvn exec:java -Dexec.mainClass="com.ubs.tariffapp.utils.PipelineExecutor" -Dexec.args="all 2023"
 * 
 * Arguments:
 * - Country code(s): Single code (USA), comma-separated (USA,CHN,SGP), or "all" for default set
 * - Year (e.g., 2023)
 * 
 * This will:
 * 1. Run Python web scraper for each specified country/year
 * 2. Run HSDataCleaner on each downloaded CSV
 * 3. Load cleaned data into database via DataLoaderService
 */
public class PipelineExecutor {
    
    // Default countries to scrape when "all" is specified
    private static final List<String> DEFAULT_COUNTRIES = Arrays.asList("USA", "CHN", "SGP", "JPN");
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("ERROR: Insufficient arguments");
            System.err.println("\nUsage: PipelineExecutor <country_code(s)> <year>");
            System.err.println("\nExamples:");
            System.err.println("  Single country:    PipelineExecutor USA 2023");
            System.err.println("  Multiple countries: PipelineExecutor USA,CHN,SGP 2023");
            System.err.println("  All defaults:      PipelineExecutor all 2023");
            System.err.println("\nSupported country codes:");
            System.err.println("  USA - United States");
            System.err.println("  CHN - China");
            System.err.println("  SGP - Singapore");
            System.err.println("  JPN - Japan");
            System.err.println("  DEU - Germany");
            System.err.println("  GBR - United Kingdom");
            System.err.println("  FRA - France");
            System.err.println("  And more... (see web_scraper.py for full list)");
            System.exit(1);
        }
        
        String countryInput = args[0].toUpperCase();
        String year = args[1];
        
        // Parse country codes
        List<String> countryCodes;
        if ("ALL".equals(countryInput)) {
            countryCodes = DEFAULT_COUNTRIES;
            System.out.println("Using default country set: " + String.join(", ", countryCodes));
        } else if (countryInput.contains(",")) {
            countryCodes = Arrays.stream(countryInput.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        } else {
            countryCodes = Arrays.asList(countryInput);
        }
        
        System.out.println("=".repeat(70));
        System.out.println("TARIFF DATA PIPELINE EXECUTOR");
        System.out.println("=".repeat(70));
        System.out.println("Countries:    " + String.join(", ", countryCodes));
        System.out.println("Year:         " + year);
        System.out.println("Total tasks:  " + countryCodes.size());
        System.out.println("=".repeat(70));
        System.out.println();
        
        ConfigurableApplicationContext context = null;
        
        try {
            // Initialize Spring context using the main App class
            System.out.println("[1/3] Initializing Spring Boot context...");
            context = SpringApplication.run(App.class, args);
            System.out.println("✓ Spring context initialized successfully");
            System.out.println();
            
            // Get the Python scraper service from Spring context
            System.out.println("[2/3] Retrieving scraper service...");
            PythonScraperService scraperService = context.getBean(PythonScraperService.class);
            System.out.println("✓ Scraper service retrieved");
            System.out.println();
            
            // Execute the full pipeline for each country
            System.out.println("[3/3] Executing pipeline for " + countryCodes.size() + " country(ies)...");
            System.out.println();
            
            int successCount = 0;
            int failureCount = 0;
            
            for (int i = 0; i < countryCodes.size(); i++) {
                String countryCode = countryCodes.get(i);
                
                System.out.println("-".repeat(70));
                System.out.println("Processing country " + (i + 1) + "/" + countryCodes.size() + ": " + countryCode);
                System.out.println("-".repeat(70));
                
                try {
                    boolean success = scraperService.scrapeAndProcessCountryData(countryCode);
                    
                    if (success) {
                        successCount++;
                        System.out.println("✓ " + countryCode + " completed successfully");
                    } else {
                        failureCount++;
                        System.err.println("✗ " + countryCode + " failed");
                    }
                    
                    // Add delay between countries to avoid overwhelming the data source
                    if (i < countryCodes.size() - 1) {
                        System.out.println("\nWaiting 10 seconds before next country...\n");
                        Thread.sleep(10000);
                    }
                    
                } catch (Exception e) {
                    failureCount++;
                    System.err.println("✗ Error processing " + countryCode + ": " + e.getMessage());
                    e.printStackTrace();
                }
                
                System.out.println();
            }
            
            // Print final results
            System.out.println("=".repeat(70));
            System.out.println("PIPELINE EXECUTION SUMMARY");
            System.out.println("=".repeat(70));
            System.out.println("Total countries processed: " + countryCodes.size());
            System.out.println("Successful:                " + successCount);
            System.out.println("Failed:                    " + failureCount);
            System.out.println("=".repeat(70));
            
            if (failureCount == 0) {
                System.out.println("✓✓✓ ALL PIPELINES COMPLETED SUCCESSFULLY ✓✓✓");
                System.out.println("=".repeat(70));
                System.out.println("Data for " + year + " has been scraped, cleaned, and loaded for:");
                countryCodes.forEach(code -> System.out.println("  - " + code));
                System.out.println("=".repeat(70));
                System.exit(0);
            } else {
                System.err.println("✗✗✗ SOME PIPELINES FAILED ✗✗✗");
                System.err.println("=".repeat(70));
                System.err.println("Check the error messages above for details.");
                System.err.println("Common issues:");
                System.err.println("  - WITS credentials not configured in application.properties");
                System.err.println("  - Python script path incorrect");
                System.err.println("  - Network connectivity issues");
                System.err.println("  - Database connection problems");
                System.err.println("=".repeat(70));
                System.exit(1);
            }
            
        } catch (Exception e) {
            System.err.println();
            System.err.println("=".repeat(70));
            System.err.println("✗✗✗ FATAL ERROR ✗✗✗");
            System.err.println("=".repeat(70));
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            System.err.println("Stack trace:");
            e.printStackTrace();
            System.err.println("=".repeat(70));
            System.exit(1);
            
        } finally {
            // Clean up Spring context
            if (context != null) {
                System.out.println("\nCleaning up resources...");
                context.close();
                System.out.println("✓ Cleanup completed");
            }
        }
    }
}