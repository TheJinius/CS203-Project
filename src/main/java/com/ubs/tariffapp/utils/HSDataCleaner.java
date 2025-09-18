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

 /* Steps to compile for quick testing
  * We assume that the csv file and this class is in ./src/main/java
  * 
  * mvn exec:java -Dexec.mainClass="com.ubs.tariffapp.utils.HSDataCleaner"
  */
 
 /*  Current Format of Raw Dataset:
     *  Column Index	Description
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
     * 
     * Cleaning Steps:
     * - Drop rows with empty HS codes
     * - Normalize HS codes to be exactly 8 digits (pad with trailing zeros if necessary)
     * - Deduplicate rows based on HS code and description
     * - Add "Industry" column based on HS code ranges
     *   - Chapters 1-24: "Agriculture"
     *   - Chapters 25-27: "Energy"
     *   - Chapters 72-83: "Metals"
     *   - Others: "Other"
     * - Standardise "ReporterName" and "PartnerName" to Title Case
     * 
     * Output:
     * - Save cleaned data to target/clean_hsca_data.csv
     */
public class HSDataCleaner {

    public static void main(String[] args) {
        // Read input from resources
        String inputFileName = "HS2017SGYear2023.csv"; // Original file name
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

            // Write the header with an additional "Industry" column
            writer.write(line + ",Industry");
            writer.newLine();
        
            // Use a HashSet to deduplicate
            Set<String> seen = new HashSet<>();

            // Process each line
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",", -1); // Use -1 to preserve empty columns

                // Skip rows with empty HS codes
                String hsCode = columns[5].trim();
                if (hsCode.isEmpty()) {
                    continue;
                }

                // Normalize HS code to 8 digits
                hsCode = hsCode.length() < 8
                       ? String.format("%-8d", Integer.parseInt(hsCode)).replace(' ', '0')
                       : hsCode;

                // Deduplication key
                String hsDesc = columns[11].trim();
                String key = hsCode + "|" + hsDesc; // Combine HS code and description for deduplication
                if (seen.contains(key)) {
                    continue;
                }
                seen.add(key);

                // Classify industry based on HS code
                String industry = classifyIndustry(hsCode);

                // Standardise ReporterName and PartnerName to Title Case
                columns[1] = toTitleCase(columns[1].trim());
                columns[3] = toTitleCase(columns[3].trim());

                // Ensure the row has exactly 17 columns
                String[] updatedColumns = Arrays.copyOf(columns, 17); // Ensure array has 17 elements
                for (int i = columns.length; i < 17; i++) {
                    updatedColumns[i] = ""; // Fill missing columns with empty strings
                }

                // Add the industry column to the row
                updatedColumns[16] = industry; // Add industry as the 17th column

                // Use StringBuilder to reconstruct the line
                String updatedLine = Arrays.stream(updatedColumns)
                    .map(value -> value == null ? "" : value) // Ensure null values are replaced with empty strings
                    .collect(Collectors.joining(","));

                // Write the cleaned row to the output file
                writer.write(updatedLine);
                writer.newLine();
            }

            System.out.println("Output saved to " + outputFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Define industry ranges using a TreeMap
    private static final NavigableMap<Integer, String> INDUSTRY_MAP = new TreeMap<>();

    static {
        INDUSTRY_MAP.put(1, "Agriculture"); // Covers 1 to 24
        INDUSTRY_MAP.put(25, "Energy");     // Covers 25 to 27
        INDUSTRY_MAP.put(72, "Metals");     // Covers 72 to 83
        INDUSTRY_MAP.put(84, "Other");      // Covers everything else
    }
    
    private static String classifyIndustry(String hsCode) {
        int chapter = Integer.parseInt(hsCode.substring(0, 2));
        Map.Entry<Integer, String> entry = INDUSTRY_MAP.floorEntry(chapter);
        if (entry != null) {
            String industry = entry.getValue();
            if ((entry.getKey() == 1 && chapter <= 24) || // Agriculture: 1-24
                (entry.getKey() == 25 && chapter <= 27) || // Energy: 25-27
                (entry.getKey() == 72 && chapter <= 83)) { // Metals: 72-83
                return industry;
            }
        }
        return "Other"; // Default if no range matches
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
}