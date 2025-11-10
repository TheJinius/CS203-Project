package com.ubs.tariffapp.services;

import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.ubs.tariffapp.utils.HSDataCleaner;

@Service
@ConditionalOnProperty(value = "app.python.script.path", matchIfMissing = false)
public class PythonScraperService {
    
    private static final Logger logger = LoggerFactory.getLogger(PythonScraperService.class);
    private static final int MAX_RETRIES = 3;
    private static final long TIMEOUT_MINUTES = 30;
    
    @Value("${app.python.script.path:scripts/python/web_scraper.py}")
    private String pythonScriptPath;
    
    @Value("${app.python.executable:python3}")
    private String pythonExecutable;
    
    @Value("${app.wits.username:}")
    private String witsUsername;
    
    @Value("${app.wits.password:}")
    private String witsPassword;
    
    @Value("${app.wits.api.key:}")
    private String witsApiKey;

    private final DataLoaderService dataLoaderService;
    private final JdbcTemplate jdbcTemplate;
    
    // Constructor injection
    public PythonScraperService(DataLoaderService dataLoaderService, JdbcTemplate jdbcTemplate) {
        this.dataLoaderService = dataLoaderService;
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Main method to scrape data for a country and process it through the entire pipeline
     * @param countryCode ISO country code (e.g., "USA", "CHN", "SGP")
     * @return true if successful, false otherwise
     */
    public boolean scrapeAndProcessCountryData(String countryCode) {
        try {
            // Default to 2023 for scraping
            String targetYear = "2023";
            logger.info("Scraping data for country: {}, year: {}", countryCode, targetYear);
            
            // Attempt to scrape with retries
            String downloadedFileName = null;
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                logger.info("Scraping attempt {} for country {}", attempt, countryCode);
                
                downloadedFileName = runWitsDataScraper(countryCode, targetYear);
                if (downloadedFileName != null) {
                    logger.info("Successfully scraped data on attempt {}: {}", attempt, downloadedFileName);
                    break;
                }
                
                if (attempt < MAX_RETRIES) {
                    logger.warn("Scraping attempt {} failed, retrying...", attempt);
                    Thread.sleep(5000); // Wait 5 seconds before retry
                }
            }
            
            if (downloadedFileName == null) {
                logger.error("Failed to scrape data for country {} after {} attempts", countryCode, MAX_RETRIES);
                return false;
            }
            
            // Python script already moved file to resources, just process it
            return processScrapedFile(downloadedFileName, countryCode);
            
        } catch (Exception e) {
            logger.error("Error during scraping process for country {}: {}", countryCode, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Executes the Python web_scraper script with retry logic
     */
    private String runWitsDataScraper(String countryCode, String mostRecentYear) {
        try {
            // Get the absolute path to the script
            Path scriptPath = Paths.get(pythonScriptPath).toAbsolutePath();
            
            if (!Files.exists(scriptPath)) {
                logger.error("Python script not found at: {}", scriptPath);
                return null;
            }
            
            logger.info("Using Python script: {}", scriptPath);
            
            // Build command with absolute script path
            // Add -u flag to run Python in unbuffered mode for real-time output
            ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExecutable,
                "-u",  // Unbuffered mode - see output in real-time!
                scriptPath.toString(),
                countryCode, 
                mostRecentYear,
                "--headless"
            );
            
            Map<String, String> env = processBuilder.environment();
            
            if (witsUsername != null && !witsUsername.isEmpty()) {
                env.put("WITS_USERNAME", witsUsername);
                logger.debug("WITS_USERNAME environment variable set for scraper from properties");
            } else {
                logger.error("WITS_USERNAME not configured in application properties");
                return null;
            }
            
            if (witsPassword != null && !witsPassword.isEmpty()) {
                env.put("WITS_PASSWORD", witsPassword);
                logger.debug("WITS_PASSWORD environment variable set for scraper from properties");
            } else {
                logger.error("WITS_PASSWORD not configured in application properties");
                return null;
            }
            
            // Also pass API key if available
            if (witsApiKey != null && !witsApiKey.isEmpty()) {
                env.put("WITS_API_KEY", witsApiKey);
            }
            
            // Don't set working directory - let Python script handle paths
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // Log ALL output in real-time (including password-safe lines)
                    if (!line.toLowerCase().contains("password") && !line.toLowerCase().contains("credential")) {
                        // Log to console AND logger
                        System.out.println("Python: " + line);  // Console output
                        logger.info("Python: {}", line);         // Logger output
                    }
                }
            }
            
            boolean completed = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!completed) {
                process.destroyForcibly();
                logger.error("Web scraper timed out after {} minutes", TIMEOUT_MINUTES);
                return null;
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                // Extract filename from output (look for SUCCESS: filename pattern)
                String[] lines = output.toString().trim().split("\n");
                for (String line : lines) {
                    if (line.startsWith("SUCCESS: ") && line.endsWith(".csv")) {
                        String filename = line.substring("SUCCESS: ".length()).trim();
                        logger.info("Web scraper returned filename: {}", filename);
                        return filename;
                    }
                }
                logger.error("Web scraper succeeded but didn't return a valid filename");
                logger.debug("Full output: {}", output.toString());
                return null;
            } else {
                logger.error("Web scraper failed with exit code {}: {}", exitCode, output.toString());
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Error running web scraper: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Processes the scraped file through HSDataCleaner and DataLoaderService
     * File is already in src/main/resources/data/test_data/ from Python script
     */
    private boolean processScrapedFile(String fileName, String countryCode) {
        try {
            logger.info("Processing scraped file: {}", fileName);
            
            // Verify file exists in resources folder
            Path targetFile = Paths.get("src/main/resources/data/test_data").resolve(fileName);
            if (!Files.exists(targetFile)) {
                logger.error("Scraped file not found in resources folder: {}", targetFile);
                return false;
            }
            
            // Run HSDataCleaner with the actual file name as argument
            try {
                String[] args = {fileName};
                HSDataCleaner.main(args);
                logger.info("HSDataCleaner processing completed for file: {}", fileName);
                
                // Delete the original scraped file after cleaning
                Path originalFile = Paths.get("src/main/resources/data/test_data").resolve(fileName);
                try {
                    Files.delete(originalFile);
                    logger.info("Deleted original scraped file: {}", fileName);
                } catch (Exception e) {
                    logger.warn("Could not delete original file {}: {}", fileName, e.getMessage());
                }
                
            } catch (Exception e) {
                logger.error("Error running HSDataCleaner: {}", e.getMessage(), e);
                return false;
            }
            
            // The cleaned file name follows the pattern "clean_" + originalFileName
            String cleanedFileName = "clean_" + fileName;
            
            // Load the cleaned data into database
            dataLoaderService.loadCleanedData(cleanedFileName);
            
            logger.info("Successfully processed scraped data for country: {}", countryCode);
            return true;
            
        } catch (Exception e) {
            logger.error("Error processing scraped file {}: {}", fileName, e.getMessage(), e);
            return false;
        }
    }
}