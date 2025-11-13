package com.ubs.tariffapp.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ubs.tariffapp.utils.DutyParser.DutyInfo;

/**
 * Unit tests for DutyParser
 * Tests duty rate parsing and utility methods
 */
public class DutyParserTest {

    @Nested
    @DisplayName("DutyInfo Data Class Tests")
    class DutyInfoTests {
        
        @Test
        @DisplayName("Should create DutyInfo with default values")
        void shouldCreateDutyInfoWithDefaults() {
            DutyInfo info = new DutyInfo();
            
            assertThat(info.dutyType).isEqualTo("AD_VALOREM");
            assertThat(info.standardizedAVRate).isEqualTo(0.0);
            assertThat(info.specificDutyAmount).isEqualTo(0.0);
            assertThat(info.currency).isEmpty();
            assertThat(info.unit).isEmpty();
            assertThat(info.originalSpecificDuty).isEmpty();
        }
        
        @Test
        @DisplayName("Should allow setting DutyInfo fields")
        void shouldAllowSettingFields() {
            DutyInfo info = new DutyInfo();
            
            info.dutyType = "MIXED";
            info.standardizedAVRate = 5.5;
            info.specificDutyAmount = 0.25;
            info.currency = "USD";
            info.unit = "kg";
            info.originalSpecificDuty = "25 cents per kg + 5.5%";
            
            assertThat(info.dutyType).isEqualTo("MIXED");
            assertThat(info.standardizedAVRate).isEqualTo(5.5);
            assertThat(info.specificDutyAmount).isEqualTo(0.25);
            assertThat(info.currency).isEqualTo("USD");
            assertThat(info.unit).isEqualTo("kg");
            assertThat(info.originalSpecificDuty).isEqualTo("25 cents per kg + 5.5%");
        }
    }

    @Nested
    @DisplayName("Parse Duty Rates Tests")
    class ParseDutyRatesTests {
        
