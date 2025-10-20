package com.ubs.tariffapp.utils;

import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class DutyParser {
    
    private static TokenizerME tokenizer;
    private static POSTaggerME posTagger;
    
    // Enhanced currency conversion rates (to standardize to USD)
    private static final double CENTS_TO_DOLLARS = 0.01;
    private static final java.util.Map<String, Double> CURRENCY_TO_USD = java.util.Map.of(
        "usd", 1.0,
        "dollar", 1.0,
        "cent", CENTS_TO_DOLLARS,
        "cents", CENTS_TO_DOLLARS,
        "eur", 1.08,  // Approximate - should be updated with real rates
        "gbp", 1.27,
        "jpy", 0.0067
    );
    
    // Enhanced unit conversion factors (to standardize to per kg)
    private static final java.util.Map<String, Double> UNIT_CONVERSION = createUnitConversionMap();

    private static java.util.Map<String, Double> createUnitConversionMap() {
        java.util.Map<String, Double> map = new java.util.HashMap<>();
        map.put("kg", 1.0);
        map.put("kilogram", 1.0);
        map.put("gram", 0.001);
        map.put("g", 0.001);
        map.put("pound", 0.453592);
        map.put("lb", 0.453592);
        map.put("liter", 1.0);
        map.put("litre", 1.0);
        map.put("each", 1.0);
        map.put("ton", 1000.0);
        map.put("tonne", 1000.0);
        map.put("ounce", 0.0283495);
        map.put("oz", 0.0283495);
        return java.util.Collections.unmodifiableMap(map);
    }
    
    // Static initializer to load NLP models
    static {
        try {
            initializeNLPModels();
        } catch (Exception e) {
            System.err.println("Warning: Could not initialize NLP models: " + e.getMessage());
        }
    }
    
    private static void initializeNLPModels() throws Exception {
        // Load tokenizer model
        try (InputStream tokenModelIn = DutyParser.class.getResourceAsStream("/models/en-token.bin")) {
            if (tokenModelIn != null) {
                TokenizerModel tokenModel = new TokenizerModel(tokenModelIn);
                tokenizer = new TokenizerME(tokenModel);
            }
        }
        
        // Load POS tagger model
        try (InputStream posModelIn = DutyParser.class.getResourceAsStream("/models/en-pos-maxent.bin")) {
            if (posModelIn != null) {
                POSModel posModel = new POSModel(posModelIn);
                posTagger = new POSTaggerME(posModel);
            }
        }
    }
    
    /**
     * Data structure to hold parsed duty information
     */
    public static class DutyInfo {
        public String dutyType = "AD_VALOREM";         // Default to ad valorem
        public double standardizedAVRate = 0.0;        // Standardized ad valorem rate
        public double specificDutyAmount = 0.0;        // Specific duty amount in USD
        public String currency = "";                    // Currency type (USD, EUR, etc.)
        public String unit = "";                        // Unit type (kg, liter, each, etc.)
        public String originalSpecificDuty = "";       // Original specific duty text
    }
    
    // ...existing code...
    
    /**
     * Parse duty rates using NLP to identify duty types and extract components
     * @param avRate The ad valorem rate as a string (may be empty)
     * @param specificRate The specific duty rate text
     * @return DutyInfo object with parsed components
     */
    public static DutyInfo parseDutyRates(String avRate, String specificRate) {
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
            ParsedDutyRate parsed = parseSpecificDutyRate(specificRate);
            
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
    public static DutyInfo manualParseDutyRate(String dutyText) {
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
    public static boolean isConditionalDutyPattern(String text) {
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
    public static boolean isCentsBasedCurrency(String dutyText) {
        if (dutyText == null) return false;
        
        String lower = dutyText.toLowerCase();
        // Check for cents explicitly mentioned
        return lower.contains("cent") && !lower.contains("percent");
    }

    /**
     * Extract currency type from duty text using pattern matching
     * @param dutyText The duty rate text
     * @return Currency type (USD, EUR, GBP, etc.) or empty string if unknown
     */
    public static String extractCurrencyType(String dutyText) {
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
     * Round a double value to avoid floating point precision errors
     * @param value The value to round
     * @param decimalPlaces Number of decimal places to round to
     * @return Rounded value
     */
    public static double roundToPrecision(double value, int decimalPlaces) {
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
     * Format a number for output, removing unnecessary decimal places
     * @param value The number to format
     * @return Formatted string representation
     */
    public static String formatNumber(double value) {
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
    
    public static ParsedDutyRate parseSpecificDutyRate(String specificDutyRate) {
        if (specificDutyRate == null || specificDutyRate.trim().isEmpty()) {
            return new ParsedDutyRate(0, 0, 0, "each", "0", false, specificDutyRate, "");
        }
        
        String input = specificDutyRate.trim().toLowerCase();
        
        // Enhanced condition detection
        boolean hasCondition = input.contains("but") || 
                              input.contains("however") ||
                              input.contains("except") ||
                              input.contains("provided") ||
                              input.contains("whichever") ||
                              input.contains("greater") ||
                              input.contains("lesser") ||
                              input.contains("minimum") ||
                              input.contains("maximum") ||
                              input.contains("if") ||
                              input.contains("when") ||
                              input.contains("unless") ||
                              input.contains("subject to");
        
        // Extract components using NLP
        List<DutyComponent> components = extractComponents(input);
        
        // Convert to mathematical expression
        return buildMathematicalExpression(components, specificDutyRate, hasCondition);
    }
    
    private static List<DutyComponent> extractComponents(String input) {
        List<DutyComponent> components = new ArrayList<>();
        
        if (tokenizer == null || posTagger == null) {
            // Fallback to regex if NLP models not available
            return extractComponentsWithRegex(input);
        }
        
        // Tokenize
        String[] tokens = tokenizer.tokenize(input);
        String[] posTags = posTagger.tag(tokens);
        
        // Extract numerical values and their context
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            String posTag = posTags[i];
            
            // Look for numbers (CD = Cardinal Number)
            if (posTag.equals("CD") || token.matches("\\d+(\\.\\d+)?")) {
                double value = Double.parseDouble(token);
                
                // Look at surrounding context for currency and units
                String currency = extractCurrency(tokens, posTags, i);
                String unit = extractUnit(tokens, posTags, i);
                boolean isPercentage = extractPercentage(tokens, i);
                
                components.add(new DutyComponent(value, currency, unit, isPercentage));
            }
        }
        
        return components;
    }
    
    private static List<DutyComponent> extractComponentsWithRegex(String input) {
        List<DutyComponent> components = new ArrayList<>();
        
        // Enhanced regex patterns
        Pattern moneyPattern = Pattern.compile("(\\$?)(\\d+(?:\\.\\d+)?)\\s*(cents?|dollars?|usd|eur|gbp|jpy|€|£|¥)?");
        Pattern percentPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");
        Pattern unitPattern = Pattern.compile("per\\s+(\\w+)|/(\\w+)|(\\w+)(?=\\s*$|\\s*\\+|\\s*and)");
        
        Matcher moneyMatcher = moneyPattern.matcher(input);
        while (moneyMatcher.find()) {
            String currencySymbol = moneyMatcher.group(1);
            double value = Double.parseDouble(moneyMatcher.group(2));
            String currencyWord = moneyMatcher.group(3);
            
            String currency = determineCurrency(currencySymbol, currencyWord);
            components.add(new DutyComponent(value, currency, "", false));
        }
        
        Matcher percentMatcher = percentPattern.matcher(input);
        while (percentMatcher.find()) {
            double value = Double.parseDouble(percentMatcher.group(1));
            components.add(new DutyComponent(value, "", "", true));
        }
        
        // Extract units
        Matcher unitMatcher = unitPattern.matcher(input);
        if (unitMatcher.find()) {
            String unit = null;
            for (int i = 1; i <= unitMatcher.groupCount(); i++) {
                if (unitMatcher.group(i) != null) {
                    unit = unitMatcher.group(i);
                    break;
                }
            }
            if (unit != null && !components.isEmpty()) {
                components.get(components.size() - 1).unit = unit;
            }
        }
        
        return components;
    }
    
    private static String extractCurrency(String[] tokens, String[] posTags, int index) {
        // Check previous and next tokens for currency indicators
        for (int i = Math.max(0, index - 2); i < Math.min(tokens.length, index + 3); i++) {
            String token = tokens[i].toLowerCase();
            if (token.equals("cents") || token.equals("cent")) return "cents";
            if (token.equals("$") || token.equals("dollars") || token.equals("dollar")) return "dollars";
        }
        return "";
    }
    
    private static String extractUnit(String[] tokens, String[] posTags, int index) {
        for (int i = Math.max(0, index - 2); i < Math.min(tokens.length, index + 3); i++) {
            String token = tokens[i].toLowerCase();
            if (UNIT_CONVERSION.containsKey(token)) return token;
            if (token.equals("each")) return "each";
        }
        return "kg"; // default
    }
    
    private static boolean extractPercentage(String[] tokens, int index) {
        // Check if next token is %
        return index + 1 < tokens.length && tokens[index + 1].equals("%");
    }
    
    private static String determineCurrency(String symbol, String word) {
        if ("$".equals(symbol) || "dollar".equals(word) || "dollars".equals(word)) {
            return "dollars";
        } else if ("cent".equals(word) || "cents".equals(word)) {
            return "cents";
        } else if ("€".equals(symbol) || "eur".equals(word)) {
            return "eur";
        } else if ("£".equals(symbol) || "gbp".equals(word)) {
            return "gbp";
        } else if ("¥".equals(symbol) || "jpy".equals(word)) {
            return "jpy";
        }
        return "dollars"; // default
    }
    
    private static ParsedDutyRate buildMathematicalExpression(List<DutyComponent> components, 
                                                      String originalText, boolean hasCondition) {
        double fixedAmount = 0.0;
        double originalFixedAmount = 0.0;
        double percentageRate = 0.0;
        String unit = "kg";
        String originalCurrency = "";
        
        StringBuilder mathExpr = new StringBuilder();
        
        for (DutyComponent comp : components) {
            if (comp.isPercentage) {
                double rate = roundToPrecision(comp.value / 100.0, 8);
                percentageRate += rate;
                if (mathExpr.length() > 0) mathExpr.append(" + ");
                mathExpr.append("(value * qty * ").append(rate).append(")");
            } else {
                // Keep original amount and convert for mathematical expression
                double originalValue = roundToPrecision(comp.value, 6);
                double standardizedValue = roundToPrecision(convertToUSD(comp.value, comp.currency), 6);
                
                originalFixedAmount += originalValue;
                fixedAmount += standardizedValue;
                
                // Store the first currency found as the original currency
                if (originalCurrency.isEmpty() && comp.currency != null && !comp.currency.isEmpty()) {
                    originalCurrency = comp.currency.toUpperCase();
                }
                
                // Convert unit if necessary
                Double unitFactor = UNIT_CONVERSION.get(comp.unit.toLowerCase());
                if (unitFactor != null) {
                    unit = standardizeUnit(comp.unit);
                }
                
                if (mathExpr.length() > 0) mathExpr.append(" + ");
                mathExpr.append("(").append(standardizedValue).append(" * qty)");
            }
        }
        
        if (mathExpr.length() == 0) {
            mathExpr.append("0");
        }
        
        // Round final values
        fixedAmount = roundToPrecision(fixedAmount, 6);
        originalFixedAmount = roundToPrecision(originalFixedAmount, 6);
        percentageRate = roundToPrecision(percentageRate, 8);
        
        return new ParsedDutyRate(fixedAmount, originalFixedAmount, percentageRate, unit, 
                                 mathExpr.toString(), hasCondition, originalText, originalCurrency);
    }
    
    private static double convertToUSD(double value, String currency) {
        if (currency == null || currency.isEmpty()) {
            return value; // Assume USD if no currency specified
        }
        
        Double conversionRate = CURRENCY_TO_USD.get(currency.toLowerCase());
        return conversionRate != null ? value * conversionRate : value;
    }
    
    private static String standardizeUnit(String unit) {
        if (unit == null) return "kg";
        
        String lower = unit.toLowerCase();
        if (UNIT_CONVERSION.containsKey(lower)) {
            // Return standardized form
            switch (lower) {
                case "g": case "gram": case "grams": return "kg";
                case "lb": case "lbs": case "pound": case "pounds": return "kg";  
                case "l": case "liter": case "liters": case "litre": case "litres": return "liter";
                default: return lower;
            }
        }
        return "kg"; // default
    }
    
    private static class DutyComponent {
        double value;
        String currency;
        String unit;
        boolean isPercentage;
        
        DutyComponent(double value, String currency, String unit, boolean isPercentage) {
            this.value = value;
            this.currency = currency;
            this.unit = unit;
            this.isPercentage = isPercentage;
        }
    }
    
    public static class ParsedDutyRate {
        private double fixedAmount;        // In USD (converted)
        private double originalFixedAmount; // In original currency
        private double percentageRate;     // As decimal (e.g., 0.149 for 14.9%)
        private String unit;               // Standardized unit
        private String mathExpression;    // Evaluatable expression
        private boolean hasCondition;     // Whether there are complex conditions
        private String originalText;      // Original for reference
        private String originalCurrency;  // Original currency before conversion
        
        public ParsedDutyRate(double fixedAmount, double originalFixedAmount, double percentageRate, String unit, 
                             String mathExpression, boolean hasCondition, String originalText, String originalCurrency) {
            this.fixedAmount = fixedAmount;
            this.originalFixedAmount = originalFixedAmount;
            this.percentageRate = percentageRate;
            this.unit = unit;
            this.mathExpression = mathExpression;
            this.hasCondition = hasCondition;
            this.originalText = originalText;
            this.originalCurrency = originalCurrency;
        }
        
        // Calculate duty for a given value and quantity
        public double calculateDuty(double unitValue, double quantity) {
            try {
                if (mathExpression != null && !mathExpression.isEmpty()) {
                    Expression e = new ExpressionBuilder(mathExpression)
                        .variables("value", "qty")
                        .build()
                        .setVariable("value", unitValue)
                        .setVariable("qty", quantity);
                    return e.evaluate();
                }
                
                // Fallback calculation
                return (fixedAmount * quantity) + (unitValue * quantity * percentageRate);
            } catch (Exception e) {
                System.err.println("Error calculating duty: " + e.getMessage());
                return 0.0;
            }
        }
        
        // Getters
        public double getFixedAmount() { return fixedAmount; }
        public double getOriginalFixedAmount() { return originalFixedAmount; }
        public double getPercentageRate() { return percentageRate; }
        public String getUnit() { return unit; }
        public String getMathExpression() { return mathExpression; }
        public boolean hasCondition() { return hasCondition; }
        public String getOriginalText() { return originalText; }
        public String getOriginalCurrency() { return originalCurrency; }
        
        @Override
        public String toString() {
            return String.format("Fixed: $%.3f (orig: %.3f %s), Rate: %.3f%%, Unit: %s, Expr: %s", 
                fixedAmount, originalFixedAmount, originalCurrency, percentageRate * 100, unit, mathExpression);
        }
    }
}