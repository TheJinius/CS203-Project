package com.ubs.tariffapp.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

 /* Steps to compile for quick testing
  * We assume that the csv file and this class is in ./src/main/java
  * 
  * mvn compile exec:java -Dexec.mainClass="com.ubs.tariffapp.utils.HSDataCleaner"
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
        String inputFileName = "HS2017USAYear2023.csv"; // Original file name
        InputStream inputStream = HSDataCleaner.class.getResourceAsStream("/data/test_data/" + inputFileName);
        if (inputStream == null) {
            System.err.println("Input CSV file not found in resources folder.");
            return;
        }

        //String outputFile = "target/clean_hsca_data.csv";
        String outputFileName = "clean_" + inputFileName;
        String outputFile = "src/main/resources/data/clean_data/" + outputFileName;
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream));
             CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {

            List<String[]> allRows = reader.readAll();
            List<String[]> cleanedRows = new ArrayList<>();

            // Keep header
            String[] header = allRows.get(0);
            String[] newHeader = Arrays.copyOf(header, header.length + 1);
            newHeader[newHeader.length - 1] = "Industry";
            cleanedRows.add(newHeader);

            // Use a Set to deduplicate
            Set<String> seen = new HashSet<>();

            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);

                String hsCode = row[5].trim();
                String hsDesc = row[11].trim();

                // Drop empty HS codes
                if (hsCode.isEmpty()) {
                    continue; 
                }

                // Normalize HS code to 8 digits (pad if necessary)
                hsCode = String.format("%-8s", hsCode).replace(' ', '0');

                // Deduplication key
                String key = hsCode + "|" + hsDesc;
                if (seen.contains(key)) {
                    continue;
                }
                seen.add(key);

                // Industry tagging
                String industry = classifyIndustry(hsCode);

                // Add industry column
                String[] newRow = Arrays.copyOf(row, row.length + 1);
                newRow[newRow.length - 1] = industry;

                // Replace original HS code with normalized one
                newRow[5] = hsCode;

                // Standardise ReporterName and PartnerName to Title Case
                newRow[1] = capitalizeWordsStream(row[1].trim());
                newRow[3] = capitalizeWordsStream(row[3].trim());

                cleanedRows.add(newRow);
            }

            writer.writeAll(cleanedRows);
            System.out.println("Output saved to " + outputFile);

        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
    }

    private static String classifyIndustry(String hsCode) {
        int chapter = Integer.parseInt(hsCode.substring(0, 2));

        if (chapter >= 1 && chapter <= 24) {
            return "Agriculture";
        }
        if (chapter >= 25 && chapter <= 27) {
            return "Energy"; // coal, oil, gas
        }
        if (chapter >= 72 && chapter <= 83) {
            return "Metals";
        }
        return "Other";
    }

    private static String capitalizeWordsStream(String str) {
    if (str == null || str.isEmpty()) return str;

    return Arrays.stream(str.toLowerCase().split("\\s+"))
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