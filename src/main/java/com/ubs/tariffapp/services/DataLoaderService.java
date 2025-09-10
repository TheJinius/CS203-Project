package com.ubs.tariffapp.services;

import com.ubs.tariffapp.models.*;
import com.ubs.tariffapp.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataLoaderService {

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TariffScheduleRepository tariffScheduleRepository;

    @Autowired
    private DutyTypeRepository dutyTypeRepository;

    @Autowired
    private AdValoremDutyRepository adValoremDutyRepository;

    @Autowired
    private SpecificDutyRepository specificDutyRepository;

    @Autowired
    private CombinedDutyRepository combinedDutyRepository;

    public void loadCleanedData(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            List<TariffSchedule> tariffSchedules = new ArrayList<>();

            // Read the header
            String header = reader.readLine();
            if (header == null) {
                System.err.println("The cleaned data file is empty.");
                return;
            }

            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");

                // Parse the data
                String reporterId = columns[0].trim();
                String partnerId = columns[1].trim();
                Integer year = Integer.parseInt(columns[2].trim());
                String hsCode = columns[3].trim();
                String hsDescription = columns[4].trim();
                String dutyTypeCode = columns[5].trim();
                String dutyCode = columns[6].trim();
                String tlsSuffix = columns[7].trim();
                String note = columns[8].trim();
                BigDecimal adValoremRate = new BigDecimal(columns[9].trim());
                BigDecimal specificRate = new BigDecimal(columns[10].trim());
                String industry = columns[11].trim();

                // Fetch or create related entities
                Country reporter = countryRepository.findById(reporterId)
                        .orElseGet(() -> countryRepository.save(new Country(reporterId, null, null, null, null)));

                Country partner = countryRepository.findById(partnerId)
                        .orElseGet(() -> countryRepository.save(new Country(partnerId, null, null, null, null)));

                Product product = productRepository.findById(hsCode)
                        .orElseGet(() -> productRepository.save(new Product(hsCode, hsDescription, industry, null)));

                DutyTypeId dutyTypeId = new DutyTypeId(dutyTypeCode, dutyCode);
                DutyType dutyType = dutyTypeRepository.findById(dutyTypeId)
                        .orElseGet(() -> dutyTypeRepository.save(new DutyType(dutyTypeId, null, null)));

                // Create and save AdValoremDuty
                AdValoremDuty adValoremDuty = new AdValoremDuty();
                adValoremDuty.setRatePercent(adValoremRate);
                adValoremDuty = adValoremDutyRepository.save(adValoremDuty);

                // Create and save SpecificDuty
                SpecificDuty specificDuty = new SpecificDuty();
                specificDuty.setRate(specificRate);
                specificDuty = specificDutyRepository.save(specificDuty);

                // Create and save CombinedDuty
                CombinedDuty combinedDuty = new CombinedDuty();
                combinedDuty.setAdValoremDuty(adValoremDuty);
                combinedDuty.setSpecificDuty(specificDuty);
                combinedDuty = combinedDutyRepository.save(combinedDuty);

                // Create and save TariffSchedule
                TariffSchedule tariffSchedule = new TariffSchedule();
                tariffSchedule.setReporter(reporter);
                tariffSchedule.setPartner(partner);
                tariffSchedule.setYear(year);
                tariffSchedule.setProduct(product);
                tariffSchedule.setTlsSuffix(tlsSuffix);
                tariffSchedule.setDutyType(dutyType);
                tariffSchedule.setNote(note);
                tariffSchedule.setCombinedDuty(combinedDuty);

                tariffSchedules.add(tariffSchedule);
            }

            // Save all TariffSchedules
            tariffScheduleRepository.saveAll(tariffSchedules);
            System.out.println("Cleaned data loaded successfully into the database.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}