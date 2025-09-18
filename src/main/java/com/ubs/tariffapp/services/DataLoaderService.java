package com.ubs.tariffapp.services;

import com.ubs.tariffapp.models.*;
import com.ubs.tariffapp.models.duty.*;
import com.ubs.tariffapp.repositories.*;
import com.ubs.tariffapp.repositories.duty.*;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

 /*  Current Format of Raw Dataset:
     *  Column Index	Description
     *  0	"Reporter"
     *  1	"ReporterName"
     *  2	"Partner"
     *  3	"Partner Name"
     *  4	"Year"
     *  5	"TL (HS Code)"
     *  6	"TLS (Additional HS Code Sub-classification - if any)"
     *  7	"Duty Type"
     *  8	"Duty Code"
     *  9	"Ad Valorem Duty Rate (%)"
     *  10	"Specific Duty Rate"
     *  11	"HS Code Description"
     *  12	"Duty Type Description"
     *  13	"Duty Nature"
     *  14	"Ad Valorem Calculation Code/Description"
     *  15	"Notes"   
     *  16  "Industry"
*/

@Service
public class DataLoaderService {

    @Autowired
    private AdValoremDutyRepository adValoremDutyRepository;   

    @Autowired
    private CombinedDutyRepository combinedDutyRepository;

    @Autowired
    private DutyRepository dutyRepository;

    @Autowired
    private OtherDutyRepository otherDutyRepository;

    @Autowired
    private SpecificDutyRepository specificDutyRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private DutyTypeRepository dutyTypeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TariffScheduleRepository tariffScheduleRepository;


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
                String reporterName = columns[1].trim();
                String partnerId = columns[2].trim();
                String partnerName = columns[3].trim();
                int year = Integer.parseInt(columns[4].trim());
                String hsCode = columns[5].trim();
                String tls = columns[6].trim();
                String dutyTypeCode = columns[7].trim();
                String dutyCode = columns[8].trim();
                BigDecimal adValoremRate = new BigDecimal(columns[9].trim());
                BigDecimal specificRate = new BigDecimal(columns[10].trim());
                String hsDescription = columns[11].trim();
                String dutyTypeDescription = columns[12].trim();
                String dutyNature = columns[13].trim();
                String adValoremCalcDesc = columns[14].trim();
                String notes = columns[15].trim();
                String industry = columns[16].trim();

                // Create related entities and add to the repository
                
                // Create or fetch related entities
                Country reporter = countryRepository.findById(reporterId)
                        .orElseGet(() -> countryRepository.save(new Country(reporterId, reporterName, null, null, null)));

                Country partner = countryRepository.findById(partnerId)
                        .orElseGet(() -> countryRepository.save(new Country(partnerId, partnerName, null, null, null)));

                Product product = productRepository.findById(hsCode)
                        .orElseGet(() -> productRepository.save(new Product(hsCode, hsDescription, null, null)));

                DutyTypeId dutyTypeId = new DutyTypeId(dutyTypeCode, dutyCode);
                DutyType dutyType = dutyTypeRepository.findById(dutyTypeId)
                        .orElseGet(() -> dutyTypeRepository.save(new DutyType(dutyTypeId, dutyTypeDescription, null)));

                // Create TariffSchedule
                TariffSchedule tariffSchedule = new TariffSchedule();
                tariffSchedule.setReporter(reporter);
                tariffSchedule.setPartner(partner);
                tariffSchedule.setTariffYear(year);
                tariffSchedule.setProduct(product);
                tariffSchedule.setTlsSuffix(tls);
                tariffSchedule.setDutyType(dutyType);
                tariffSchedule.setNote(notes);

                // Create Duty based on type
                Duty duty;
                if (adValoremRate.compareTo(BigDecimal.ZERO) > 0) {
                    duty = new AdValoremDuty(null, tariffSchedule, dutyNature, adValoremCalcDesc, adValoremRate);
                    adValoremDutyRepository.save((AdValoremDuty) duty);
                } else if (specificRate.compareTo(BigDecimal.ZERO) > 0) {
                    duty = new SpecificDuty(null, tariffSchedule, dutyNature, null, specificRate, "kg", 100, null);
                    specificDutyRepository.save((SpecificDuty) duty);
                } else {
                    duty = new OtherDuty(null, tariffSchedule, dutyNature, null, columns[10].trim(), false);
                    otherDutyRepository.save((OtherDuty) duty);
                }

                tariffSchedule.setDuty(duty);
                tariffSchedules.add(tariffSchedule);

                // Batch save every 1000 records to optimize performance
                if (tariffSchedules.size() >= 1000) {
                    tariffScheduleRepository.saveAll(tariffSchedules);
                    tariffSchedules.clear();
                }
            }

            // Save all TariffSchedules in batch
            tariffScheduleRepository.saveAll(tariffSchedules);

            System.out.println("Data loaded successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}