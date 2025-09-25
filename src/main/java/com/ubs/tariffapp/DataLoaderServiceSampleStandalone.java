package com.ubs.tariffapp;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;

public class DataLoaderServiceSampleStandalone {

  public static void main(String[] args) throws Exception {
    // CLI: load-data <file.csv> [rows]
    if (args.length == 0 || !"load-data".equalsIgnoreCase(args[0])) {
      System.out.println("Usage: java ... DataLoaderServiceSampleStandalone load-data <file.csv> [rows]");
      return;
    }
    String csvPath = (args.length > 1) ? args[1] : "clean_HS2017SGYear2023.csv";
    int rows       = (args.length > 2) ? Integer.parseInt(args[2]) : 50;

    String url  = envOr("PG_URL",  "jdbc:postgresql://cs203-postgres-db.cvycqw6640dd.ap-southeast-1.rds.amazonaws.com:5432/cs203db?sslmode=require");
    String user = envOr("PG_USER", "postgres");
    String pass = envOr("PG_PASS", "ilovecs203andubs");

    try (Connection c = DriverManager.getConnection(url, user, pass)) {
      c.setAutoCommit(false);
      createStaging(c);
      try (Reader r = slice(csvPath, rows)) {
        copyIn(c, r);
      }
      merge(c);
      c.commit();
      System.out.println("Done.");
    }
  }

  static String envOr(String k, String d){ String v=System.getenv(k); return (v==null||v.isBlank())?d:v; }

  static void createStaging(Connection c) throws Exception {
    String ddl = """
      CREATE TABLE IF NOT EXISTS hs2017_au_staging (
        "Reporter" integer, "ReporterName" text, "Partner" integer, "PartnerName" text,
        "Year" integer, "TL" bigint, "TLS" real, "Duty Type" integer, "Duty Code" integer,
        "AV Duty Rate" double precision, "Specific Duty Rate" text, "TrfLineDescription" text,
        "DutyTypeDescription" text, "Duty Nature" text, "AvMethod" text, "Note" text, "Industry" text
      );
    """;
    try (PreparedStatement ps = c.prepareStatement(ddl)) { ps.executeUpdate(); }
    try (PreparedStatement ps = c.prepareStatement("TRUNCATE hs2017_au_staging")) { ps.executeUpdate(); }
  }

  static Reader slice(String path, int rows) throws IOException {
    BufferedReader br = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8);
    String header = br.readLine();
    if (header == null) throw new IllegalArgumentException("Empty CSV: " + path);
    StringBuilder sb = new StringBuilder(128*1024).append(header).append('\n');
    String line; int n=0;
    while ((line = br.readLine()) != null && n<rows) { sb.append(line).append('\n'); n++; }
    System.out.println("Prepared " + n + " data rows from " + path);
    return new StringReader(sb.toString());
  }

  static void copyIn(Connection c, Reader reader) throws Exception {
    String sql = """
      COPY hs2017_au_staging
      ("Reporter","ReporterName","Partner","PartnerName","Year","TL","TLS",
       "Duty Type","Duty Code","AV Duty Rate","Specific Duty Rate",
       "TrfLineDescription","DutyTypeDescription","Duty Nature","AvMethod","Note","Industry")
      FROM STDIN WITH (FORMAT csv, HEADER true)
    """;
    CopyManager cm = new CopyManager(c.unwrap(BaseConnection.class));
    long rows = cm.copyIn(sql, reader);
    System.out.println("Copied into staging: " + rows);
  }

  static void merge(Connection c) throws Exception {
    String merge = """
      INSERT INTO tariff_schedule (tl_code, digits, description)
      SELECT "TL"::text, length("TL"::text), NULLIF("TrfLineDescription"::text,'')
      FROM hs2017_au_staging
      ON CONFLICT (tl_code) DO UPDATE
      SET digits = EXCLUDED.digits, description = EXCLUDED.description;
    """;
    try (PreparedStatement ps = c.prepareStatement(merge)) { ps.executeUpdate(); }
  }
}
