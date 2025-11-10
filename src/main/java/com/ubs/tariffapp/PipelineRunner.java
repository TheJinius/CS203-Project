package com.ubs.tariffapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.ubs.tariffapp.services.PythonScraperService;

/**
 * Command-line runner for the scraper→cleaner→loader pipeline
 * 
 * Usage:
 * mvn exec:java -Dexec.mainClass="com.ubs.tariffapp.PipelineRunner" -Dexec.args="USA,CHN,SGP 2023"
 * 
 * Arguments:
 * - Country codes (comma-separated, e.g., USA,CHN,SGP,JPN)
 * - Year (e.g., 2023)
 */
@SpringBootApplication
public class PipelineRunner {
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: PipelineRunner <country_codes> <year>");
            System.err.println("Example: PipelineRunner USA,CHN,SGP 2023");
            System.err.println("Example: PipelineRunner USA 2023");
            System.err.println("\nSupported country codes: USA, CHN, SGP, JPN, DEU, GBR, FRA, etc.");
            System.exit(1);
        }
        
        String[] countryCodes = args[0].split(",");
        String year = args[1];
        
        System.out.println("=".repeat(60));
        System.out.println("Starting Tariff Data Pipeline");
        System.out.println("Countries: " + String.join(", ", countryCodes));
        System.out.println("Year: " + year);
        System.out.println("=".repeat(60));
        
        ConfigurableApplicationContext context = null;
        int successCount = 0;
        int failCount = 0;
        
        try {
            context = SpringApplication.run(PipelineRunner.class, args);
            PythonScraperService scraperService = context.getBean(PythonScraperService.class);
            
            for (String countryCode : countryCodes) {
                String country = countryCode.trim().toUpperCase();
                
                System.out.println("\n" + "-".repeat(60));
                System.out.println("Processing: " + country);
                System.out.println("-".repeat(60));
                
                try {
                    boolean success = scraperService.scrapeAndProcessCountryData(country);
                    
                    if (success) {
                        successCount++;
                        System.out.println("✓ " + country + " completed successfully");
                    } else {
                        failCount++;
                        System.err.println("✗ " + country + " failed");
                    }
                } catch (Exception e) {
                    failCount++;
                    System.err.println("✗ " + country + " failed with error: " + e.getMessage());
                }
            }
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("Pipeline Summary:");
            System.out.println("  Total: " + countryCodes.length);
            System.out.println("  Success: " + successCount);
            System.out.println("  Failed: " + failCount);
            System.out.println("=".repeat(60));
            
            System.exit(failCount == 0 ? 0 : 1);
            
        } catch (Exception e) {
            System.err.println("\nFatal error running pipeline: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }
}