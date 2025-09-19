package com.ubs.tariffapp;

import com.ubs.tariffapp.services.DataLoaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoaderRunner implements CommandLineRunner {

    @Autowired
    private DataLoaderService dataLoaderService;

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0 && "load-data".equals(args[0])) {
            String fileName = args.length > 1 ? args[1] : "clean_HS2017SGYear2023.csv";
            System.out.println("Loading data from: " + fileName);
            dataLoaderService.loadCleanedData(fileName);
            System.out.println("Data loading completed!");
        }
    }
}