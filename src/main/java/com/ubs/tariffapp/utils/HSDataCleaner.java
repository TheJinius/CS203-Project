package com.ubs.tariffapp.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.NavigableMap;
import java.math.BigDecimal;
import java.math.RoundingMode;

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
 * 3. Duty Rate Deconstruction using NLP:
 *    - Parses specific duty rate text using Natural Language Processing
 *    - Identifies 4 types of duty structures:
 *      * AD_VALOREM: Only percentage-based (e.g., "5%")
 *      * SPECIFIC: Fixed amount per unit (e.g., "25 cents per kg" → standardized to "$0.25 per kg")
 *      * MIXED: Both specific and ad valorem (e.g., "$1.03 per litre + 12%")
 *      * CONDITIONAL: Complex conditions or unparseable text
 * 
 * 4. Currency and Unit Standardization:
 *    - Converts all currencies to USD (cents → dollars, other currencies using conversion rates)
 *    - Standardizes units (grams → kg, pounds → kg, litres → liters, etc.)
 *    - Extracts currency type and unit type into separate columns
 * 
 * 5. Mixed Duty Handling:
 *    - For mixed duties, retroactively fills the AV Duty Rate column (column 9)
 *    - Preserves original specific duty text for reference
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
 *  16	"Industry"                     - New column
 *  17	"DutyType"                     - New column
 *  18	"StandardizedAVRate"           - New column
 *  19	"SpecificDutyAmount"           - New column
 *  20	"Currency"                     - New column
 *  21	"Unit"                         - New column
 *  22	"OriginalSpecificDuty"         - New column
 */

public class HSDataCleaner {

    private static NLPSpecificDutyParser dutyParser;
    private static final int EXPECTED_ORIGINAL_COLUMNS = 16; // Expected number of columns in original data

    public static void main(String[] args) {
        // Initialize NLP parser
        try {
            dutyParser = new NLPSpecificDutyParser();
        } catch (Exception e) {
            System.err.println("Failed to initialize NLP parser: " + e.getMessage());
            return;
        }

        String[] inputFileNames = {
            "HS2017USDYear2023.csv",
            "HS2017CNYear2023.csv",
            "HS2017SGYear2023.csv",
        };
        // Read input from resources
        String inputFileName = "HS2017CNYear2023.csv"; // Original file name
        InputStream inputStream = HSDataCleaner.class.getResourceAsStream("/data/test_data/" + inputFileName);
        if (inputStream == null) {
            System.err.println("Input CSV file not found in resources folder.");
            return;
        }

        String outputFileName = "clean_" + inputFileName;
        String outputFile = "src/main/resources/data/clean_data/" + outputFileName;
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String line = reader.readLine();
            if (line == null) {
                System.err.println("Input file is empty.");
                return;
            }

            // Count expected columns from header using proper CSV parsing
            String[] headerColumns = parseCSVLine(line);
            int expectedColumns = headerColumns.length;
            System.out.println("Expected number of original columns: " + expectedColumns);

            // Write new header with additional columns for duty analysis
            writer.write(line + ",Industry,DutyType,StandardizedAVRate,SpecificDutyAmount,Currency,Unit,OriginalSpecificDuty");
            writer.newLine();
        
            Set<String> seen = new HashSet<>();
            int lineNumber = 1; // Track line numbers for debugging
            int skippedRows = 0;
            int processedRows = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] columns = parseCSVLine(line); // Use proper CSV parsing

                // Check if row has correct number of columns
                if (columns.length != expectedColumns) {
                    System.err.println("Warning: Row " + lineNumber + " has " + columns.length + 
                                     " columns, expected " + expectedColumns + ". Padding/truncating row.");
                    
                    // Print formatted row values for debugging
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
                    
                    columns = normalizeColumnCount(columns, expectedColumns);
                }

                // Skip rows with empty HS codes
                String hsCode = (columns.length > 5) ? columns[5].trim() : "";
                if (hsCode.isEmpty()) {
                    skippedRows++;
                    continue;
                }
                // Remove unnecessary quotes from key columns
                int[] colsToClean = {0,2,4,5};
                columns = removeQuotesFromColumns(columns, colsToClean);
                // Skip rows not in Agriculture, Energy, or Metals
                String industry = classifyIndustry(hsCode);
                if (industry.equals("Other")) {
                    skippedRows++;
                    continue;
                }
                
