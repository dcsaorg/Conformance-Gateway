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


  public static final KeywordDataset DG_INHALATION_ZONES = KeywordDataset.fromCSV(EblDatasets.class, "/standards/ebl/datasets/inhalationzones.csv");
  public static final KeywordDataset DG_SEGREGATION_GROUPS = KeywordDataset.fromCSV(EblDatasets.class, "/standards/ebl/datasets/segregationgroups.csv");

  public static final KeywordDataset WOOD_DECLARATION_VALUES = KeywordDataset.staticDataset(
    "Not Applicable",
    "Not treated and not certified",
    "Processed",
    "Treated and certified"
  );
  public static final KeywordDataset MODE_OF_TRANSPORT = KeywordDataset.staticDataset(
    "VESSEL",
    "RAIL",
    "TRUCK",
    "BARGE",
    "MULTIMODAL"
  );
  public static final KeywordDataset NATIONAL_COMMODITY_CODES = KeywordDataset.staticDataset(
    "NCM",
    "HTS",
    "Schedule B",
    "TARIC",
    "CN",
    "CUS"
  );

}
