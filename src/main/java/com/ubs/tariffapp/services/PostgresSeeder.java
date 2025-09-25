package com.ubs.tariffapp.services;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

public class PostgresSeeder {

  // TODO: set these to your database info
  private static final String URL  = "jdbc:postgresql://localhost:5432/yourdb";
  private static final String USER = "youruser";
  private static final String PASS = "yourpass";

  // Path to your CSV file
  private static final Path CSV = Path.of("data/clean_HS2017AUSYear2023.csv");

  public static void main(String[] args) throws Exception {
    try (Connection c = DriverManager.getConnection(URL, USER, PASS)) {
      c.setAutoCommit(false);

      createStagingIfNeeded(c);
      truncateStaging(c);
      copyIntoStaging(c, CSV);

      mergeIntoTariffSchedule(c);

      c.commit();
      System.out.println("Seeding complete ✅");
    }
  }

  private static void createStagingIfNeeded(Connection c) throws SQLException {
    String ddl = """
      CREATE TABLE IF NOT EXISTS hs2017_au_staging (
        "Reporter"               integer,
        "ReporterName"           text,
        "Partner"                integer,
        "PartnerName"            text,
        "Year"                   integer,
        "TL"                     bigint,
        "TLS"                    real,
        "Duty Type"              integer,
        "Duty Code"              integer,
        "AV Duty Rate"           double precision,
        "Specific Duty Rate"     text,
        "TrfLineDescription"     text,
        "DutyTypeDescription"    text,
        "Duty Nature"            text,
        "AvMethod"               text,
        "Note"                   text,
        "Industry"               text
      );
    """;
    try (PreparedStatement ps = c.prepareStatement(ddl)) {
      ps.executeUpdate();
    }
  }

  private static void truncateStaging(Connection c) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("TRUNCATE hs2017_au_staging")) {
      ps.executeUpdate();
    }
  }

  private static void copyIntoStaging(Connection c, Path csvPath) throws Exception {
    if (!Files.exists(csvPath)) {
      throw new IllegalArgumentException("CSV not found: " + csvPath.toAbsolutePath());
    }
    String copySql = """
      COPY hs2017_au_staging
      ("Reporter","ReporterName","Partner","PartnerName","Year","TL","TLS",
       "Duty Type","Duty Code","AV Duty Rate","Specific Duty Rate",
       "TrfLineDescription","DutyTypeDescription","Duty Nature","AvMethod","Note","Industry")
      FROM STDIN WITH (FORMAT csv, HEADER true)
    """;
    CopyManager cm = new CopyManager(c.unwrap(BaseConnection.class));
    try (Reader r = Files.newBufferedReader(csvPath)) {
      long rows = cm.copyIn(copySql, r);
      System.out.println("Copied into staging: " + rows + " rows");
    }
  }

  private static void mergeIntoTariffSchedule(Connection c) throws SQLException {
    String mergeTariff = """
      INSERT INTO tariff_schedule (tl_code, digits, description)
      SELECT
        "TL"::text                              AS tl_code,
        length("TL"::text)                      AS digits,
        NULLIF("TrfLineDescription"::text, '') AS description
      FROM hs2017_au_staging
      ON CONFLICT (tl_code) DO UPDATE
      SET digits = EXCLUDED.digits,
          description = EXCLUDED.description;
    """;
    try (PreparedStatement ps = c.prepareStatement(mergeTariff)) {
      int count = ps.executeUpdate();
      System.out.println("Merged into tariff_schedule: " + count + " rows");
    }
  }
}