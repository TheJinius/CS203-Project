package com.ubs.tariffapp.utils;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for HSDataCleaner.parseCSVLine() method
 * Tests comprehensive CSV parsing scenarios including quoted fields, edge cases, and special characters
 * Note: Only parseCSVLine() is public, all other utility methods are private and tested indirectly through the main method
 */
@DisplayName("HSDataCleaner CSV Parsing Tests")
public class HSDataCleanerTest {
    
    @Test
    @DisplayName("Should parse simple CSV line")
    void shouldParseSimpleCSVLine() {
        String line = "value1,value2,value3";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(3);
        assertThat(result[0]).isEqualTo("value1");
        assertThat(result[1]).isEqualTo("value2");
        assertThat(result[2]).isEqualTo("value3");
    }
    
    @Test
    @DisplayName("Should parse quoted fields with commas")
    void shouldParseQuotedFieldsWithCommas() {
        String line = "\"value1,with,commas\",value2,value3";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(3);
        assertThat(result[0]).isEqualTo("\"value1,with,commas\"");
        assertThat(result[1]).isEqualTo("value2");
        assertThat(result[2]).isEqualTo("value3");
    }
    
    @Test
    @DisplayName("Should handle escaped quotes within quoted fields")
    void shouldHandleEscapedQuotes() {
        String line = "\"value with \"\"escaped\"\" quotes\",value2";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(2);
        assertThat(result[0]).isEqualTo("\"value with \"\"escaped\"\" quotes\"");
        assertThat(result[1]).isEqualTo("value2");
    }
    
    @Test
    @DisplayName("Should handle empty fields")
    void shouldHandleEmptyFields() {
        String line = "value1,,value3";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(3);
        assertThat(result[0]).isEqualTo("value1");
        assertThat(result[1]).isEmpty();
        assertThat(result[2]).isEqualTo("value3");
    }
    
    @Test
    @DisplayName("Should handle trailing comma")
    void shouldHandleTrailingComma() {
        String line = "value1,value2,";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(3);
        assertThat(result[2]).isEmpty();
    }
    
