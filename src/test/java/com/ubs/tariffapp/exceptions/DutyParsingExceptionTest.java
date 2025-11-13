package com.ubs.tariffapp.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for DutyParsingException.
 * Tests exception construction, message handling, and cause propagation.
 */
@DisplayName("DutyParsingException Tests")
class DutyParsingExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with message only")
        void testConstructorWithMessage() {
            // Arrange
            String errorMessage = "Invalid duty rate format: '15% + $5.50/kg'";

            // Act
            DutyParsingException exception = new DutyParsingException(errorMessage);

            // Assert
            assertThat(exception.getMessage()).isEqualTo(errorMessage);
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("Should create exception with message and cause")
        void testConstructorWithMessageAndCause() {
            // Arrange
            String errorMessage = "Cannot parse duty rate";
            NumberFormatException cause = new NumberFormatException("For input string: \"ABC%\"");

            // Act
            DutyParsingException exception = new DutyParsingException(errorMessage, cause);

            // Assert
            assertThat(exception.getMessage()).isEqualTo(errorMessage);
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getCause()).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("Should handle null message")
        void testConstructorWithNullMessage() {
            // Act
            DutyParsingException exception = new DutyParsingException(null);

            // Assert
            assertThat(exception.getMessage()).isNull();
        }

        @Test
        @DisplayName("Should handle empty message")
        void testConstructorWithEmptyMessage() {
            // Act
            DutyParsingException exception = new DutyParsingException("");

            // Assert
            assertThat(exception.getMessage()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Exception Throwing Tests")
    class ThrowingTests {

        @Test
        @DisplayName("Should be throwable and catchable")
        void testExceptionCanBeThrown() {
            // Act & Assert
            DutyParsingException exception = assertThrows(DutyParsingException.class, () -> {
                throw new DutyParsingException("Invalid percentage format");
            });

            assertThat(exception.getMessage()).isEqualTo("Invalid percentage format");
        }

        @Test
        @DisplayName("Should preserve stack trace when thrown")
        void testStackTracePreservation() {
            // Act
            DutyParsingException exception = assertThrows(DutyParsingException.class, () -> {
                simulateDutyParsing();
            });

            // Assert
            assertThat(exception.getStackTrace()).isNotEmpty();
            assertThat(exception.getStackTrace()[0].getMethodName()).isEqualTo("simulateDutyParsing");
        }

        @Test
        @DisplayName("Should preserve cause exception stack trace")
        void testCauseStackTracePreservation() {
            // Act
            DutyParsingException exception = assertThrows(DutyParsingException.class, () -> {
                simulateDutyParsingWithCause();
            });

            // Assert
            assertThat(exception.getCause()).isNotNull();
            assertThat(exception.getCause().getStackTrace()).isNotEmpty();
        }

        private void simulateDutyParsing() {
            throw new DutyParsingException("Failed to parse duty rate");
        }

        private void simulateDutyParsingWithCause() {
            NumberFormatException numberException = new NumberFormatException("Invalid number");
            throw new DutyParsingException("Parsing failed", numberException);
        }
    }

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTests {

        @Test
        @DisplayName("Should be a RuntimeException")
        void testIsRuntimeException() {
            // Act
            DutyParsingException exception = new DutyParsingException("Test");

            // Assert
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should be an Exception")
        void testIsException() {
            // Act
            DutyParsingException exception = new DutyParsingException("Test");

            // Assert
            assertThat(exception).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should be a Throwable")
        void testIsThrowable() {
            // Act
            DutyParsingException exception = new DutyParsingException("Test");

            // Assert
            assertThat(exception).isInstanceOf(Throwable.class);
        }
    }

    @Nested
    @DisplayName("Cause Handling Tests")
    class CauseHandlingTests {

        @Test
        @DisplayName("Should handle NumberFormatException as cause")
        void testNumberFormatExceptionCause() {
            // Arrange
            NumberFormatException cause = new NumberFormatException("For input string: \"25.5%abc\"");

            // Act
            DutyParsingException exception = new DutyParsingException("Invalid numeric value in duty rate", cause);

            // Assert
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getCause().getMessage()).isEqualTo("For input string: \"25.5%abc\"");
        }

        @Test
        @DisplayName("Should handle IllegalArgumentException as cause")
        void testIllegalArgumentExceptionCause() {
            // Arrange
            IllegalArgumentException cause = new IllegalArgumentException("Negative duty rate not allowed");

            // Act
            DutyParsingException exception = new DutyParsingException("Invalid duty rate value", cause);

            // Assert
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getCause().getMessage()).isEqualTo("Negative duty rate not allowed");
        }

        @Test
        @DisplayName("Should handle null cause")
        void testNullCause() {
            // Act
            DutyParsingException exception = new DutyParsingException("Error", null);

            // Assert
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("Should access nested cause information")
        void testNestedCauseAccess() {
            // Arrange
            IllegalStateException rootCause = new IllegalStateException("Parser not initialized");
            IllegalArgumentException intermediateCause = new IllegalArgumentException("Invalid input", rootCause);

            // Act
            DutyParsingException exception = new DutyParsingException("Parsing failed", intermediateCause);

            // Assert
            assertThat(exception.getCause()).isEqualTo(intermediateCause);
            assertThat(exception.getCause().getCause()).isEqualTo(rootCause);
        }
    }

    @Nested
    @DisplayName("Real-World Scenarios")
    class RealWorldScenarioTests {

        @Test
        @DisplayName("Should handle invalid percentage format")
        void testInvalidPercentageFormat() {
            // Act
            DutyParsingException exception = assertThrows(DutyParsingException.class, () -> {
                parseInvalidPercentage();
            });

            // Assert
            assertThat(exception.getMessage()).contains("Invalid percentage");
            assertThat(exception.getCause()).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("Should handle invalid compound duty format")
        void testInvalidCompoundDutyFormat() {
            // Act
            DutyParsingException exception = assertThrows(DutyParsingException.class, () -> {
                parseCompoundDuty();
            });

            // Assert
            assertThat(exception.getMessage()).contains("Cannot parse compound duty");
        }

        @Test
        @DisplayName("Should handle missing percentage symbol")
        void testMissingPercentageSymbol() {
            // Act
            DutyParsingException exception = assertThrows(DutyParsingException.class, () -> {
                parseMissingPercentSymbol();
            });

            // Assert
            assertThat(exception.getMessage()).contains("Expected '%' symbol");
        }

        @Test
        @DisplayName("Should handle invalid specific duty format")
        void testInvalidSpecificDutyFormat() {
            // Act
            DutyParsingException exception = assertThrows(DutyParsingException.class, () -> {
                parseInvalidSpecificDuty();
            });

            // Assert
            assertThat(exception.getMessage()).contains("Invalid specific duty");
            assertThat(exception.getCause()).isNotNull();
        }

        @Test
        @DisplayName("Should handle empty duty rate string")
        void testEmptyDutyRateString() {
            // Act
            DutyParsingException exception = assertThrows(DutyParsingException.class, () -> {
                parseEmptyDuty();
            });

            // Assert
            assertThat(exception.getMessage()).contains("Duty rate cannot be empty");
        }

        @Test
        @DisplayName("Should handle duty rate with invalid characters")
        void testDutyRateWithInvalidCharacters() {
            // Act
            DutyParsingException exception = assertThrows(DutyParsingException.class, () -> {
                parseInvalidCharacters();
            });

            // Assert
            assertThat(exception.getMessage()).contains("Invalid characters in duty rate");
        }

        @Test
        @DisplayName("Should handle negative duty rate")
        void testNegativeDutyRate() {
            // Act
            DutyParsingException exception = assertThrows(DutyParsingException.class, () -> {
                parseNegativeDuty();
            });

            // Assert
            assertThat(exception.getMessage()).contains("Duty rate cannot be negative");
        }

        private void parseInvalidPercentage() {
            NumberFormatException cause = new NumberFormatException("For input string: \"XYZ\"");
            throw new DutyParsingException("Invalid percentage value: 'XYZ%'", cause);
        }

        private void parseCompoundDuty() {
            throw new DutyParsingException("Cannot parse compound duty format: '15% + ABC/kg'");
        }

        private void parseMissingPercentSymbol() {
            throw new DutyParsingException("Expected '%' symbol in duty rate: '15'");
        }

        private void parseInvalidSpecificDuty() {
            NumberFormatException cause = new NumberFormatException("Invalid currency amount");
            throw new DutyParsingException("Invalid specific duty format: '$ABC.50/kg'", cause);
        }

        private void parseEmptyDuty() {
            throw new DutyParsingException("Duty rate cannot be empty or null");
        }

        private void parseInvalidCharacters() {
            throw new DutyParsingException("Invalid characters in duty rate: '15%@#$'");
        }

        private void parseNegativeDuty() {
            throw new DutyParsingException("Duty rate cannot be negative: '-5.0%'");
        }
    }
}