                // Normalize HS code to 8 digits
                try {
                    hsCode = hsCode.length() < 8
                           ? String.format("%-8d", Integer.parseInt(hsCode)).replace(' ', '0')
                           : hsCode;
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Invalid HS code at line " + lineNumber + ": " + hsCode);
                    skippedRows++;
                    continue;
                }

                // Deduplication key
                String reporter = columns[0].trim();
                String partner = columns[2].trim();
                String year = columns[4].trim();
                String tl = columns[5].trim();
                String tls = columns[6].trim();
                String dutyType = columns[7].trim();
                String key = reporter + "|" + partner + "|" + year + "|" + tl + "|" + tls + "|" + dutyType;
                if (seen.contains(key)) {
                    skippedRows++;
                    continue;
                }
                seen.add(key);

                // Safely update reporter and partner names
                if (columns.length > 1) columns[1] = toTitleCase(columns[1].trim());
                if (columns.length > 3) columns[3] = toTitleCase(columns[3].trim());

                // Parse duty rates using NLP - safely extract AV rate and Specific rate
                String avRate = (columns.length > 9) ? columns[9] : "";
                String specificRate = (columns.length > 10) ? columns[10] : "";
                DutyInfo dutyInfo = parseDutyRates(avRate, specificRate);

                // Ensure the row has the right number of columns before adding new ones
                String[] updatedColumns = Arrays.copyOf(columns, expectedColumns + 7); // Original + 7 new columns
                for (int i = columns.length; i < updatedColumns.length; i++) {
                    updatedColumns[i] = "";
                }

                // Update AV rate if mixed duty was found and original AV rate is empty
                if (dutyInfo.standardizedAVRate > 0 && (avRate == null || avRate.trim().isEmpty())) {
                    updatedColumns[9] = String.valueOf(dutyInfo.standardizedAVRate);
                }

                // Add new columns for duty analysis
                int baseIndex = expectedColumns;
                updatedColumns[baseIndex] = industry;                                    // Industry classification
                updatedColumns[baseIndex + 1] = dutyInfo.dutyType;                     // Type of duty
                updatedColumns[baseIndex + 2] = formatNumber(dutyInfo.standardizedAVRate);  // Standardized AV rate - formatted
                updatedColumns[baseIndex + 3] = formatNumber(dutyInfo.specificDutyAmount);  // Specific duty amount in USD - formatted
                updatedColumns[baseIndex + 4] = dutyInfo.currency;                     // Currency type
                updatedColumns[baseIndex + 5] = dutyInfo.unit;                         // Unit type
                updatedColumns[baseIndex + 6] = dutyInfo.originalSpecificDuty;         // Original text for reference

                String updatedLine = Arrays.stream(updatedColumns)
                    .map(value -> value == null ? "" : value)
                    .collect(Collectors.joining(","));

