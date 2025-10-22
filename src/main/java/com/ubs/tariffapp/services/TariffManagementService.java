package com.ubs.tariffapp.services;

import com.ubs.tariffapp.exceptions.TariffNotFoundException;
import com.ubs.tariffapp.exceptions.InvalidRequestException;
import com.ubs.tariffapp.exceptions.DutyNotFoundException;
import com.ubs.tariffapp.models.*;
import com.ubs.tariffapp.models.dto.TariffRequest;
import com.ubs.tariffapp.models.dto.TariffResponse;
import com.ubs.tariffapp.models.duty.*;
import com.ubs.tariffapp.repositories.*;
import com.ubs.tariffapp.repositories.duty.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TariffManagementService {

    private final TariffScheduleRepository tariffRepository;
    private final CountryRepository countryRepository;
    private final ProductRepository productRepository;
    private final DutyTypeRepository dutyTypeRepository;
    
    // Duty repositories
    private final DutyRepository dutyRepository;
    private final AdValoremDutyRepository adValoremDutyRepository;
    private final SpecificDutyRepository specificDutyRepository;
    private final CombinedDutyRepository combinedDutyRepository;
    private final OtherDutyRepository otherDutyRepository;

    @Transactional
    public TariffResponse createTariff(TariffRequest request) {
        // Validate and fetch related entities
        Country reporter = countryRepository.findById(request.getReporterCode())
                .orElseThrow(() -> new InvalidRequestException("Reporter country not found: " + request.getReporterCode()));
        
        Country partner = countryRepository.findById(request.getPartnerCode())
                .orElseThrow(() -> new InvalidRequestException("Partner country not found: " + request.getPartnerCode()));
        
        Product product = productRepository.findById(request.getTlCode())
                .orElseThrow(() -> new InvalidRequestException("Product not found: " + request.getTlCode()));
        
        DutyTypeId dutyTypeId = new DutyTypeId(request.getDutyType(), request.getDutyCode());
        DutyType dutyType = dutyTypeRepository.findById(dutyTypeId)
                .orElseThrow(() -> new InvalidRequestException("Duty type not found: " + request.getDutyType() + "-" + request.getDutyCode()));

        // Validate duty rates are provided
        validateDutyRates(request);

        // Create TariffSchedule
        TariffSchedule tariff = new TariffSchedule();
        tariff.setTariffYear(request.getTariffYear());
        tariff.setReporter(reporter);
        tariff.setPartner(partner);
        tariff.setProduct(product);
        tariff.setDutyType(dutyType);
        tariff.setTlsSuffix(request.getTlsSuffix());
        tariff.setNote(request.getNote());

        // Save tariff first to get ID
        tariff = tariffRepository.save(tariff);

        // Create appropriate Duty entity based on type
        Duty duty = createDutyFromRequest(request, tariff);
        if (duty == null) {
            throw new InvalidRequestException("At least one duty rate must be specified");
        }
        
        tariff.setDuty(duty);
        tariff = tariffRepository.save(tariff);

        return convertToResponse(tariff);
    }

    @Transactional
    public TariffResponse updateTariff(Integer id, TariffRequest request) {
        TariffSchedule tariff = tariffRepository.findById(id)
                .orElseThrow(() -> new TariffNotFoundException("Tariff not found with id: " + id));

        // Update basic fields
        tariff.setTariffYear(request.getTariffYear());
        tariff.setTlsSuffix(request.getTlsSuffix());
        tariff.setNote(request.getNote());

        // Update related entities if changed
        if (!tariff.getReporter().getCountryId().equals(request.getReporterCode())) {
            Country reporter = countryRepository.findById(request.getReporterCode())
                    .orElseThrow(() -> new InvalidRequestException("Reporter country not found: " + request.getReporterCode()));
            tariff.setReporter(reporter);
        }

        if (!tariff.getPartner().getCountryId().equals(request.getPartnerCode())) {
            Country partner = countryRepository.findById(request.getPartnerCode())
                    .orElseThrow(() -> new InvalidRequestException("Partner country not found: " + request.getPartnerCode()));
            tariff.setPartner(partner);
        }

        if (!tariff.getProduct().getTlCode().equals(request.getTlCode())) {
            Product product = productRepository.findById(request.getTlCode())
                    .orElseThrow(() -> new InvalidRequestException("Product not found: " + request.getTlCode()));
            tariff.setProduct(product);
        }

        // Update duty type if changed
        DutyTypeId newDutyTypeId = new DutyTypeId(request.getDutyType(), request.getDutyCode());
        if (!tariff.getDutyType().getId().equals(newDutyTypeId)) {
            DutyType dutyType = dutyTypeRepository.findById(newDutyTypeId)
                    .orElseThrow(() -> new InvalidRequestException("Duty type not found: " + request.getDutyType() + "-" + request.getDutyCode()));
            tariff.setDutyType(dutyType);
        }

        // Validate duty rates
        validateDutyRates(request);

        // Update duty information
        if (tariff.getDuty() != null) {
            updateDutyFromRequest(request, tariff.getDuty());
        } else {
            Duty duty = createDutyFromRequest(request, tariff);
            if (duty == null) {
                throw new InvalidRequestException("At least one duty rate must be specified");
            }
            tariff.setDuty(duty);
        }

        tariff = tariffRepository.save(tariff);
        return convertToResponse(tariff);
    }

    @Transactional(readOnly = true)
    public TariffResponse getTariffById(Integer id) {
        TariffSchedule tariff = tariffRepository.findById(id)
                .orElseThrow(() -> new TariffNotFoundException("Tariff not found with id: " + id));
        return convertToResponse(tariff);
    }

    @Transactional(readOnly = true)
    public List<TariffResponse> getAllTariffs() {
        return tariffRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteTariff(Integer id) {
        if (!tariffRepository.existsById(id)) {
            throw new TariffNotFoundException("Tariff not found with id: " + id);
        }
        tariffRepository.deleteById(id);
    }

    private void validateDutyRates(TariffRequest request) {
        boolean hasAdValorem = request.getAdValoremRate() != null;
        boolean hasSpecific = request.getSpecificRate() != null;
        boolean hasCompound = request.getCompoundRate1() != null && request.getCompoundRate2() != null;

        if (!hasAdValorem && !hasSpecific && !hasCompound) {
            throw new InvalidRequestException("At least one duty rate must be specified");
        }

        // Validate ad valorem rate range
        if (hasAdValorem && (request.getAdValoremRate() < 0 || request.getAdValoremRate() > 100)) {
            throw new InvalidRequestException("Ad valorem rate must be between 0 and 100, got: " + request.getAdValoremRate());
        }

        // Validate specific rate
        if (hasSpecific) {
            if (request.getSpecificRate() < 0) {
                throw new InvalidRequestException("Specific rate must be non-negative, got: " + request.getSpecificRate());
            }
            if (request.getSpecificRateUnit() == null || request.getSpecificRateUnit().trim().isEmpty()) {
                throw new InvalidRequestException("Specific rate unit must be specified when specific rate is provided");
            }
        }

        // Validate compound rates
        if (request.getCompoundRate1() != null && request.getCompoundRate2() == null) {
            throw new InvalidRequestException("Both compound rates must be specified together");
        }
        if (request.getCompoundRate2() != null && request.getCompoundRate1() == null) {
            throw new InvalidRequestException("Both compound rates must be specified together");
        }

        // Validate compound rate values
        if (hasCompound) {
            if (request.getCompoundRate1() < 0) {
                throw new InvalidRequestException("Compound rate 1 must be non-negative, got: " + request.getCompoundRate1());
            }
            if (request.getCompoundRate2() < 0) {
                throw new InvalidRequestException("Compound rate 2 must be non-negative, got: " + request.getCompoundRate2());
            }
        }
    }

    private Duty createDutyFromRequest(TariffRequest request, TariffSchedule tariff) {
        Duty duty = null;
        
        if (request.getAdValoremRate() != null) {
            AdValoremDuty adValorem = new AdValoremDuty();
            adValorem.setTariffSchedule(tariff);
            adValorem.setDutyNature("AD_VALOREM");
            adValorem.setRatePercent(BigDecimal.valueOf(request.getAdValoremRate()));
            adValorem.setMathExpression(request.getAdValoremRate() + "%");
            duty = adValoremDutyRepository.save(adValorem);
            
        } else if (request.getSpecificRate() != null) {
            SpecificDuty specific = new SpecificDuty();
            specific.setTariffSchedule(tariff);
            specific.setDutyNature("SPECIFIC");
            specific.setAmount(BigDecimal.valueOf(request.getSpecificRate()));
            specific.setUnit(request.getSpecificRateUnit());
            specific.setMultiplier(1); // Default multiplier
            specific.setSpecificDutyRateRaw(request.getSpecificRate() + " " + request.getSpecificRateUnit());
            specific.setMathExpression(request.getSpecificRate() + " per " + request.getSpecificRateUnit());
            duty = specificDutyRepository.save(specific);
            
        } else if (request.getCompoundRate1() != null && request.getCompoundRate2() != null) {
            // Use CombinedDuty for compound rates
            CombinedDuty combined = new CombinedDuty();
            combined.setTariffSchedule(tariff);
            combined.setDutyNature("COMBINED");
            combined.setMixedOrConditional("M"); // M for mixed (compound)
            combined.setRatePercent(BigDecimal.valueOf(request.getCompoundRate1()));
            combined.setAmount(BigDecimal.valueOf(request.getCompoundRate2()));
            combined.setUnit(request.getSpecificRateUnit() != null ? request.getSpecificRateUnit() : "unit");
            combined.setMultiplier(1);
            combined.setSpecificDutyRateRaw(
                String.format("%.2f%% + %.2f per %s", 
                    request.getCompoundRate1(), 
                    request.getCompoundRate2(), 
                    combined.getUnit())
            );
            combined.setMathExpression(
                String.format("%.2f%% + %.2f/%s", 
                    request.getCompoundRate1(), 
                    request.getCompoundRate2(), 
                    combined.getUnit())
            );
            duty = combinedDutyRepository.save(combined);
        }
        
        return duty;
    }

    private void updateDutyFromRequest(TariffRequest request, Duty duty) {
        if (duty instanceof AdValoremDuty && request.getAdValoremRate() != null) {
            AdValoremDuty adValorem = (AdValoremDuty) duty;
            adValorem.setRatePercent(BigDecimal.valueOf(request.getAdValoremRate()));
            adValorem.setMathExpression(request.getAdValoremRate() + "%");
            adValoremDutyRepository.save(adValorem);
            
        } else if (duty instanceof SpecificDuty && request.getSpecificRate() != null) {
            SpecificDuty specific = (SpecificDuty) duty;
            specific.setAmount(BigDecimal.valueOf(request.getSpecificRate()));
            specific.setUnit(request.getSpecificRateUnit());
            specific.setSpecificDutyRateRaw(request.getSpecificRate() + " " + request.getSpecificRateUnit());
            specific.setMathExpression(request.getSpecificRate() + " per " + request.getSpecificRateUnit());
            specificDutyRepository.save(specific);
            
        } else if (duty instanceof CombinedDuty && request.getCompoundRate1() != null) {
            CombinedDuty combined = (CombinedDuty) duty;
            combined.setRatePercent(BigDecimal.valueOf(request.getCompoundRate1()));
            combined.setAmount(BigDecimal.valueOf(request.getCompoundRate2()));
            if (request.getSpecificRateUnit() != null) {
                combined.setUnit(request.getSpecificRateUnit());
            }
            combined.setSpecificDutyRateRaw(
                String.format("%.2f%% + %.2f per %s", 
                    request.getCompoundRate1(), 
                    request.getCompoundRate2(), 
                    combined.getUnit())
            );
            combined.setMathExpression(
                String.format("%.2f%% + %.2f/%s", 
                    request.getCompoundRate1(), 
                    request.getCompoundRate2(), 
                    combined.getUnit())
            );
            combinedDutyRepository.save(combined);
            
        } else {
            throw new DutyNotFoundException("Cannot update duty: incompatible duty type or missing duty rates for tariff");
        }
    }

    private TariffResponse convertToResponse(TariffSchedule tariff) {
        TariffResponse.TariffResponseBuilder builder = TariffResponse.builder()
                .tariffId(tariff.getTariffId())
                .tariffYear(tariff.getTariffYear())
                .reporterCode(tariff.getReporter().getCountryId())
                .reporterName(tariff.getReporter().getCountryName())
                .partnerCode(tariff.getPartner().getCountryId())
                .partnerName(tariff.getPartner().getCountryName())
                .tlCode(tariff.getProduct().getTlCode())
                .productDescription(tariff.getProduct().getDescription())
                .dutyType(tariff.getDutyType().getId().getDutyType())
                .dutyCode(tariff.getDutyType().getId().getDutyCode())
                .dutyTypeDescription(tariff.getDutyType().getDutyTypeDescription())
                .tlsSuffix(tariff.getTlsSuffix())
                .note(tariff.getNote());

        // Add duty details
        if (tariff.getDuty() != null) {
            Duty duty = tariff.getDuty();
            builder.dutyCategory(duty.getDutyNature());
            
            if (duty instanceof AdValoremDuty) {
                AdValoremDuty adValorem = (AdValoremDuty) duty;
                builder.adValoremRate(adValorem.getRatePercent().doubleValue());
                
            } else if (duty instanceof SpecificDuty) {
                SpecificDuty specific = (SpecificDuty) duty;
                builder.specificRate(specific.getAmount().doubleValue())
                       .specificRateUnit(specific.getUnit());
                       
            } else if (duty instanceof CombinedDuty) {
                CombinedDuty combined = (CombinedDuty) duty;
                builder.adValoremRate(combined.getRatePercent() != null ? combined.getRatePercent().doubleValue() : null)
                       .specificRate(combined.getAmount() != null ? combined.getAmount().doubleValue() : null)
                       .specificRateUnit(combined.getUnit())
                       .compoundRate1(combined.getRatePercent() != null ? combined.getRatePercent().doubleValue() : null)
                       .compoundRate2(combined.getAmount() != null ? combined.getAmount().doubleValue() : null);
                       
            } else if (duty instanceof OtherDuty) {
                // Other duty types - just include the duty nature
                // No specific rate information to add
            }
        } else {
            throw new DutyNotFoundException("Duty information not found for tariff id: " + tariff.getTariffId());
        }

        return builder.build();
    }
}
