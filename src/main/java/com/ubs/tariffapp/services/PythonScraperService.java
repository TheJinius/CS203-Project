package com.ubs.tariffapp.services;

import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
            // Get most recent year for this country from database
            String mostRecentYear = getMostRecentYearFromDatabase(countryCode);
            logger.info("Scraping data for country: {}, most recent year: {}", countryCode, mostRecentYear);
            
            // Attempt to scrape with retries
            String downloadedFileName = null;
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                logger.info("Scraping attempt {} for country {}", attempt, countryCode);
                
                downloadedFileName = runWitsDataScraper(countryCode, mostRecentYear);
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
            
            // Move file to test_data folder and process through pipeline
            return moveFileAndProcess(downloadedFileName, countryCode);
            
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
            // Build command with country code and year as arguments
            ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExecutable, 
                pythonScriptPath, 
                countryCode, 
                mostRecentYear,
                "--headless"
            );
            
            Map<String, String> env = processBuilder.environment();
            
            // Use the configured credentials from application properties
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
            
            // Set working directory to script directory
            Path scriptDir = Paths.get(pythonScriptPath).getParent();
            if (scriptDir != null && Files.exists(scriptDir)) {
                processBuilder.directory(scriptDir.toFile());
                logger.debug("Working directory set to: {}", scriptDir);
            }
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // Don't log credentials that might appear in output
                    if (!line.toLowerCase().contains("password") && !line.toLowerCase().contains("credential")) {
                        logger.debug("Web scraper output: {}", line);
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
     * Moves the scraped file to resources folder and processes it through the data pipeline
     */
    private boolean moveFileAndProcess(String fileName, String countryCode) {
        try {
            // Construct paths
            Path scriptDir = Paths.get(pythonScriptPath).getParent();
            Path sourceFile = scriptDir.resolve(fileName);
            Path targetDir = Paths.get("src/main/resources/data/test_data");
            Path targetFile = targetDir.resolve(fileName);
            
            // Ensure target directory exists
            Files.createDirectories(targetDir);
            
            // Move file
            if (Files.exists(sourceFile)) {
                Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Moved file {} to {}", sourceFile, targetFile);
                
                // Process the file through cleaning and loading pipeline
                return processScrapedFile(fileName, countryCode);
            } else {
                logger.error("Downloaded file not found: {}", sourceFile);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error moving and processing file {}: {}", fileName, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Processes the scraped file through HSDataCleaner and DataLoaderService
     */
    private boolean processScrapedFile(String fileName, String countryCode) {
        try {
            logger.info("Processing scraped file: {}", fileName);
            
            // Since HSDataCleaner.main() processes hardcoded files and creates cleaned files,
            // we'll run it and then look for the cleaned file it produces
            try {
                String[] args = {}; // HSDataCleaner.main doesn't use arguments
                HSDataCleaner.main(args);
                logger.info("HSDataCleaner processing completed");
            } catch (Exception e) {
                logger.error("Error running HSDataCleaner: {}", e.getMessage(), e);
                return false;
            }
            
            // The cleaned file name follows the pattern "clean_" + originalFileName
            // HSDataCleaner creates files with "clean_" prefix in /data/clean_data/
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
    
    /**
     * Gets the most recent year for tariff data - defaults to 2023
     */
    private String getMostRecentYearFromDatabase(String countryCode) {
        try {
            // Query to get the most recent year for a specific country
            String sql = "SELECT MAX(tariff_year) FROM tariff_schedule ts " +
                        "JOIN country c ON ts.reporter_id = c.country_id " +
                        "WHERE c.iso_code = ? OR c.country_id = ?";
            
            Integer maxYear = jdbcTemplate.queryForObject(sql, Integer.class, countryCode, countryCode);
            
            if (maxYear != null) {
                logger.info("Found most recent year {} for country {}", maxYear, countryCode);
                return String.valueOf(maxYear);
            } else {
                logger.info("No previous data found for country {}, defaulting to 2023", countryCode);
                return "2023"; // Default to 2023
            }
            
        } catch (Exception e) {
            logger.warn("Error querying most recent year for country {}: {}. Using default year 2023", 
                       countryCode, e.getMessage());
            return "2023"; // Default to 2023 on error
        }
    }
}