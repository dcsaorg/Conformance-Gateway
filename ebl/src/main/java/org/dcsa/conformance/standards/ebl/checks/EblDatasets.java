package org.dcsa.conformance.standards.ebl.checks;

import com.opencsv.CSVReader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import lombok.SneakyThrows;
import org.dcsa.conformance.core.check.KeywordDataset;

public class EblDatasets {

  public static final KeywordDataset UN_LOCODE_DATASET = KeywordDataset.lazyLoaded(EblDatasets::loadUNLocationCodeDataset);
  public static final KeywordDataset EBL_PLATFORMS_DATASET = KeywordDataset.staticDataset("WAVE", "CARX", "ESSD", "BOLE", "EDOX", "IQAX", "SECR", "TRGO");

  public static final KeywordDataset CARGO_MOVEMENT_TYPE = KeywordDataset.staticDataset("FCL", "LCL");

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
      var stream = EblDatasets.class.getResourceAsStream("/standards/ebl/datasets/" + file);
      if (stream == null) {
        throw new IllegalStateException("Missing resource: /standards/ebl/datasets/" + file);
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
