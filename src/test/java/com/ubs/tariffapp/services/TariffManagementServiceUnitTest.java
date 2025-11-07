package com.ubs.tariffapp.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import com.ubs.tariffapp.exceptions.InvalidRequestException;
import com.ubs.tariffapp.exceptions.TariffNotFoundException;
import com.ubs.tariffapp.models.Country;
import com.ubs.tariffapp.models.DutyType;
import com.ubs.tariffapp.models.DutyTypeId;
import com.ubs.tariffapp.models.Product;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.dto.TariffRequest;
import com.ubs.tariffapp.models.dto.TariffResponse;
import com.ubs.tariffapp.models.duty.AdValoremDuty;
import com.ubs.tariffapp.models.duty.CombinedDuty;
import com.ubs.tariffapp.models.duty.SpecificDuty;
import com.ubs.tariffapp.repositories.CountryRepository;
import com.ubs.tariffapp.repositories.DutyTypeRepository;
import com.ubs.tariffapp.repositories.ProductRepository;
import com.ubs.tariffapp.repositories.TariffScheduleRepository;
import com.ubs.tariffapp.repositories.duty.AdValoremDutyRepository;
import com.ubs.tariffapp.repositories.duty.CombinedDutyRepository;
import com.ubs.tariffapp.repositories.duty.DutyRepository;
import com.ubs.tariffapp.repositories.duty.OtherDutyRepository;
import com.ubs.tariffapp.repositories.duty.SpecificDutyRepository;

