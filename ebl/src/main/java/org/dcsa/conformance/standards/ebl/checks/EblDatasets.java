package org.dcsa.conformance.standards.ebl.checks;

import com.opencsv.CSVReader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import lombok.SneakyThrows;
import org.dcsa.conformance.core.check.KeywordDataset;
import org.dcsa.conformance.core.check.VersionedStaticKeywordSet;

public class EblDatasets {
  public static final KeywordDataset UN_LOCODE_DATASET = KeywordDataset.lazyLoaded(EblDatasets::loadUNLocationCodeDataset);
  public static final KeywordDataset EBL_PLATFORMS_DATASET = KeywordDataset.staticVersionedDataset(
    VersionedStaticKeywordSet.versionedKeywords(
      _ignored -> false,
      Set.of("ESSD"),
      Set.of("ICED")
    ),
    "WAVE",
    "BOLE",
    "EDOX",
    "IQAX",
    "SECR",
    "CARX",
    "TRGO"
  );


  public static final KeywordDataset CARGO_MOVEMENT_TYPE = KeywordDataset.staticDataset("FCL", "LCL");
  public static final KeywordDataset REFERENCE_TYPE = KeywordDataset.staticDataset(
    "CR",
    "AKG"
  );

  public static final KeywordDataset DG_IMO_CLASSES = KeywordDataset.fromCSV(EblDatasets.class, "/standards/ebl/datasets/imoclasses.csv");
  public static final KeywordDataset DG_INHALATION_ZONES = KeywordDataset.fromCSV(EblDatasets.class, "/standards/ebl/datasets/inhalationzones.csv");
  public static final KeywordDataset DG_SEGREGATION_GROUPS = KeywordDataset.fromCSV(EblDatasets.class, "/standards/ebl/datasets/segregationgroups.csv");

  public static final KeywordDataset ISO_3166_ALPHA2_COUNTRY_CODES = KeywordDataset.fromCSV(EblDatasets.class, "/standards/ebl/datasets/country-codes-iso3166-alpha2.csv", "Code");

  public static final KeywordDataset AMF_CC_MTC_REQUIRES_SELF_FILER_CODE = KeywordDataset.staticDataset(
    "US/ACE",
    "CA/ACI"
  );
  public static final KeywordDataset AMF_CC_MTC_COMBINATIONS = KeywordDataset.fromVersionedCSV(EblDatasets.class, "/standards/ebl/datasets/advancemanifestfilings-v%s.csv", "/", "Country Code", "Advance Manifest Filing Type Code");
  public static final KeywordDataset LTR_CC_T_COMBINATIONS = KeywordDataset.fromVersionedCSV(EblDatasets.class, "/standards/ebl/datasets/taxandlegalreferences-v%s.csv", "/", "Tax and Legal Reference Country Code", "Tax and Legal Reference Type Code");
  public static final KeywordDataset CUSTOMS_REFERENCE_CC_RTC_COMBINATIONS = KeywordDataset.fromVersionedCSV(EblDatasets.class, "/standards/ebl/datasets/customsreferences-v%s.csv", "/", "Customs Reference Country Code", "Customs Reference Type Code");

  public static final KeywordDataset OUTER_PACKAGING_CODE = KeywordDataset.fromCSV(EblDatasets.class, "/standards/ebl/datasets/rec21_Rev12e_Annex-V-VI_2021.csv", "Code");

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
