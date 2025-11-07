package com.ubs.tariffapp.services;

// import java.util.Map;
// import java.io.BufferedReader;
// import java.io.InputStreamReader;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.nio.file.StandardCopyOption;
// import java.util.concurrent.TimeUnit;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.stereotype.Service;

// import com.ubs.tariffapp.utils.HSDataCleaner;

// @Service
// public class PythonScraperService {
    
//     private static final Logger logger = LoggerFactory.getLogger(PythonScraperService.class);
//     private static final int MAX_RETRIES = 3;
//     private static final long TIMEOUT_MINUTES = 30;
    
//     @Value("${app.python.script.path:scripts/WitsDataScraper.py}")
//     private String pythonScriptPath;
    
//     @Value("${app.python.executable:python3}")
//     private String pythonExecutable;
    
//     @Value("${app.wits.username:}")
//     private String witsUsername;
    
//     @Value("${app.wits.password:}")
//     private String witsPassword;
    
//     @Value("${app.wits.api.key:}")
//     private String witsApiKey;

//     @Autowired
//     private DataLoaderService dataLoaderService;
    
//     @Autowired
//     private JdbcTemplate jdbcTemplate;
    
//     /**
//      * Main method to scrape data for a country and process it through the entire pipeline
//      * @param countryCode ISO country code (e.g., "USA", "CHN", "SGP")
//      * @return true if successful, false otherwise
//      */
//     public boolean scrapeAndProcessCountryData(String countryCode) {
//         try {
//             // Get most recent year for this country from database
//             String mostRecentYear = getMostRecentYearFromDatabase(countryCode);
//             logger.info("Scraping data for country: {}, most recent year: {}", countryCode, mostRecentYear);
            
//             // Attempt to scrape with retries
//             String downloadedFileName = null;
//             for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
//                 logger.info("Scraping attempt {} for country {}", attempt, countryCode);
                
//                 downloadedFileName = runWitsDataScraper(countryCode, mostRecentYear);
//                 if (downloadedFileName != null) {
//                     logger.info("Successfully scraped data on attempt {}: {}", attempt, downloadedFileName);
//                     break;
//                 }
                
//                 if (attempt < MAX_RETRIES) {
//                     logger.warn("Scraping attempt {} failed, retrying...", attempt);
//                     Thread.sleep(5000); // Wait 5 seconds before retry
//                 }
//             }
            
//             if (downloadedFileName == null) {
//                 logger.error("Failed to scrape data for country {} after {} attempts", countryCode, MAX_RETRIES);
//                 return false;
//             }
            
//             // Move file to test_data folder and process through pipeline
//             return moveFileAndProcess(downloadedFileName, countryCode);
            
//         } catch (Exception e) {
//             logger.error("Error during scraping process for country {}: {}", countryCode, e.getMessage(), e);
//             return false;
//         }
//     }
    
//     /**
//      * Executes the Python WitsDataScraper script with retry logic
//      */
//     private String runWitsDataScraper(String countryCode, String mostRecentYear) {
//         try {
//             // Build command with credentials as environment variables for security
//             ProcessBuilder processBuilder = new ProcessBuilder(
//                 pythonExecutable, 
//                 pythonScriptPath, 
//                 countryCode, 
//                 mostRecentYear
//             );
            
//             // Pass credentials as environment variables (more secure than command line args)
//             Map<String, String> env = processBuilder.environment();
//             if (!witsUsername.isEmpty()) {
//                 env.put("WITS_USERNAME", witsUsername);
//             }
//             if (!witsPassword.isEmpty()) {
//                 env.put("WITS_PASSWORD", witsPassword);
//             }
//             if (!witsApiKey.isEmpty()) {
//                 env.put("WITS_API_KEY", witsApiKey);
//             }
            
//             processBuilder.redirectErrorStream(true);
//             Process process = processBuilder.start();
            
//             StringBuilder output = new StringBuilder();
//             try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//                 String line;
//                 while ((line = reader.readLine()) != null) {
//                     output.append(line).append("\n");
//                     // Don't log credentials that might appear in output
//                     if (!line.toLowerCase().contains("password") && !line.toLowerCase().contains("credential")) {
//                         logger.debug("WitsDataScraper output: {}", line);
//                     }
//                 }
//             }
            
