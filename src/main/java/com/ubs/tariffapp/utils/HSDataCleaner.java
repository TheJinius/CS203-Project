package com.ubs.tariffapp.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

/* Steps to compile for quick testing
 * We assume that the csv file and this class is in ./src/main/java
 * 
 * mvn compile exec:java -Dexec.mainClass="com.ubs.tariffapp.utils.HSDataCleaner"
 */

public class HSDataCleaner {

    public static void main(String[] args) {
        // Read input from resources
        InputStream inputStream = HSDataCleaner.class.getResourceAsStream("/data/raw_hsca_data.csv");
        if (inputStream == null) {
            System.err.println("Input CSV file not found in resources folder.");
            return;
        }

        String outputFile = "target/clean_hsca_data.csv";
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

                String hsCode = row[1].trim();
                String hsDesc = row[2].trim();

                if (hsCode.isEmpty()) {
                    continue; // drop empty HS codes
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
                newRow[1] = hsCode; // replace with normalized code

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
}