package org.dcsa.conformance.standards.an.checks;

import org.dcsa.conformance.core.check.KeywordDataset;

public class ANDatasets {
  public static final KeywordDataset DELIVERY_TYPE_AT_DESTINATION = KeywordDataset.staticDataset("CY", "SD", "CFS");
  public static final KeywordDataset CARRIER_CODE_LIST_PROVIDER = KeywordDataset.staticDataset("SMDG", "NMFTA");
  public static final KeywordDataset PARTY_FUNCTION = KeywordDataset.staticDataset("OS", "CN", "END", "RW", "CG", "N1", "N2", "NI", "SCO", "DDR", "DDS", "COW", "COX", "CS", "MF", "WH");
  public static final KeywordDataset FACILITY_CODE_LIST_PROVIDER =
      KeywordDataset.staticDataset("SMDG", "BIC");
  public static final KeywordDataset CARGO_GROSS_WEIGHT_UNIT =
      KeywordDataset.staticDataset("KGM", "LBR", "GRM", "ONZ");
  public static final KeywordDataset FREE_TIME_TYPE_CODES =
      KeywordDataset.staticDataset("DEM", "DET", "STO");
  public static final KeywordDataset FREE_TIME_TIME_UNIT =
      KeywordDataset.staticDataset("CD", "WD", "HR");
  public static final KeywordDataset PAYMENT_TERM_CODE = KeywordDataset.staticDataset("PRE", "COL");
}
