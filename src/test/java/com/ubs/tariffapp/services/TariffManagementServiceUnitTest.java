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

        @Test
        @DisplayName("Should throw exception when no duty rates provided")
        void shouldThrowExceptionWhenNoDutyRatesProvided() {
            // Arrange
            validRequest.setAdValoremRate(null);
            validRequest.setSpecificRate(null);
            validRequest.setCompoundRate1(null);
            validRequest.setCompoundRate2(null);
            
            when(countryRepository.findById(usa.getCountryId())).thenReturn(Optional.of(usa));
            when(countryRepository.findById(china.getCountryId())).thenReturn(Optional.of(china));
            when(productRepository.findById(product.getTlCode())).thenReturn(Optional.of(product));
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.of(dutyType));

            // Act & Assert
            assertThatThrownBy(() -> tariffManagementService.createTariff(validRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("At least one duty rate must be specified");
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
    @DisplayName("Update Tariff Tests")
    class UpdateTariffTests {

        @Test
        @DisplayName("Should update tariff suffix and note successfully")
        void shouldUpdateTariffSuffixAndNote() {
            // Arrange
            AdValoremDuty duty = TestEntityFactory.createAdValoremDuty();
            duty.setRatePercent(BigDecimal.valueOf(10.0));
            
            TariffSchedule existingTariff = TestEntityFactory.createTariffSchedule(usa, china, product, dutyType, duty);
            existingTariff.setTariffId(1);
            existingTariff.setTlsSuffix("00");
            existingTariff.setNote("Original note");
            
            when(tariffRepository.findById(1)).thenReturn(Optional.of(existingTariff));
            when(tariffRepository.save(any(TariffSchedule.class))).thenReturn(existingTariff);

            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("tlsSuffix", "01");
            updates.put("note", "Updated note");

            // Act
            TariffResponse response = tariffManagementService.updateTariff(1, updates);

            // Assert
            assertThat(response).isNotNull();
            assertThat(existingTariff.getTlsSuffix()).isEqualTo("01");
            assertThat(existingTariff.getNote()).isEqualTo("Updated note");
            verify(tariffRepository, times(1)).save(existingTariff);
        }

        @Test
        @DisplayName("Should update Ad Valorem duty rate successfully")
        void shouldUpdateAdValoremDutyRate() {
            // Arrange
            AdValoremDuty duty = TestEntityFactory.createAdValoremDuty();
            duty.setRatePercent(BigDecimal.valueOf(10.0));
            
            TariffSchedule existingTariff = TestEntityFactory.createTariffSchedule(usa, china, product, dutyType, duty);
            existingTariff.setTariffId(1);
            
            when(tariffRepository.findById(1)).thenReturn(Optional.of(existingTariff));
            when(tariffRepository.save(any(TariffSchedule.class))).thenReturn(existingTariff);
            when(adValoremDutyRepository.save(any(AdValoremDuty.class))).thenReturn(duty);

            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("adValoremRate", 15.5);

            // Act
            TariffResponse response = tariffManagementService.updateTariff(1, updates);

            // Assert
            assertThat(response).isNotNull();
            assertThat(duty.getRatePercent()).isEqualByComparingTo(BigDecimal.valueOf(15.5));
            verify(adValoremDutyRepository, times(1)).save(duty);
        }

        @Test
        @DisplayName("Should update Specific duty rate successfully")
        void shouldUpdateSpecificDutyRate() {
            // Arrange
            SpecificDuty duty = TestEntityFactory.createSpecificDuty();
            duty.setAmount(BigDecimal.valueOf(5.0));
            duty.setUnit("kg");
            
            TariffSchedule existingTariff = TestEntityFactory.createTariffSchedule(usa, china, product, dutyType, duty);
            existingTariff.setTariffId(1);
            
            when(tariffRepository.findById(1)).thenReturn(Optional.of(existingTariff));
            when(tariffRepository.save(any(TariffSchedule.class))).thenReturn(existingTariff);
            when(specificDutyRepository.save(any(SpecificDuty.class))).thenReturn(duty);

            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("specificRate", 7.5);
            updates.put("specificRateUnit", "lb");

            // Act
            TariffResponse response = tariffManagementService.updateTariff(1, updates);

            // Assert
            assertThat(response).isNotNull();
            assertThat(duty.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(7.5));
            assertThat(duty.getUnit()).isEqualTo("lb");
            verify(specificDutyRepository, times(1)).save(duty);
        }

        @Test
        @DisplayName("Should update Combined duty rates successfully")
        void shouldUpdateCombinedDutyRates() {
            // Arrange
            CombinedDuty duty = TestEntityFactory.createCombinedDuty();
            duty.setRatePercent(BigDecimal.valueOf(10.0));
            duty.setAmount(BigDecimal.valueOf(5.0));
            duty.setUnit("kg");
            
            TariffSchedule existingTariff = TestEntityFactory.createTariffSchedule(usa, china, product, dutyType, duty);
            existingTariff.setTariffId(1);
            
            when(tariffRepository.findById(1)).thenReturn(Optional.of(existingTariff));
            when(tariffRepository.save(any(TariffSchedule.class))).thenReturn(existingTariff);
            when(combinedDutyRepository.save(any(CombinedDuty.class))).thenReturn(duty);

            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("compoundRate1", 12.0);
            updates.put("compoundRate2", 6.5);

            // Act
            TariffResponse response = tariffManagementService.updateTariff(1, updates);

            // Assert
            assertThat(response).isNotNull();
            assertThat(duty.getRatePercent()).isEqualByComparingTo(BigDecimal.valueOf(12.0));
            assertThat(duty.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(6.5));
            verify(combinedDutyRepository, times(1)).save(duty);
        }

        @Test
        @DisplayName("Should update only compound rate 1")
        void shouldUpdateOnlyCompoundRate1() {
            // Arrange
            CombinedDuty duty = TestEntityFactory.createCombinedDuty();
            duty.setRatePercent(BigDecimal.valueOf(10.0));
            duty.setAmount(BigDecimal.valueOf(5.0));
            duty.setUnit("kg");
            
            TariffSchedule existingTariff = TestEntityFactory.createTariffSchedule(usa, china, product, dutyType, duty);
            existingTariff.setTariffId(1);
            
            when(tariffRepository.findById(1)).thenReturn(Optional.of(existingTariff));
            when(tariffRepository.save(any(TariffSchedule.class))).thenReturn(existingTariff);
            when(combinedDutyRepository.save(any(CombinedDuty.class))).thenReturn(duty);

            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("compoundRate1", 12.0);

            // Act
            TariffResponse response = tariffManagementService.updateTariff(1, updates);

            // Assert
            assertThat(response).isNotNull();
            assertThat(duty.getRatePercent()).isEqualByComparingTo(BigDecimal.valueOf(12.0));
            assertThat(duty.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5.0)); // unchanged
            verify(combinedDutyRepository, times(1)).save(duty);
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent tariff")
        void shouldThrowExceptionWhenUpdatingNonExistentTariff() {
            // Arrange
            when(tariffRepository.findById(999)).thenReturn(Optional.empty());

            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("note", "Test");

            // Act & Assert
            assertThatThrownBy(() -> tariffManagementService.updateTariff(999, updates))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tariff not found");
        }

        @Test
        @DisplayName("Should throw exception when duty not found during rate update")
        void shouldThrowExceptionWhenDutyNotFoundDuringUpdate() {
            // Arrange: build TariffSchedule manually with null duty
            TariffSchedule existingTariff = new TariffSchedule();
            existingTariff.setTariffId(1);
            existingTariff.setTariffYear(2023);
            existingTariff.setReporter(usa);
            existingTariff.setPartner(china);
            existingTariff.setProduct(product);
            existingTariff.setDutyType(dutyType);
            existingTariff.setDuty(null);
            
            when(tariffRepository.findById(1)).thenReturn(Optional.of(existingTariff));

            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("adValoremRate", 15.0);

            // Act & Assert
            assertThatThrownBy(() -> tariffManagementService.updateTariff(1, updates))
                .isInstanceOf(com.ubs.tariffapp.exceptions.DutyNotFoundException.class)
                .hasMessageContaining("Duty information not found for tariff id: 1");
        }

        @Test
        @DisplayName("Should throw exception when updating with mismatched duty type")
        void shouldThrowExceptionWhenUpdatingWithMismatchedDutyType() {
            // Arrange
            AdValoremDuty duty = TestEntityFactory.createAdValoremDuty();
            duty.setRatePercent(BigDecimal.valueOf(10.0));
            
            TariffSchedule existingTariff = TestEntityFactory.createTariffSchedule(usa, china, product, dutyType, duty);
            existingTariff.setTariffId(1);
            
            when(tariffRepository.findById(1)).thenReturn(Optional.of(existingTariff));

            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("specificRate", 5.0); // Trying to update specific rate for Ad Valorem duty

            // Act & Assert
            assertThatThrownBy(() -> tariffManagementService.updateTariff(1, updates))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Cannot update duty rates")
                .hasMessageContaining("provided rates don't match existing duty type");
        }

        @Test
        @DisplayName("Should update specific duty without changing unit when unit not provided")
        void shouldUpdateSpecificDutyWithoutChangingUnit() {
            // Arrange
            SpecificDuty duty = TestEntityFactory.createSpecificDuty();
            duty.setAmount(BigDecimal.valueOf(5.0));
            duty.setUnit("kg");
            
            TariffSchedule existingTariff = TestEntityFactory.createTariffSchedule(usa, china, product, dutyType, duty);
            existingTariff.setTariffId(1);
            
            when(tariffRepository.findById(1)).thenReturn(Optional.of(existingTariff));
            when(tariffRepository.save(any(TariffSchedule.class))).thenReturn(existingTariff);
            when(specificDutyRepository.save(any(SpecificDuty.class))).thenReturn(duty);

            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("specificRate", 8.0);
            // Not providing specificRateUnit

            // Act
            TariffResponse response = tariffManagementService.updateTariff(1, updates);

            // Assert
            assertThat(response).isNotNull();
            assertThat(duty.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(8.0));
            assertThat(duty.getUnit()).isEqualTo("kg"); // Should remain unchanged
            verify(specificDutyRepository, times(1)).save(duty);
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

    @Nested
    @DisplayName("ConvertToResponse Tests")
    class ConvertToResponseTests {

        @Test
        @DisplayName("Should convert tariff with Ad Valorem duty to response")
        void shouldConvertTariffWithAdValoremDutyToResponse() {
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
            assertThat(response.getAdValoremRate()).isEqualTo(10.0);
            assertThat(response.getDutyCategory()).isEqualTo("A");
        }

        @Test
        @DisplayName("Should convert tariff with Specific duty to response")
        void shouldConvertTariffWithSpecificDutyToResponse() {
            // Arrange
            SpecificDuty duty = TestEntityFactory.createSpecificDuty();
            duty.setAmount(BigDecimal.valueOf(5.0));
            duty.setUnit("kg");
            
            TariffSchedule tariff = TestEntityFactory.createTariffSchedule(usa, china, product, dutyType, duty);
            tariff.setTariffId(1);
            
            when(tariffRepository.findById(1)).thenReturn(Optional.of(tariff));

            // Act
            TariffResponse response = tariffManagementService.getTariffById(1);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getSpecificRate()).isEqualTo(5.0);
            assertThat(response.getSpecificRateUnit()).isEqualTo("kg");
            assertThat(response.getDutyCategory()).isEqualTo("S");
        }

        @Test
        @DisplayName("Should convert tariff with Combined duty to response")
        void shouldConvertTariffWithCombinedDutyToResponse() {
            // Arrange
            CombinedDuty duty = TestEntityFactory.createCombinedDuty();
            duty.setRatePercent(BigDecimal.valueOf(10.0));
            duty.setAmount(BigDecimal.valueOf(5.0));
            duty.setUnit("kg");
            
            TariffSchedule tariff = TestEntityFactory.createTariffSchedule(usa, china, product, dutyType, duty);
            tariff.setTariffId(1);
            
            when(tariffRepository.findById(1)).thenReturn(Optional.of(tariff));

            // Act
            TariffResponse response = tariffManagementService.getTariffById(1);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getCompoundRate1()).isEqualTo(10.0);
            assertThat(response.getCompoundRate2()).isEqualTo(5.0);
            assertThat(response.getSpecificRateUnit()).isEqualTo("kg");
            assertThat(response.getDutyCategory()).isEqualTo("C");
        }

        @Test
        @DisplayName("Should convert tariff with OtherDuty to response")
        void shouldConvertTariffWithOtherDutyToResponse() {
            // Arrange
            com.ubs.tariffapp.models.duty.OtherDuty duty = new com.ubs.tariffapp.models.duty.OtherDuty();
            duty.setDutyNature("O");
            duty.setRawText("Special duty");
            duty.setIsComputable(false);
            
            TariffSchedule tariff = TestEntityFactory.createTariffSchedule(usa, china, product, dutyType, duty);
            tariff.setTariffId(1);
            
            when(tariffRepository.findById(1)).thenReturn(Optional.of(tariff));

            // Act
            TariffResponse response = tariffManagementService.getTariffById(1);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getRawText()).isEqualTo("Special duty");
            assertThat(response.getIsComputable()).isFalse();
            assertThat(response.getDutyCategory()).isEqualTo("O");
        }

        @Test
        @DisplayName("Should throw exception when duty is null during conversion")
        void shouldThrowExceptionWhenDutyIsNullDuringConversion() {
            // Arrange: create TariffSchedule manually with null duty
            TariffSchedule tariff = new TariffSchedule();
            tariff.setTariffId(1);
            tariff.setTariffYear(2023);
            tariff.setReporter(usa);
            tariff.setPartner(china);
            tariff.setProduct(product);
            tariff.setDutyType(dutyType);
            tariff.setDuty(null);
            
            when(tariffRepository.findById(1)).thenReturn(Optional.of(tariff));

            // Act & Assert
            assertThatThrownBy(() -> tariffManagementService.getTariffById(1))
                .isInstanceOf(com.ubs.tariffapp.exceptions.DutyNotFoundException.class)
                .hasMessageContaining("Duty information not found for tariff id: 1");
        }
    }

    @Nested
    @DisplayName("GenerateDutyTypeDescription Tests")
    class GenerateDutyTypeDescriptionTests {

        @Test
        @DisplayName("Should generate description for Standard MFN duty type (0-0)")
        void shouldGenerateDescriptionForStandardMFN() {
            // Arrange
            validRequest.setDutyType("0");
            validRequest.setDutyCode("0");
            
            when(countryRepository.findById(usa.getCountryId())).thenReturn(Optional.of(usa));
            when(countryRepository.findById(china.getCountryId())).thenReturn(Optional.of(china));
            when(productRepository.findById(product.getTlCode())).thenReturn(Optional.of(product));
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.empty());
            
            DutyType newDutyType = new DutyType();
            newDutyType.setId(new DutyTypeId("0", "0"));
            newDutyType.setDutyTypeDescription("Standard (MFN)");
            when(dutyTypeRepository.save(any(DutyType.class))).thenReturn(newDutyType);
            
            AdValoremDuty duty = TestEntityFactory.createAdValoremDuty();
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
        @DisplayName("Should generate description for Duty-Free type (0-2)")
        void shouldGenerateDescriptionForDutyFree() {
            // Arrange
            validRequest.setDutyType("0");
            validRequest.setDutyCode("2");
            
            when(countryRepository.findById(usa.getCountryId())).thenReturn(Optional.of(usa));
            when(countryRepository.findById(china.getCountryId())).thenReturn(Optional.of(china));
            when(productRepository.findById(product.getTlCode())).thenReturn(Optional.of(product));
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.empty());
            
            DutyType newDutyType = new DutyType();
            newDutyType.setId(new DutyTypeId("0", "2"));
            newDutyType.setDutyTypeDescription("Duty-Free");
            when(dutyTypeRepository.save(any(DutyType.class))).thenReturn(newDutyType);
            
            AdValoremDuty duty = TestEntityFactory.createAdValoremDuty();
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
        @DisplayName("Should generate description for Preferential Trade Agreement type (1-0)")
        void shouldGenerateDescriptionForPreferentialTradeAgreement() {
            // Arrange
            validRequest.setDutyType("1");
            validRequest.setDutyCode("0");
            
            when(countryRepository.findById(usa.getCountryId())).thenReturn(Optional.of(usa));
            when(countryRepository.findById(china.getCountryId())).thenReturn(Optional.of(china));
            when(productRepository.findById(product.getTlCode())).thenReturn(Optional.of(product));
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.empty());
            
            DutyType newDutyType = new DutyType();
            newDutyType.setId(new DutyTypeId("1", "0"));
            newDutyType.setDutyTypeDescription("Preferential (Trade Agreement)");
            when(dutyTypeRepository.save(any(DutyType.class))).thenReturn(newDutyType);
            
            AdValoremDuty duty = TestEntityFactory.createAdValoremDuty();
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
        @DisplayName("Should generate description for Preferential Specific type (1-1)")
        void shouldGenerateDescriptionForPreferentialSpecific() {
            // Arrange
            validRequest.setDutyType("1");
            validRequest.setDutyCode("1");
            
            when(countryRepository.findById(usa.getCountryId())).thenReturn(Optional.of(usa));
            when(countryRepository.findById(china.getCountryId())).thenReturn(Optional.of(china));
            when(productRepository.findById(product.getTlCode())).thenReturn(Optional.of(product));
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.empty());
            
            DutyType newDutyType = new DutyType();
            newDutyType.setId(new DutyTypeId("1", "1"));
            newDutyType.setDutyTypeDescription("Preferential (Specific)");
            when(dutyTypeRepository.save(any(DutyType.class))).thenReturn(newDutyType);
            
            AdValoremDuty duty = TestEntityFactory.createAdValoremDuty();
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
        @DisplayName("Should generate description for GSP type (2-0)")
        void shouldGenerateDescriptionForGSP() {
            // Arrange
            validRequest.setDutyType("2");
            validRequest.setDutyCode("0");
            
            when(countryRepository.findById(usa.getCountryId())).thenReturn(Optional.of(usa));
            when(countryRepository.findById(china.getCountryId())).thenReturn(Optional.of(china));
            when(productRepository.findById(product.getTlCode())).thenReturn(Optional.of(product));
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.empty());
            
            DutyType newDutyType = new DutyType();
            newDutyType.setId(new DutyTypeId("2", "0"));
            newDutyType.setDutyTypeDescription("GSP (Developing Countries)");
            when(dutyTypeRepository.save(any(DutyType.class))).thenReturn(newDutyType);
            
            AdValoremDuty duty = TestEntityFactory.createAdValoremDuty();
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
        @DisplayName("Should generate description for Temporary type (3-0)")
        void shouldGenerateDescriptionForTemporary() {
            // Arrange
            validRequest.setDutyType("3");
            validRequest.setDutyCode("0");
            
            when(countryRepository.findById(usa.getCountryId())).thenReturn(Optional.of(usa));
            when(countryRepository.findById(china.getCountryId())).thenReturn(Optional.of(china));
            when(productRepository.findById(product.getTlCode())).thenReturn(Optional.of(product));
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.empty());
            
            DutyType newDutyType = new DutyType();
            newDutyType.setId(new DutyTypeId("3", "0"));
            newDutyType.setDutyTypeDescription("Temporary");
            when(dutyTypeRepository.save(any(DutyType.class))).thenReturn(newDutyType);
            
            AdValoremDuty duty = TestEntityFactory.createAdValoremDuty();
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
        @DisplayName("Should generate fallback description for unknown duty type")
        void shouldGenerateFallbackDescriptionForUnknownDutyType() {
            // Arrange
            validRequest.setDutyType("9");
            validRequest.setDutyCode("9");
            
            when(countryRepository.findById(usa.getCountryId())).thenReturn(Optional.of(usa));
            when(countryRepository.findById(china.getCountryId())).thenReturn(Optional.of(china));
            when(productRepository.findById(product.getTlCode())).thenReturn(Optional.of(product));
            when(dutyTypeRepository.findById(any(DutyTypeId.class))).thenReturn(Optional.empty());
            
            DutyType newDutyType = new DutyType();
            newDutyType.setId(new DutyTypeId("9", "9"));
            newDutyType.setDutyTypeDescription("Custom duty type 9-9 - Added via admin");
            when(dutyTypeRepository.save(any(DutyType.class))).thenReturn(newDutyType);
            
            AdValoremDuty duty = TestEntityFactory.createAdValoremDuty();
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
    }
}
