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
import com.ubs.tariffapp.testutils.TestEntityFactory;

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
        // Use TestEntityFactory for common test data
        usa = TestEntityFactory.createReporterCountry();
        china = TestEntityFactory.createPartnerCountry();
        product = TestEntityFactory.createProduct();
        dutyType = TestEntityFactory.createDutyType();

        validRequest = new TariffRequest();
        validRequest.setTariffYear(2023);
        validRequest.setReporterCode(usa.getCountryId());
        validRequest.setPartnerCode(china.getCountryId());
        validRequest.setTlCode(product.getTlCode());
        validRequest.setDutyType(dutyType.getId().getDutyType());
        validRequest.setDutyCode(dutyType.getId().getDutyCode());
        validRequest.setAdValoremRate(10.0);
    }

    @Nested
    @DisplayName("Create Tariff Tests")
    class CreateTariffTests {

        @Test
        @DisplayName("Should create tariff with Ad Valorem duty successfully")
        void shouldCreateTariffWithAdValoremDuty() {
            // Arrange
            when(countryRepository.findById(usa.getCountryId())).thenReturn(Optional.of(usa));
            when(countryRepository.findById(china.getCountryId())).thenReturn(Optional.of(china));
            when(productRepository.findById(product.getTlCode())).thenReturn(Optional.of(product));
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.of(dutyType));
            
            AdValoremDuty duty = TestEntityFactory.createAdValoremDuty();
            duty.setRatePercent(BigDecimal.valueOf(10.0));  // Override for this test
            when(adValoremDutyRepository.save(any(AdValoremDuty.class))).thenReturn(duty);
            
            TariffSchedule savedTariff = TestEntityFactory.createTariffSchedule(usa, china, product, dutyType, duty);
            savedTariff.setTariffId(1);
            when(tariffRepository.save(any(TariffSchedule.class))).thenReturn(savedTariff);

            // Act
            TariffResponse response = tariffManagementService.createTariff(validRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getTariffId()).isEqualTo(1);
            verify(tariffRepository, times(2)).save(any(TariffSchedule.class));  // Called twice in createTariff
            verify(adValoremDutyRepository, times(1)).save(any(AdValoremDuty.class));
        }

        @Test
        @DisplayName("Should create tariff with Specific duty successfully")
        void shouldCreateTariffWithSpecificDuty() {
            // Arrange
            validRequest.setAdValoremRate(null);
            validRequest.setSpecificRate(5.0);
            validRequest.setSpecificRateUnit("kg");
            
            when(countryRepository.findById(usa.getCountryId())).thenReturn(Optional.of(usa));
            when(countryRepository.findById(china.getCountryId())).thenReturn(Optional.of(china));
            when(productRepository.findById(product.getTlCode())).thenReturn(Optional.of(product));
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.of(dutyType));
            
            SpecificDuty duty = TestEntityFactory.createSpecificDuty();
            duty.setAmount(BigDecimal.valueOf(5.0));  // Override for this test
            duty.setUnit("kg");  // Override for this test
            when(specificDutyRepository.save(any(SpecificDuty.class))).thenReturn(duty);
            
            TariffSchedule savedTariff = TestEntityFactory.createTariffSchedule(usa, china, product, dutyType, duty);
            savedTariff.setTariffId(2);
            when(tariffRepository.save(any(TariffSchedule.class))).thenReturn(savedTariff);

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
            
            when(countryRepository.findById(usa.getCountryId())).thenReturn(Optional.of(usa));
            when(countryRepository.findById(china.getCountryId())).thenReturn(Optional.of(china));
            when(productRepository.findById(product.getTlCode())).thenReturn(Optional.of(product));
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.of(dutyType));
            
            CombinedDuty duty = TestEntityFactory.createCombinedDuty();
            duty.setRatePercent(BigDecimal.valueOf(10.0));  // Override for this test
            duty.setAmount(BigDecimal.valueOf(5.0));  // Override for this test
            duty.setUnit("kg");  // Override for this test
            when(combinedDutyRepository.save(any(CombinedDuty.class))).thenReturn(duty);
            
            TariffSchedule savedTariff = TestEntityFactory.createTariffSchedule(usa, china, product, dutyType, duty);
            savedTariff.setTariffId(3);
            when(tariffRepository.save(any(TariffSchedule.class))).thenReturn(savedTariff);

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
            
            when(countryRepository.findById(usa.getCountryId())).thenReturn(Optional.of(usa));
            when(countryRepository.findById(china.getCountryId())).thenReturn(Optional.of(china));
            when(productRepository.findById(newHsCode)).thenReturn(Optional.empty());
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.of(dutyType));
            
            Product newProduct = new Product();
            newProduct.setTlCode(newHsCode);
            newProduct.setDescription("Pending classification - Added via admin");
            when(productRepository.save(any(Product.class))).thenReturn(newProduct);
            
            AdValoremDuty duty = TestEntityFactory.createAdValoremDuty();
            duty.setRatePercent(BigDecimal.valueOf(10.0));
            when(adValoremDutyRepository.save(any(AdValoremDuty.class))).thenReturn(duty);
            
            TariffSchedule savedTariff = TestEntityFactory.createTariffSchedule(usa, china, newProduct, dutyType, duty);
            savedTariff.setTariffId(1);
            when(tariffRepository.save(any(TariffSchedule.class))).thenReturn(savedTariff);

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
            when(countryRepository.findById(usa.getCountryId())).thenReturn(Optional.of(usa));
            when(countryRepository.findById(china.getCountryId())).thenReturn(Optional.of(china));
            when(productRepository.findById(product.getTlCode())).thenReturn(Optional.of(product));
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.empty());
            
            DutyType newDutyType = new DutyType();
            newDutyType.setId(new DutyTypeId("1", "1"));
            when(dutyTypeRepository.save(any(DutyType.class))).thenReturn(newDutyType);
            
            AdValoremDuty duty = TestEntityFactory.createAdValoremDuty();
            duty.setRatePercent(BigDecimal.valueOf(10.0));
            when(adValoremDutyRepository.save(any(AdValoremDuty.class))).thenReturn(duty);
            
            TariffSchedule savedTariff = TestEntityFactory.createTariffSchedule(usa, china, product, newDutyType, duty);
            savedTariff.setTariffId(1);
            when(tariffRepository.save(any(TariffSchedule.class))).thenReturn(savedTariff);

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
            when(countryRepository.findById(usa.getCountryId())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> tariffManagementService.createTariff(validRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Reporter country not found: " + usa.getCountryId());
        }

        @Test
        @DisplayName("Should throw exception when partner country not found")
        void shouldThrowExceptionWhenPartnerNotFound() {
            // Arrange
            when(countryRepository.findById(usa.getCountryId())).thenReturn(Optional.of(usa));
            when(countryRepository.findById(china.getCountryId())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> tariffManagementService.createTariff(validRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Partner country not found: " + china.getCountryId());
        }

        @Test
        @DisplayName("Should throw exception when only one compound rate provided")
        void shouldThrowExceptionWhenOnlyOneCompoundRate() {
            // Arrange
            validRequest.setAdValoremRate(null);
            validRequest.setCompoundRate1(10.0);
            validRequest.setCompoundRate2(null); // Missing second rate
            
            when(countryRepository.findById(usa.getCountryId())).thenReturn(Optional.of(usa));
            when(countryRepository.findById(china.getCountryId())).thenReturn(Optional.of(china));
            when(productRepository.findById(product.getTlCode())).thenReturn(Optional.of(product));
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
            AdValoremDuty duty = TestEntityFactory.createAdValoremDuty();
            duty.setRatePercent(BigDecimal.valueOf(10.0));
            
            TariffSchedule tariff = TestEntityFactory.createTariffSchedule(usa, china, product, dutyType, duty);
            tariff.setTariffId(1);
            
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
            AdValoremDuty duty1 = TestEntityFactory.createAdValoremDuty();
            duty1.setRatePercent(BigDecimal.valueOf(10.0));
            
            AdValoremDuty duty2 = TestEntityFactory.createAdValoremDuty();
            duty2.setRatePercent(BigDecimal.valueOf(15.0));
            
            TariffSchedule tariff1 = TestEntityFactory.createTariffSchedule(usa, china, product, dutyType, duty1);
            tariff1.setTariffId(1);
            
            TariffSchedule tariff2 = TestEntityFactory.createTariffSchedule(usa, china, product, dutyType, duty2);
            tariff2.setTariffId(2);
            
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
