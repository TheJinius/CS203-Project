package com.ubs.tariffapp.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for duty calculation helper methods.
 * Contains shared logic used by both DutyService and DutyController.
 */
public class DutyCalculationUtils {

    private DutyCalculationUtils() {
        // Private constructor to prevent instantiation
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Calculate Ad Valorem duty amount.
     * Formula: productValue × (ratePercent / 100)
     * 
     * @param productValue The value of the product in dollars
     * @param ratePercent The ad valorem rate as a percentage (e.g., 5.5 for 5.5%)
     * @return The calculated ad valorem duty amount
     */
    public static double calculateAdValoremAmount(double productValue, double ratePercent) {
        return productValue * ratePercent / 100.0;
    }

    /**
     * Calculate billing units for specific duty.
     * Formula: productQuantity / multiplier
     * 
     * @param productQuantity The quantity of product in the original units
     * @param multiplier The conversion multiplier (e.g., 100 for per-100-kg)
     * @return The number of billing units
     */
    public static double calculateBillingUnits(double productQuantity, double multiplier) {
        return productQuantity / multiplier;
    }

    /**
     * Calculate Specific Duty amount.
     * Formula: billingUnits × amountPerUnit
     * 
     * @param productQuantity The quantity of product in the original units
     * @param multiplier The conversion multiplier
     * @param amountPerUnit The duty amount per billing unit
     * @return The calculated specific duty amount
     */
    public static double calculateSpecificAmount(double productQuantity, double multiplier, double amountPerUnit) {
        double billingUnits = calculateBillingUnits(productQuantity, multiplier);
        return billingUnits * amountPerUnit;
    }

    /**
     * Calculate Combined Duty result based on Mixed vs Compound logic.
     * 
     * @param mixedOrCompound "C" for Compound (sum), "M" for Mixed (max)
     * @param adValorem The Ad Valorem component amount
     * @param specific The Specific Duty component amount
     * @return The calculated tariff amount based on the combination type
     */
    public static double calculateCombinedDutyResult(String mixedOrCompound, double adValorem, double specific) {
        if ("C".equals(mixedOrCompound)) {
            // Compound: Sum both components
            return adValorem + specific;
        } else if ("M".equals(mixedOrCompound)) {
            // Mixed: Take maximum of both
            return Math.max(adValorem, specific);
        } else {
            return 0.0;
        }
    }

    /**
     * Round a double value to 2 decimal places using HALF_EVEN rounding mode.
     * This is the standard rounding used for monetary calculations.
     * 
     * @param value The value to round
     * @return The rounded value
     */
    public static double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_EVEN)
                .doubleValue();
    }
}
