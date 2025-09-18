package com.ubs.tariffapp.services;

import com.ubs.tariffapp.models.*;
import com.ubs.tariffapp.models.duty.*;
import com.ubs.tariffapp.repositories.*;
import com.ubs.tariffapp.repositories.duty.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DataLoaderService {

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

    @Transactional
    public void loadCleanedData(String fileName) {
        try {
            InputStream inputStream = getClass().getResourceAsStream("/data/clean_data/" + fileName);
            if (inputStream == null) {
                throw new RuntimeException("Cleaned data file not found: " + fileName);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String headerLine = reader.readLine(); // Skip header
                if (headerLine == null) {
                    throw new RuntimeException("File is empty or invalid");
                }

                String line;
                int processedCount = 0;
                List<String> errors = new ArrayList<>();

                while ((line = reader.readLine()) != null) {
                    try {
                        processDataRow(line);
                        processedCount++;
                        
                        if (processedCount % 100 == 0) {
                            System.out.println("Processed " + processedCount + " rows");
                        }
                    } catch (Exception e) {
                        String error = "Error processing row " + (processedCount + 1) + ": " + e.getMessage();
                        errors.add(error);
                        System.err.println(error);
                    }
                }

                System.out.println("Data loading completed. Processed: " + processedCount + " rows");
                if (!errors.isEmpty()) {
                    System.err.println("Errors encountered: " + errors.size());
                    errors.forEach(System.err::println);
                }

            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load data from file: " + fileName, e);
        }
    }

    private void processDataRow(String line) {
        String[] columns = parseCsvLine(line);
        
        if (columns.length < 17) {
            throw new RuntimeException("Invalid row format - expected 17 columns, got " + columns.length);
        }

        // Extract data from columns based on your CSV format
        String reporterCode = columns[0].trim();
        String reporterName = columns[1].trim();
        String partnerCode = columns[2].trim();
        String partnerName = columns[3].trim();
        int year = Integer.parseInt(columns[4].trim());
        String tlCode = columns[5].trim(); // TL (HS Code)
        String tlsSuffix = columns[6].trim(); // TLS suffix
        String dutyTypeCode = columns[7].trim();
        String dutyCode = columns[8].trim();
        String avDutyRate = columns[9].trim();
        String specificDutyRate = columns[10].trim();
        String description = columns[11].trim(); // TrfLineDescription
        String dutyTypeDescription = columns[12].trim();
        String dutyNature = columns[13].trim();
        String avMethod = columns[14].trim();
        String note = columns[15].trim();
        String industry = columns.length > 16 ? columns[16].trim() : "";

        // Create or get entities
        Country reporter = createOrGetCountry(reporterCode, reporterName);
        Country partner = createOrGetCountry(partnerCode, partnerName);
        Product product = createOrGetProduct(tlCode, description, industry);
        DutyType dutyType = createOrGetDutyType(dutyTypeCode, dutyCode, dutyTypeDescription);

        // Create tariff schedule entry
        TariffSchedule tariffSchedule = new TariffSchedule();
        tariffSchedule.setReporter(reporter);
        tariffSchedule.setPartner(partner);
        tariffSchedule.setProduct(product);
        tariffSchedule.setDutyType(dutyType);
        tariffSchedule.setTariffYear(year);
        tariffSchedule.setNote(note);
        
        if (!tlsSuffix.isEmpty()) {
            tariffSchedule.setTlsSuffix(tlsSuffix);
        }

        tariffSchedule = tariffScheduleRepository.save(tariffSchedule);

        // Create appropriate duty based on av method and rates
        createDutyEntry(tariffSchedule, avMethod, avDutyRate, specificDutyRate, dutyNature);
    }

    private Country createOrGetCountry(String countryId, String countryName) {
        Optional<Country> existing = countryRepository.findById(countryId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Country country = new Country();
        country.setCountryId(countryId);
        country.setCountryName(countryName);
        // You might want to derive ISO code from country name or have a lookup
        country.setIsoCode(deriveIsoCode(countryName));
        return countryRepository.save(country);
    }

    private Product createOrGetProduct(String tlCode, String description, String industry) {
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

    private void createDutyEntry(TariffSchedule tariffSchedule, String avMethod, 
                                String avDutyRate, String specificDutyRate, String dutyNature) {
        
        boolean hasAdValorem = !avDutyRate.isEmpty() && !avDutyRate.equals("0");
        boolean hasSpecific = !specificDutyRate.isEmpty() && !specificDutyRate.equals("0");

        if (avMethod.equals("A") && hasAdValorem && !hasSpecific) {
            // Pure Ad Valorem
            createAdValoremDuty(tariffSchedule, avDutyRate, dutyNature);
        } else if (avMethod.equals("S") && hasSpecific && !hasAdValorem) {
            // Pure Specific
            createSpecificDuty(tariffSchedule, specificDutyRate, dutyNature);
        } else if ((avMethod.equals("C") || avMethod.equals("M")) && hasAdValorem && hasSpecific) {
            // Combined (Compound or Mixed)
            createCombinedDuty(tariffSchedule, avMethod, avDutyRate, specificDutyRate, dutyNature);
        } else {
            // Other duty types
            createOtherDuty(tariffSchedule, avMethod, avDutyRate, specificDutyRate, dutyNature);
        }
    }

    private void createAdValoremDuty(TariffSchedule tariffSchedule, String avDutyRate, String dutyNature) {
        try {
            BigDecimal rate = new BigDecimal(avDutyRate);
            AdValoremDuty duty = new AdValoremDuty();
            duty.setTariffSchedule(tariffSchedule);
            duty.setRatePercent(rate);
            duty.setDutyNature(dutyNature);
            adValoremDutyRepository.save(duty);
        } catch (NumberFormatException e) {
            System.err.println("Invalid ad valorem rate: " + avDutyRate);
        }
    }

    private void createSpecificDuty(TariffSchedule tariffSchedule, String specificDutyRate, String dutyNature) {
        SpecificDuty duty = new SpecificDuty();
        duty.setTariffSchedule(tariffSchedule);
        duty.setSpecificDutyRateRaw(specificDutyRate);
        duty.setDutyNature(dutyNature);
        
        // Parse specific duty rate if possible
        parseSpecificDutyRate(duty, specificDutyRate);
        
        specificDutyRepository.save(duty);
    }

    private void createCombinedDuty(TariffSchedule tariffSchedule, String avMethod, 
                                  String avDutyRate, String specificDutyRate, String dutyNature) {
        CombinedDuty duty = new CombinedDuty();
        duty.setTariffSchedule(tariffSchedule);
        duty.setMixedOrConditional(avMethod.equals("M") ? "M" : "C");
        duty.setDutyNature(dutyNature);
        duty.setSpecificDutyRateRaw(specificDutyRate);
        
        try {
            duty.setRatePercent(new BigDecimal(avDutyRate));
        } catch (NumberFormatException e) {
            System.err.println("Invalid ad valorem rate in combined duty: " + avDutyRate);
        }
        
        parseSpecificDutyRate(duty, specificDutyRate);
        
        combinedDutyRepository.save(duty);
    }

    private void createOtherDuty(TariffSchedule tariffSchedule, String avMethod, 
                               String avDutyRate, String specificDutyRate, String dutyNature) {
        OtherDuty duty = new OtherDuty();
        duty.setTariffSchedule(tariffSchedule);
        duty.setDutyNature(dutyNature);
        
        String rawText = !specificDutyRate.isEmpty() ? specificDutyRate : avDutyRate;
        duty.setRawText(rawText);
        duty.setIsComputable(false); // Default to false for "Other" duties
        
        otherDutyRepository.save(duty);
    }

    private void parseSpecificDutyRate(Object duty, String specificDutyRate) {
        // Simple parsing logic - unused 
        // Example: "5 USD per 100 kg" -> amount=5, unit=kg, multiplier=100
        // For now, just store the raw text
    }

    private String deriveIsoCode(String countryName) {
        // Simple mapping - you might want a proper lookup table
        if (countryName.toLowerCase().contains("singapore")) return "SG";
        if (countryName.toLowerCase().contains("world")) return "WLD";
        return countryName.length() >= 3 ? countryName.substring(0, 3).toUpperCase() : countryName.toUpperCase();
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        
        return result.toArray(new String[0]);
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
}