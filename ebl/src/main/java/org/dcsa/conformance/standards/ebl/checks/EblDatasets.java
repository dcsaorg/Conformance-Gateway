package org.dcsa.conformance.standards.ebl.checks;


import org.dcsa.conformance.core.check.KeywordDataset;

public class EblDatasets {

  public static final KeywordDataset EBL_PLATFORMS_DATASET = KeywordDataset.staticDataset(
    "WAVE",
    "CARX",
    "ESSD",
    "IDT",
    "BOLE",
    "EDOX",
    "IQAX",
    "SECR",
    "TRGO",
    "ETEU",
    "TRAC",
    "BRIT"
  );


  public static final KeywordDataset CARGO_MOVEMENT_TYPE = KeywordDataset.staticDataset("FCL", "LCL");
  public static final KeywordDataset REFERENCE_TYPE = KeywordDataset.staticDataset(
    "CR",
    "AKG"
  );


  public static final KeywordDataset DG_INHALATION_ZONES = KeywordDataset.fromCSV("/standards/ebl/datasets/inhalationzones.csv");
  public static final KeywordDataset DG_SEGREGATION_GROUPS = KeywordDataset.fromCSV("/standards/ebl/datasets/segregationgroups.csv");

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

  public static final KeywordDataset DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES = KeywordDataset.staticDataset(
    "WAVE",
    "CARX",
    "ESSD",
    "IDT",
    "BOLE",
    "EDOX",
    "IQAX",
    "SECR",
    "TRGO",
    "ETEU",
    "TRAC",
    "BRIT",
    "GSBN",
    "WISE",
    "GLEIF",
    "W3C",
    "DNB",
    "FMC",
    "DCSA",
    "ZZZ"
  );

}
