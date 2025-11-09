package com.ubs.tariffapp.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ubs.tariffapp.services.DataLoaderService;

/**
 * HSDataCleaner - A comprehensive data cleaning utility for HS Tariff data
 * 
 *  * Usage:
 * * - Place the input CSV file in the resources/data/test_data/ folder
 * * - Run the file using: mvn exec:java -Dexec.mainClass="com.ubs.tariffapp.utils.HSDataCleaner"
 * 
 * This class performs the following operations on raw HS (Harmonized System) tariff data:
 * 
 * 1. Data Standardization:
 *    - Normalizes HS codes to exactly 8 digits (padding with trailing zeros)
 *    - Converts ReporterName and PartnerName to Title Case
 *    - Removes duplicate entries based on HS code and description
 * 
 * 2. Industry Classification:
 *    - Classifies products into 4 major industry categories based on HS chapter:
 *      * Agriculture (Chapters 1-24)
 *      * Energy (Chapters 27) PREVIOUSLY 25-27 BUT 25-26 WERE NOT ENERGY 
 *      * Metals (Chapters 72-83)
 *      * Other (all remaining chapters), columns with this are filtered out
 * 
 * 3. Country Code Mapping:
 *   - Maps WITS country codes to ISO codes using an external CSV file
 *   - Adds new columns for ReporterISOCode and PartnerISOCode
 *   - Standardises country names to Title Case
 * 
 * 4. Duty Rate Deconstruction using NLP:
 *    - Parses specific duty rate text using Natural Language Processing
 *    - Identifies 4 types of duty structures:
 *      * AD_VALOREM: Only percentage-based (e.g., "5%")
 *      * SPECIFIC: Fixed amount per unit (e.g., "25 cents per kg" → standardized to "$0.25 per kg")
 *      * MIXED: Both specific and ad valorem (e.g., "$1.03 per litre + 12%")
 *      * CONDITIONAL: Complex conditions or unparseable text
 * 
 * 5. Currency and Unit Standardization:
 *    - Converts all currencies to USD (cents → dollars, other currencies using conversion rates)
 *    - Standardizes units (grams → kg, pounds → kg, litres → liters, etc.)
 *    - Extracts currency type and unit type into separate columns
 * 
 * 6. Mixed Duty Handling:
 *    - For mixed duties, retroactively fills the AV Duty Rate column (column 9)
 *    - Preserves oriERROR: duplicate key value violates unique constraint "unique_tariff_business_key"
  Detail: Key (reporter_id, partner_id, tl_code, duty_type, duty_code, tariff_year, tls_suffix)=(840, 000, 27079940, 0, 2, 2023, 0) already exists.ginal specific duty text for reference
 * 
 * Output Structure:
 * The cleaned CSV includes additional columns:
 * - Industry: Product category classification
 * - DutyType: Type of duty structure (AD_VALOREM/SPECIFIC/MIXED/CONDITIONAL)
 * - StandardizedAVRate: Extracted/standardized ad valorem rate
 * - SpecificDutyAmount: Standardized specific duty amount in USD
 * - Currency: Currency type (USD, EUR, GBP, etc.)
 * - Unit: Standardized unit (kg, liter, each, etc.)
 * - OriginalSpecificDuty: Original specific duty text for reference
 * 
 * Output Columns:
 * The output CSV will have the following columns:
 *  0	"Reporter"
 *  1	"ReporterName"
 *  2	"Partner"
 *  3	"Partner Name"
 *  4	"Year"
 *  5	"TL (HS Code)"
 *  6	"TLS (Additional HS Code Sub-classification - if any)"
 *  7	"Duty Type"
 *  8	"Duty Code"
 *  9	"Ad Valorem Duty Rate (%)"
 *  10	"Specific Duty Rate"
 *  11	"HS Code Description"
 *  12	"Duty Type Description"
 *  13	"Duty Nature"
 *  14	"Ad Valorem Calculation Code/Description"
 *  15	"Notes"
 *  16  "Reporter ISO Code"            - New column
 *  17  "Partner ISO Code"             - New column
 *  18	"Industry"                     - New column
 *  19	"DutyType"                     - New column
 *  20	"StandardizedAVRate"           - New column
 *  21	"SpecificDutyAmount"           - New column
 *  22	"Currency"                     - New column
 *  23	"Unit"                         - New column
 *  24	"OriginalSpecificDuty"         - New column
 */

