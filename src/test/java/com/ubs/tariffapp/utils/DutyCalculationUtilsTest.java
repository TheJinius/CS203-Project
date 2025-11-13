package com.ubs.tariffapp.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for DutyCalculationUtils
 * Tests all calculation methods used for duty calculations
 */
public class DutyCalculationUtilsTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Should throw exception when trying to instantiate utility class")
        void shouldThrowExceptionWhenInstantiating() {
            assertThatThrownBy(() -> {
                // Use reflection to access private constructor
                var constructor = DutyCalculationUtils.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                constructor.newInstance();
            }).hasCauseInstanceOf(UnsupportedOperationException.class)
              .hasRootCauseMessage("This is a utility class and cannot be instantiated");
        }
    }

    @Nested
    @DisplayName("Ad Valorem Calculation Tests")
    class AdValoremCalculationTests {
        
        @Test
        @DisplayName("Should calculate ad valorem for standard rate")
        void shouldCalculateAdValoremForStandardRate() {
            double productValue = 1000.0;
            double ratePercent = 5.5;
            
            double result = DutyCalculationUtils.calculateAdValoremAmount(productValue, ratePercent);
            
            assertThat(result).isEqualTo(55.0, within(0.001));
        }
        
        @Test
        @DisplayName("Should calculate ad valorem for zero rate")
        void shouldCalculateAdValoremForZeroRate() {
            double productValue = 1000.0;
            double ratePercent = 0.0;
            
            double result = DutyCalculationUtils.calculateAdValoremAmount(productValue, ratePercent);
            
            assertThat(result).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should calculate ad valorem for 100% rate")
        void shouldCalculateAdValoremForFullRate() {
            double productValue = 1000.0;
            double ratePercent = 100.0;
            
            double result = DutyCalculationUtils.calculateAdValoremAmount(productValue, ratePercent);
            
            assertThat(result).isEqualTo(1000.0);
        }
        
        @Test
        @DisplayName("Should calculate ad valorem for fractional rate")
        void shouldCalculateAdValoremForFractionalRate() {
            double productValue = 1234.56;
            double ratePercent = 3.75;
            
            double result = DutyCalculationUtils.calculateAdValoremAmount(productValue, ratePercent);
            
            assertThat(result).isEqualTo(46.296, within(0.001));
        }
        
        @Test
        @DisplayName("Should handle zero product value")
        void shouldHandleZeroProductValue() {
            double productValue = 0.0;
            double ratePercent = 10.0;
            
            double result = DutyCalculationUtils.calculateAdValoremAmount(productValue, ratePercent);
            
            assertThat(result).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should calculate ad valorem for large values")
        void shouldCalculateAdValoremForLargeValues() {
            double productValue = 1000000.0;
            double ratePercent = 15.5;
            
            double result = DutyCalculationUtils.calculateAdValoremAmount(productValue, ratePercent);
            
            assertThat(result).isEqualTo(155000.0);
        }
    }

    @Nested
    @DisplayName("Billing Units Calculation Tests")
    class BillingUnitsCalculationTests {
        
        @Test
        @DisplayName("Should calculate billing units for standard multiplier")
        void shouldCalculateBillingUnitsForStandardMultiplier() {
            double productQuantity = 500.0;
            double multiplier = 100.0;
            
            double result = DutyCalculationUtils.calculateBillingUnits(productQuantity, multiplier);
            
            assertThat(result).isEqualTo(5.0);
        }
        
        @Test
        @DisplayName("Should calculate billing units when multiplier is 1")
        void shouldCalculateBillingUnitsWhenMultiplierIsOne() {
            double productQuantity = 500.0;
            double multiplier = 1.0;
            
            double result = DutyCalculationUtils.calculateBillingUnits(productQuantity, multiplier);
            
            assertThat(result).isEqualTo(500.0);
        }
        
        @Test
        @DisplayName("Should calculate billing units for fractional result")
        void shouldCalculateBillingUnitsForFractionalResult() {
            double productQuantity = 150.0;
            double multiplier = 100.0;
            
            double result = DutyCalculationUtils.calculateBillingUnits(productQuantity, multiplier);
            
            assertThat(result).isEqualTo(1.5);
        }
        
        @Test
        @DisplayName("Should handle zero quantity")
        void shouldHandleZeroQuantity() {
            double productQuantity = 0.0;
            double multiplier = 100.0;
            
            double result = DutyCalculationUtils.calculateBillingUnits(productQuantity, multiplier);
            
            assertThat(result).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should calculate billing units for large quantities")
        void shouldCalculateBillingUnitsForLargeQuantities() {
            double productQuantity = 10000.0;
            double multiplier = 1000.0;
            
            double result = DutyCalculationUtils.calculateBillingUnits(productQuantity, multiplier);
            
            assertThat(result).isEqualTo(10.0);
        }
    }

    @Nested
    @DisplayName("Specific Amount Calculation Tests")
    class SpecificAmountCalculationTests {
        
        @Test
        @DisplayName("Should calculate specific amount for standard values")
        void shouldCalculateSpecificAmountForStandardValues() {
            double productQuantity = 500.0;
            double multiplier = 100.0;
            double amountPerUnit = 25.0;
            
            double result = DutyCalculationUtils.calculateSpecificAmount(productQuantity, multiplier, amountPerUnit);
            
            assertThat(result).isEqualTo(125.0);
        }
        
        @Test
        @DisplayName("Should calculate specific amount with multiplier of 1")
        void shouldCalculateSpecificAmountWithMultiplierOne() {
            double productQuantity = 50.0;
            double multiplier = 1.0;
            double amountPerUnit = 10.0;
            
            double result = DutyCalculationUtils.calculateSpecificAmount(productQuantity, multiplier, amountPerUnit);
            
            assertThat(result).isEqualTo(500.0);
        }
        
        @Test
        @DisplayName("Should calculate specific amount for fractional billing units")
        void shouldCalculateSpecificAmountForFractionalUnits() {
            double productQuantity = 150.0;
            double multiplier = 100.0;
            double amountPerUnit = 20.0;
            
            double result = DutyCalculationUtils.calculateSpecificAmount(productQuantity, multiplier, amountPerUnit);
            
            assertThat(result).isEqualTo(30.0);
        }
        
        @Test
        @DisplayName("Should handle zero quantity")
        void shouldHandleZeroQuantity() {
            double productQuantity = 0.0;
            double multiplier = 100.0;
            double amountPerUnit = 25.0;
            
            double result = DutyCalculationUtils.calculateSpecificAmount(productQuantity, multiplier, amountPerUnit);
            
            assertThat(result).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should handle zero amount per unit")
        void shouldHandleZeroAmountPerUnit() {
            double productQuantity = 500.0;
            double multiplier = 100.0;
            double amountPerUnit = 0.0;
            
            double result = DutyCalculationUtils.calculateSpecificAmount(productQuantity, multiplier, amountPerUnit);
            
            assertThat(result).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should calculate specific amount for decimal values")
        void shouldCalculateSpecificAmountForDecimalValues() {
            double productQuantity = 123.45;
            double multiplier = 10.0;
            double amountPerUnit = 5.50;
            
            double result = DutyCalculationUtils.calculateSpecificAmount(productQuantity, multiplier, amountPerUnit);
            
            assertThat(result).isEqualTo(67.8975, within(0.0001));
        }
    }

    @Nested
    @DisplayName("Combined Duty Calculation Tests")
    class CombinedDutyCalculationTests {
        
        @Test
        @DisplayName("Should sum components for Compound duty (C)")
        void shouldSumComponentsForCompoundDuty() {
            String mixedOrCompound = "C";
            double adValorem = 50.0;
            double specific = 75.0;
            
            double result = DutyCalculationUtils.calculateCombinedDutyResult(mixedOrCompound, adValorem, specific);
            
            assertThat(result).isEqualTo(125.0);
        }
        
        @Test
        @DisplayName("Should take maximum for Mixed duty (M)")
        void shouldTakeMaximumForMixedDuty() {
            String mixedOrCompound = "M";
            double adValorem = 50.0;
            double specific = 75.0;
            
            double result = DutyCalculationUtils.calculateCombinedDutyResult(mixedOrCompound, adValorem, specific);
            
            assertThat(result).isEqualTo(75.0);
        }
        
        @Test
        @DisplayName("Should take maximum when ad valorem is higher")
        void shouldTakeMaximumWhenAdValoremIsHigher() {
            String mixedOrCompound = "M";
            double adValorem = 100.0;
            double specific = 75.0;
            
            double result = DutyCalculationUtils.calculateCombinedDutyResult(mixedOrCompound, adValorem, specific);
            
            assertThat(result).isEqualTo(100.0);
        }
        
        @Test
        @DisplayName("Should return 0 for unknown type")
        void shouldReturnZeroForUnknownType() {
            String mixedOrCompound = "X";
            double adValorem = 50.0;
            double specific = 75.0;
            
            double result = DutyCalculationUtils.calculateCombinedDutyResult(mixedOrCompound, adValorem, specific);
            
            assertThat(result).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should handle zero values for Compound")
        void shouldHandleZeroValuesForCompound() {
            String mixedOrCompound = "C";
            double adValorem = 0.0;
            double specific = 0.0;
            
            double result = DutyCalculationUtils.calculateCombinedDutyResult(mixedOrCompound, adValorem, specific);
            
            assertThat(result).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should handle zero values for Mixed")
        void shouldHandleZeroValuesForMixed() {
            String mixedOrCompound = "M";
            double adValorem = 0.0;
            double specific = 0.0;
            
            double result = DutyCalculationUtils.calculateCombinedDutyResult(mixedOrCompound, adValorem, specific);
            
            assertThat(result).isEqualTo(0.0);
        }
        
        @Test
        @DisplayName("Should handle equal values for Mixed")
        void shouldHandleEqualValuesForMixed() {
            String mixedOrCompound = "M";
            double adValorem = 50.0;
            double specific = 50.0;
            
            double result = DutyCalculationUtils.calculateCombinedDutyResult(mixedOrCompound, adValorem, specific);
            
            assertThat(result).isEqualTo(50.0);
        }
        
        @Test
        @DisplayName("Should handle null type as unknown")
        void shouldHandleNullType() {
            String mixedOrCompound = null;
            double adValorem = 50.0;
            double specific = 75.0;
            
            double result = DutyCalculationUtils.calculateCombinedDutyResult(mixedOrCompound, adValorem, specific);
            
            assertThat(result).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Rounding Tests")
    class RoundingTests {
        
        @Test
        @DisplayName("Should round to 2 decimal places - round up")
        void shouldRoundUpToTwoDecimals() {
            double value = 123.456;
            
            double result = DutyCalculationUtils.roundToTwoDecimals(value);
            
            assertThat(result).isEqualTo(123.46);
        }
        
        @Test
        @DisplayName("Should round to 2 decimal places - round down")
        void shouldRoundDownToTwoDecimals() {
            double value = 123.454;
            
            double result = DutyCalculationUtils.roundToTwoDecimals(value);
            
            assertThat(result).isEqualTo(123.45);
        }
        
        @Test
        @DisplayName("Should handle values with less than 2 decimals")
        void shouldHandleValuesWithLessThanTwoDecimals() {
            double value = 123.4;
            
            double result = DutyCalculationUtils.roundToTwoDecimals(value);
            
            assertThat(result).isEqualTo(123.40);
        }
        
        @Test
        @DisplayName("Should handle integer values")
        void shouldHandleIntegerValues() {
            double value = 123.0;
            
            double result = DutyCalculationUtils.roundToTwoDecimals(value);
            
            assertThat(result).isEqualTo(123.00);
        }
        
        @Test
        @DisplayName("Should handle zero")
        void shouldHandleZero() {
            double value = 0.0;
            
            double result = DutyCalculationUtils.roundToTwoDecimals(value);
            
            assertThat(result).isEqualTo(0.00);
        }
        
        @Test
        @DisplayName("Should use HALF_EVEN rounding mode (banker's rounding)")
        void shouldUseHalfEvenRounding() {
            // 2.5 should round to 2 (even)
            double value1 = 2.125;
            double result1 = DutyCalculationUtils.roundToTwoDecimals(value1);
            assertThat(result1).isEqualTo(2.12);
            
            // 2.135 should round to 2.14 (even)
            double value2 = 2.135;
            double result2 = DutyCalculationUtils.roundToTwoDecimals(value2);
            assertThat(result2).isEqualTo(2.14);
        }
        
        @Test
        @DisplayName("Should handle negative values")
        void shouldHandleNegativeValues() {
            double value = -123.456;
            
            double result = DutyCalculationUtils.roundToTwoDecimals(value);
            
            assertThat(result).isEqualTo(-123.46);
        }
        
        @Test
        @DisplayName("Should handle very small values")
        void shouldHandleVerySmallValues() {
            double value = 0.001;
            
            double result = DutyCalculationUtils.roundToTwoDecimals(value);
            
            assertThat(result).isEqualTo(0.00);
        }
        
        @Test
        @DisplayName("Should handle very large values")
        void shouldHandleVeryLargeValues() {
            double value = 999999999.999;
            
            double result = DutyCalculationUtils.roundToTwoDecimals(value);
            
            assertThat(result).isEqualTo(1000000000.00);
        }
    }

    @Nested
    @DisplayName("Integration Tests - Realistic Scenarios")
    class IntegrationTests {
        
        @Test
        @DisplayName("Should calculate complete compound duty scenario")
        void shouldCalculateCompleteCompoundDutyScenario() {
            // Scenario: Product value $1000, quantity 250kg
            // Ad valorem: 5%, Specific: $0.40 per kg, Compound (sum both)
            double productValue = 1000.0;
            double productQuantity = 250.0;
            double avRate = 5.0;
            double multiplier = 1.0;
            double amountPerUnit = 0.40;
            
            double adValoremAmount = DutyCalculationUtils.calculateAdValoremAmount(productValue, avRate);
            double specificAmount = DutyCalculationUtils.calculateSpecificAmount(productQuantity, multiplier, amountPerUnit);
            double totalDuty = DutyCalculationUtils.calculateCombinedDutyResult("C", adValoremAmount, specificAmount);
            double roundedTotal = DutyCalculationUtils.roundToTwoDecimals(totalDuty);
            
            assertThat(adValoremAmount).isEqualTo(50.0);
            assertThat(specificAmount).isEqualTo(100.0);
            assertThat(roundedTotal).isEqualTo(150.00);
        }
        
        @Test
        @DisplayName("Should calculate complete mixed duty scenario")
        void shouldCalculateCompleteMixedDutyScenario() {
            // Scenario: Product value $2000, quantity 500kg
            // Ad valorem: 10%, Specific: $0.25 per 100kg, Mixed (take max)
            double productValue = 2000.0;
            double productQuantity = 500.0;
            double avRate = 10.0;
            double multiplier = 100.0;
            double amountPerUnit = 0.25;
            
            double adValoremAmount = DutyCalculationUtils.calculateAdValoremAmount(productValue, avRate);
            double specificAmount = DutyCalculationUtils.calculateSpecificAmount(productQuantity, multiplier, amountPerUnit);
            double totalDuty = DutyCalculationUtils.calculateCombinedDutyResult("M", adValoremAmount, specificAmount);
            double roundedTotal = DutyCalculationUtils.roundToTwoDecimals(totalDuty);
            
            assertThat(adValoremAmount).isEqualTo(200.0);
            assertThat(specificAmount).isEqualTo(1.25);
            assertThat(roundedTotal).isEqualTo(200.00); // Max of 200 and 1.25
        }
    }
}
