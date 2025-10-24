package com.ubs.tariffapp.services;

import static com.ubs.tariffapp.utils.HSDataCleaner.parseCSVLine;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.ubs.tariffapp.models.Country;
import com.ubs.tariffapp.models.DutyType;
import com.ubs.tariffapp.models.DutyTypeId;
import com.ubs.tariffapp.models.Product;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.duty.AdValoremDuty;
import com.ubs.tariffapp.models.duty.CombinedDuty;
import com.ubs.tariffapp.models.duty.OtherDuty;
import com.ubs.tariffapp.models.duty.SpecificDuty;
import com.ubs.tariffapp.repositories.CountryRepository;
import com.ubs.tariffapp.repositories.DutyTypeRepository;
import com.ubs.tariffapp.repositories.ProductRepository;
import com.ubs.tariffapp.repositories.TariffScheduleRepository;
import com.ubs.tariffapp.repositories.duty.AdValoremDutyRepository;
import com.ubs.tariffapp.repositories.duty.CombinedDutyRepository;
import com.ubs.tariffapp.repositories.duty.OtherDutyRepository;
import com.ubs.tariffapp.repositories.duty.SpecificDutyRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class DataLoaderService {

    @PersistenceContext
    private EntityManager entityManager;

    private static final int BATCH_SIZE = 100; // Process in batches

    @Autowired
    private CountryRepository countryRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private DutyTypeRepository dutyTypeRepository;
    
    @Autowired
    private TariffScheduleRepository tariffScheduleRepository;
    
    @Autowired
    private AdValoremDutyRepository adValoremDutyRepository;
    
    @Autowired
    private SpecificDutyRepository specificDutyRepository;
    
    @Autowired
    private CombinedDutyRepository combinedDutyRepository;
    
    @Autowired
    private OtherDutyRepository otherDutyRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;
    
// ====================================================================================================================
    // Main method to load cleaned data
    @Transactional
    public void loadCleanedData(String fileName) {
        System.out.println("Attempting to load file: " + fileName);
        InputStream inputStream = getClass().getResourceAsStream("/data/clean_data/" + fileName);
        if (inputStream == null) {
            throw new RuntimeException("Cleaned data file not found: " + fileName);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String headerLine = reader.readLine(); // Skip header
            if (headerLine == null) {
                throw new RuntimeException("File is empty or invalid");
            }

            System.out.println("Header: " + headerLine);

            String line;
            int processedCount = 0;
            int insertedCount = 0;
            int skippedCount = 0;
            List<String> errors = new ArrayList<>();
            List<String> batch = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                batch.add(line);
                
                // Process in batches for better performance
                if (batch.size() >= BATCH_SIZE) {
                    var results = processBatchSafely(batch, processedCount);
                    processedCount += batch.size();
                    insertedCount += results[0];
                    skippedCount += results[1];
                    
                    batch.clear();
                    
                    // Progress reporting every 1000 records
                    if (processedCount % 1000 == 0) {
                        System.out.println("Processed: " + processedCount + " rows (Inserted: " + insertedCount + ", Skipped: " + skippedCount + ")");
                    }
                }
            }

            // Process remaining batch
            if (!batch.isEmpty()) {
                var results = processBatchSafely(batch, processedCount);
                processedCount += batch.size();
                insertedCount += results[0];
                skippedCount += results[1];
            }

            System.out.println("Data loading completed. Total rows processed: " + processedCount);
            System.out.println("Records inserted: " + insertedCount);
            System.out.println("Records skipped (duplicates): " + skippedCount);
            if (!errors.isEmpty()) {
                System.err.println("Errors encountered: " + errors.size());
                errors.forEach(System.err::println);
            }

            // Print database summary after loading
            printDatabaseSummary();
            System.out.println("Data loading finished successfully.");

        } catch (Exception e) {
            throw new RuntimeException("Failed to load data from file: " + fileName, e);
        }
    }

    public int[] processBatchSafely(List<String> batch, int startRowNumber) {
        final int[] results = new int[2]; // [inserted, skipped]
        
        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    for (int i = 0; i < batch.size(); i++) {
                        try {
                            boolean wasInserted = processDataRowFast(batch.get(i), startRowNumber + i);
                            if (wasInserted) {
                                results[0]++; // inserted
                            } else {
                                results[1]++; // skipped
                            }
                            
                            // Force flush every 10 records to catch constraint violations early
                            if (i > 0 && i % 10 == 0) {
                                entityManager.flush();
                            }
                            
                        } catch (DataIntegrityViolationException e) {
                            if (e.getMessage() != null && 
                                (e.getMessage().contains("unique_tariff_business_key") || 
                                 e.getMessage().contains("duplicate key"))) {
                                results[1]++; // skipped due to duplicate
                            } else {
                                throw e; // Re-throw other integrity violations
                            }
                        } catch (Exception e) {
                            System.err.println("Error processing row " + (startRowNumber + i + 1) + ": " + e.getMessage());
                            results[1]++; // Count errors as skipped
                        }
                    }
                    // Final flush
                    entityManager.flush();
                }
            });
        } catch (Exception e) {
            // If the entire batch fails due to constraint violations, process individually
            System.out.println("Batch failed, processing individually...");
            return processIndividually(batch, startRowNumber);
        }
        
        return results;
    }

    private int[] processIndividually(List<String> batch, int startRowNumber) {
        int insertedCount = 0;
        int skippedCount = 0;
        
        for (int i = 0; i < batch.size(); i++) {
            final int rowIndex = i;
            final String line = batch.get(i);
            
            try {
                Boolean wasInserted = transactionTemplate.execute(status -> {
                    try {
                        return processDataRowFast(line, startRowNumber + rowIndex);
                    } catch (DataIntegrityViolationException e) {
                        if (e.getMessage() != null && 
                            (e.getMessage().contains("unique_tariff_business_key") || 
                             e.getMessage().contains("duplicate key"))) {
                            return false; // Duplicate
                        } else {
                            throw e;
                        }
                    }
                });
                
                if (Boolean.TRUE.equals(wasInserted)) {
                    insertedCount++;
                } else {
                    skippedCount++;
                }
                
            } catch (Exception e) {
                System.err.println("Error processing row " + (startRowNumber + i + 1) + ": " + e.getMessage());
                skippedCount++;
            }
        }
        
        return new int[]{insertedCount, skippedCount};
    }

    private boolean processDataRowFast(String line, int rowNumber) {
        String[] columns = parseCSVLine(line);

        if (columns.length < 25) {
            System.err.println("Skipping row " + rowNumber + " due to insufficient columns. Expected 25, got " + columns.length);
            return false;
        }

        // Extract data from original columns (0-15)
        String reporterCode = columns[0].trim();
        String reporterName = removeQuotes(columns[1].trim());
        String partnerCode = columns[2].trim();
        String partnerName = removeQuotes(columns[3].trim());
        int year = Integer.parseInt(columns[4].trim());
        String tlCode = columns[5].trim();
        String tlsSuffix = columns[6].trim();
        String dutyTypeCode = columns[7].trim();
        String dutyCode = columns[8].trim();
        String avDutyRate = columns[9].trim();
        String specificDutyRate = removeQuotes(columns[10].trim());
        String description = removeQuotes(columns[11].trim());
        String dutyTypeDescription = removeQuotes(columns[12].trim());
        String dutyNature = columns[13].trim();
        String avMethod = columns[14].trim();
        String note = removeQuotes(columns[15].trim());
        
        // Extract data from new columns (16-24)
        String reporterISOCode = columns[16].trim();
        String partnerISOCode = columns[17].trim();
        String industry = columns[18].trim();
        String cleanedDutyType = columns[19].trim(); // AD_VALOREM, SPECIFIC, MIXED, CONDITIONAL
        String standardizedAVRate = columns[20].trim();
        String specificDutyAmount = columns[21].trim();
        String currency = columns[22].trim();
        String unit = columns[23].trim();
        String originalSpecificDuty = removeQuotes(columns[24].trim());

        try {
            // Create or get entities
            Country reporter = createOrGetCountry(reporterCode, reporterName, reporterISOCode);
            Country partner = createOrGetCountry(partnerCode, partnerName, partnerISOCode);
            Product product = createOrGetProduct(tlCode, description);
            DutyType dutyType = createOrGetDutyType(dutyTypeCode, dutyCode, dutyTypeDescription);

            // Normalize tlsSuffix for consistency
            String normalizedTlsSuffix = (tlsSuffix.isEmpty() || tlsSuffix.equals("0")) ? null : tlsSuffix;

            // Create tariff schedule entry
            TariffSchedule tariffSchedule = new TariffSchedule();
            tariffSchedule.setReporter(reporter);
            tariffSchedule.setPartner(partner);
            tariffSchedule.setProduct(product);
            tariffSchedule.setDutyType(dutyType);
            tariffSchedule.setTariffYear(year);
            tariffSchedule.setNote(note);
            tariffSchedule.setTlsSuffix(normalizedTlsSuffix);

            // Attempt to save - let constraint violations be caught
            tariffSchedule = tariffScheduleRepository.save(tariffSchedule);

            // Create appropriate duty based on cleaned duty type analysis
            createDutyEntryFromCleanedData(tariffSchedule, cleanedDutyType, avDutyRate, specificDutyRate, 
                                         standardizedAVRate, specificDutyAmount, currency, unit, 
                                         originalSpecificDuty, dutyNature, avMethod);
            
            return true; // Successfully inserted
            
        } catch (DataIntegrityViolationException e) {
            // Handle duplicate key violations silently
            if (e.getMessage() != null && 
                (e.getMessage().contains("unique_tariff_business_key") || 
                 e.getMessage().contains("duplicate key"))) {
                return false; // Skip duplicate
            } else {
                throw e; // Re-throw other constraint violations
            }
        }
    }

    private String removeQuotes(String value) {
        if (value == null) return "";
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private void createDutyEntryFromCleanedData(TariffSchedule tariffSchedule, String cleanedDutyType,
                                              String avDutyRate, String specificDutyRate,
                                              String standardizedAVRate, String specificDutyAmount,
                                              String currency, String unit, String originalSpecificDuty,
                                              String dutyNature, String avMethod) {
        
        switch (cleanedDutyType) {
            case "AD_VALOREM":
                createAdValoremDutyFromCleaned(tariffSchedule, avDutyRate, standardizedAVRate, dutyNature);
                break;
                
            case "SPECIFIC":
                createSpecificDutyFromCleaned(tariffSchedule, specificDutyRate, specificDutyAmount, 
                                            currency, unit, originalSpecificDuty, dutyNature);
                break;
                
            case "MIXED":
                createCombinedDutyFromCleaned(tariffSchedule, avMethod, avDutyRate, specificDutyRate,
                                            standardizedAVRate, specificDutyAmount, currency, unit,
                                            originalSpecificDuty, dutyNature);
                break;
                
            case "CONDITIONAL":
            default:
                createOtherDutyFromCleaned(tariffSchedule, specificDutyRate, originalSpecificDuty, 
                                         avDutyRate, dutyNature, avMethod);
                break;
        }
    }

    private void createAdValoremDutyFromCleaned(TariffSchedule tariffSchedule, String avDutyRate, 
                                              String standardizedAVRate, String dutyNature) {
        try {
            // Use standardized rate if available, otherwise fall back to original
            String rateToUse = !standardizedAVRate.isEmpty() && !standardizedAVRate.equals("") 
                             ? standardizedAVRate : avDutyRate;
            
            // Create duty even if rate is 0 (duty-free is still a valid tariff)
            if (!rateToUse.isEmpty()) {
                BigDecimal rate = new BigDecimal(rateToUse);
                AdValoremDuty duty = new AdValoremDuty();
                duty.setTariffSchedule(tariffSchedule);
                duty.setRatePercent(rate);
                duty.setDutyNature(dutyNature);
                adValoremDutyRepository.save(duty);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid ad valorem rate: " + standardizedAVRate + " or " + avDutyRate);
        }
    }

    private void createSpecificDutyFromCleaned(TariffSchedule tariffSchedule, String specificDutyRate,
                                             String specificDutyAmount, String currency, String unit,
                                             String originalSpecificDuty, String dutyNature) {
        SpecificDuty duty = new SpecificDuty();
        duty.setTariffSchedule(tariffSchedule);
        duty.setDutyNature(dutyNature);
        
        // Use original specific duty text if available, otherwise the raw specific duty rate
        String rawText = !originalSpecificDuty.isEmpty() ? originalSpecificDuty : specificDutyRate;
        duty.setSpecificDutyRateRaw(rawText);
        
        // Set parsed amount if available (including 0 amounts - duty-free specific duties)
        if (!specificDutyAmount.isEmpty()) {
            try {
                duty.setAmount(new BigDecimal(specificDutyAmount));
            } catch (NumberFormatException e) {
                System.err.println("Invalid specific duty amount: " + specificDutyAmount);
            }
        }
        
        // Set unit if available
        if (!unit.isEmpty()) {
            duty.setUnit(unit);
            // For now, set multiplier to 1 as default - could be enhanced with more parsing
            duty.setMultiplier(1);
        }
        
        specificDutyRepository.save(duty);
    }

    private void createCombinedDutyFromCleaned(TariffSchedule tariffSchedule, String avMethod,
                                             String avDutyRate, String specificDutyRate,
                                             String standardizedAVRate, String specificDutyAmount,
                                             String currency, String unit, String originalSpecificDuty,
                                             String dutyNature) {
        CombinedDuty duty = new CombinedDuty();
        duty.setTariffSchedule(tariffSchedule);
        duty.setDutyNature(dutyNature);
        
        // Determine if it's Mixed (M) or Compound (C) based on avMethod
        if ("M".equals(avMethod)) {
            duty.setMixedOrCompound("M");
        } else if ("C".equals(avMethod)) {
            duty.setMixedOrCompound("C");
        } else {
            duty.setMixedOrCompound("C"); // Default to compound
        }
        
        // Set ad valorem component (including 0 rates)
        try {
            String rateToUse = !standardizedAVRate.isEmpty() && !standardizedAVRate.equals("") 
                             ? standardizedAVRate : avDutyRate;
            
            if (!rateToUse.isEmpty()) {
                duty.setRatePercent(new BigDecimal(rateToUse));
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid ad valorem rate in combined duty: " + standardizedAVRate);
        }
        
        // Set specific component (including 0 amounts)
        if (!specificDutyAmount.isEmpty()) {
            try {
                duty.setAmount(new BigDecimal(specificDutyAmount));
            } catch (NumberFormatException e) {
                System.err.println("Invalid specific amount in combined duty: " + specificDutyAmount);
            }
        }
        
        // Set unit information
        if (!unit.isEmpty()) {
            duty.setUnit(unit);
            duty.setMultiplier(1); // Default multiplier
        }
        
        // Store original specific duty text
        String rawText = !originalSpecificDuty.isEmpty() ? originalSpecificDuty : specificDutyRate;
        duty.setSpecificDutyRateRaw(rawText);
        
        combinedDutyRepository.save(duty);
    }

    private void createOtherDutyFromCleaned(TariffSchedule tariffSchedule, String specificDutyRate,
                                          String originalSpecificDuty, String avDutyRate,
                                          String dutyNature, String avMethod) {
        OtherDuty duty = new OtherDuty();
        duty.setTariffSchedule(tariffSchedule);
        duty.setDutyNature(dutyNature);
        
        // Determine the raw text to store
        String rawText = "";
        if (!originalSpecificDuty.isEmpty()) {
            rawText = originalSpecificDuty;
        } else if (!specificDutyRate.isEmpty()) {
            rawText = specificDutyRate;
        } else if (!avDutyRate.isEmpty()) {
            rawText = avDutyRate;
        }
        
        duty.setRawText(rawText);
        
        // Determine if it's computable based on the content
        boolean isComputable = false;
        if (!rawText.isEmpty()) {
            String lowerText = rawText.toLowerCase();
            // Consider it non-computable if it contains references to other sections or complex conditions
            if (lowerText.contains("see ") || lowerText.contains("heading ") || 
                lowerText.contains("whichever") || lowerText.contains("subject to") ||
                lowerText.contains("provided") || lowerText.contains("except")) {
                isComputable = false;
            } else {
                // If it contains numbers and basic duty terms, it might be computable
                isComputable = rawText.matches(".*\\d.*") && 
                              (lowerText.contains("%") || lowerText.contains("per") || 
                               lowerText.contains("$") || lowerText.contains("cent"));
            }
        }
        
        duty.setIsComputable(isComputable);
        
        otherDutyRepository.save(duty);
    }

    private Country createOrGetCountry(String countryId, String countryName, String isoCode) {
        Optional<Country> existing = countryRepository.findById(countryId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Country country = new Country();
        country.setCountryId(countryId);
        country.setCountryName(countryName);
        // Use countryId (which should be WITS code) to derive ISO code
        country.setIsoCode(isoCode);
        return countryRepository.save(country);
    }

    private Product createOrGetProduct(String tlCode, String description) {
        Optional<Product> existing = productRepository.findById(tlCode);
        if (existing.isPresent()) {
            return existing.get();
        }

        Product product = new Product();
        product.setTlCode(tlCode);
        product.setDescription(description);
        product.setDigits(tlCode.length());
        return productRepository.save(product);
    }

    private DutyType createOrGetDutyType(String dutyTypeCode, String dutyCode, String description) {
        DutyTypeId dutyTypeId = new DutyTypeId(dutyTypeCode, dutyCode);
        Optional<DutyType> existing = dutyTypeRepository.findById(dutyTypeId);
        if (existing.isPresent()) {
            return existing.get();
        }

        DutyType dutyType = new DutyType();
        dutyType.setId(dutyTypeId);
        dutyType.setDutyTypeDescription(description);
        return dutyTypeRepository.save(dutyType);
    }

    // Helper methods for testing
    public long getTariffScheduleCount() {
        return tariffScheduleRepository.count();
    }

    public long getCountryCount() {
        return countryRepository.count();
    }

    public long getProductCount() {
        return productRepository.count();
    }

    public long getDutyTypeCount() {
        return dutyTypeRepository.count();
    }

    public long getAdValoremDutyCount() {
        return adValoremDutyRepository.count();
    }

    public long getSpecificDutyCount() {
        return specificDutyRepository.count();
    }

    public long getCombinedDutyCount() {
        return combinedDutyRepository.count();
    }

    public long getOtherDutyCount() {
        return otherDutyRepository.count();
    }

    public void printDatabaseSummary() {
        System.out.println("=== DATABASE SUMMARY ===");
        System.out.println("Tariff Schedules: " + getTariffScheduleCount());
        System.out.println("Countries: " + getCountryCount());
        System.out.println("Products: " + getProductCount());
        System.out.println("Duty Types: " + getDutyTypeCount());
        System.out.println("Ad Valorem Duties: " + getAdValoremDutyCount());
        System.out.println("Specific Duties: " + getSpecificDutyCount());
        System.out.println("Combined Duties: " + getCombinedDutyCount());
        System.out.println("Other Duties: " + getOtherDutyCount());
        System.out.println("========================");
    }
}