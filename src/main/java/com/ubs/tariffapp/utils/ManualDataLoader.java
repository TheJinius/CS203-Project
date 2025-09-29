// package com.ubs.tariffapp.utils;

// import com.ubs.tariffapp.services.DataLoaderService;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.context.ApplicationContext;

// @SpringBootApplication
// public class ManualDataLoader implements CommandLineRunner {

//     private final DataLoaderService dataLoaderService;

//     public ManualDataLoader(DataLoaderService dataLoaderService) {
//         this.dataLoaderService = dataLoaderService;
//     }

//     public static void main(String[] args) {
//         SpringApplication app = new SpringApplication(ManualDataLoader.class);
//         app.run(args);
//     }

//     @Override
//     public void run(String... args) throws Exception {
//         System.out.println("=== Manual Data Loader ===");
        
//         String fileName = args.length > 0 ? args[0] : "clean_HS2017SGYear2023.csv";
        
//         try {
//             System.out.println("Loading data from file: " + fileName);
//             dataLoaderService.loadCleanedData(fileName);
//             System.out.println("✅ Data loading completed successfully!");
//         } catch (Exception e) {
//             System.err.println("❌ Error loading data: " + e.getMessage());
//             e.printStackTrace();
//         }
        
//         System.exit(0); // Exit after completion
//     }
// }