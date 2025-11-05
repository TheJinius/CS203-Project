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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
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

    // Manual constructor (replaces @RequiredArgsConstructor)
    public TariffManagementService(TariffScheduleRepository tariffRepository,
                                 CountryRepository countryRepository,
                                 ProductRepository productRepository,
                                 DutyTypeRepository dutyTypeRepository,
                                 DutyRepository dutyRepository,
                                 AdValoremDutyRepository adValoremDutyRepository,
                                 SpecificDutyRepository specificDutyRepository,
                                 CombinedDutyRepository combinedDutyRepository,
                                 OtherDutyRepository otherDutyRepository) {
        this.tariffRepository = tariffRepository;
        this.countryRepository = countryRepository;
        this.productRepository = productRepository;
        this.dutyTypeRepository = dutyTypeRepository;
        this.dutyRepository = dutyRepository;
        this.adValoremDutyRepository = adValoremDutyRepository;
        this.specificDutyRepository = specificDutyRepository;
        this.combinedDutyRepository = combinedDutyRepository;
        this.otherDutyRepository = otherDutyRepository;
    }

    @Transactional
    public TariffResponse createTariff(TariffRequest request) {
        // Validate and fetch related entities
        Country reporter = countryRepository.findById(request.getReporterCode())
                .orElseThrow(() -> new InvalidRequestException("Reporter country not found: " + request.getReporterCode()));
        
        Country partner = countryRepository.findById(request.getPartnerCode())
                .orElseThrow(() -> new InvalidRequestException("Partner country not found: " + request.getPartnerCode()));
        
        // ✅ Auto-create product if it doesn't exist (allows admins to add new HS codes)
        Product product = productRepository.findById(request.getTlCode())
                .orElseGet(() -> {
                    Product newProduct = new Product();
                    newProduct.setTlCode(request.getTlCode());
                    newProduct.setDescription("Pending classification - Added via admin");
                    newProduct.setDigits(request.getTlCode().length());
                    return productRepository.save(newProduct);
                });
        
        // ✅ Auto-create duty type if it doesn't exist (allows admins to add new duty types)
        DutyTypeId dutyTypeId = new DutyTypeId(request.getDutyType(), request.getDutyCode());
        DutyType dutyType = dutyTypeRepository.findById(dutyTypeId)
                .orElseGet(() -> {
                    DutyType newDutyType = new DutyType();
                    newDutyType.setId(dutyTypeId);
                    // Generate description based on the codes from frontend
                    String description = generateDutyTypeDescription(request.getDutyType(), request.getDutyCode());
                    newDutyType.setDutyTypeDescription(description);
                    return dutyTypeRepository.save(newDutyType);
                });

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
    public TariffResponse updateTariff(Integer id, Map<String, Object> updates) {
        TariffSchedule tariff = tariffRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tariff not found"));
        
        // Only update tariff-level editable fields that are present
        if (updates.containsKey("tlsSuffix")) {
            tariff.setTlsSuffix((String) updates.get("tlsSuffix"));
        }
        if (updates.containsKey("note")) {
            tariff.setNote((String) updates.get("note"));
        }

        // Detect if any duty rate fields are present in the update payload
        boolean hasDutyUpdate = updates.containsKey("adValoremRate")
                || updates.containsKey("specificRate")
                || updates.containsKey("compoundRate1")
                || updates.containsKey("compoundRate2")
                || updates.containsKey("specificRateUnit");

        if (hasDutyUpdate) {
            // Ensure duty exists on the tariff before attempting to update rates
            if (tariff.getDuty() == null) {
                throw new DutyNotFoundException("Duty information not found for tariff id: " + id);
            }

            // Build a TariffRequest DTO with provided rate fields (only set those present)
            TariffRequest req = new TariffRequest();
            if (updates.containsKey("adValoremRate")) {
                Object v = updates.get("adValoremRate");
                req.setAdValoremRate(v != null ? ((Number) v).doubleValue() : null);
            }
            if (updates.containsKey("specificRate")) {
                Object v = updates.get("specificRate");
                req.setSpecificRate(v != null ? ((Number) v).doubleValue() : null);
            }
            if (updates.containsKey("specificRateUnit")) {
                req.setSpecificRateUnit((String) updates.get("specificRateUnit"));
            }
            if (updates.containsKey("compoundRate1")) {
                Object v = updates.get("compoundRate1");
                req.setCompoundRate1(v != null ? ((Number) v).doubleValue() : null);
            }
            if (updates.containsKey("compoundRate2")) {
                Object v = updates.get("compoundRate2");
                req.setCompoundRate2(v != null ? ((Number) v).doubleValue() : null);
            }

            // Delegate duty-specific updates to helper
            updateDutyFromRequest(req, tariff.getDuty());
        }
        
        tariff = tariffRepository.save(tariff);
        return convertToResponse(tariff);
    }

    // ✅ Helper method
    private boolean hasAnyDutyRate(TariffRequest request) {
        return request.getAdValoremRate() != null ||
               request.getSpecificRate() != null ||
               request.getCompoundRate1() != null ||
               request.getCompoundRate2() != null;
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
        boolean hasCompound = request.getCompoundRate1() != null || request.getCompoundRate2() != null;

        // Validate ad valorem rate range
        if (hasAdValorem) {
            if (request.getAdValoremRate() < 0 || request.getAdValoremRate() > 100) {
                throw new InvalidRequestException(
                    "Ad valorem rate must be between 0 and 100, got: " + request.getAdValoremRate()
                );
            }
        }

        // Validate specific rate
        if (hasSpecific) {
            if (request.getSpecificRate() < 0) {
                throw new InvalidRequestException(
                    "Specific rate must be non-negative, got: " + request.getSpecificRate()
                );
            }
            // ✅ Require unit ONLY if creating new specific duty (not for updates)
            // This check is now removed - we'll keep the existing unit if not provided
        }

        // Validate compound rates - both must be provided together
        if (request.getCompoundRate1() != null && request.getCompoundRate2() == null) {
            throw new InvalidRequestException("Both compound rates must be specified together");
        }
        if (request.getCompoundRate2() != null && request.getCompoundRate1() == null) {
            throw new InvalidRequestException("Both compound rates must be specified together");
        }

        // Validate compound rate values
        if (hasCompound) {
            if (request.getCompoundRate1() != null && request.getCompoundRate1() < 0) {
                throw new InvalidRequestException(
                    "Compound rate 1 must be non-negative, got: " + request.getCompoundRate1()
                );
            }
            if (request.getCompoundRate2() != null && request.getCompoundRate2() < 0) {
                throw new InvalidRequestException(
                    "Compound rate 2 must be non-negative, got: " + request.getCompoundRate2()
                );
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
        // ✅ Only update if the request contains rates matching the duty type
        boolean updated = false;
        
        if (duty instanceof AdValoremDuty && request.getAdValoremRate() != null) {
            AdValoremDuty adValorem = (AdValoremDuty) duty;
            adValorem.setRatePercent(BigDecimal.valueOf(request.getAdValoremRate()));
            adValorem.setMathExpression(request.getAdValoremRate() + "%");
            adValoremDutyRepository.save(adValorem);
            updated = true;
            
        } else if (duty instanceof SpecificDuty && request.getSpecificRate() != null) {
            SpecificDuty specific = (SpecificDuty) duty;
            specific.setAmount(BigDecimal.valueOf(request.getSpecificRate()));
            
            // ✅ Only update unit if provided, otherwise keep existing
            if (request.getSpecificRateUnit() != null && !request.getSpecificRateUnit().trim().isEmpty()) {
                specific.setUnit(request.getSpecificRateUnit());
            }
            
            specific.setSpecificDutyRateRaw(request.getSpecificRate() + " " + specific.getUnit());
            specific.setMathExpression(request.getSpecificRate() + " per " + specific.getUnit());
            specificDutyRepository.save(specific);
            updated = true;
            
        } else if (duty instanceof CombinedDuty && 
                   (request.getCompoundRate1() != null || request.getCompoundRate2() != null)) {
            CombinedDuty combined = (CombinedDuty) duty;
            
            // ✅ Update only the rates that are provided
            if (request.getCompoundRate1() != null) {
                combined.setRatePercent(BigDecimal.valueOf(request.getCompoundRate1()));
            }
            if (request.getCompoundRate2() != null) {
                combined.setAmount(BigDecimal.valueOf(request.getCompoundRate2()));
            }
            if (request.getSpecificRateUnit() != null && !request.getSpecificRateUnit().trim().isEmpty()) {
                combined.setUnit(request.getSpecificRateUnit());
            }
            
            combined.setSpecificDutyRateRaw(
                String.format("%.2f%% + %.2f per %s", 
                    combined.getRatePercent().doubleValue(), 
                    combined.getAmount().doubleValue(), 
                    combined.getUnit())
            );
            combined.setMathExpression(
                String.format("%.2f%% + %.2f/%s", 
                    combined.getRatePercent().doubleValue(), 
                    combined.getAmount().doubleValue(), 
                    combined.getUnit())
            );
            combinedDutyRepository.save(combined);
            updated = true;
        }
        
        // ✅ Only throw error if rates were provided but didn't match duty type
        if (!updated && hasAnyDutyRate(request)) {
            throw new InvalidRequestException(
                "Cannot update duty rates: provided rates don't match existing duty type (" + 
                duty.getClass().getSimpleName() + ")"
            );
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
            
            System.out.println("🔍 DUTY TYPE CHECK - TariffID: " + tariff.getTariffId() + ", Duty Nature: " + duty.getDutyNature() + ", Class: " + duty.getClass().getSimpleName());
            
            if (duty instanceof AdValoremDuty) {
                AdValoremDuty adValorem = (AdValoremDuty) duty;
                System.out.println("   → Ad Valorem: " + adValorem.getRatePercent() + "%");
                builder.adValoremRate(adValorem.getRatePercent().doubleValue());
                
            } else if (duty instanceof SpecificDuty) {
                SpecificDuty specific = (SpecificDuty) duty;
                System.out.println("   → Specific: " + specific.getAmount() + " " + specific.getUnit());
                builder.specificRate(specific.getAmount().doubleValue())
                       .specificRateUnit(specific.getUnit());
                       
            } else if (duty instanceof CombinedDuty) {
                CombinedDuty combined = (CombinedDuty) duty;
                System.out.println("   → Combined: " + combined.getRatePercent() + "% + " + combined.getAmount() + " " + combined.getUnit());
                builder.adValoremRate(combined.getRatePercent() != null ? combined.getRatePercent().doubleValue() : null)
                       .specificRate(combined.getAmount() != null ? combined.getAmount().doubleValue() : null)
                       .specificRateUnit(combined.getUnit())
                       .compoundRate1(combined.getRatePercent() != null ? combined.getRatePercent().doubleValue() : null)
                       .compoundRate2(combined.getAmount() != null ? combined.getAmount().doubleValue() : null);
                       
            } else if (duty instanceof OtherDuty) {
                OtherDuty other = (OtherDuty) duty;
                System.out.println("   → Other: rawText=" + other.getRawText() + ", isComputable=" + other.getIsComputable());
                builder.rawText(other.getRawText())
                       .isComputable(other.getIsComputable());
            }
        } else {
            throw new DutyNotFoundException("Duty information not found for tariff id: " + tariff.getTariffId());
        }

        return builder.build();
    }
    
    /**
     * Generate a human-readable description for a duty type based on codes.
     * Matches the frontend dutyTypes array descriptions.
     */
    private String generateDutyTypeDescription(String dutyType, String dutyCode) {
        String key = dutyType + "-" + dutyCode;
        
        // Match the frontend descriptions from TariffsTab.tsx
        switch (key) {
            case "0-0":
                return "Standard (MFN)";
            case "0-2":
                return "Duty-Free";
            case "1-0":
                return "Preferential (Trade Agreement)";
            case "1-1":
                return "Preferential (Specific)";
            case "2-0":
                return "GSP (Developing Countries)";
            case "3-0":
                return "Temporary";
            default:
                // Fallback for unknown duty types
                return "Custom duty type " + key + " - Added via admin";
        }
    }
}