//             boolean completed = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
//             if (!completed) {
//                 process.destroyForcibly();
//                 logger.error("WitsDataScraper timed out after {} minutes", TIMEOUT_MINUTES);
//                 return null;
//             }
            
//             int exitCode = process.exitValue();
//             if (exitCode == 0) {
//                 // Extract filename from output (last line should contain the filename)
//                 String[] lines = output.toString().trim().split("\n");
//                 if (lines.length > 0) {
//                     String lastLine = lines[lines.length - 1].trim();
//                     if (lastLine.endsWith(".csv")) {
//                         return lastLine;
//                     }
//                 }
//                 logger.error("WitsDataScraper succeeded but didn't return a valid filename");
//                 return null;
//             } else {
//                 logger.error("WitsDataScraper failed with exit code {}: {}", exitCode, output.toString());
//                 return null;
//             }
            
//         } catch (Exception e) {
//             logger.error("Error running WitsDataScraper: {}", e.getMessage(), e);
//             return null;
//         }
//     }
    
//     /**
//      * Moves the scraped file to resources folder and processes it through the data pipeline
//      */
//     private boolean moveFileAndProcess(String fileName, String countryCode) {
//         try {
//             // Construct paths
//             Path scriptDir = Paths.get(pythonScriptPath).getParent();
//             Path sourceFile = scriptDir.resolve(fileName);
//             Path targetDir = Paths.get("src/main/resources/data/test_data");
//             Path targetFile = targetDir.resolve(fileName);
            
//             // Ensure target directory exists
//             Files.createDirectories(targetDir);
            
//             // Move file
//             if (Files.exists(sourceFile)) {
//                 Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
//                 logger.info("Moved file {} to {}", sourceFile, targetFile);
                
//                 // Process the file through cleaning and loading pipeline
//                 return processScrapedFile(fileName, countryCode);
//             } else {
//                 logger.error("Downloaded file not found: {}", sourceFile);
//                 return false;
//             }
            
//         } catch (Exception e) {
//             logger.error("Error moving and processing file {}: {}", fileName, e.getMessage(), e);
//             return false;
//         }
//     }
    
//     /**
//      * Processes the scraped file through HSDataCleaner and DataLoaderService
//      */
//     private boolean processScrapedFile(String fileName, String countryCode) {
//         try {
//             logger.info("Processing scraped file: {}", fileName);
            
//             // Clean the data using HSDataCleaner
//             String cleanedFileName = HSDataCleaner.cleanDataFile(fileName);
//             if (cleanedFileName == null) {
//                 logger.error("Failed to clean data file: {}", fileName);
//                 return false;
//             }
            
//             // Load the cleaned data into database
//             dataLoaderService.loadCleanedData(cleanedFileName);
            
//             logger.info("Successfully processed scraped data for country: {}", countryCode);
//             return true;
            
//         } catch (Exception e) {
//             logger.error("Error processing scraped file {}: {}", fileName, e.getMessage(), e);
//             return false;
//         }
//     }
    
//     /**
//      * Queries database to find the most recent year of tariff data for a country
//      */
//     private String getMostRecentYearFromDatabase(String countryCode) {
//         try {
//             // Query to get the most recent year for a specific country
//             String sql = "SELECT MAX(tariff_year) FROM tariff_schedule ts " +
//                         "JOIN country c ON ts.reporter_id = c.country_id " +
//                         "WHERE c.iso_code = ? OR c.country_id = ?";
            
//             Integer maxYear = jdbcTemplate.queryForObject(sql, Integer.class, countryCode, countryCode);
            
//             if (maxYear != null) {
//                 logger.info("Found most recent year {} for country {}", maxYear, countryCode);
//                 return String.valueOf(maxYear);
//             } else {
//                 logger.info("No previous data found for country {}, defaulting to 2020", countryCode);
//                 return "2020"; // Default starting year
//             }
            
//         } catch (Exception e) {
//             logger.warn("Error querying most recent year for country {}: {}. Using default year 2020", 
//                        countryCode, e.getMessage());
//             return "2020";
//         }
//     }
// }