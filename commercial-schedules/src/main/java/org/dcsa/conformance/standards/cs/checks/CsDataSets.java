package org.dcsa.conformance.standards.cs.checks;

import org.dcsa.conformance.core.check.KeywordDataset;

public class CsDataSets {
  public static final KeywordDataset RECEIPT_TYPE_AT_ORIGIN = KeywordDataset.staticDataset("CY", "SD", "CFS");
  public static final KeywordDataset DELIVERY_TYPE_AT_DESTINATION = KeywordDataset.staticDataset("CY", "SD", "CFS");
  public static final KeywordDataset CUTOFF_DATE_TIME_CODES = KeywordDataset.staticDataset("DCO", "VCO", "FCO", "LCO", "PCO","ECP","EFC","RCO","DGC","OBC","TCO","STA","SPA","CUA","AFC");
}
