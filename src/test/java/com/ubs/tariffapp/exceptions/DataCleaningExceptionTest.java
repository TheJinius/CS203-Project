package com.ubs.tariffapp.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for DataCleaningException.
 * Tests exception construction, message handling, and line number tracking.
 */
@DisplayName("DataCleaningException Tests")
class DataCleaningExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with message and line number")
        void testConstructorWithMessageAndLineNumber() {
            // Arrange
            String errorMessage = "Invalid data format in CSV";
            int lineNumber = 42;

            // Act
            DataCleaningException exception = new DataCleaningException(errorMessage, lineNumber);

            // Assert
            assertThat(exception.getMessage()).isEqualTo(errorMessage);
            assertThat(exception.getLineNumber()).isEqualTo(lineNumber);
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("Should create exception with message, line number, and cause")
        void testConstructorWithMessageLineNumberAndCause() {
            // Arrange
            String errorMessage = "Failed to parse numeric value";
            int lineNumber = 15;
            NumberFormatException cause = new NumberFormatException("For input string: \"abc\"");

            // Act
            DataCleaningException exception = new DataCleaningException(errorMessage, lineNumber, cause);

            // Assert
            assertThat(exception.getMessage()).isEqualTo(errorMessage);
            assertThat(exception.getLineNumber()).isEqualTo(lineNumber);
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getCause()).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("Should handle line number zero")
        void testConstructorWithZeroLineNumber() {
            // Act
            DataCleaningException exception = new DataCleaningException("Error at beginning", 0);

            // Assert
            assertThat(exception.getLineNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle negative line number")
        void testConstructorWithNegativeLineNumber() {
            // Act
            DataCleaningException exception = new DataCleaningException("Error before start", -1);

            // Assert
            assertThat(exception.getLineNumber()).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("Exception Throwing Tests")
    class ThrowingTests {

        @Test
        @DisplayName("Should be throwable and catchable")
        void testExceptionCanBeThrown() {
            // Act & Assert
            DataCleaningException exception = assertThrows(DataCleaningException.class, () -> {
                throw new DataCleaningException("Invalid column count", 25);
            });

            assertThat(exception.getMessage()).isEqualTo("Invalid column count");
            assertThat(exception.getLineNumber()).isEqualTo(25);
        }

        @Test
        @DisplayName("Should preserve stack trace when thrown")
        void testStackTracePreservation() {
            // Act
            DataCleaningException exception = assertThrows(DataCleaningException.class, () -> {
                simulateDataCleaning();
            });

            // Assert
            assertThat(exception.getStackTrace()).isNotEmpty();
            assertThat(exception.getStackTrace()[0].getMethodName()).isEqualTo("simulateDataCleaning");
        }

        private void simulateDataCleaning() {
            throw new DataCleaningException("Duplicate entry found", 100);
        }
    }

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTests {

        @Test
        @DisplayName("Should be a RuntimeException")
        void testIsRuntimeException() {
            // Act
            DataCleaningException exception = new DataCleaningException("Test", 1);

            // Assert
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should be an Exception")
        void testIsException() {
            // Act
            DataCleaningException exception = new DataCleaningException("Test", 1);

            // Assert
            assertThat(exception).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should be a Throwable")
        void testIsThrowable() {
            // Act
            DataCleaningException exception = new DataCleaningException("Test", 1);

            // Assert
            assertThat(exception).isInstanceOf(Throwable.class);
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return correct line number from getter")
        void testGetLineNumber() {
            // Arrange
            DataCleaningException exception = new DataCleaningException("Error", 123);

            // Act
            int lineNumber = exception.getLineNumber();

            // Assert
            assertThat(lineNumber).isEqualTo(123);
        }

        @Test
        @DisplayName("Should return consistent line number on multiple calls")
        void testGetLineNumberConsistency() {
            // Arrange
            DataCleaningException exception = new DataCleaningException("Error", 456);

            // Act & Assert
            assertThat(exception.getLineNumber()).isEqualTo(456);
            assertThat(exception.getLineNumber()).isEqualTo(456);
            assertThat(exception.getLineNumber()).isEqualTo(456);
        }
    }

    @Nested
    @DisplayName("Real-World Scenarios")
    class RealWorldScenarioTests {

        @Test
        @DisplayName("Should handle CSV parsing error scenario")
        void testCsvParsingErrorScenario() {
            // Act
            DataCleaningException exception = assertThrows(DataCleaningException.class, () -> {
                processInvalidCsvLine();
            });

            // Assert
            assertThat(exception.getMessage()).contains("Invalid CSV format");
            assertThat(exception.getLineNumber()).isEqualTo(42);
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("Should handle numeric conversion error scenario")
        void testNumericConversionErrorScenario() {
            // Act
            DataCleaningException exception = assertThrows(DataCleaningException.class, () -> {
                processInvalidNumericData();
            });

            // Assert
            assertThat(exception.getMessage()).contains("Cannot convert to number");
            assertThat(exception.getLineNumber()).isEqualTo(78);
            assertThat(exception.getCause()).isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("Should handle missing required field scenario")
        void testMissingRequiredFieldScenario() {
            // Act
            DataCleaningException exception = assertThrows(DataCleaningException.class, () -> {
                processMissingField();
            });

            // Assert
            assertThat(exception.getMessage()).contains("Missing required field");
            assertThat(exception.getLineNumber()).isEqualTo(99);
        }

        private void processInvalidCsvLine() {
            throw new DataCleaningException("Invalid CSV format: expected 5 columns, found 3", 42);
        }

        private void processInvalidNumericData() {
            NumberFormatException cause = new NumberFormatException("For input string: \"N/A\"");
            throw new DataCleaningException("Cannot convert to number at column 'price'", 78, cause);
        }

        private void processMissingField() {
            throw new DataCleaningException("Missing required field: 'hsCode'", 99);
        }
    }
}