public class HSDataCleaner {

    // Static map that maps WITS code to ISO code
    private static final Map<String, String[]> WITS_TO_ISO_MAP = new HashMap<>();
    private static final String COUNTRY_CODE_FILEPATH = "/data/country_iso_and_wits_code_data.csv";
    
    public static void main(String[] args) {
        // Columns used for deduplication key
        final int[] KEY_COLS = {0, 2, 4, 5, 6, 7, 8};

        // Load country code mappings
        loadCountryCodeMap(COUNTRY_CODE_FILEPATH);
        if (WITS_TO_ISO_MAP.isEmpty()) {
            System.err.println("Country code mapping is empty. Please check the country code CSV file.");
            return;
        }
        System.out.println("Loaded " + WITS_TO_ISO_MAP.size() + " country code mappings.");

        // Determine input file name
        String inputFileName;
        if (args.length > 0) {
            // Use command-line argument if provided
            inputFileName = args[0];
            System.out.println("Processing file from argument: " + inputFileName);
        } else {
            // Fallback to hardcoded file for backward compatibility
            inputFileName = "HS2017USAYear2023.csv";
            System.out.println("No file argument provided, using default: " + inputFileName);
        }

        // Read input from resources
        InputStream inputStream = HSDataCleaner.class.getResourceAsStream("/data/test_data/" + inputFileName);
        if (inputStream == null) {
            System.err.println("Input CSV file not found in resources folder: " + inputFileName);
            return;
        }

        String outputFileName = "clean_" + inputFileName;
        String outputFile = "src/main/resources/data/clean_data/" + outputFileName;
        
        // Initialize readers and writers
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));
            writer = new BufferedWriter(new FileWriter(outputFile));

            // Check if file is empty
            String line = reader.readLine();
            if (line == null) {
                System.err.println("Input file is empty.");
                return;
            }

            // Count expected columns from header using proper CSV parsing
            String[] headerColumns = parseCSVLine(line);
            int expectedColumns = headerColumns.length;
            System.out.println("Expected number of original columns: " + expectedColumns);

            // Append new columns to header
            StringBuilder header = new StringBuilder(line);
            header.append(",ReporterISOCode")
                  .append(",PartnerISOCode")
                  .append(",Industry")
                  .append(",DutyType")
                  .append(",StandardizedAVRate")
                  .append(",SpecificDutyAmount")
                  .append(",Currency")
                  .append(",Unit")
                  .append(",OriginalSpecificDuty");

            writer.write(header.toString());
            writer.newLine();
            // DEBUG: Track line numbers and rows processed/skipped
            int lineNumber = 1;
            int skippedRows = 0;
            int processedRows = 0;

            // Indices of columns to unquote
            final int[] COLS_TO_UNQUOTE = {0, 2, 4, 5};

            // Initialise deduplication set
            Set<String> seen = new HashSet<>();

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] columns = parseCSVLine(line); 

                // Check if row has correct number of columns, if not pad or truncate
                // DEBUG: Print warning & row info
                if (columns.length != expectedColumns) {
                    columns = normalizeColumnCount(columns, expectedColumns, lineNumber);
                }

                // Skip rows with empty HS codes
                String hsCode = columns[5].trim();
                if (hsCode.isEmpty()) {
                    skippedRows++;
                    continue;
                }

                // Normalize HS code to 8 digits, padding with trailing zeros if necessary
                try {
                    hsCode = hsCode.length() < 8
                            ? String.format("%-8d", Integer.parseInt(hsCode)).replace(' ', '0')
                            : hsCode;
                    columns[5] = hsCode;
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Invalid HS code at line " + lineNumber + ": " + hsCode);
                    skippedRows++;
                    continue;
                }

                // Skip rows with data not in Agriculture, Energy, or Metals
                String industry = classifyIndustry(hsCode);
                if (industry.equals("Other")) {
                    skippedRows++;
                    continue;
                }

                // Remove unnecessary quotes from key columns
                columns = removeQuotesFromColumns(columns, COLS_TO_UNQUOTE);

                // Get Reporter and Partner Standardised Names & ISO codes
                String[] reporterInfo = WITS_TO_ISO_MAP.getOrDefault(columns[0].trim(), null);
                columns[1] = reporterInfo[0];
                String reporterISOCode = reporterInfo[1];
                String[] partnerInfo = WITS_TO_ISO_MAP.getOrDefault(columns[2].trim(), null);
                columns[3] = partnerInfo[0];
                String partnerISOCode = partnerInfo[1];

                // Deduplication key
                String key = getDeduplicateKey(columns, KEY_COLS);
                if (seen.contains(key)) {
                    skippedRows++;
                    continue;
                }
                seen.add(key);

                // Truncate long description fields to reasonable lengths
                columns = truncateDescriptionColumns(columns, 250);

                // Parse duty rates using DutyParser
                String avRate = columns[9];
                String specificRate = columns[10];
                DutyParser.DutyInfo dutyInfo = DutyParser.parseDutyRates(avRate, specificRate);

                // Ensure the row has the right number of columns before adding new ones
                String[] updatedColumns = Arrays.copyOf(columns, expectedColumns + 9); 
                for (int i = columns.length; i < updatedColumns.length; i++) {
                    updatedColumns[i] = "";
                }

                // Update AV rate if mixed duty was found and original AV rate is empty
                if (dutyInfo.standardizedAVRate > 0 && (avRate == null || avRate.trim().isEmpty())) {
                    updatedColumns[9] = String.valueOf(dutyInfo.standardizedAVRate);
                }

                int baseIndex = expectedColumns;
                // Add ISO codes columns for reporter and partner
                updatedColumns[baseIndex] = reporterISOCode; // Reporter ISO code
                updatedColumns[baseIndex + 1] = partnerISOCode;   // Partner ISO code
                // Add new columns for duty analysis
                updatedColumns[baseIndex + 2] = industry;                                    // Industry classification
                updatedColumns[baseIndex + 3] = dutyInfo.dutyType;                           // Type of duty
                updatedColumns[baseIndex + 4] = DutyParser.formatNumber(dutyInfo.standardizedAVRate);   // Standardized AV rate - formatted
                updatedColumns[baseIndex + 5] = DutyParser.formatNumber(dutyInfo.specificDutyAmount);   // Specific duty amount in USD - formatted
                updatedColumns[baseIndex + 6] = dutyInfo.currency;                           // Currency type
                updatedColumns[baseIndex + 7] = dutyInfo.unit;                               // Unit type
                updatedColumns[baseIndex + 8] = dutyInfo.originalSpecificDuty;               // Original text for reference

                // Write updated row to output CSV
                String updatedLine = Arrays.stream(updatedColumns)
                    .map(value -> value == null ? "" : value)
                    .collect(Collectors.joining(","));

                writer.write(updatedLine);
                writer.newLine();

                processedRows++;
                if (processedRows % 1000 == 0) {
                    writer.flush();
                    System.out.println("Processed rows: " + processedRows);
                }
            }

            writer.flush();

            System.out.println("Processing complete:");
            System.out.println("- Processed rows: " + processedRows);
            System.out.println("- Skipped rows: " + skippedRows);
            System.out.println("- Total lines processed: " + (lineNumber - 1));
            System.out.println("Output saved to " + outputFile);
            System.out.println("Added columns: Industry, DutyType, StandardizedAVRate, SpecificDutyAmount, Currency, Unit, OriginalSpecificDuty");

        } catch (IOException e) {
            e.printStackTrace();
            return;
        } finally {
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

/**************************************************************
 * HELPER METHODS
 **************************************************************/ 

    /**
     * Normalize the number of columns in a row to match expected count
     * @param columns The original columns array
     * @param expectedCount The expected number of columns
     * @param lineNumber The line number for debugging purposes
     * @return A normalized array with the correct number of columns
     */
    private static String[] normalizeColumnCount(String[] columns, int expectedCount, int lineNumber) {
        // Print debug information when column count doesn't match
        if (columns.length != expectedCount) {
            System.err.println("Warning: Row " + lineNumber + " has " + columns.length + 
                             " columns, expected " + expectedCount + ". Padding/truncating row.");
            
            // DEBUG: Print the row values for inspection
            System.err.println("Row " + lineNumber + " values:");
            for (int i = 0; i < columns.length; i++) {
                String value = columns[i];
                // Truncate long values for readability
                if (value.length() > 50) {
                    value = value.substring(0, 47) + "...";
                }
                System.err.printf("  [%2d]: %-50s%n", i, "\"" + value + "\"");
            }
            System.err.println("  " + "-".repeat(60));
        }
        
        String[] normalized = new String[expectedCount];
        
        // Copy existing columns
        for (int i = 0; i < expectedCount; i++) {
            if (i < columns.length) {
                normalized[i] = columns[i];
            } else {
                normalized[i] = ""; // Fill missing columns with empty strings
            }
        }
        
        return normalized;
    }

    /**
     * Remove surrounding quotes and clean up the value
     * @param value The original string value
     * @return Cleaned string without surrounding quotes
     */
    private static String removeQuotes(String value) {
        if (value == null) return "";
        
        // Remove surrounding quotes if present
        String cleaned = value.trim();
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() > 1) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        
        // Handle escaped quotes inside
        cleaned = cleaned.replace("\"\"", "\"");
        
        return cleaned.trim();
    }

    /**
     * Remove unnecessary quotation marks from specified columns
     * @param columns The original columns array
     * @param startCol The starting column index (inclusive)
     * @param endCol The ending column index (inclusive)
     * @return A new array with quotes removed from specified columns
     */
    private static String[] removeQuotesFromColumns(String[] columns, int[] cols) {
        String[] cleanedColumns = columns.clone();
        
        for (int i = 0; i < cols.length; i++) {
            cleanedColumns[i] = removeQuotes(cleanedColumns[i]);
        }
        
        return cleanedColumns;
    }

    /**
     * Classify industry based on HS code chapter
     * @param hsCode The HS code (should be at least 2 digits)
     * @return Industry classification: Agriculture, Energy, Metals, or Other
     */
    private static String classifyIndustry(String hsCode) {
        try {
            int chapter = Integer.parseInt(hsCode.substring(0, 2));
            
            // Agriculture: Chapters 1-24
            if (chapter >= 1 && chapter <= 24) {
                return "Agriculture";
            }
            // Energy: Chapters 27
            else if (chapter == 27) {
                return "Energy";
            }
            // Metals: Chapters 72-83
            else if (chapter >= 72 && chapter <= 83) {
                return "Metals";
            }
            // Other: All remaining chapters (25-26, 28-71, 84-99)
            else {
                return "Other";
            }
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            // Handle invalid HS codes
            System.err.println("Warning: Invalid HS code format for industry classification: " + hsCode);
            return "Other";
        }
    }

    /**
     * Helper function to load WITS code to ISO code mapping
     * @param: COUNTRY_MAP_FILEPATH Path to the country code mapping CSV file
     */
    private static void loadCountryCodeMap(String COUNTRY_MAP_FILEPATH) {
        try {
            InputStream inputStream = DataLoaderService.class.getResourceAsStream(COUNTRY_MAP_FILEPATH);
            if (inputStream == null) {
                System.err.println("Country code mapping file not found");
                return;
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line = reader.readLine(); // Skip header
                
                while ((line = reader.readLine()) != null) {
                    String[] columns = parseCSVLine(line);
                    if (columns.length >= 3) {
                        // Create array with columns: [0] Country Name, [1] ISO Code
                        String[] arr = {toTitleCase(columns[0].trim()), columns[1].trim()};
                        String witsCode = columns[2].trim();
                        
                        // Map WITS code to Country Name & ISO code
                        if (!witsCode.isEmpty() && !arr[0].isEmpty()) {
                            WITS_TO_ISO_MAP.put(witsCode, arr);
                        }
                    }
                }
                
                System.out.println("Loaded " + WITS_TO_ISO_MAP.size() + " WITS to ISO code mappings");
                
            }
        } catch (Exception e) {
            System.err.println("Error loading country code mappings: " + e.getMessage());
        }
    }

    /**
     * Generate a deduplication key based on specified columns
     * @param columns The array of column values
     * @param keyCols The indices of columns to use for the key
     * @return A concatenated string key for deduplication
     */
    private static String getDeduplicateKey(String[] columns, int[] keyCols) {
        StringBuilder keyBuilder = new StringBuilder();
        for (int col : keyCols) {
            if (col < columns.length) {
                keyBuilder.append(columns[col].trim().toLowerCase()).append("|");
            } else {
                keyBuilder.append("|"); // Append empty for missing columns
            }
        }
        return keyBuilder.toString();
    }

    /**
     * tr
     */
    private static String[] truncateDescriptionColumns(String[] columns, int defaultLimit) {
        // Columns that typically contain long descriptions
        int[] descriptionColumns = {11,12};
        
        for (int columnIndex : descriptionColumns) {
            if (columnIndex < columns.length && columns[columnIndex] != null) {
                columns[columnIndex] = truncateString(columns[columnIndex], defaultLimit);
            }
        }
        
        return columns;
    }

    /**
     * Utility method to truncate a single string with proper handling
     */
    private static String truncateString(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        
        // For very short limits, just truncate
        if (maxLength <= 10) {
            return value.substring(0, maxLength);
        }
        
        // Try to truncate at a word boundary near the limit
        String truncated = value.substring(0, maxLength - 3);
        int lastSpace = truncated.lastIndexOf(' ');
        
        // If there's a space within reasonable distance, truncate there
        if (lastSpace > maxLength - 50 && lastSpace > 0) {
            truncated = truncated.substring(0, lastSpace);
        }
        
        return truncated + "...";
    }
    /**
     * Parse a CSV line respecting quoted fields that may contain commas
     * Preserves original quotation marks in the output
     * @param line The CSV line to parse
     * @return Array of field values with original quotes preserved
     */
    public static String[] parseCSVLine(String line) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        
        if (line == null || line.isEmpty()) {
            return new String[0];
        }
        
        int i = 0;
        while (i < line.length()) {
            StringBuilder field = new StringBuilder();
            
            // Skip leading whitespace (optional - depends on CSV format requirements)
            while (i < line.length() && line.charAt(i) == ' ') {
                i++;
            }
            
            if (i >= line.length()) {
                // End of line reached, add empty field if we're expecting one
                if (line.endsWith(",") || fields.isEmpty()) {
                    fields.add("");
                }
                break;
            }
            
            if (line.charAt(i) == '"') {
                // Quoted field - preserve the quotes
                field.append('"'); // Add opening quote to output
                i++; // Skip opening quote for parsing
                
                while (i < line.length()) {
                    if (line.charAt(i) == '"') {
                        if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                            // Escaped quote ("") - preserve both quotes
                            field.append("\"\"");
                            i += 2;
                        } else {
                            // End of quoted field - add closing quote
                            field.append('"');
                            i++; // Skip closing quote
                            break;
                        }
                    } else {
                        field.append(line.charAt(i));
                        i++;
                    }
                }
                // Skip to comma or end
                while (i < line.length() && line.charAt(i) != ',') {
                    i++;
                }
            } else {
                // Unquoted field - preserve as is
                while (i < line.length() && line.charAt(i) != ',') {
                    field.append(line.charAt(i));
                    i++;
                }
            }
            
            fields.add(field.toString());
            
            // Skip comma
            if (i < line.length() && line.charAt(i) == ',') {
                i++;
                // If comma is at the end, there's an empty trailing field
                if (i >= line.length()) {
                    fields.add("");
                }
            }
        }
        
        return fields.toArray(new String[0]);
    }

    
    private static String toTitleCase(String input) {
    if (input == null || input.isEmpty()) return input;

    return Arrays.stream(input.toLowerCase().split("\\s+"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
            .collect(Collectors.joining(" "));
    }


    // Static lookup map for ad valorem method descriptions
    private static final Map<String, String> avMethodMap = new HashMap<>();
    static {
        avMethodMap.put("A", "Ad valorem (percentage of value, e.g. 5% of import value)");
        avMethodMap.put("S", "Specific duty (fixed per unit, e.g. $10 per kg)");
        avMethodMap.put("O", "Other / mixed method (compound, formula-based, or special cases)");
        avMethodMap.put("C", "Compound (ad valorem + specific)");
        avMethodMap.put("M", "Mixed (either/or, whichever is higher/lower)");
        avMethodMap.put("P", "Formula-based or price-band system");
        avMethodMap.put("X", "Not elsewhere classified");
    }
}