/**
 * Unit tests for TariffManagementService calculation and validation logic.
 * Tests business logic in isolation using mocks.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TariffManagementService Unit Tests")
class TariffManagementServiceUnitTest {

    @Mock
    private TariffScheduleRepository tariffRepository;
    
    @Mock
    private CountryRepository countryRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private DutyTypeRepository dutyTypeRepository;
    
    @Mock
    private DutyRepository dutyRepository;
    
    @Mock
    private AdValoremDutyRepository adValoremDutyRepository;
    
    @Mock
    private SpecificDutyRepository specificDutyRepository;
    
    @Mock
    private CombinedDutyRepository combinedDutyRepository;
    
    @Mock
    private OtherDutyRepository otherDutyRepository;

    @InjectMocks
    private TariffManagementService tariffManagementService;

    private Country usa;
    private Country china;
    private Product product;
    private DutyType dutyType;
    private TariffRequest validRequest;

    @BeforeEach
    void setUp() {
        // Setup common test data
        usa = new Country();
        usa.setCountryId("840");
        usa.setCountryName("United States");

        china = new Country();
        china.setCountryId("156");
        china.setCountryName("China");

        product = new Product();
        product.setTlCode("01012100");
        product.setDescription("Pure-bred breeding horses");

        dutyType = new DutyType();
        DutyTypeId dutyTypeId = new DutyTypeId("0", "0");
        dutyType.setId(dutyTypeId);
        dutyType.setDutyTypeDescription("Standard (MFN)");

        validRequest = new TariffRequest();
        validRequest.setTariffYear(2023);
        validRequest.setReporterCode("840");
        validRequest.setPartnerCode("156");
        validRequest.setTlCode("01012100");
        validRequest.setDutyType("0");
        validRequest.setDutyCode("0");
        validRequest.setAdValoremRate(10.0);
    }

    @Nested
    @DisplayName("Create Tariff Tests")
    class CreateTariffTests {

        @Test
        @DisplayName("Should create tariff with Ad Valorem duty successfully")
        void shouldCreateTariffWithAdValoremDuty() {
            // Arrange
            when(countryRepository.findById("840")).thenReturn(Optional.of(usa));
            when(countryRepository.findById("156")).thenReturn(Optional.of(china));
            when(productRepository.findById("01012100")).thenReturn(Optional.of(product));
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.of(dutyType));
            
            TariffSchedule savedTariff = new TariffSchedule();
            savedTariff.setTariffId(1);
            when(tariffRepository.save(any(TariffSchedule.class))).thenReturn(savedTariff);
            
            AdValoremDuty duty = new AdValoremDuty();
            duty.setRatePercent(BigDecimal.valueOf(10.0));
            when(adValoremDutyRepository.save(any(AdValoremDuty.class))).thenReturn(duty);

            // Act
            TariffResponse response = tariffManagementService.createTariff(validRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getTariffId()).isEqualTo(1);
            verify(tariffRepository, times(1)).save(any(TariffSchedule.class));
            verify(adValoremDutyRepository, times(1)).save(any(AdValoremDuty.class));
        }

        @Test
        @DisplayName("Should create tariff with Specific duty successfully")
        void shouldCreateTariffWithSpecificDuty() {
            // Arrange
            validRequest.setAdValoremRate(null);
            validRequest.setSpecificRate(5.0);
            validRequest.setSpecificRateUnit("kg");
            
            when(countryRepository.findById("840")).thenReturn(Optional.of(usa));
            when(countryRepository.findById("156")).thenReturn(Optional.of(china));
            when(productRepository.findById("01012100")).thenReturn(Optional.of(product));
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.of(dutyType));
            
            TariffSchedule savedTariff = new TariffSchedule();
            savedTariff.setTariffId(2);
            when(tariffRepository.save(any(TariffSchedule.class))).thenReturn(savedTariff);
            
            SpecificDuty duty = new SpecificDuty();
            duty.setAmount(BigDecimal.valueOf(5.0));
            when(specificDutyRepository.save(any(SpecificDuty.class))).thenReturn(duty);

            // Act
            TariffResponse response = tariffManagementService.createTariff(validRequest);

            // Assert
            assertThat(response).isNotNull();
            verify(specificDutyRepository, times(1)).save(any(SpecificDuty.class));
            verify(adValoremDutyRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create tariff with Combined duty successfully")
        void shouldCreateTariffWithCombinedDuty() {
            // Arrange
            validRequest.setAdValoremRate(null);
            validRequest.setCompoundRate1(10.0); // Ad valorem component
            validRequest.setCompoundRate2(5.0);  // Specific component
            validRequest.setSpecificRateUnit("kg");
            
            when(countryRepository.findById("840")).thenReturn(Optional.of(usa));
            when(countryRepository.findById("156")).thenReturn(Optional.of(china));
            when(productRepository.findById("01012100")).thenReturn(Optional.of(product));
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.of(dutyType));
            
            TariffSchedule savedTariff = new TariffSchedule();
            savedTariff.setTariffId(3);
            when(tariffRepository.save(any(TariffSchedule.class))).thenReturn(savedTariff);
            
            CombinedDuty duty = new CombinedDuty();
            duty.setRatePercent(BigDecimal.valueOf(10.0));
            duty.setAmount(BigDecimal.valueOf(5.0));
            when(combinedDutyRepository.save(any(CombinedDuty.class))).thenReturn(duty);

            // Act
            TariffResponse response = tariffManagementService.createTariff(validRequest);

            // Assert
            assertThat(response).isNotNull();
            verify(combinedDutyRepository, times(1)).save(any(CombinedDuty.class));
        }

        @Test
        @DisplayName("Should auto-create product if not exists")
        void shouldAutoCreateProductIfNotExists() {
            // Arrange
            String newHsCode = "99999999";
            validRequest.setTlCode(newHsCode);
            
            when(countryRepository.findById("840")).thenReturn(Optional.of(usa));
            when(countryRepository.findById("156")).thenReturn(Optional.of(china));
            when(productRepository.findById(newHsCode)).thenReturn(Optional.empty());
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.of(dutyType));
            
            Product newProduct = new Product();
            newProduct.setTlCode(newHsCode);
            newProduct.setDescription("Pending classification - Added via admin");
            when(productRepository.save(any(Product.class))).thenReturn(newProduct);
            
            TariffSchedule savedTariff = new TariffSchedule();
            savedTariff.setTariffId(1);
            when(tariffRepository.save(any(TariffSchedule.class))).thenReturn(savedTariff);
            
            AdValoremDuty duty = new AdValoremDuty();
            when(adValoremDutyRepository.save(any(AdValoremDuty.class))).thenReturn(duty);

            // Act
            TariffResponse response = tariffManagementService.createTariff(validRequest);

            // Assert
            assertThat(response).isNotNull();
            verify(productRepository, times(1)).save(any(Product.class));
        }

        @Test
        @DisplayName("Should auto-create duty type if not exists")
        void shouldAutoCreateDutyTypeIfNotExists() {
            // Arrange
            when(countryRepository.findById("840")).thenReturn(Optional.of(usa));
            when(countryRepository.findById("156")).thenReturn(Optional.of(china));
            when(productRepository.findById("01012100")).thenReturn(Optional.of(product));
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.empty());
            
            DutyType newDutyType = new DutyType();
            when(dutyTypeRepository.save(any(DutyType.class))).thenReturn(newDutyType);
            
            TariffSchedule savedTariff = new TariffSchedule();
            savedTariff.setTariffId(1);
            when(tariffRepository.save(any(TariffSchedule.class))).thenReturn(savedTariff);
            
            AdValoremDuty duty = new AdValoremDuty();
            when(adValoremDutyRepository.save(any(AdValoremDuty.class))).thenReturn(duty);

            // Act
            TariffResponse response = tariffManagementService.createTariff(validRequest);

            // Assert
            assertThat(response).isNotNull();
            verify(dutyTypeRepository, times(1)).save(any(DutyType.class));
        }

        @Test
        @DisplayName("Should throw exception when reporter country not found")
        void shouldThrowExceptionWhenReporterNotFound() {
            // Arrange
            when(countryRepository.findById("840")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> tariffManagementService.createTariff(validRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Reporter country not found: 840");
        }

        @Test
        @DisplayName("Should throw exception when partner country not found")
        void shouldThrowExceptionWhenPartnerNotFound() {
            // Arrange
            when(countryRepository.findById("840")).thenReturn(Optional.of(usa));
            when(countryRepository.findById("156")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> tariffManagementService.createTariff(validRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Partner country not found: 156");
        }

        @Test
        @DisplayName("Should throw exception when only one compound rate provided")
        void shouldThrowExceptionWhenOnlyOneCompoundRate() {
            // Arrange
            validRequest.setAdValoremRate(null);
            validRequest.setCompoundRate1(10.0);
            validRequest.setCompoundRate2(null); // Missing second rate
            
            when(countryRepository.findById("840")).thenReturn(Optional.of(usa));
            when(countryRepository.findById("156")).thenReturn(Optional.of(china));
            when(productRepository.findById("01012100")).thenReturn(Optional.of(product));
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.of(dutyType));

            // Act & Assert
            assertThatThrownBy(() -> tariffManagementService.createTariff(validRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Both compound rates must be specified together");
        }
    }

    @Nested
    @DisplayName("Get Tariff Tests")
    class GetTariffTests {

        @Test
        @DisplayName("Should get tariff by ID successfully")
        void shouldGetTariffById() {
            // Arrange
            TariffSchedule tariff = new TariffSchedule();
            tariff.setTariffId(1);
            tariff.setReporter(usa);
            tariff.setPartner(china);
            tariff.setProduct(product);
            tariff.setDutyType(dutyType);
            
            when(tariffRepository.findById(1)).thenReturn(Optional.of(tariff));

            // Act
            TariffResponse response = tariffManagementService.getTariffById(1);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getTariffId()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should throw exception when tariff not found")
        void shouldThrowExceptionWhenTariffNotFound() {
            // Arrange
            when(tariffRepository.findById(999)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> tariffManagementService.getTariffById(999))
                .isInstanceOf(TariffNotFoundException.class)
                .hasMessageContaining("Tariff not found with id: 999");
        }

        @Test
        @DisplayName("Should get all tariffs successfully")
        void shouldGetAllTariffs() {
            // Arrange
            TariffSchedule tariff1 = new TariffSchedule();
            tariff1.setTariffId(1);
            tariff1.setReporter(usa);
            tariff1.setPartner(china);
            tariff1.setProduct(product);
            tariff1.setDutyType(dutyType);
            
            TariffSchedule tariff2 = new TariffSchedule();
            tariff2.setTariffId(2);
            tariff2.setReporter(usa);
            tariff2.setPartner(china);
            tariff2.setProduct(product);
            tariff2.setDutyType(dutyType);
            
            when(tariffRepository.findAll()).thenReturn(Arrays.asList(tariff1, tariff2));

            // Act
            List<TariffResponse> responses = tariffManagementService.getAllTariffs();

            // Assert
            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(TariffResponse::getTariffId)
                .containsExactly(1, 2);
        }
    }

    @Nested
    @DisplayName("Delete Tariff Tests")
    class DeleteTariffTests {

        @Test
        @DisplayName("Should delete tariff successfully")
        void shouldDeleteTariff() {
            // Arrange
            when(tariffRepository.existsById(1)).thenReturn(true);

            // Act
            tariffManagementService.deleteTariff(1);

            // Assert
            verify(tariffRepository, times(1)).deleteById(1);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent tariff")
        void shouldThrowExceptionWhenDeletingNonExistentTariff() {
            // Arrange
            when(tariffRepository.existsById(999)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> tariffManagementService.deleteTariff(999))
                .isInstanceOf(TariffNotFoundException.class)
                .hasMessageContaining("Tariff not found with id: 999");
            
            verify(tariffRepository, never()).deleteById(anyInt());
        }
    }
}
