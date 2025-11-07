package com.ubs.tariffapp.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ubs.tariffapp.models.Country;
import com.ubs.tariffapp.models.Product;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.dto.TariffOptionsResponse;
import com.ubs.tariffapp.models.dto.TariffSearchResult;
import com.ubs.tariffapp.models.duty.AdValoremDuty;
import com.ubs.tariffapp.repositories.TariffScheduleRepository;
import com.ubs.tariffapp.testutils.TestEntityFactory;

/**
 * Unit tests for TariffScheduleService using Mockito.
 * Tests business logic in isolation without database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TariffScheduleService Unit Tests")
class TariffScheduleServiceUnitTest {

    @Mock
    private TariffScheduleRepository tariffScheduleRepository;

    @InjectMocks
    private TariffScheduleService tariffScheduleService;

    private TariffSchedule sampleTariff;
    private Country usa;
    private Country china;
    private Product product;

    @BeforeEach
    void setUp() {
        // Use TestEntityFactory for consistent test data
        usa = TestEntityFactory.createReporterCountry();
        china = TestEntityFactory.createPartnerCountry();
        product = TestEntityFactory.createProduct();

        // Create sample tariff with AdValorem duty for testing
        AdValoremDuty duty = TestEntityFactory.createAdValoremDuty();
        sampleTariff = TestEntityFactory.createTariffSchedule(usa, china, product, 
            TestEntityFactory.createDutyType(), duty);
        sampleTariff.setTariffId(1);
    }

    @Nested
    @DisplayName("Search Tariff Schedules Tests")
    class SearchTariffSchedulesTests {

        @Test
        @DisplayName("Should find tariffs by reporter, partner, product, and year")
        void shouldFindTariffsByAllCriteria() {
            // Arrange
            List<TariffSchedule> expectedTariffs = Arrays.asList(sampleTariff);
            when(tariffScheduleRepository.findAllByReporterAndPartnerAndTlAndYear(
                usa.getCountryId(), china.getCountryId(), product.getTlCode(), 2023))
                .thenReturn(expectedTariffs);

            // Act
            List<TariffSchedule> result = tariffScheduleService.searchTariffSchedules(
                usa.getCountryId(), china.getCountryId(), product.getTlCode(), 2023);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(sampleTariff);
            verify(tariffScheduleRepository, times(1))
                .findAllByReporterAndPartnerAndTlAndYear(usa.getCountryId(), china.getCountryId(), product.getTlCode(), 2023);
        }

        @Test
        @DisplayName("Should return empty list when no tariffs found")
        void shouldReturnEmptyListWhenNoTariffsFound() {
            // Arrange
            when(tariffScheduleRepository.findAllByReporterAndPartnerAndTlAndYear(
                anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of());

            // Act
            List<TariffSchedule> result = tariffScheduleService.searchTariffSchedules(
                "999", "888", "99999999", 2023);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle multiple tariffs for same criteria")
        void shouldHandleMultipleTariffs() {
            // Arrange
            AdValoremDuty duty2 = TestEntityFactory.createAdValoremDuty();
            TariffSchedule tariff2 = TestEntityFactory.createTariffSchedule(usa, china, product,
                TestEntityFactory.createDutyType(), duty2);
            tariff2.setTariffId(2);

            List<TariffSchedule> expectedTariffs = Arrays.asList(sampleTariff, tariff2);
            when(tariffScheduleRepository.findAllByReporterAndPartnerAndTlAndYear(
                usa.getCountryId(), china.getCountryId(), product.getTlCode(), 2023))
                .thenReturn(expectedTariffs);

            // Act
            List<TariffSchedule> result = tariffScheduleService.searchTariffSchedules(
                usa.getCountryId(), china.getCountryId(), product.getTlCode(), 2023);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(sampleTariff, tariff2);
        }
    }

    @Nested
    @DisplayName("Get Tariff by ID Tests")
    class GetTariffByIdTests {

        @Test
        @DisplayName("Should find tariff by ID")
        void shouldFindTariffById() {
            // Arrange
            when(tariffScheduleRepository.findById(1))
                .thenReturn(Optional.of(sampleTariff));

            // Act
            TariffSchedule result = tariffScheduleService.getTariffScheduleById(1);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTariffId()).isEqualTo(1);
            verify(tariffScheduleRepository, times(1)).findById(1);
        }

        @Test
        @DisplayName("Should return null when tariff not found by ID")
        void shouldReturnNullWhenTariffNotFound() {
            // Arrange
            when(tariffScheduleRepository.findById(999))
                .thenReturn(Optional.empty());

            // Act
            TariffSchedule result = tariffScheduleService.getTariffScheduleById(999);

            // Assert
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Get Tariff Schedule (without year) Tests")
    class GetTariffScheduleWithoutYearTests {

        @Test
        @DisplayName("Should find tariff without year parameter")
        void shouldFindTariffWithoutYear() {
            // Arrange
            when(tariffScheduleRepository.findByReporterAndPartnerAndTl(
                "840", "156", "01012100"))
                .thenReturn(Optional.of(sampleTariff));

            // Act
            TariffSchedule result = tariffScheduleService.getTariffSchedule(
                "840", "156", "01012100");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(sampleTariff);
        }

        @Test
        @DisplayName("Should return null when tariff not found without year")
        void shouldReturnNullWhenNotFound() {
            // Arrange
            when(tariffScheduleRepository.findByReporterAndPartnerAndTl(
                anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

            // Act
            TariffSchedule result = tariffScheduleService.getTariffSchedule(
                "999", "888", "99999999");

            // Assert
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Get Tariff Schedule (with year) Tests")
    class GetTariffScheduleWithYearTests {

        @Test
        @DisplayName("Should find tariff with year parameter")
        void shouldFindTariffWithYear() {
            // Arrange
            when(tariffScheduleRepository.findByReporterAndPartnerAndTlAndYear(
                "840", "156", "01012100", 2023))
                .thenReturn(Optional.of(sampleTariff));

            // Act
            TariffSchedule result = tariffScheduleService.getTariffSchedule(
                "840", "156", "01012100", 2023);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTariffYear()).isEqualTo(2023);
        }

        @Test
        @DisplayName("Should differentiate between different years")
        void shouldDifferentiateBetweenYears() {
            // Arrange
            AdValoremDuty duty2024 = TestEntityFactory.createAdValoremDuty();
            TariffSchedule tariff2024 = TestEntityFactory.createTariffSchedule(usa, china, product,
                TestEntityFactory.createDutyType(), duty2024);
            tariff2024.setTariffYear(2024);
            
            when(tariffScheduleRepository.findByReporterAndPartnerAndTlAndYear(
                usa.getCountryId(), china.getCountryId(), product.getTlCode(), 2023))
                .thenReturn(Optional.of(sampleTariff));
            
            when(tariffScheduleRepository.findByReporterAndPartnerAndTlAndYear(
                usa.getCountryId(), china.getCountryId(), product.getTlCode(), 2024))
                .thenReturn(Optional.of(tariff2024));

            // Act
            TariffSchedule result2023 = tariffScheduleService.getTariffSchedule(
                usa.getCountryId(), china.getCountryId(), product.getTlCode(), 2023);
            TariffSchedule result2024 = tariffScheduleService.getTariffSchedule(
                usa.getCountryId(), china.getCountryId(), product.getTlCode(), 2024);

            // Assert
            assertThat(result2023.getTariffYear()).isEqualTo(2023);
            assertThat(result2024.getTariffYear()).isEqualTo(2024);
        }
    }

    @Nested
    @DisplayName("Count and First Tariff Tests")
    class CountAndFirstTariffTests {

        @Test
        @DisplayName("Should return total count of tariffs")
        void shouldReturnTotalCount() {
            // Arrange
            when(tariffScheduleRepository.count()).thenReturn(1000L);

            // Act
            long count = tariffScheduleService.getTotalCount();

            // Assert
            assertThat(count).isEqualTo(1000L);
        }

        @Test
        @DisplayName("Should return zero when no tariffs exist")
        void shouldReturnZeroWhenNoTariffs() {
            // Arrange
            when(tariffScheduleRepository.count()).thenReturn(0L);

            // Act
            long count = tariffScheduleService.getTotalCount();

            // Assert
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("Should return first tariff schedule")
        void shouldReturnFirstTariff() {
            // Arrange
            when(tariffScheduleRepository.findAll())
                .thenReturn(Arrays.asList(sampleTariff));

            // Act
            TariffSchedule result = tariffScheduleService.getFirstTariffSchedule();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(sampleTariff);
        }

        @Test
        @DisplayName("Should return null when no tariffs exist")
        void shouldReturnNullWhenNoTariffsExist() {
            // Arrange
            when(tariffScheduleRepository.findAll())
                .thenReturn(List.of());

            // Act
            TariffSchedule result = tariffScheduleService.getFirstTariffSchedule();

            // Assert
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Search Options Tests")
    class SearchOptionsTests {

        @Test
        @DisplayName("Should pad TL code to 8 digits")
        void shouldPadTlCodeTo8Digits() {
            // Arrange
            TariffSearchResult searchResult = new TariffSearchResult(
                1, "Test", "A", "0", "0", 2023);
            
            when(tariffScheduleRepository.findOptions("840", "156", "00010121", 2023))
                .thenReturn(Arrays.asList(searchResult));

            // Act
            TariffOptionsResponse result = tariffScheduleService.searchOptions(
                "840", "156", "10121", 2023);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.options()).hasSize(1);
            assertThat(result.fallbackUsed()).isFalse();
            verify(tariffScheduleRepository).findOptions("840", "156", "00010121", 2023);
        }

        @Test
        @DisplayName("Should use exact match when available")
        void shouldUseExactMatchWhenAvailable() {
            // Arrange
            TariffSearchResult searchResult = new TariffSearchResult(
                1, "Test", "A", "0", "0", 2023);
            
            when(tariffScheduleRepository.findOptions("840", "156", "01012100", 2023))
                .thenReturn(Arrays.asList(searchResult));

            // Act
            TariffOptionsResponse result = tariffScheduleService.searchOptions(
                "840", "156", "01012100", 2023);

            // Assert
            assertThat(result.options()).hasSize(1);
            assertThat(result.fallbackUsed()).isFalse();
            assertThat(result.fallbackReason()).isNull();
        }

        @Test
        @DisplayName("Should fallback to World partner when exact match not found")
        void shouldFallbackToWorldPartner() {
            // Arrange
            TariffSearchResult worldResult = new TariffSearchResult(
                2, "World Fallback", "A", "0", "0", 2023);
            
            when(tariffScheduleRepository.findOptions("840", "156", "01012100", 2023))
                .thenReturn(List.of());
            
            when(tariffScheduleRepository.findWorldOptions("840", "01012100", 2023))
                .thenReturn(Arrays.asList(worldResult));

            // Act
            TariffOptionsResponse result = tariffScheduleService.searchOptions(
                "840", "156", "01012100", 2023);

            // Assert
            assertThat(result.options()).hasSize(1);
            assertThat(result.fallbackUsed()).isTrue();
            assertThat(result.fallbackReason()).contains("World partner fallback");
        }

        @Test
        @DisplayName("Should return empty with message when no options found")
        void shouldReturnEmptyWhenNoOptionsFound() {
            // Arrange
            when(tariffScheduleRepository.findOptions(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of());
            when(tariffScheduleRepository.findWorldOptions(anyString(), anyString(), anyInt()))
                .thenReturn(List.of());

            // Act
            TariffOptionsResponse result = tariffScheduleService.searchOptions(
                "999", "888", "99999999", 2023);

            // Assert
            assertThat(result.options()).isEmpty();
            assertThat(result.fallbackUsed()).isTrue();
            assertThat(result.fallbackReason()).contains("No tariff data available");
        }

        @Test
        @DisplayName("Should handle numeric TL code conversion")
        void shouldHandleNumericTlCodeConversion() {
            // Arrange
            TariffSearchResult searchResult = new TariffSearchResult(
                1, "Test", "A", "0", "0", 2023);
            
            when(tariffScheduleRepository.findOptions("840", "156", "00001234", 2023))
                .thenReturn(Arrays.asList(searchResult));

            // Act
            TariffOptionsResponse result = tariffScheduleService.searchOptions(
                "840", "156", "1234", 2023);

            // Assert
            assertThat(result.options()).isNotEmpty();
            verify(tariffScheduleRepository).findOptions("840", "156", "00001234", 2023);
        }

        @Test
        @DisplayName("Should handle already padded TL code")
        void shouldHandleAlreadyPaddedTlCode() {
            // Arrange
            TariffSearchResult searchResult = new TariffSearchResult(
                1, "Test", "A", "0", "0", 2023);
            
            when(tariffScheduleRepository.findOptions("840", "156", "01012100", 2023))
                .thenReturn(Arrays.asList(searchResult));

            // Act
            TariffOptionsResponse result = tariffScheduleService.searchOptions(
                "840", "156", "01012100", 2023);

            // Assert
            assertThat(result.options()).hasSize(1);
            verify(tariffScheduleRepository).findOptions("840", "156", "01012100", 2023);
        }
    }
}