                writer.write(updatedLine);
                writer.newLine();
                processedRows++;
            }

            System.out.println("Processing complete:");
            System.out.println("- Processed rows: " + processedRows);
            System.out.println("- Skipped rows: " + skippedRows);
            System.out.println("- Total lines processed: " + (lineNumber - 1));
            System.out.println("Output saved to " + outputFile);
            System.out.println("Added columns: Industry, DutyType, StandardizedAVRate, SpecificDutyAmount, Currency, Unit, OriginalSpecificDuty");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Normalize the number of columns in a row to match expected count
     * @param columns The original columns array
     * @param expectedCount The expected number of columns
     * @return A normalized array with the correct number of columns
     */
    private static String[] normalizeColumnCount(String[] columns, int expectedCount) {
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
     * Data structure to hold parsed duty information
     */
    private static class DutyInfo {
        String dutyType = "CONDITIONAL";        // Default to conditional
        double standardizedAVRate = 0.0;        // Standardized ad valorem rate
        double specificDutyAmount = 0.0;        // Specific duty amount in USD
        String currency = "";                   // Currency type (USD, EUR, etc.)
        String unit = "";                       // Unit type (kg, liter, each, etc.)
        String originalSpecificDuty = "";       // Original specific duty text
    }

    /**
     * Round a double value to avoid floating point precision errors
     * @param value The value to round
     * @param decimalPlaces Number of decimal places to round to
     * @return Rounded value
     */
    private static double roundToPrecision(double value, int decimalPlaces) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        
        try {
            BigDecimal bd = BigDecimal.valueOf(value);
            bd = bd.setScale(decimalPlaces, RoundingMode.HALF_UP);
            return bd.doubleValue();
        } catch (Exception e) {
            // Fallback for extreme values
            return Math.round(value * Math.pow(10, decimalPlaces)) / Math.pow(10, decimalPlaces);
        }
    }

    /**
     * Parse duty rates using NLP to identify duty types and extract components
     */
    private static DutyInfo parseDutyRates(String avRate, String specificRate) {
        DutyInfo info = new DutyInfo();
        info.originalSpecificDuty = specificRate != null ? specificRate.trim() : "";
        
        // Check if there's already an AV rate
        boolean hasExistingAV = false;
        if (avRate != null && !avRate.trim().isEmpty()) {
            try {
                double parsedRate = Double.parseDouble(avRate.trim());
                // Round to 6 decimal places to avoid floating point errors
                info.standardizedAVRate = roundToPrecision(parsedRate, 6);
                hasExistingAV = parsedRate > 0; // Only consider non-zero as existing AV
            } catch (NumberFormatException e) {
                // Invalid AV rate, ignore and continue processing
            }
        }

        // If no specific duty rate, determine type based on existing AV rate
        if (specificRate == null || specificRate.trim().isEmpty()) {
            if (hasExistingAV) {
                info.dutyType = "AD_VALOREM";
            } else {
                // Default to AD_VALOREM with 0% rate instead of CONDITIONAL
                info.dutyType = "AD_VALOREM";
                info.standardizedAVRate = 0.0;
            }
            return info;
        }

        try {
            // First try manual parsing to separate components properly
            DutyInfo manualParsed = manualParseDutyRate(specificRate);
            if (manualParsed != null) {
                // Use manual parsing results
                info.dutyType = manualParsed.dutyType;
                info.specificDutyAmount = manualParsed.specificDutyAmount;
                info.standardizedAVRate = hasExistingAV ? info.standardizedAVRate : manualParsed.standardizedAVRate;
                info.currency = manualParsed.currency;
                info.unit = manualParsed.unit;
                
                // Adjust duty type if there's existing AV rate
                if (hasExistingAV && info.specificDutyAmount > 0) {
                    info.dutyType = "MIXED";
                }
                
                return info;
            }
            
            // Fallback to NLP parser
            NLPSpecificDutyParser.ParsedDutyRate parsed = dutyParser.parseSpecificDutyRate(specificRate);
            
            // Determine duty type based on parsed components
            boolean hasFixedComponent = parsed.getFixedAmount() > 0;
            boolean hasPercentageComponent = parsed.getPercentageRate() > 0;
            
            if (hasFixedComponent && hasPercentageComponent) {
                // Mixed duty: has both specific amount and percentage
                info.dutyType = "MIXED";
                // Use getOriginalFixedAmount() to get just the specific portion
                double specificAmount = parsed.getOriginalFixedAmount();
                // Convert cents to dollars if currency is cents/USD
                if (isCentsBasedCurrency(specificRate)) {
                    specificAmount = specificAmount / 100.0;
                }
                info.specificDutyAmount = roundToPrecision(specificAmount, 6);
                info.standardizedAVRate = roundToPrecision(parsed.getPercentageRate() * 100, 6); // Convert to percentage
                info.currency = extractCurrencyType(specificRate);
                info.unit = parsed.getUnit();
            } else if (hasFixedComponent) {
                // Specific duty only (or mixed with existing AV rate)
                info.dutyType = hasExistingAV ? "MIXED" : "SPECIFIC";
                double specificAmount = parsed.getOriginalFixedAmount();
                // Convert cents to dollars if currency is cents/USD
                if (isCentsBasedCurrency(specificRate)) {
                    specificAmount = specificAmount / 100.0;
                }
                info.specificDutyAmount = roundToPrecision(specificAmount, 6);
                info.currency = extractCurrencyType(specificRate);
                info.unit = parsed.getUnit();
            } else if (hasPercentageComponent) {
                // Only percentage found in specific rate (unusual but possible)
                info.dutyType = "AD_VALOREM";
                info.standardizedAVRate = roundToPrecision(parsed.getPercentageRate() * 100, 6);
            } else if (parsed.hasCondition()) {
                // Complex conditional duty with conditions like "whichever is higher"
                info.dutyType = "CONDITIONAL";
            } else {
                // Could not parse meaningful components - check if it's just empty/zero
                String trimmedRate = specificRate.trim().toLowerCase();
                if (trimmedRate.isEmpty() || trimmedRate.equals("0") || trimmedRate.equals("0%") || trimmedRate.equals("free")) {
                    info.dutyType = "AD_VALOREM";
                    info.standardizedAVRate = 0.0;
                } else {
                    info.dutyType = "CONDITIONAL";
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing duty rate: " + specificRate + " - " + e.getMessage());
            // Only mark as conditional if it's truly unparseable
            String trimmedRate = specificRate.trim().toLowerCase();
            if (trimmedRate.isEmpty() || trimmedRate.equals("0") || trimmedRate.equals("0%") || trimmedRate.equals("free")) {
                info.dutyType = "AD_VALOREM";
                info.standardizedAVRate = 0.0;
            } else {
                info.dutyType = "CONDITIONAL";
            }
        }
        
        return info;
    }

    /**
     * Manual parsing for common duty rate patterns to ensure proper separation
     * @param dutyText The duty rate text to parse
     * @return DutyInfo with separated components, or null if cannot parse manually
     */
    private static DutyInfo manualParseDutyRate(String dutyText) {
        if (dutyText == null || dutyText.trim().isEmpty()) {
            return null;
        }
        
        String text = dutyText.trim().toLowerCase();
        DutyInfo info = new DutyInfo();
        
        // Check for complex conditional patterns first
        if (isConditionalDutyPattern(text)) {
            info.dutyType = "CONDITIONAL";
            return info;
        }
        
        // Pattern: "40 cents/kg + 10.4%"
        if (text.matches(".*\\d+(?:\\.\\d+)?\\s*cent.*\\+.*\\d+(?:\\.\\d+)?\\s*%.*")) {
            try {
                // Extract cents amount
                java.util.regex.Pattern centsPattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*cent");
                java.util.regex.Matcher centsMatcher = centsPattern.matcher(text);
                
                // Extract percentage
                java.util.regex.Pattern percentPattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");
                java.util.regex.Matcher percentMatcher = percentPattern.matcher(text);
                
                // Extract unit - handle both alphabetic and numeric units
                java.util.regex.Pattern unitPattern = java.util.regex.Pattern.compile("cent[s]?/([a-zA-Z0-9]+)");
                java.util.regex.Matcher unitMatcher = unitPattern.matcher(text);
                
                if (centsMatcher.find() && percentMatcher.find()) {
                    double centsAmount = Double.parseDouble(centsMatcher.group(1));
                    double percentRate = Double.parseDouble(percentMatcher.group(1));
                    
                    info.dutyType = "MIXED";
                    info.specificDutyAmount = roundToPrecision(centsAmount / 100.0, 6); // Convert cents to dollars
                    info.standardizedAVRate = roundToPrecision(percentRate, 6);
                    info.currency = "USD";
                    info.unit = unitMatcher.find() ? unitMatcher.group(1) : "kg";
                    
                    return info;
                }
            } catch (Exception e) {
                // Fall through to return null
            }
        }
        
        // Pattern: "$1.50 per kg + 5.5%"
        if (text.matches(".*\\$\\d+(?:\\.\\d+)?.*\\+.*\\d+(?:\\.\\d+)?\\s*%.*")) {
            try {
                // Extract dollar amount
                java.util.regex.Pattern dollarPattern = java.util.regex.Pattern.compile("\\$(\\d+(?:\\.\\d+)?)");
                java.util.regex.Matcher dollarMatcher = dollarPattern.matcher(text);
                
                // Extract percentage
                java.util.regex.Pattern percentPattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");
                java.util.regex.Matcher percentMatcher = percentPattern.matcher(text);
                
                // Extract unit - handle both alphabetic and numeric units
                java.util.regex.Pattern unitPattern = java.util.regex.Pattern.compile("per\\s+([a-zA-Z0-9]+)");
                java.util.regex.Matcher unitMatcher = unitPattern.matcher(text);
                
                if (dollarMatcher.find() && percentMatcher.find()) {
                    double dollarAmount = Double.parseDouble(dollarMatcher.group(1));
                    double percentRate = Double.parseDouble(percentMatcher.group(1));
                    
                    info.dutyType = "MIXED";
                    info.specificDutyAmount = roundToPrecision(dollarAmount, 6);
                    info.standardizedAVRate = roundToPrecision(percentRate, 6);
                    info.currency = "USD";
                    info.unit = unitMatcher.find() ? unitMatcher.group(1) : "kg";
                    
                    return info;
                }
            } catch (Exception e) {
                // Fall through to return null
            }
        }
        
        // Pattern: just cents - "25 cents per kg" or "55.7 cents/1000"
        if (text.matches(".*\\d+(?:\\.\\d+)?\\s*cent.*") && !text.contains("+") && !text.contains("%")) {
            try {
                java.util.regex.Pattern centsPattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*cent");
                java.util.regex.Matcher centsMatcher = centsPattern.matcher(text);
                
                // Extract unit - handle both alphabetic and numeric units, with or without "per"
                java.util.regex.Pattern unitPattern = java.util.regex.Pattern.compile("cent[s]?\\s*(?:per\\s+)?(?:/\\s*)?([a-zA-Z0-9]+)");
                java.util.regex.Matcher unitMatcher = unitPattern.matcher(text);
                
                if (centsMatcher.find()) {
                    double centsAmount = Double.parseDouble(centsMatcher.group(1));
                    
                    info.dutyType = "SPECIFIC";
                    info.specificDutyAmount = roundToPrecision(centsAmount / 100.0, 6);
                    info.standardizedAVRate = 0.0;
                    info.currency = "USD";
                    info.unit = unitMatcher.find() ? unitMatcher.group(1) : "each";
                    
                    return info;
                }
            } catch (Exception e) {
                // Fall through to return null
            }
        }
        
        // Pattern: just dollars - "$1.50 per 1000" or "$0.25/kg"
        if (text.matches(".*\\$\\d+(?:\\.\\d+)?.*") && !text.contains("+") && !text.contains("%")) {
            try {
                java.util.regex.Pattern dollarPattern = java.util.regex.Pattern.compile("\\$(\\d+(?:\\.\\d+)?)");
                java.util.regex.Matcher dollarMatcher = dollarPattern.matcher(text);
                
                // Extract unit - handle both alphabetic and numeric units, with or without "per"
                java.util.regex.Pattern unitPattern = java.util.regex.Pattern.compile("\\$\\d+(?:\\.\\d+)?\\s*(?:per\\s+)?(?:/\\s*)?([a-zA-Z0-9]+)");
                java.util.regex.Matcher unitMatcher = unitPattern.matcher(text);
                
                if (dollarMatcher.find()) {
                    double dollarAmount = Double.parseDouble(dollarMatcher.group(1));
                    
                    info.dutyType = "SPECIFIC";
                    info.specificDutyAmount = roundToPrecision(dollarAmount, 6);
                    info.standardizedAVRate = 0.0;
                    info.currency = "USD";
                    info.unit = unitMatcher.find() ? unitMatcher.group(1) : "each";
                    
                    return info;
                }
            } catch (Exception e) {
                // Fall through to return null
            }
        }
        
        return null; // Could not parse manually
    }

    /**
     * Check if the duty text contains complex conditional patterns that should be classified as CONDITIONAL
     * @param text The duty text in lowercase
     * @return true if the text contains conditional patterns
     */
    private static boolean isConditionalDutyPattern(String text) {
        // References to other headings/tariff classifications
        if (text.contains("heading") || text.contains("subheading") || text.contains("tariff")) {
            return true;
        }
        
        // Complex mathematical formulas with conditions
        if (text.contains("less") && text.contains("for each") && text.contains("degree")) {
            return true;
        }
        
        // "But not less than" or "but not more than" conditions
        if (text.contains("but not less than") || text.contains("but not more than")) {
            return true;
        }
        
        // "Whichever is" conditions
        if (text.contains("whichever is higher") || text.contains("whichever is lower") || 
            text.contains("whichever is greater") || text.contains("whichever is less")) {
            return true;
        }
        
        // Temperature or other measurement-based conditions
        if (text.contains("degrees") && (text.contains("under") || text.contains("over") || text.contains("above") || text.contains("below"))) {
            return true;
        }
        
        // Fraction or proportion-based calculations
        if (text.contains("fractions") && text.contains("proportion")) {
            return true;
        }
        
        // Multiple rates with conditions
        if (text.contains("applicable to") && text.contains("in heading")) {
            return true;
        }
        
        // Complex formulas with multiple operations
        if ((text.contains("less") || text.contains("minus")) && 
            (text.contains("for each") || text.contains("per degree") || text.contains("per unit"))) {
            return true;
        }
        
        // Rate references to other classifications
        if (text.matches(".*rate.*applicable.*to.*")) {
            return true;
        }
        
        // Sliding scale duties
        if (text.contains("sliding scale") || (text.contains("scale") && text.contains("rate"))) {
            return true;
        }
        
        return false;
    }

    /**
     * Check if the duty text indicates a cents-based currency that should be converted to dollars
     * @param dutyText The duty rate text
     * @return true if the text indicates cents-based currency
     */
    private static boolean isCentsBasedCurrency(String dutyText) {
        if (dutyText == null) return false;
        
        String lower = dutyText.toLowerCase();
        // Check for cents explicitly mentioned
        return lower.contains("cent") && !lower.contains("percent");
    }

    /**
     * Format a number for output, removing unnecessary decimal places
     * @param value The number to format
     * @return Formatted string representation
     */
    private static String formatNumber(double value) {
        if (value == 0.0) {
            return "0";
        }
        
        // Round to 6 decimal places first
        double rounded = roundToPrecision(value, 6);
        
        // If it's effectively an integer, format as integer
        if (Math.abs(rounded - Math.round(rounded)) < 1e-9) {
            return String.valueOf(Math.round(rounded));
        }
        
        // Otherwise format with appropriate precision, removing trailing zeros
        String formatted = String.format("%.6f", rounded);
        // Remove trailing zeros and decimal point if not needed
        formatted = formatted.replaceAll("0*$", "").replaceAll("\\.$", "");
        return formatted;
    }

    /**
     * Extract currency type from duty text using pattern matching
     * @param dutyText The duty rate text
     * @return Currency type (USD, EUR, GBP, etc.) or empty string if unknown
     */
    private static String extractCurrencyType(String dutyText) {
        if (dutyText == null) return "";
        
        String lower = dutyText.toLowerCase();
        
        // Common currency patterns - return original currency codes
        if (lower.contains("$") || lower.contains("dollar") || lower.contains("usd")) {
            return "USD";
        } else if (lower.contains("cent")) {
            return "USD"; // Cents are USD denomination
        } else if (lower.contains("€") || lower.contains("euro") || lower.contains("eur")) {
            return "EUR";
        } else if (lower.contains("£") || lower.contains("pound") || lower.contains("gbp")) {
            return "GBP";
        } else if (lower.contains("¥") || lower.contains("yen") || lower.contains("jpy")) {
            return "JPY";
        }
        
        return ""; // Unknown currency
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

    private static String toTitleCase(String input) {
    if (input == null || input.isEmpty()) return input;

    return Arrays.stream(input.toLowerCase().split("\\s+"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
            .collect(Collectors.joining(" "));
    }


    // Static lookup map
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
    
    // Method to get AV description from AVMethod column
    public static String getDescription(String code) {
        if (code == null || code.isEmpty()) {
            return "Invalid code";
        }
        return avMethodMap.getOrDefault(code.toUpperCase(), "Unknown method");
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
}