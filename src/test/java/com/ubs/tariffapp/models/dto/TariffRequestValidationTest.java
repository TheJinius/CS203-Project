package com.ubs.tariffapp.models.dto;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Unit tests for Jakarta Bean Validation annotations on TariffRequest.
 * These tests verify that validation constraints are correctly applied.
 */
@DisplayName("TariffRequest Validation Tests")
class TariffRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private TariffRequest createValidTariffRequest() {
        TariffRequest request = new TariffRequest();
        request.setTariffYear(2024);
        request.setReporterCode("USA");
        request.setPartnerCode("CHN");
        request.setTlCode("010121");
        request.setDutyType("0");
        request.setDutyCode("0");
        request.setAdValoremRate(5.0);
        return request;
    }

    @Nested
    @DisplayName("Create Operation Validation")
    class CreateValidation {

        @Test
        @DisplayName("Should pass validation with all required fields for Create")
        void testValidCreateRequest() {
            // Arrange
            TariffRequest request = createValidTariffRequest();

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail when tariffYear is null for Create")
        void testNullTariffYear() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setTariffYear(null);

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).hasSize(1);
            ConstraintViolation<TariffRequest> violation = violations.iterator().next();
            assertThat(violation.getPropertyPath().toString()).isEqualTo("tariffYear");
            assertThat(violation.getMessage()).isEqualTo("Tariff year is required");
        }

        @Test
        @DisplayName("Should fail when tariffYear is below minimum (2000)")
        void testTariffYearBelowMinimum() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setTariffYear(1999);

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).hasSize(1);
            ConstraintViolation<TariffRequest> violation = violations.iterator().next();
            assertThat(violation.getMessage()).isEqualTo("Year must be 2000 or later");
        }

        @Test
        @DisplayName("Should fail when tariffYear exceeds maximum (2100)")
        void testTariffYearAboveMaximum() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setTariffYear(2101);

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).hasSize(1);
            ConstraintViolation<TariffRequest> violation = violations.iterator().next();
            assertThat(violation.getMessage()).isEqualTo("Year must be 2100 or earlier");
        }

        @Test
        @DisplayName("Should fail when reporterCode is blank for Create")
        void testBlankReporterCode() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setReporterCode("");

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
            assertThat(violations).anyMatch(v -> 
                v.getPropertyPath().toString().equals("reporterCode"));
        }

        @Test
        @DisplayName("Should fail when reporterCode is not 3 characters")
        void testReporterCodeInvalidLength() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setReporterCode("US"); // Only 2 characters

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).hasSize(1);
            ConstraintViolation<TariffRequest> violation = violations.iterator().next();
            assertThat(violation.getMessage()).isEqualTo("Reporter code must be exactly 3 characters");
        }

        @Test
        @DisplayName("Should fail when partnerCode is not 3 characters")
        void testPartnerCodeInvalidLength() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setPartnerCode("CHIN"); // 4 characters

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).hasSize(1);
            ConstraintViolation<TariffRequest> violation = violations.iterator().next();
            assertThat(violation.getMessage()).isEqualTo("Partner code must be exactly 3 characters");
        }

        @Test
        @DisplayName("Should fail when tlCode is blank for Create")
        void testBlankTlCode() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setTlCode("");

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).hasSize(1);
            ConstraintViolation<TariffRequest> violation = violations.iterator().next();
            assertThat(violation.getPropertyPath().toString()).isEqualTo("tlCode");
            assertThat(violation.getMessage()).isEqualTo("Product code (TL code) is required");
        }

        @Test
        @DisplayName("Should fail when dutyType is blank for Create")
        void testBlankDutyType() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setDutyType("");

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).hasSize(1);
            assertThat(violations).anyMatch(v -> 
                v.getPropertyPath().toString().equals("dutyType") &&
                v.getMessage().equals("Duty type is required"));
        }

        @Test
        @DisplayName("Should fail when multiple required fields are missing")
        void testMultipleViolations() {
            // Arrange
            TariffRequest request = new TariffRequest();
            // Leave all required fields null/blank

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert - Should have violations for all required fields
            assertThat(violations).hasSizeGreaterThanOrEqualTo(6);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("tariffYear"));
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("reporterCode"));
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("partnerCode"));
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("tlCode"));
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("dutyType"));
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("dutyCode"));
        }
    }

    @Nested
    @DisplayName("Rate Field Validation")
    class RateValidation {

        @Test
        @DisplayName("Should fail when adValoremRate is negative")
        void testNegativeAdValoremRate() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setAdValoremRate(-5.0);

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).hasSize(1);
            ConstraintViolation<TariffRequest> violation = violations.iterator().next();
            assertThat(violation.getPropertyPath().toString()).isEqualTo("adValoremRate");
            assertThat(violation.getMessage()).isEqualTo("Ad valorem rate must be non-negative");
        }

        @Test
        @DisplayName("Should fail when adValoremRate exceeds 100%")
        void testAdValoremRateExceeds100() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setAdValoremRate(101.0);

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).hasSize(1);
            ConstraintViolation<TariffRequest> violation = violations.iterator().next();
            assertThat(violation.getPropertyPath().toString()).isEqualTo("adValoremRate");
            assertThat(violation.getMessage()).isEqualTo("Ad valorem rate cannot exceed 100%");
        }

        @Test
        @DisplayName("Should pass with adValoremRate at boundary (0.0)")
        void testAdValoremRateAtLowerBoundary() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setAdValoremRate(0.0);

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should pass with adValoremRate at boundary (100.0)")
        void testAdValoremRateAtUpperBoundary() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setAdValoremRate(100.0);

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should fail when specificRate is negative")
        void testNegativeSpecificRate() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setAdValoremRate(null);
            request.setSpecificRate(-10.0);
            request.setSpecificRateUnit("kg");

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).hasSize(1);
            ConstraintViolation<TariffRequest> violation = violations.iterator().next();
            assertThat(violation.getPropertyPath().toString()).isEqualTo("specificRate");
            assertThat(violation.getMessage()).isEqualTo("Specific rate must be non-negative");
        }

        @Test
        @DisplayName("Should fail when compoundRate1 is negative")
        void testNegativeCompoundRate1() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setAdValoremRate(null);
            request.setCompoundRate1(-5.0);
            request.setCompoundRate2(10.0);

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).hasSize(1);
            ConstraintViolation<TariffRequest> violation = violations.iterator().next();
            assertThat(violation.getPropertyPath().toString()).isEqualTo("compoundRate1");
            assertThat(violation.getMessage()).isEqualTo("Compound rate 1 must be non-negative");
        }

        @Test
        @DisplayName("Should fail when compoundRate2 is negative")
        void testNegativeCompoundRate2() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setAdValoremRate(null);
            request.setCompoundRate1(5.0);
            request.setCompoundRate2(-10.0);

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).hasSize(1);
            ConstraintViolation<TariffRequest> violation = violations.iterator().next();
            assertThat(violation.getPropertyPath().toString()).isEqualTo("compoundRate2");
            assertThat(violation.getMessage()).isEqualTo("Compound rate 2 must be non-negative");
        }

        @Test
        @DisplayName("Should pass with valid compound rates")
        void testValidCompoundRates() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setAdValoremRate(null);
            request.setCompoundRate1(5.0);
            request.setCompoundRate2(10.0);
            request.setSpecificRateUnit("kg");

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should allow null rate values (optional fields)")
        void testNullRateValues() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setAdValoremRate(null);
            request.setSpecificRate(null);
            request.setCompoundRate1(null);
            request.setCompoundRate2(null);

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("Update Operation Validation")
    class UpdateValidation {

        @Test
        @DisplayName("Should allow missing required fields for Update")
        void testUpdateWithoutRequiredFields() {
            // Arrange
            TariffRequest request = new TariffRequest();
            request.setNote("Updated note");
            request.setTlsSuffix("A");

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Update.class);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should still validate rate constraints for Update")
        void testUpdateWithInvalidRate() {
            // Arrange
            TariffRequest request = new TariffRequest();
            request.setAdValoremRate(-5.0); // Invalid

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Update.class);

            // Assert
            assertThat(violations).hasSize(1);
            assertThat(violations).anyMatch(v -> 
                v.getPropertyPath().toString().equals("adValoremRate"));
        }

        @Test
        @DisplayName("Should still validate tariffYear range for Update if provided")
        void testUpdateWithInvalidYear() {
            // Arrange
            TariffRequest request = new TariffRequest();
            request.setTariffYear(1999); // Below minimum

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Update.class);

            // Assert
            assertThat(violations).hasSize(1);
            assertThat(violations).anyMatch(v -> 
                v.getPropertyPath().toString().equals("tariffYear"));
        }
    }

    @Nested
    @DisplayName("Optional Field Validation")
    class OptionalFieldValidation {

        @Test
        @DisplayName("Should allow any string value for specificRateUnit")
        void testSpecificRateUnitLiberalValidation() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setSpecificRateUnit("custom_unit_type_xyz");

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should allow null specificRateUnit")
        void testNullSpecificRateUnit() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setSpecificRateUnit(null);

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should allow null tlsSuffix")
        void testNullTlsSuffix() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setTlsSuffix(null);

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should allow null note")
        void testNullNote() {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setNote(null);

            // Act
            Set<ConstraintViolation<TariffRequest>> violations = 
                validator.validate(request, TariffRequest.Create.class);

            // Assert
            assertThat(violations).isEmpty();
        }
    }
}