    @Test
    @DisplayName("Should handle empty string")
    void shouldHandleEmptyString() {
        String line = "";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("Should handle null input")
    void shouldHandleNullInput() {
        String[] result = HSDataCleaner.parseCSVLine(null);
        
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("Should handle quoted empty fields")
    void shouldHandleQuotedEmptyFields() {
        String line = "\"\",value2,\"\"";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(3);
        assertThat(result[0]).isEqualTo("\"\"");
        assertThat(result[1]).isEqualTo("value2");
        assertThat(result[2]).isEqualTo("\"\"");
    }
    
    @Test
    @DisplayName("Should handle mixed quoted and unquoted fields")
    void shouldHandleMixedFields() {
        String line = "unquoted,\"quoted,value\",unquoted2";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(3);
        assertThat(result[0]).isEqualTo("unquoted");
        assertThat(result[1]).isEqualTo("\"quoted,value\"");
        assertThat(result[2]).isEqualTo("unquoted2");
    }
    
    @Test
    @DisplayName("Should handle fields with spaces")
    void shouldHandleFieldsWithSpaces() {
        String line = "  value1  ,value2,  value3";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(3);
        assertThat(result[0]).isEqualTo("value1  ");  // Unquoted fields preserve trailing spaces
        assertThat(result[1]).isEqualTo("value2");
        assertThat(result[2]).isEqualTo("value3");  // Leading spaces are trimmed
    }
    
    @Test
    @DisplayName("Should preserve whitespace in quoted fields")
    void shouldPreserveWhitespaceInQuotedFields() {
        String line = "\"  value1  \",value2";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(2);
        assertThat(result[0]).isEqualTo("\"  value1  \"");
    }
    
    @Test
    @DisplayName("Should handle real-world CSV line with multiple quoted fields")
    void shouldHandleRealWorldCSVLine() {
        String line = "\"840\",\"United States\",\"156\",\"China\",\"2023\",\"01234567\",\"0\",\"0\",\"2\",\"5.5\",\"\",\"Product Description\"";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(12);
        assertThat(result[0]).isEqualTo("\"840\"");
        assertThat(result[1]).isEqualTo("\"United States\"");
        assertThat(result[11]).isEqualTo("\"Product Description\"");
    }
    
    @Test
    @DisplayName("Should handle complex quote escaping patterns")
    void shouldHandleComplexQuoteEscaping() {
        String line = "\"She said \"\"hello\"\" and I said \"\"goodbye\"\"\",normal,\"quoted\"";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(3);
        assertThat(result[0]).contains("hello");
        assertThat(result[0]).contains("goodbye");
        assertThat(result[1]).isEqualTo("normal");
        assertThat(result[2]).isEqualTo("\"quoted\"");
    }
    
    @Test
    @DisplayName("Should handle consecutive commas")
    void shouldHandleConsecutiveCommas() {
        String line = "value1,,,value4";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(4);
        assertThat(result[0]).isEqualTo("value1");
        assertThat(result[1]).isEmpty();
        assertThat(result[2]).isEmpty();
        assertThat(result[3]).isEqualTo("value4");
    }
    
    @Test
    @DisplayName("Should handle newlines within quoted fields")
    void shouldHandleNewlinesInQuotedFields() {
        String line = "\"value with\nnewline\",value2";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(2);
        assertThat(result[0]).contains("\n");
        assertThat(result[1]).isEqualTo("value2");
    }
    
    @Test
    @DisplayName("Should handle tabs in fields")
    void shouldHandleTabsInFields() {
        String line = "value1\twithtab,\"quoted\twith\ttab\",normal";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(3);
        assertThat(result[0]).contains("\t");
        assertThat(result[1]).contains("\t");
    }
    
    @Test
    @DisplayName("Should handle very long fields")
    void shouldHandleVeryLongFields() {
        String longValue = "A".repeat(10000);
        String line = "\"" + longValue + "\",value2";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(2);
        assertThat(result[0].length()).isEqualTo(10002); // quotes included
        assertThat(result[1]).isEqualTo("value2");
    }
    
    @Test
    @DisplayName("Should handle single field")
    void shouldHandleSingleField() {
        String line = "singlevalue";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(1);
        assertThat(result[0]).isEqualTo("singlevalue");
    }
    
    @Test
    @DisplayName("Should handle only commas")
    void shouldHandleOnlyCommas() {
        String line = ",,,";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(4);
        for (String field : result) {
            assertThat(field).isEmpty();
        }
    }
    
    @Test
    @DisplayName("Should handle leading quote without closing quote")
    void shouldHandleUnclosedQuote() {
        // This tests malformed CSV - behavior depends on implementation
        String line = "\"unclosed,value2";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        // Verify it doesn't crash and returns something reasonable
        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
    }
    
    @Test
    @DisplayName("Should handle multiple consecutive quoted fields")
    void shouldHandleMultipleConsecutiveQuotedFields() {
        String line = "\"field1\",\"field2\",\"field3\"";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(3);
        assertThat(result[0]).isEqualTo("\"field1\"");
        assertThat(result[1]).isEqualTo("\"field2\"");
        assertThat(result[2]).isEqualTo("\"field3\"");
    }
    
    @Test
    @DisplayName("Should handle special characters in fields")
    void shouldHandleSpecialCharacters() {
        String line = "field1,\"field!@#$%^&*()2\",field3";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(3);
        assertThat(result[1]).contains("!@#$%^&*()");
    }
    
    @Test
    @DisplayName("Should handle numeric fields")
    void shouldHandleNumericFields() {
        String line = "123,456.78,-999";
        String[] result = HSDataCleaner.parseCSVLine(line);
        
        assertThat(result).hasSize(3);
        assertThat(result[0]).isEqualTo("123");
        assertThat(result[1]).isEqualTo("456.78");
        assertThat(result[2]).isEqualTo("-999");
    }
}
