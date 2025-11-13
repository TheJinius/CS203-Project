package com.ubs.tariffapp.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for DataLoadException.
 * Tests exception construction, message handling, and cause propagation.
 */
@DisplayName("DataLoadException Tests")
class DataLoadExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with message only")
        void testConstructorWithMessage() {
            // Arrange
            String errorMessage = "Failed to load data from database";

            // Act
            DataLoadException exception = new DataLoadException(errorMessage);

            // Assert
            assertThat(exception.getMessage()).isEqualTo(errorMessage);
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("Should create exception with message and cause")
        void testConstructorWithMessageAndCause() {
            // Arrange
            String errorMessage = "Failed to read CSV file";
            IOException cause = new IOException("File not found: data.csv");

            // Act
            DataLoadException exception = new DataLoadException(errorMessage, cause);

            // Assert
            assertThat(exception.getMessage()).isEqualTo(errorMessage);
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getCause()).isInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("Should handle null message")
        void testConstructorWithNullMessage() {
            // Act
            DataLoadException exception = new DataLoadException(null);

            // Assert
            assertThat(exception.getMessage()).isNull();
        }

        @Test
        @DisplayName("Should handle empty message")
        void testConstructorWithEmptyMessage() {
            // Act
            DataLoadException exception = new DataLoadException("");

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
            DataLoadException exception = assertThrows(DataLoadException.class, () -> {
                throw new DataLoadException("Database connection failed");
            });

            assertThat(exception.getMessage()).isEqualTo("Database connection failed");
        }

        @Test
        @DisplayName("Should preserve stack trace when thrown")
        void testStackTracePreservation() {
            // Act
            DataLoadException exception = assertThrows(DataLoadException.class, () -> {
                simulateDataLoad();
            });

            // Assert
            assertThat(exception.getStackTrace()).isNotEmpty();
            assertThat(exception.getStackTrace()[0].getMethodName()).isEqualTo("simulateDataLoad");
        }

        @Test
        @DisplayName("Should preserve cause exception stack trace")
        void testCauseStackTracePreservation() {
            // Act
            DataLoadException exception = assertThrows(DataLoadException.class, () -> {
                simulateDataLoadWithCause();
            });

            // Assert
            assertThat(exception.getCause()).isNotNull();
            assertThat(exception.getCause().getStackTrace()).isNotEmpty();
        }

        private void simulateDataLoad() {
            throw new DataLoadException("Simulated data load failure");
        }

        private void simulateDataLoadWithCause() {
            SQLException sqlException = new SQLException("Connection timeout");
            throw new DataLoadException("Failed to execute query", sqlException);
        }
    }

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTests {

        @Test
        @DisplayName("Should be a RuntimeException")
        void testIsRuntimeException() {
            // Act
            DataLoadException exception = new DataLoadException("Test");

            // Assert
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("Should be an Exception")
        void testIsException() {
            // Act
            DataLoadException exception = new DataLoadException("Test");

            // Assert
            assertThat(exception).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("Should be a Throwable")
        void testIsThrowable() {
            // Act
            DataLoadException exception = new DataLoadException("Test");

            // Assert
            assertThat(exception).isInstanceOf(Throwable.class);
        }
    }

    @Nested
    @DisplayName("Cause Handling Tests")
    class CauseHandlingTests {

        @Test
        @DisplayName("Should handle IOException as cause")
        void testIoExceptionCause() {
            // Arrange
            IOException cause = new IOException("Disk full");

            // Act
            DataLoadException exception = new DataLoadException("Failed to write data", cause);

            // Assert
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getCause().getMessage()).isEqualTo("Disk full");
        }

        @Test
        @DisplayName("Should handle SQLException as cause")
        void testSqlExceptionCause() {
            // Arrange
            SQLException cause = new SQLException("Table not found");

            // Act
            DataLoadException exception = new DataLoadException("Database error", cause);

            // Assert
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getCause().getMessage()).isEqualTo("Table not found");
        }

        @Test
        @DisplayName("Should handle null cause")
        void testNullCause() {
            // Act
            DataLoadException exception = new DataLoadException("Error", null);

            // Assert
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("Should access nested cause information")
        void testNestedCauseAccess() {
            // Arrange
            NullPointerException rootCause = new NullPointerException("Null value encountered");
            IOException intermediateCause = new IOException("Stream error", rootCause);

            // Act
            DataLoadException exception = new DataLoadException("Data load failed", intermediateCause);

            // Assert
            assertThat(exception.getCause()).isEqualTo(intermediateCause);
            assertThat(exception.getCause().getCause()).isEqualTo(rootCause);
        }
    }

    @Nested
    @DisplayName("Real-World Scenarios")
    class RealWorldScenarioTests {

        @Test
        @DisplayName("Should handle file not found scenario")
        void testFileNotFoundScenario() {
            // Act
            DataLoadException exception = assertThrows(DataLoadException.class, () -> {
                loadMissingFile();
            });

            // Assert
            assertThat(exception.getMessage()).contains("Cannot read file");
            assertThat(exception.getCause()).isInstanceOf(IOException.class);
            assertThat(exception.getCause().getMessage()).contains("File not found");
        }

        @Test
        @DisplayName("Should handle database connection failure scenario")
        void testDatabaseConnectionFailureScenario() {
            // Act
            DataLoadException exception = assertThrows(DataLoadException.class, () -> {
                connectToDatabase();
            });

            // Assert
            assertThat(exception.getMessage()).contains("Failed to connect to database");
            assertThat(exception.getCause()).isInstanceOf(SQLException.class);
        }

        @Test
        @DisplayName("Should handle JSON parsing error scenario")
        void testJsonParsingErrorScenario() {
            // Act
            DataLoadException exception = assertThrows(DataLoadException.class, () -> {
                parseInvalidJson();
            });

            // Assert
            assertThat(exception.getMessage()).contains("Invalid JSON format");
            assertThat(exception.getCause()).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should handle resource initialization failure")
        void testResourceInitializationFailure() {
            // Act
            DataLoadException exception = assertThrows(DataLoadException.class, () -> {
                initializeResource();
            });

            // Assert
            assertThat(exception.getMessage()).contains("Resource initialization failed");
            assertThat(exception.getCause()).isNull();
        }

        private void loadMissingFile() {
            IOException cause = new IOException("File not found: /data/tariffs.csv");
            throw new DataLoadException("Cannot read file", cause);
        }

        private void connectToDatabase() {
            SQLException cause = new SQLException("Connection refused");
            throw new DataLoadException("Failed to connect to database", cause);
        }

        private void parseInvalidJson() {
            IllegalArgumentException cause = new IllegalArgumentException("Unexpected token");
            throw new DataLoadException("Invalid JSON format in configuration", cause);
        }

        private void initializeResource() {
            throw new DataLoadException("Resource initialization failed: insufficient memory");
        }
    }
}
