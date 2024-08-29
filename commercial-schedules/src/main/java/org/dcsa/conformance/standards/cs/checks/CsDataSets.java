package org.dcsa.conformance.standards.cs.checks;

import lombok.NoArgsConstructor;
import org.dcsa.conformance.core.check.KeywordDataset;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class CsDataSets {
  public static final KeywordDataset CUTOFF_DATE_TIME_CODES =
      KeywordDataset.staticDataset(
          "DCO", "VCO", "FCO", "LCO", "PCO", "ECP", "EFC", "RCO", "DGC", "OBC", "TCO", "STA", "SPA",
          "CUA", "AFC");
}