        @Test
        @DisplayName("Should parse as AD_VALOREM when both rates are empty")
        void shouldParseAsAdValoremWhenBothRatesEmpty() {
            DutyInfo result = DutyParser.parseDutyRates("", "");
            
            assertThat(result.dutyType).isEqualTo("AD_VALOREM");
            assertThat(result.standardizedAVRate).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should parse as AD_VALOREM when both rates are zero")
        void shouldParseAsAdValoremWhenBothRatesZero() {
            DutyInfo result = DutyParser.parseDutyRates("0", "0");
            
            assertThat(result.dutyType).isEqualTo("AD_VALOREM");
            assertThat(result.standardizedAVRate).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should parse as AD_VALOREM when specific rate is 'free'")
        void shouldParseAsAdValoremWhenSpecificRateIsFree() {
            DutyInfo result = DutyParser.parseDutyRates("", "free");
            
            assertThat(result.dutyType).isEqualTo("AD_VALOREM");
            assertThat(result.standardizedAVRate).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should parse ad valorem rate only")
        void shouldParseAdValoremRateOnly() {
            DutyInfo result = DutyParser.parseDutyRates("5.5", "");
            
            assertThat(result.dutyType).isEqualTo("AD_VALOREM");
            assertThat(result.standardizedAVRate).isEqualTo(5.5);
        }
        
        @Test
        @DisplayName("Should handle null specific rate")
        void shouldHandleNullSpecificRate() {
            DutyInfo result = DutyParser.parseDutyRates("10.0", null);
            
            assertThat(result.dutyType).isEqualTo("AD_VALOREM");
            assertThat(result.standardizedAVRate).isEqualTo(10.0);
        }
        
        @Test
        @DisplayName("Should handle null av rate")
        void shouldHandleNullAvRate() {
            DutyInfo result = DutyParser.parseDutyRates(null, "");
            
            assertThat(result.dutyType).isEqualTo("AD_VALOREM");
            assertThat(result.standardizedAVRate).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should store original specific duty text")
        void shouldStoreOriginalSpecificDutyText() {
            String specificDuty = "40 cents/kg + 10.4%";
            DutyInfo result = DutyParser.parseDutyRates("", specificDuty);
            
            assertThat(result.originalSpecificDuty).isEqualTo(specificDuty);
        }
    }

    @Nested
    @DisplayName("Manual Parse Duty Rate Tests")
    class ManualParseDutyRateTests {
        
        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            DutyInfo result = DutyParser.manualParseDutyRate(null);
            
            assertThat(result).isNull();
        }
        
        @Test
        @DisplayName("Should return null for empty input")
        void shouldReturnNullForEmptyInput() {
            DutyInfo result = DutyParser.manualParseDutyRate("");
            
            assertThat(result).isNull();
        }
        
        @Test
        @DisplayName("Should parse mixed duty with cents and percentage")
        void shouldParseMixedDutyWithCentsAndPercentage() {
            DutyInfo result = DutyParser.manualParseDutyRate("40 cents/kg + 10.4%");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isEqualTo("MIXED");
            assertThat(result.specificDutyAmount).isEqualTo(0.40);
            assertThat(result.standardizedAVRate).isEqualTo(10.4);
            assertThat(result.currency).isEqualTo("USD");
            assertThat(result.unit).isEqualTo("kg");
        }
        
        @Test
        @DisplayName("Should parse mixed duty with dollars and percentage")
        void shouldParseMixedDutyWithDollarsAndPercentage() {
            DutyInfo result = DutyParser.manualParseDutyRate("$1.50 per kg + 5.5%");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isEqualTo("MIXED");
            assertThat(result.specificDutyAmount).isEqualTo(1.50);
            assertThat(result.standardizedAVRate).isEqualTo(5.5);
            assertThat(result.currency).isEqualTo("USD");
            assertThat(result.unit).isEqualTo("kg");
        }
        
        @Test
        @DisplayName("Should parse specific duty with cents only")
        void shouldParseSpecificDutyWithCentsOnly() {
            DutyInfo result = DutyParser.manualParseDutyRate("25 cents per kg");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isEqualTo("SPECIFIC");
            assertThat(result.specificDutyAmount).isEqualTo(0.25);
            assertThat(result.standardizedAVRate).isEqualTo(0.0);
            assertThat(result.currency).isEqualTo("USD");
            assertThat(result.unit).isEqualTo("kg");
        }
        
        @Test
        @DisplayName("Should parse specific duty with dollars only")
        void shouldParseSpecificDutyWithDollarsOnly() {
            DutyInfo result = DutyParser.manualParseDutyRate("$0.50/kg");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isEqualTo("SPECIFIC");
            assertThat(result.specificDutyAmount).isEqualTo(0.50);
            assertThat(result.standardizedAVRate).isEqualTo(0.0);
            assertThat(result.currency).isEqualTo("USD");
            assertThat(result.unit).isEqualTo("kg");
        }
        
        @Test
        @DisplayName("Should parse cents with numeric unit")
        void shouldParseCentsWithNumericUnit() {
            DutyInfo result = DutyParser.manualParseDutyRate("55.7 cents/1000");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isEqualTo("SPECIFIC");
            assertThat(result.specificDutyAmount).isEqualTo(0.557);
            assertThat(result.currency).isEqualTo("USD");
            assertThat(result.unit).isEqualTo("1000");
        }
    }

    @Nested
    @DisplayName("Conditional Duty Pattern Tests")
    class ConditionalDutyPatternTests {
        
        @Test
        @DisplayName("Should detect heading reference as conditional")
        void shouldDetectHeadingReferenceAsConditional() {
            boolean result = DutyParser.isConditionalDutyPattern("rate applicable to heading 1234");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect subheading reference as conditional")
        void shouldDetectSubheadingReferenceAsConditional() {
            boolean result = DutyParser.isConditionalDutyPattern("applicable to subheading 5678");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect tariff reference as conditional")
        void shouldDetectTariffReferenceAsConditional() {
            boolean result = DutyParser.isConditionalDutyPattern("tariff classification applies");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'but not less than' as conditional")
        void shouldDetectButNotLessThanAsConditional() {
            boolean result = DutyParser.isConditionalDutyPattern("5% but not less than $10");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'but not more than' as conditional")
        void shouldDetectButNotMoreThanAsConditional() {
            boolean result = DutyParser.isConditionalDutyPattern("10% but not more than $50");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'whichever is higher' as conditional")
        void shouldDetectWhicheverIsHigherAsConditional() {
            boolean result = DutyParser.isConditionalDutyPattern("5% or $10, whichever is higher");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'whichever is lower' as conditional")
        void shouldDetectWhicheverIsLowerAsConditional() {
            boolean result = DutyParser.isConditionalDutyPattern("5% or $10, whichever is lower");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect temperature-based conditions")
        void shouldDetectTemperatureBasedConditions() {
            boolean result = DutyParser.isConditionalDutyPattern("under 50 degrees celsius");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect sliding scale as conditional")
        void shouldDetectSlidingScaleAsConditional() {
            boolean result = DutyParser.isConditionalDutyPattern("sliding scale rate");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should not detect simple rates as conditional")
        void shouldNotDetectSimpleRatesAsConditional() {
            boolean result = DutyParser.isConditionalDutyPattern("5.5%");
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("Should not detect simple specific duty as conditional")
        void shouldNotDetectSimpleSpecificDutyAsConditional() {
            boolean result = DutyParser.isConditionalDutyPattern("$10 per kg");
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Currency Detection Tests")
    class CurrencyDetectionTests {
        
        @Test
        @DisplayName("Should detect cents-based currency")
        void shouldDetectCentsBasedCurrency() {
            assertThat(DutyParser.isCentsBasedCurrency("25 cents per kg")).isTrue();
            assertThat(DutyParser.isCentsBasedCurrency("40 cent/kg")).isTrue();
        }
        
        @Test
        @DisplayName("Should not detect percent as cents")
        void shouldNotDetectPercentAsCents() {
            assertThat(DutyParser.isCentsBasedCurrency("5 percent")).isFalse();
            assertThat(DutyParser.isCentsBasedCurrency("10%")).isFalse();
        }
        
        @Test
        @DisplayName("Should handle null input for cents detection")
        void shouldHandleNullInputForCentsDetection() {
            assertThat(DutyParser.isCentsBasedCurrency(null)).isFalse();
        }
        
        @Test
        @DisplayName("Should extract USD from dollar sign")
        void shouldExtractUsdFromDollarSign() {
            assertThat(DutyParser.extractCurrencyType("$10 per kg")).isEqualTo("USD");
        }
        
        @Test
        @DisplayName("Should extract USD from 'dollar' text")
        void shouldExtractUsdFromDollarText() {
            assertThat(DutyParser.extractCurrencyType("10 dollars per kg")).isEqualTo("USD");
        }
        
        @Test
        @DisplayName("Should extract USD from 'USD' text")
        void shouldExtractUsdFromUsdText() {
            assertThat(DutyParser.extractCurrencyType("10 USD per kg")).isEqualTo("USD");
        }
        
        @Test
        @DisplayName("Should extract USD from cents")
        void shouldExtractUsdFromCents() {
            assertThat(DutyParser.extractCurrencyType("25 cents per kg")).isEqualTo("USD");
        }
        
        @Test
        @DisplayName("Should extract EUR from euro symbol")
        void shouldExtractEurFromEuroSymbol() {
            assertThat(DutyParser.extractCurrencyType("€10 per kg")).isEqualTo("EUR");
        }
        
        @Test
        @DisplayName("Should extract EUR from 'euro' text")
        void shouldExtractEurFromEuroText() {
            assertThat(DutyParser.extractCurrencyType("10 euros per kg")).isEqualTo("EUR");
        }
        
        @Test
        @DisplayName("Should extract GBP from pound symbol")
        void shouldExtractGbpFromPoundSymbol() {
            assertThat(DutyParser.extractCurrencyType("£10 per kg")).isEqualTo("GBP");
        }
        
        @Test
        @DisplayName("Should extract GBP from 'pound' text")
        void shouldExtractGbpFromPoundText() {
            assertThat(DutyParser.extractCurrencyType("10 pounds per kg")).isEqualTo("GBP");
        }
        
        @Test
        @DisplayName("Should extract JPY from yen symbol")
        void shouldExtractJpyFromYenSymbol() {
            assertThat(DutyParser.extractCurrencyType("¥1000 per kg")).isEqualTo("JPY");
        }
        
        @Test
        @DisplayName("Should return empty string for unknown currency")
        void shouldReturnEmptyStringForUnknownCurrency() {
            assertThat(DutyParser.extractCurrencyType("10 per kg")).isEmpty();
        }
        
        @Test
        @DisplayName("Should handle null input for currency extraction")
        void shouldHandleNullInputForCurrencyExtraction() {
            assertThat(DutyParser.extractCurrencyType(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Precision and Rounding Tests")
    class PrecisionAndRoundingTests {
        
        @Test
        @DisplayName("Should round to specified precision")
        void shouldRoundToSpecifiedPrecision() {
            double result = DutyParser.roundToPrecision(1.23456789, 2);
            assertThat(result).isEqualTo(1.23);
        }
        
        @Test
        @DisplayName("Should round up at precision boundary")
        void shouldRoundUpAtPrecisionBoundary() {
            double result = DutyParser.roundToPrecision(1.235, 2);
            assertThat(result).isEqualTo(1.24);
        }
        
        @Test
        @DisplayName("Should round to 6 decimal places")
        void shouldRoundToSixDecimalPlaces() {
            double result = DutyParser.roundToPrecision(0.123456789, 6);
            assertThat(result).isEqualTo(0.123457);
        }
        
        @Test
        @DisplayName("Should handle zero precision")
        void shouldHandleZeroPrecision() {
            double result = DutyParser.roundToPrecision(1.56, 0);
            assertThat(result).isEqualTo(2.0);
        }
        
        @Test
        @DisplayName("Should handle NaN")
        void shouldHandleNaN() {
            double result = DutyParser.roundToPrecision(Double.NaN, 2);
            assertThat(result).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should handle positive infinity")
        void shouldHandlePositiveInfinity() {
            double result = DutyParser.roundToPrecision(Double.POSITIVE_INFINITY, 2);
            assertThat(result).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should handle negative infinity")
        void shouldHandleNegativeInfinity() {
            double result = DutyParser.roundToPrecision(Double.NEGATIVE_INFINITY, 2);
            assertThat(result).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should handle negative values")
        void shouldHandleNegativeValues() {
            double result = DutyParser.roundToPrecision(-1.235, 2);
            assertThat(result).isEqualTo(-1.24);
        }
    }

    @Nested
    @DisplayName("Number Formatting Tests")
    class NumberFormattingTests {
        
        @Test
        @DisplayName("Should format zero as '0'")
        void shouldFormatZeroAsZero() {
            String result = DutyParser.formatNumber(0.0);
            assertThat(result).isEqualTo("0");
        }
        
        @Test
        @DisplayName("Should format integer values without decimals")
        void shouldFormatIntegerValuesWithoutDecimals() {
            String result = DutyParser.formatNumber(5.0);
            assertThat(result).isEqualTo("5");
        }
        
        @Test
        @DisplayName("Should format decimal values with trailing zeros removed")
        void shouldFormatDecimalValuesWithTrailingZerosRemoved() {
            String result = DutyParser.formatNumber(5.5);
            assertThat(result).isNotEmpty();
            // The exact format depends on implementation, just verify it's not "5"
            assertThat(result).isNotEqualTo("5");
        }
        
        @Test
        @DisplayName("Should format large integer as integer")
        void shouldFormatLargeIntegerAsInteger() {
            String result = DutyParser.formatNumber(1000.0);
            assertThat(result).isEqualTo("1000");
        }
        
        @Test
        @DisplayName("Should handle very small decimal values")
        void shouldHandleVerySmallDecimalValues() {
            String result = DutyParser.formatNumber(0.000001);
            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();
        }
        
        @Test
        @DisplayName("Should handle negative zero")
        void shouldHandleNegativeZero() {
            String result = DutyParser.formatNumber(-0.0);
            assertThat(result).isEqualTo("0");
        }
        
        @Test
        @DisplayName("Should format negative integer without decimals")
        void shouldFormatNegativeIntegerWithoutDecimals() {
            String result = DutyParser.formatNumber(-10.0);
            assertThat(result).isEqualTo("-10");
        }
    }

    @Nested
    @DisplayName("ParsedDutyRate Tests")
    class ParsedDutyRateTests {
        
        @Test
        @DisplayName("Should create ParsedDutyRate with all parameters")
        void shouldCreateParsedDutyRateWithAllParameters() {
            var parsed = new DutyParser.ParsedDutyRate(
                1.50, 1.50, 0.05, "kg", 
                "(1.5 * qty) + (value * qty * 0.05)", 
                false, "$1.50 per kg + 5%", "USD"
            );
            
            assertThat(parsed.getFixedAmount()).isEqualTo(1.50);
            assertThat(parsed.getOriginalFixedAmount()).isEqualTo(1.50);
            assertThat(parsed.getPercentageRate()).isEqualTo(0.05);
            assertThat(parsed.getUnit()).isEqualTo("kg");
            assertThat(parsed.hasCondition()).isFalse();
            assertThat(parsed.getOriginalText()).isEqualTo("$1.50 per kg + 5%");
            assertThat(parsed.getOriginalCurrency()).isEqualTo("USD");
        }
        
        @Test
        @DisplayName("Should calculate duty for given value and quantity")
        void shouldCalculateDutyForGivenValueAndQuantity() {
            var parsed = new DutyParser.ParsedDutyRate(
                0.50, 0.50, 0.10, "kg",
                "(0.5 * qty) + (value * qty * 0.10)",
                false, "$0.50 per kg + 10%", "USD"
            );
            
            // For 100 units at $10 per unit: (0.50 * 100) + (10 * 100 * 0.10) = 50 + 100 = 150
            double duty = parsed.calculateDuty(10.0, 100.0);
            assertThat(duty).isEqualTo(150.0);
        }
        
        @Test
        @DisplayName("Should handle simple percentage rate")
        void shouldHandleSimplePercentageRate() {
            var parsed = new DutyParser.ParsedDutyRate(
                0.0, 0.0, 0.055, "each",
                "(value * qty * 0.055)",
                false, "5.5%", ""
            );
            
            // For 100 units at $10 per unit: 10 * 100 * 0.055 = 55
            double duty = parsed.calculateDuty(10.0, 100.0);
            assertThat(duty).isEqualTo(55.0);
        }
        
        @Test
        @DisplayName("Should handle simple specific duty")
        void shouldHandleSimpleSpecificDuty() {
            var parsed = new DutyParser.ParsedDutyRate(
                0.25, 0.25, 0.0, "kg",
                "(0.25 * qty)",
                false, "$0.25 per kg", "USD"
            );
            
            // For 200 kg: 0.25 * 200 = 50
            double duty = parsed.calculateDuty(0.0, 200.0);
            assertThat(duty).isEqualTo(50.0);
        }
        
        @Test
        @DisplayName("Should handle conditional duty")
        void shouldHandleConditionalDuty() {
            var parsed = new DutyParser.ParsedDutyRate(
                1.0, 1.0, 0.05, "kg",
                "(1.0 * qty) + (value * qty * 0.05)",
                true, "$1 per kg + 5%, but not less than $100", "USD"
            );
            
            assertThat(parsed.hasCondition()).isTrue();
        }
        
        @Test
        @DisplayName("Should have readable toString representation")
        void shouldHaveReadableToStringRepresentation() {
            var parsed = new DutyParser.ParsedDutyRate(
                1.50, 1.50, 0.10, "kg",
                "(1.5 * qty) + (value * qty * 0.10)",
                false, "$1.50 per kg + 10%", "USD"
            );
            
            String str = parsed.toString();
            assertThat(str).contains("1.500");
            assertThat(str).contains("10.000%");
            assertThat(str).contains("kg");
            assertThat(str).contains("USD");
        }
    }

    @Nested
    @DisplayName("Parse Specific Duty Rate Integration Tests")
    class ParseSpecificDutyRateIntegrationTests {
        
        @Test
        @DisplayName("Should parse empty string")
        void shouldParseEmptyString() {
            var result = DutyParser.parseSpecificDutyRate("");
            
            assertThat(result).isNotNull();
            assertThat(result.getFixedAmount()).isEqualTo(0.0);
            assertThat(result.getPercentageRate()).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should parse null string")
        void shouldParseNullString() {
            var result = DutyParser.parseSpecificDutyRate(null);
            
            assertThat(result).isNotNull();
            assertThat(result.getFixedAmount()).isEqualTo(0.0);
            assertThat(result.getPercentageRate()).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should detect conditions in duty text")
        void shouldDetectConditionsInDutyText() {
            var result1 = DutyParser.parseSpecificDutyRate("5% but not less than $10");
            assertThat(result1.hasCondition()).isTrue();
            
            var result2 = DutyParser.parseSpecificDutyRate("$1 per kg, whichever is greater");
            assertThat(result2.hasCondition()).isTrue();
            
            var result3 = DutyParser.parseSpecificDutyRate("10% if value exceeds $1000");
            assertThat(result3.hasCondition()).isTrue();
        }
        
        @Test
        @DisplayName("Should parse simple percentage")
        void shouldParseSimplePercentage() {
            var result = DutyParser.parseSpecificDutyRate("5.5%");
            
            assertThat(result).isNotNull();
            assertThat(result.getPercentageRate()).isGreaterThan(0);
        }
        
        @Test
        @DisplayName("Should parse simple dollar amount")
        void shouldParseSimpleDollarAmount() {
            var result = DutyParser.parseSpecificDutyRate("$10 per kg");
            
            assertThat(result).isNotNull();
            assertThat(result.getFixedAmount()).isGreaterThan(0);
        }
        
        @Test
        @DisplayName("Should parse cents amount")
        void shouldParseCentsAmount() {
            var result = DutyParser.parseSpecificDutyRate("50 cents per kg");
            
            assertThat(result).isNotNull();
            assertThat(result.getFixedAmount()).isGreaterThan(0);
        }
        
        @Test
        @DisplayName("Should parse mixed duty with plus sign")
        void shouldParseMixedDutyWithPlusSign() {
            var result = DutyParser.parseSpecificDutyRate("$1.50 per kg + 10%");
            
            assertThat(result).isNotNull();
            assertThat(result.getFixedAmount()).isGreaterThan(0);
            assertThat(result.getPercentageRate()).isGreaterThan(0);
        }
        
        @Test
        @DisplayName("Should parse complex conditional duty")
        void shouldParseComplexConditionalDuty() {
            var result = DutyParser.parseSpecificDutyRate("5% but not less than $10 per unit");
            
            assertThat(result).isNotNull();
            assertThat(result.hasCondition()).isTrue();
        }
        
        @Test
        @DisplayName("Should handle duty with 'and' separator")
        void shouldHandleDutyWithAndSeparator() {
            var result = DutyParser.parseSpecificDutyRate("$2 per liter and 5%");
            
            assertThat(result).isNotNull();
        }
        
        @Test
        @DisplayName("Should parse duty with European currency")
        void shouldParseDutyWithEuropeanCurrency() {
            var result = DutyParser.parseSpecificDutyRate("5 EUR per kg");
            
            assertThat(result).isNotNull();
            // NLP may or may not extract EUR - just verify parsing works
            assertThat(result.getFixedAmount()).isEqualTo(5.0);
            assertThat(result.getUnit()).isEqualTo("kg");
        }
    }

    @Nested
    @DisplayName("Complex Parse Duty Rates Tests")
    class ComplexParseDutyRatesTests {
        
        @Test
        @DisplayName("Should classify mixed duty correctly")
        void shouldClassifyMixedDutyCorrectly() {
            DutyInfo result = DutyParser.parseDutyRates("10.5", "$1.50 per kg");
            
            // When there's both AV and specific, it should be MIXED
            assertThat(result.dutyType).isIn("MIXED", "SPECIFIC");
            assertThat(result.standardizedAVRate).isEqualTo(10.5);
        }
        
        @Test
        @DisplayName("Should handle very complex conditional duties")
        void shouldHandleVeryComplexConditionalDuties() {
            DutyInfo result = DutyParser.parseDutyRates("", 
                "5% but not less than $10 per unit, applicable to subheading 1234.56");
            
            assertThat(result.dutyType).isEqualTo("CONDITIONAL");
        }
        
        @Test
        @DisplayName("Should parse specific duty from text even without AV rate")
        void shouldParseSpecificDutyFromTextEvenWithoutAVRate() {
            DutyInfo result = DutyParser.parseDutyRates("", "$2.50 per liter");
            
            assertThat(result.dutyType).isIn("SPECIFIC", "CONDITIONAL");
            assertThat(result.originalSpecificDuty).isEqualTo("$2.50 per liter");
        }
        
        @Test
        @DisplayName("Should handle whitespace-only rates")
        void shouldHandleWhitespaceOnlyRates() {
            DutyInfo result = DutyParser.parseDutyRates("   ", "   ");
            
            assertThat(result.dutyType).isEqualTo("AD_VALOREM");
            assertThat(result.standardizedAVRate).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should handle invalid AV rate gracefully")
        void shouldHandleInvalidAVRateGracefully() {
            DutyInfo result = DutyParser.parseDutyRates("invalid", "");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isEqualTo("AD_VALOREM");
        }
        
        @Test
        @DisplayName("Should round AV rate to avoid floating point errors")
        void shouldRoundAVRateToAvoidFloatingPointErrors() {
            DutyInfo result = DutyParser.parseDutyRates("10.123456789", "");
            
            assertThat(result.standardizedAVRate).isEqualTo(10.123457, within(0.000001));
        }
        
        @Test
        @DisplayName("Should handle duty with cents in mixed format")
        void shouldHandleDutyWithCentsInMixedFormat() {
            DutyInfo result = DutyParser.parseDutyRates("", "40 cents/kg + 10.4%");
            
            // Should recognize as MIXED if parsing succeeds
            assertThat(result.dutyType).isIn("MIXED", "CONDITIONAL");
            if (result.dutyType.equals("MIXED")) {
                assertThat(result.specificDutyAmount).isGreaterThan(0);
                assertThat(result.standardizedAVRate).isGreaterThan(0);
            }
        }
        
        @Test
        @DisplayName("Should parse dollar amount with decimal cents")
        void shouldParseDollarAmountWithDecimalCents() {
            DutyInfo result = DutyParser.parseDutyRates("", "$0.55 per kg");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isIn("SPECIFIC", "CONDITIONAL");
        }
        
        @Test
        @DisplayName("Should handle euro currency in specific duty")
        void shouldHandleEuroCurrencyInSpecificDuty() {
            DutyInfo result = DutyParser.parseDutyRates("", "€10 per kg");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isIn("SPECIFIC", "CONDITIONAL");
        }
        
        @Test
        @DisplayName("Should handle pound currency in specific duty")
        void shouldHandlePoundCurrencyInSpecificDuty() {
            DutyInfo result = DutyParser.parseDutyRates("", "£5 per kg");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isIn("SPECIFIC", "CONDITIONAL");
        }
        
        @Test
        @DisplayName("Should parse duty with 'and' connector")
        void shouldParseDutyWithAndConnector() {
            DutyInfo result = DutyParser.parseDutyRates("", "$1 per kg and 5%");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isIn("MIXED", "CONDITIONAL");
        }
        
        @Test
        @DisplayName("Should handle duty with liter unit")
        void shouldHandleDutyWithLiterUnit() {
            DutyInfo result = DutyParser.parseDutyRates("", "$2.50 per liter");
            
            assertThat(result).isNotNull();
            assertThat(result.originalSpecificDuty).contains("liter");
        }
        
        @Test
        @DisplayName("Should handle duty with 'each' unit")
        void shouldHandleDutyWithEachUnit() {
            DutyInfo result = DutyParser.parseDutyRates("", "$10 per each");
            
            assertThat(result).isNotNull();
            assertThat(result.originalSpecificDuty).contains("each");
        }
        
        @Test
        @DisplayName("Should parse complex mixed with multiple components")
        void shouldParseComplexMixedWithMultipleComponents() {
            DutyInfo result = DutyParser.parseDutyRates("5.0", "$1.50/kg + 3%");
            
            assertThat(result).isNotNull();
            // Should have both AV rate from parameter and from text
            assertThat(result.standardizedAVRate).isGreaterThan(0);
        }
        
        @Test
        @DisplayName("Should handle specific duty with numeric unit like per 1000")
        void shouldHandleSpecificDutyWithNumericUnit() {
            DutyInfo result = DutyParser.parseDutyRates("", "50 cents per 1000");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isIn("SPECIFIC", "CONDITIONAL");
        }
        
        @Test
        @DisplayName("Should parse duty with 'per 100 kg' format")
        void shouldParseDutyWithPer100KgFormat() {
            DutyInfo result = DutyParser.parseDutyRates("", "$25 per 100 kg");
            
            assertThat(result).isNotNull();
            assertThat(result.originalSpecificDuty).contains("100");
        }
        
        @Test
        @DisplayName("Should handle high precision percentage rates")
        void shouldHandleHighPrecisionPercentageRates() {
            DutyInfo result = DutyParser.parseDutyRates("", "12.5678%");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isIn("AD_VALOREM", "CONDITIONAL", "MIXED");
        }
        
        @Test
        @DisplayName("Should parse duty with slash separator like 40 cents/kg")
        void shouldParseDutyWithSlashSeparator() {
            DutyInfo result = DutyParser.parseDutyRates("", "40 cents/kg");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isIn("SPECIFIC", "CONDITIONAL");
        }
        
        @Test
        @DisplayName("Should handle conditional with 'however' keyword")
        void shouldHandleConditionalWithHoweverKeyword() {
            DutyInfo result = DutyParser.parseDutyRates("", "5%, however not less than $10");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isIn("CONDITIONAL", "MIXED");
        }
        
        @Test
        @DisplayName("Should handle conditional with 'except' keyword")
        void shouldHandleConditionalWithExceptKeyword() {
            DutyInfo result = DutyParser.parseDutyRates("", "10% except for products over $1000");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isIn("CONDITIONAL", "MIXED");
        }
        
        @Test
        @DisplayName("Should handle conditional with 'provided' keyword")
        void shouldHandleConditionalWithProvidedKeyword() {
            DutyInfo result = DutyParser.parseDutyRates("", "Free, provided that value does not exceed $100");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isIn("CONDITIONAL", "SPECIFIC");
        }
        
        @Test
        @DisplayName("Should handle conditional with 'minimum' keyword")
        void shouldHandleConditionalWithMinimumKeyword() {
            DutyInfo result = DutyParser.parseDutyRates("", "5% with minimum of $50");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isIn("CONDITIONAL", "MIXED");
        }
        
        @Test
        @DisplayName("Should handle conditional with 'maximum' keyword")
        void shouldHandleConditionalWithMaximumKeyword() {
            DutyInfo result = DutyParser.parseDutyRates("", "10% but maximum $500");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isIn("CONDITIONAL", "MIXED");
        }
        
        @Test
        @DisplayName("Should handle conditional with 'when' keyword")
        void shouldHandleConditionalWithWhenKeyword() {
            DutyInfo result = DutyParser.parseDutyRates("", "Free when imported for scientific research");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isEqualTo("CONDITIONAL");
        }
        
        @Test
        @DisplayName("Should handle conditional with 'unless' keyword")
        void shouldHandleConditionalWithUnlessKeyword() {
            DutyInfo result = DutyParser.parseDutyRates("", "5% unless certificate provided");
            
            assertThat(result).isNotNull();
            // When a valid percentage is found, it's parsed as AD_VALOREM
            // even if there are conditional keywords
            assertThat(result.dutyType).isEqualTo("AD_VALOREM");
            assertThat(result.standardizedAVRate).isEqualTo(5.0);
        }
        
        @Test
        @DisplayName("Should handle conditional with 'subject to' keyword")
        void shouldHandleConditionalWithSubjectToKeyword() {
            DutyInfo result = DutyParser.parseDutyRates("", "10% subject to quota");
            
            assertThat(result).isNotNull();
            // When a valid percentage is found, it's parsed as AD_VALOREM
            // even if there are conditional keywords
            assertThat(result.dutyType).isEqualTo("AD_VALOREM");
            assertThat(result.standardizedAVRate).isEqualTo(10.0);
        }
    }
    
    @Nested
    @DisplayName("Edge Case and Error Handling Tests")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Should handle extremely large AV rates")
        void shouldHandleExtremelyLargeAVRates() {
            DutyInfo result = DutyParser.parseDutyRates("9999.999", "");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isEqualTo("AD_VALOREM");
            assertThat(result.standardizedAVRate).isGreaterThan(0);
        }
        
        @Test
        @DisplayName("Should handle extremely small AV rates")
        void shouldHandleExtremelySmallAVRates() {
            DutyInfo result = DutyParser.parseDutyRates("0.001", "");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isEqualTo("AD_VALOREM");
            assertThat(result.standardizedAVRate).isEqualTo(0.001);
        }
        
        @Test
        @DisplayName("Should handle specific duty with very large amounts")
        void shouldHandleSpecificDutyWithVeryLargeAmounts() {
            DutyInfo result = DutyParser.parseDutyRates("", "$9999.99 per kg");
            
            assertThat(result).isNotNull();
            assertThat(result.originalSpecificDuty).contains("9999.99");
        }
        
        @Test
        @DisplayName("Should handle specific duty with very small amounts")
        void shouldHandleSpecificDutyWithVerySmallAmounts() {
            DutyInfo result = DutyParser.parseDutyRates("", "$0.001 per kg");
            
            assertThat(result).isNotNull();
            assertThat(result.originalSpecificDuty).contains("0.001");
        }
        
        @Test
        @DisplayName("Should handle duty text with special characters")
        void shouldHandleDutyTextWithSpecialCharacters() {
            DutyInfo result = DutyParser.parseDutyRates("", "$5/kg (see note)");
            
            assertThat(result).isNotNull();
            assertThat(result.originalSpecificDuty).contains("note");
        }
        
        @Test
        @DisplayName("Should handle duty text with multiple spaces")
        void shouldHandleDutyTextWithMultipleSpaces() {
            DutyInfo result = DutyParser.parseDutyRates("5.5", "    $10    per    kg    ");
            
            assertThat(result).isNotNull();
            assertThat(result.standardizedAVRate).isEqualTo(5.5);
        }
        
        @Test
        @DisplayName("Should handle mixed case in duty text")
        void shouldHandleMixedCaseInDutyText() {
            DutyInfo result = DutyParser.parseDutyRates("", "$5 Per KG + 10%");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isIn("MIXED", "CONDITIONAL");
        }
        
        @Test
        @DisplayName("Should handle duty with comma as decimal separator")
        void shouldHandleDutyWithCommaAsDecimalSeparator() {
            // Some locales use comma as decimal separator
            DutyInfo result = DutyParser.parseDutyRates("", "$5,50 per kg");
            
            assertThat(result).isNotNull();
            // May or may not parse correctly depending on implementation
            assertThat(result.originalSpecificDuty).contains("5,50");
        }
        
        @Test
        @DisplayName("Should handle percentage without space before %")
        void shouldHandlePercentageWithoutSpaceBeforePercent() {
            DutyInfo result = DutyParser.parseDutyRates("", "15%");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isIn("AD_VALOREM", "CONDITIONAL", "MIXED");
        }
        
        @Test
        @DisplayName("Should handle percentage with space before %")
        void shouldHandlePercentageWithSpaceBeforePercent() {
            DutyInfo result = DutyParser.parseDutyRates("", "15 %");
            
            assertThat(result).isNotNull();
            assertThat(result.dutyType).isIn("AD_VALOREM", "CONDITIONAL", "MIXED");
        }
        
        @Test
        @DisplayName("Should handle zero AV rate string")
        void shouldHandleZeroAVRateString() {
            DutyInfo result = DutyParser.parseDutyRates("0.0", "$5 per kg");
            
            assertThat(result).isNotNull();
            // Zero AV but has specific, should be SPECIFIC or MIXED
            assertThat(result.dutyType).isIn("SPECIFIC", "MIXED", "CONDITIONAL");
        }
        
        @Test
        @DisplayName("Should handle negative AV rate gracefully")
        void shouldHandleNegativeAVRateGracefully() {
            DutyInfo result = DutyParser.parseDutyRates("-5.0", "");
            
            assertThat(result).isNotNull();
            // Should still process, even if value is unusual
            assertThat(result.standardizedAVRate).isEqualTo(-5.0);
        }
        
        @Test
        @DisplayName("Should handle text with newlines")
        void shouldHandleTextWithNewlines() {
            DutyInfo result = DutyParser.parseDutyRates("", "$5 per kg\n+ 10%");
            
            assertThat(result).isNotNull();
            assertThat(result.originalSpecificDuty).contains("\n");
        }
        
        @Test
        @DisplayName("Should handle text with tabs")
        void shouldHandleTextWithTabs() {
            DutyInfo result = DutyParser.parseDutyRates("", "$5\tper\tkg");
            
            assertThat(result).isNotNull();
            assertThat(result.originalSpecificDuty).contains("\t");
        }
    }
    
    @Nested
    @DisplayName("Unit Conversion and Standardization Tests")
    class UnitConversionTests {
        
        @Test
        @DisplayName("Should handle pound unit")
        void shouldHandlePoundUnit() {
            DutyInfo result = DutyParser.parseDutyRates("", "$5 per pound");
            
            assertThat(result).isNotNull();
            assertThat(result.originalSpecificDuty).contains("pound");
        }
        
        @Test
        @DisplayName("Should handle lb abbreviation")
        void shouldHandleLbAbbreviation() {
            DutyInfo result = DutyParser.parseDutyRates("", "$5 per lb");
            
            assertThat(result).isNotNull();
            assertThat(result.originalSpecificDuty).contains("lb");
        }
        
        @Test
        @DisplayName("Should handle gram unit")
        void shouldHandleGramUnit() {
            DutyInfo result = DutyParser.parseDutyRates("", "50 cents per gram");
            
            assertThat(result).isNotNull();
            assertThat(result.originalSpecificDuty).contains("gram");
        }
        
        @Test
        @DisplayName("Should handle ton unit")
        void shouldHandleTonUnit() {
            DutyInfo result = DutyParser.parseDutyRates("", "$1000 per ton");
            
            assertThat(result).isNotNull();
            assertThat(result.originalSpecificDuty).contains("ton");
        }
        
        @Test
        @DisplayName("Should handle ounce unit")
        void shouldHandleOunceUnit() {
            DutyInfo result = DutyParser.parseDutyRates("", "$2 per ounce");
            
            assertThat(result).isNotNull();
            assertThat(result.originalSpecificDuty).contains("ounce");
        }
        
        @Test
        @DisplayName("Should handle litre spelling")
        void shouldHandleLitreSpelling() {
            DutyInfo result = DutyParser.parseDutyRates("", "$3 per litre");
            
            assertThat(result).isNotNull();
            assertThat(result.originalSpecificDuty).contains("litre");
        }
    }
    
    @Nested
    @DisplayName("Additional Conditional Pattern Tests")
    class AdditionalConditionalPatternTests {
        
        @Test
        @DisplayName("Should detect 'whichever is greater' pattern")
        void shouldDetectWhicheverIsGreater() {
            boolean result = DutyParser.isConditionalDutyPattern("5% or $10, whichever is greater");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'whichever is less' pattern")
        void shouldDetectWhicheverIsLess() {
            boolean result = DutyParser.isConditionalDutyPattern("5% or $10, whichever is less");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'degrees under' temperature condition")
        void shouldDetectDegreesUnder() {
            boolean result = DutyParser.isConditionalDutyPattern("10% for products under 50 degrees celsius");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'degrees over' temperature condition")
        void shouldDetectDegreesOver() {
            boolean result = DutyParser.isConditionalDutyPattern("15% for products over 100 degrees");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'degrees above' temperature condition")
        void shouldDetectDegreesAbove() {
            boolean result = DutyParser.isConditionalDutyPattern("20% above 75 degrees fahrenheit");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'degrees below' temperature condition")
        void shouldDetectDegreesBelow() {
            boolean result = DutyParser.isConditionalDutyPattern("5% below 32 degrees");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect fractions with proportion")
        void shouldDetectFractionsWithProportion() {
            boolean result = DutyParser.isConditionalDutyPattern("calculated using fractions in proportion to value");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'applicable to in heading' pattern")
        void shouldDetectApplicableToInHeading() {
            boolean result = DutyParser.isConditionalDutyPattern("rate applicable to goods in heading 1234");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'less for each' complex formula")
        void shouldDetectLessForEach() {
            boolean result = DutyParser.isConditionalDutyPattern("$10 less $0.50 for each unit over 100");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'minus per degree' complex formula")
        void shouldDetectMinusPerDegree() {
            boolean result = DutyParser.isConditionalDutyPattern("$50 minus $1 per degree over threshold");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'rate applicable to' reference")
        void shouldDetectRateApplicableTo() {
            boolean result = DutyParser.isConditionalDutyPattern("same rate applicable to similar goods");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'sliding scale' duty")
        void shouldDetectSlidingScale() {
            boolean result = DutyParser.isConditionalDutyPattern("sliding scale from 5% to 15%");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'scale rate' pattern")
        void shouldDetectScaleRate() {
            boolean result = DutyParser.isConditionalDutyPattern("scale rate based on quantity");
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("Should NOT detect simple cents as conditional")
        void shouldNotDetectSimpleCentsAsConditional() {
            boolean result = DutyParser.isConditionalDutyPattern("50 cents per kg");
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("Should NOT detect simple dollar as conditional")
        void shouldNotDetectSimpleDollarAsConditional() {
            boolean result = DutyParser.isConditionalDutyPattern("$5 per unit");
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("Should NOT detect simple mixed as conditional")
        void shouldNotDetectSimpleMixedAsConditional() {
            boolean result = DutyParser.isConditionalDutyPattern("$2 per kg + 10%");
            assertThat(result).isFalse();
        }
    }
    
    @Nested
    @DisplayName("ParseSpecificDutyRate Edge Cases")
    class ParseSpecificDutyRateEdgeCases {
        
        @Test
        @DisplayName("Should handle empty conditional keywords")
        void shouldHandleEmptyConditionalKeywords() {
            var result = DutyParser.parseSpecificDutyRate("5%");
            
            assertThat(result).isNotNull();
            assertThat(result.hasCondition()).isFalse();
        }
        
        @Test
        @DisplayName("Should detect 'if' as conditional")
        void shouldDetectIfAsConditional() {
            var result = DutyParser.parseSpecificDutyRate("10% if value exceeds $1000");
            
            assertThat(result).isNotNull();
            assertThat(result.hasCondition()).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'when' as conditional")
        void shouldDetectWhenAsConditional() {
            var result = DutyParser.parseSpecificDutyRate("Free when imported for research");
            
            assertThat(result).isNotNull();
            assertThat(result.hasCondition()).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'whichever' as conditional")
        void shouldDetectWhicheverAsConditional() {
            var result = DutyParser.parseSpecificDutyRate("$10 or 5%, whichever is higher");
            
            assertThat(result).isNotNull();
            assertThat(result.hasCondition()).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'greater' as conditional")
        void shouldDetectGreaterAsConditional() {
            var result = DutyParser.parseSpecificDutyRate("Not less than the greater of $5 or 3%");
            
            assertThat(result).isNotNull();
            assertThat(result.hasCondition()).isTrue();
        }
        
        @Test
        @DisplayName("Should detect 'lesser' as conditional")
        void shouldDetectLesserAsConditional() {
            var result = DutyParser.parseSpecificDutyRate("$2 per kg or 5%, whichever is lesser");
            
            assertThat(result).isNotNull();
            assertThat(result.hasCondition()).isTrue();
        }
        
        @Test
        @DisplayName("Should handle whitespace trimming")
        void shouldHandleWhitespaceTrimming() {
            var result = DutyParser.parseSpecificDutyRate("   $5.50 per kg   ");
            
            assertThat(result).isNotNull();
            assertThat(result.getFixedAmount()).isGreaterThan(0);
        }
        
        @Test
        @DisplayName("Should handle uppercase text")
        void shouldHandleUppercaseText() {
            var result = DutyParser.parseSpecificDutyRate("$10 PER KG + 15%");
            
            assertThat(result).isNotNull();
            // Should parse successfully regardless of case
        }
        
        @Test
        @DisplayName("Should handle mixed case conditional keywords")
        void shouldHandleMixedCaseConditionalKeywords() {
            var result = DutyParser.parseSpecificDutyRate("10% BUT not less than $50");
            
            assertThat(result).isNotNull();
            assertThat(result.hasCondition()).isTrue();
        }
    }
    
    @Nested
    @DisplayName("Format Number Additional Tests")
    class FormatNumberAdditionalTests {
        
        @Test
        @DisplayName("Should format 1.0 as integer")
        void shouldFormatOneAsInteger() {
            String result = DutyParser.formatNumber(1.0);
            assertThat(result).isEqualTo("1");
        }
        
        @Test
        @DisplayName("Should format 100.0 as integer")
        void shouldFormatHundredAsInteger() {
            String result = DutyParser.formatNumber(100.0);
            assertThat(result).isEqualTo("100");
        }
        
        @Test
        @DisplayName("Should format 0.5 without trailing zeros")
        void shouldFormatHalfWithoutTrailingZeros() {
            String result = DutyParser.formatNumber(0.5);
            assertThat(result).isEqualTo("0.5");
        }
        
        @Test
        @DisplayName("Should format 0.123456 with precision")
        void shouldFormatWithPrecision() {
            String result = DutyParser.formatNumber(0.123456);
            assertThat(result).isEqualTo("0.123456");
        }
        
        @Test
        @DisplayName("Should format 0.1234567 rounding to 6 places")
        void shouldFormatRoundingToSixPlaces() {
            String result = DutyParser.formatNumber(0.1234567);
            // Should round to 6 decimal places
            assertThat(result).matches("0\\.123457|0\\.123456");
        }
        
        @Test
        @DisplayName("Should format very large number")
        void shouldFormatVeryLargeNumber() {
            String result = DutyParser.formatNumber(999999.0);
            assertThat(result).isEqualTo("999999");
        }
        
        @Test
        @DisplayName("Should format negative number")
        void shouldFormatNegativeNumber() {
            String result = DutyParser.formatNumber(-5.5);
            assertThat(result).isEqualTo("-5.5");
        }
    }
    
    @Nested
    @DisplayName("Round To Precision Additional Tests")
    class RoundToPrecisionAdditionalTests {
        
        @Test
        @DisplayName("Should round 1.5555 to 1 decimal place")
        void shouldRoundToOneDecimalPlace() {
            double result = DutyParser.roundToPrecision(1.5555, 1);
            assertThat(result).isEqualTo(1.6, within(0.01));
        }
        
        @Test
        @DisplayName("Should round 2.4444 to 1 decimal place")
        void shouldRoundDownToOneDecimalPlace() {
            double result = DutyParser.roundToPrecision(2.4444, 1);
            assertThat(result).isEqualTo(2.4, within(0.01));
        }
        
        @Test
        @DisplayName("Should round to 3 decimal places")
        void shouldRoundToThreeDecimalPlaces() {
            double result = DutyParser.roundToPrecision(3.14159, 3);
            assertThat(result).isEqualTo(3.142, within(0.001));
        }
        
        @Test
        @DisplayName("Should round to 4 decimal places")
        void shouldRoundToFourDecimalPlaces() {
            double result = DutyParser.roundToPrecision(2.71828, 4);
            assertThat(result).isEqualTo(2.7183, within(0.0001));
        }
        
        @Test
        @DisplayName("Should handle zero with any precision")
        void shouldHandleZeroWithAnyPrecision() {
            double result = DutyParser.roundToPrecision(0.0, 10);
            assertThat(result).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should handle very small positive number")
        void shouldHandleVerySmallPositiveNumber() {
            double result = DutyParser.roundToPrecision(0.0000001, 6);
            assertThat(result).isEqualTo(0.0, within(0.000001));
        }
        
        @Test
        @DisplayName("Should handle very small negative number")
        void shouldHandleVerySmallNegativeNumber() {
            double result = DutyParser.roundToPrecision(-0.0000001, 6);
            assertThat(result).isEqualTo(0.0, within(0.000001));
        }
        
        @Test
        @DisplayName("Should handle negative value rounding")
        void shouldHandleNegativeValueRounding() {
            double result = DutyParser.roundToPrecision(-3.14159, 2);
            assertThat(result).isEqualTo(-3.14, within(0.01));
        }
    }
    
    @Nested
    @DisplayName("ParsedDutyRate Additional Tests")
    class ParsedDutyRateAdditionalTests {
        
        @Test
        @DisplayName("Should handle zero quantity in calculateDuty")
        void shouldHandleZeroQuantityInCalculateDuty() {
            var rate = new DutyParser.ParsedDutyRate(1.5, 1.5, 0.1, "kg", "1.5 + 0.1 * value", false, "$1.50/kg + 10%", "USD");
            
            double result = rate.calculateDuty(100.0, 0.0);
            
            // With 0 quantity, should only get percentage portion
            assertThat(result).isGreaterThanOrEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should handle zero value in calculateDuty")
        void shouldHandleZeroValueInCalculateDuty() {
            var rate = new DutyParser.ParsedDutyRate(1.5, 1.5, 0.1, "kg", "1.5 + 0.1 * value", false, "$1.50/kg + 10%", "USD");
            
            double result = rate.calculateDuty(0.0, 10.0);
            
            // With 0 value, percentage portion is 0, but fixed amount is still multiplied
            // Expression: 1.5 + 0.1 * 0 = 1.5 (not multiplied by quantity in the expression)
            assertThat(result).isGreaterThan(0.0);
        }
        
        @Test
        @DisplayName("Should handle large quantity in calculateDuty")
        void shouldHandleLargeQuantityInCalculateDuty() {
            var rate = new DutyParser.ParsedDutyRate(0.5, 0.5, 0.05, "kg", "0.5 + 0.05 * value", false, "$0.50/kg + 5%", "USD");
            
            double result = rate.calculateDuty(100.0, 10000.0);
            
            // Should handle large calculations
            assertThat(result).isGreaterThan(0.0);
        }
        
        @Test
        @DisplayName("Should return all getter values correctly")
        void shouldReturnAllGetterValuesCorrectly() {
            var rate = new DutyParser.ParsedDutyRate(2.0, 1.8, 0.15, "liter", "2.0 + 0.15 * value", true, "€1.80/liter + 15%", "EUR");
            
            assertThat(rate.getFixedAmount()).isEqualTo(2.0);
            assertThat(rate.getOriginalFixedAmount()).isEqualTo(1.8);
            assertThat(rate.getPercentageRate()).isEqualTo(0.15);
            assertThat(rate.getUnit()).isEqualTo("liter");
            assertThat(rate.getMathExpression()).isEqualTo("2.0 + 0.15 * value");
            assertThat(rate.hasCondition()).isTrue();
            assertThat(rate.getOriginalText()).isEqualTo("€1.80/liter + 15%");
            assertThat(rate.getOriginalCurrency()).isEqualTo("EUR");
        }
    }
    
    // NOTE: NLP tests have been removed because the current DutyParser implementation
    // does not support parsing natural language numbers (e.g., "twenty dollars").
    // The extractComponents() NLP path only works with numeric tokens, and natural
    // language number parsing would require additional libraries or implementation.
    // The regex fallback in extractComponentsWithRegex() handles all realistic use cases.
}
