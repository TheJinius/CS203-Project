package com.ubs.tariffapp.utils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class HSDataCleanerTest {
    public static void main(String[] args) {
        testCSVParsing();
    }
    
    private static void testCSVParsing() {
        // Test cases for empty fields
        String[] testCases = {
            "a,b,c",                    // Normal case
            "a,,c",                     // Empty middle field  
            "a,b,",                     // Empty trailing field
            ",b,c",                     // Empty leading field
            ",,",                       // Two empty fields
            "",                         // Empty line
            "\"a,b\",c,d",             // Quoted field with comma
            "a,\"b,c\",d",             // Quoted field in middle
            "a,\"\",c",                // Quoted empty field
            "a,,\"\"",                 // Mixed empty fields
            "\"\",,\"\"",              // All quoted empty
            "a,\"b\"\"c\",d"           // Escaped quotes
        };
        
        for (String test : testCases) {
            String[] result = HSDataCleaner.parseCSVLine(test);
            System.out.println("Input: '" + test + "' -> [" + 
                Arrays.stream(result).map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")) + "]");
        }
    }
}