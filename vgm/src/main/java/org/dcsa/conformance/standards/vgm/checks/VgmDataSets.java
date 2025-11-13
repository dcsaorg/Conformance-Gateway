package org.dcsa.conformance.standards.vgm.checks;

import lombok.NoArgsConstructor;
import org.dcsa.conformance.core.check.KeywordDataset;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class VgmDataSets {

  public static final KeywordDataset VGM_WEIGHT_UNIT = KeywordDataset.staticDataset("KGM", "LBR");

  public static final KeywordDataset VGM_METHOD = KeywordDataset.staticDataset("SM1", "SM2");
}
