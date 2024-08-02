package org.dcsa.conformance.standards.booking.checks;

import com.opencsv.CSVReader;
import lombok.SneakyThrows;
import org.dcsa.conformance.core.check.KeywordDataset;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;

public class BookingDataSets {
  public static final KeywordDataset UN_LOCODE_DATASET = KeywordDataset.lazyLoaded(BookingDataSets::loadUNLocationCodeDataset);

  public static final KeywordDataset CARGO_MOVEMENT_TYPE = KeywordDataset.staticDataset("FCL", "LCL");

  public static final KeywordDataset COMMUNICATION_CHANNEL_CODES = KeywordDataset.staticDataset("EI", "EM", "AO");

  public static final KeywordDataset INCO_TERMS_VALUES = KeywordDataset.staticDataset("EXW", "FCA", "FAS", "FOB", "CFR", "CIF", "CPT", "CIP", "DAP", "DPU", "DDP");

  public static final KeywordDataset CUTOFF_DATE_TIME_CODES = KeywordDataset.staticDataset("DCO", "VCO", "FCO", "LCO", "ECP", "EFC");

  public static final KeywordDataset AMF_CC_MTC_TYPE_CODES = KeywordDataset.fromVersionedCSV(BookingDataSets.class, "/standards/booking/datasets/advancemanifestfilings-v%s.csv",  "Advance Manifest Filing Type Code");
  public static final KeywordDataset ISO_4217_CURRENCY_CODES = KeywordDataset.fromCSV(BookingDataSets.class, "/standards/booking/datasets/currency-codes-iso-4217.csv", "CurrencyCode");

  public static final KeywordDataset REFERENCE_TYPES = KeywordDataset.fromCSV(BookingDataSets.class, "/standards/booking/datasets/general-reference-types.csv", "General Reference Type Code");

  public static final KeywordDataset ISO_6346_CONTAINER_CODES = KeywordDataset.fromCSV(BookingDataSets.class, "/standards/booking/datasets/iso-6346-container-codes.csv", "code");

  public static final KeywordDataset OUTER_PACKAGING_CODE = KeywordDataset.fromCSV(BookingDataSets.class, "/standards/booking/datasets/rec21_Rev12e_Annex-V-VI_2021.csv", "Code");

  public static final KeywordDataset DG_IMO_CLASSES = KeywordDataset.fromCSV(BookingDataSets.class, "/standards/booking/datasets/imoclasses.csv");

  public static final KeywordDataset DG_SEGREGATION_GROUPS = KeywordDataset.fromCSV(BookingDataSets.class, "/standards/booking/datasets/segregationgroups.csv");

  public static final KeywordDataset INHALATION_ZONE_CODE = KeywordDataset.staticDataset("A", "B", "C", "D");

  public static final KeywordDataset LTR_TYPE_CODES = KeywordDataset.fromVersionedCSV(BookingDataSets.class, "/standards/booking/datasets/taxandlegalreferences-v%s.csv", "/", "Tax and Legal Reference Country Code", "Tax and Legal Reference Type Code");

  public static final KeywordDataset ISO_3166_ALPHA2_COUNTRY_CODES = KeywordDataset.fromCSV(BookingDataSets.class, "/standards/booking/datasets/country-codes-iso3166-alpha2.csv", "Code");

  public static final KeywordDataset CUSTOMS_REFERENCE_RE_REC_TYPE_CODES = KeywordDataset.fromVersionedCSV(BookingDataSets.class, "/standards/booking/datasets/customsreferences-v%s.csv", "Customs Reference Type Code");

  @SneakyThrows
  private static KeywordDataset loadUNLocationCodeDataset() {
    var validCodes = new HashSet<>();

    for (String file : new String[] {
      // No clue why they decided to split the CSV version is split across 3 files
      // Combined these files takes about ~0.5 seconds to parse combined. We rely on KeywordDataset.lazyLoaded
      // plus re-use of the UN_LOCODE_DATASET to ensure it is only loaded once per report/run.
      "2023-1 UNLOCODE CodeListPart1.csv",
      "2023-1 UNLOCODE CodeListPart2.csv",
      "2023-1 UNLOCODE CodeListPart3.csv",
    }) {
      var stream = BookingDataSets.class.getResourceAsStream("/standards/booking/datasets/" + file);
      if (stream == null) {
        throw new IllegalStateException("Missing resource: /standards/booking/datasets/" + file);
      }
      var builder = new StringBuilder(5);
      try (stream) {
        try (var csvReader = new CSVReader(new BufferedReader(new InputStreamReader(stream, StandardCharsets.ISO_8859_1)))) {
          String[] line;
          while ((line = csvReader.readNext()) != null) {
            if (line[1].length() != 2) {
              throw new IllegalStateException("The CSV file " + file + " does not seem to have the expected format");
            }
            if (line[2].isBlank()) {
              continue;
            }
            if (line[2].length() != 3) {
              throw new IllegalStateException("The CSV file " + file + " does not seem to have the expected format");
            }
            builder.setLength(0);
            var code = builder.append(line[1]).append(line[2]).toString();
            if (!code.equals(code.toUpperCase())) {
              throw new IllegalStateException("The CSV file " + file + " does not seem to have the expected format");
            }
            validCodes.add(code);
          }
        }
      }
    }

    return Collections.unmodifiableSet(validCodes)::contains;
  }

}
