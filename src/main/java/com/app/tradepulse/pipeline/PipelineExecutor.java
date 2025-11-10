
package com.app.tradepulse.pipeline;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Executes the complete data pipeline for a specific country.
 * 
 * Run this for all countries using Maven:
 * mvn spring-boot:run -Dspring-boot.run.arguments="--pipeline.countries=USA,CHN,JPN,IND,BRA,CAN,KOR,AUS,MEX,IDN,SAU,TUR,CHE,SGP,NOR,THA,PHL,EU,MYS,VNM"
 * 
 * Or run for a single country:
 * mvn spring-boot:run -Dspring-boot.run.arguments="--pipeline.countries=USA"
 */
@Component
public class PipelineExecutor implements CommandLineRunner {

    // ...existing code...

    @Override
    public void run(String... args) throws Exception {
        // ...existing code...
        
        // Example: Run for specific countries (remove year parameter)
        // executePipeline("USA", null);  // null for latest year
        // executePipeline("CHN", null);
        // executePipeline("SGP", null);
    }

    /**
     * Executes the complete pipeline for a given country.
     * @param countryCode ISO country code (e.g., USA, CHN, SGP)
     * @param year Optional year (null for latest available year)
     */
    public void executePipeline(String countryCode, Integer year) {
        // ...existing code...
        
        try {
            // Step 1: Scrape data (use null for latest year)
            logger.info("Step 1: Scraping tariff data for {} (year: {})", 
                countryCode, year != null ? year : "latest");
            String csvFilename = witsScraper.scrapeTariffData(countryCode, year);
            logger.info("Scraping completed. CSV file: {}", csvFilename);
            
            // ...existing code...
            
        } catch (Exception e) {
            // ...existing code...
        }
    }
}