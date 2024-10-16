package org.dcsa.conformance.standards.booking.checks;

import org.dcsa.conformance.core.check.KeywordDataset;


public class BookingDataSets {

  public static final KeywordDataset CARGO_MOVEMENT_TYPE = KeywordDataset.staticDataset("FCL", "LCL");

  public static final KeywordDataset NATIONAL_COMMODITY_TYPE_CODES = KeywordDataset.staticDataset(    "NCM",    "HTS",    "Schedule B",    "TARIC",    "CN",    "CUS"  );

  public static final KeywordDataset CUTOFF_DATE_TIME_CODES = KeywordDataset.staticDataset("DCO", "VCO", "FCO", "LCO", "ECP", "EFC");

  public static final KeywordDataset REFERENCE_TYPES = KeywordDataset.fromCSV("/standards/booking/datasets/general-reference-types.csv", "General Reference Type Code");

  public static final KeywordDataset DG_SEGREGATION_GROUPS = KeywordDataset.fromCSV("/standards/booking/datasets/segregationgroups.csv");

  public static final KeywordDataset INHALATION_ZONE_CODE = KeywordDataset.staticDataset("A", "B", "C", "D");

}
