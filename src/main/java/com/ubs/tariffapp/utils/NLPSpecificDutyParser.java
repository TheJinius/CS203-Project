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

public class NLPSpecificDutyParser {
    
    private TokenizerME tokenizer;
    private POSTaggerME posTagger;
    
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
    
    public NLPSpecificDutyParser() throws Exception {
        // Load tokenizer model
        try (InputStream tokenModelIn = getClass().getResourceAsStream("/models/en-token.bin")) {
            if (tokenModelIn != null) {
                TokenizerModel tokenModel = new TokenizerModel(tokenModelIn);
                tokenizer = new TokenizerME(tokenModel);
            }
        }
        
        // Load POS tagger model
        try (InputStream posModelIn = getClass().getResourceAsStream("/models/en-pos-maxent.bin")) {
            if (posModelIn != null) {
                POSModel posModel = new POSModel(posModelIn);
                posTagger = new POSTaggerME(posModel);
            }
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
    
    public ParsedDutyRate parseSpecificDutyRate(String specificDutyRate) {
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
    
    private List<DutyComponent> extractComponents(String input) {
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
    
    private List<DutyComponent> extractComponentsWithRegex(String input) {
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
    
    private String extractCurrency(String[] tokens, String[] posTags, int index) {
        // Check previous and next tokens for currency indicators
        for (int i = Math.max(0, index - 2); i < Math.min(tokens.length, index + 3); i++) {
            String token = tokens[i].toLowerCase();
            if (token.equals("cents") || token.equals("cent")) return "cents";
            if (token.equals("$") || token.equals("dollars") || token.equals("dollar")) return "dollars";
        }
        return "";
    }
    
    private String extractUnit(String[] tokens, String[] posTags, int index) {
        for (int i = Math.max(0, index - 2); i < Math.min(tokens.length, index + 3); i++) {
            String token = tokens[i].toLowerCase();
            if (UNIT_CONVERSION.containsKey(token)) return token;
            if (token.equals("each")) return "each";
        }
        return "kg"; // default
    }
    
    private boolean extractPercentage(String[] tokens, int index) {
        // Check if next token is %
        return index + 1 < tokens.length && tokens[index + 1].equals("%");
    }
    
    private String determineCurrency(String symbol, String word) {
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
    
    private ParsedDutyRate buildMathematicalExpression(List<DutyComponent> components, 
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
    
    private double convertToUSD(double value, String currency) {
        if (currency == null || currency.isEmpty()) {
            return value; // Assume USD if no currency specified
        }
        
        Double conversionRate = CURRENCY_TO_USD.get(currency.toLowerCase());
        return conversionRate != null ? value * conversionRate : value;
    }
    
    private String standardizeUnit(String unit) {
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
}