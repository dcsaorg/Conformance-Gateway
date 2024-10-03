package org.dcsa.conformance.standards.booking.checks;

import org.dcsa.conformance.core.check.KeywordDataset;


public class BookingDataSets {

  public static final KeywordDataset CARGO_MOVEMENT_TYPE = KeywordDataset.staticDataset("FCL", "LCL");

  public static final KeywordDataset COMMUNICATION_CHANNEL_CODES = KeywordDataset.staticDataset("EI", "EM", "AO");

  public static final KeywordDataset INCO_TERMS_VALUES = KeywordDataset.staticDataset("EXW", "FCA", "FAS", "FOB", "CFR", "CIF", "CPT", "CIP", "DAP", "DPU", "DDP");

  public static final KeywordDataset NATIONAL_COMMODITY_TYPE_CODES = KeywordDataset.staticDataset(    "NCM",    "HTS",    "Schedule B",    "TARIC",    "CN",    "CUS"  );

  public static final KeywordDataset CUTOFF_DATE_TIME_CODES = KeywordDataset.staticDataset("DCO", "VCO", "FCO", "LCO", "ECP", "EFC");

  public static final KeywordDataset REFERENCE_TYPES = KeywordDataset.fromCSV(BookingDataSets.class, "/standards/booking/datasets/general-reference-types.csv", "General Reference Type Code");

  //public static final KeywordDataset DG_IMO_CLASSES = KeywordDataset.fromCSV(BookingDataSets.class, "/standards/booking/datasets/imoclasses.csv");

  public static final KeywordDataset DG_SEGREGATION_GROUPS = KeywordDataset.fromCSV(BookingDataSets.class, "/standards/booking/datasets/segregationgroups.csv");

  public static final KeywordDataset INHALATION_ZONE_CODE = KeywordDataset.staticDataset("A", "B", "C", "D");


}
