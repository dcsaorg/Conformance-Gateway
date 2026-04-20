package org.dcsa.conformance.standards.ovs.checks;

import org.dcsa.conformance.core.check.KeywordDataset;

public class OVSDataSets {

  public static final KeywordDataset STATUS_CODE =
      KeywordDataset.staticDataset("OMIT", "BLNK", "ADHO", "PHOT", "PHIN", "SLID", "ROTC", "CUTR");

  public static final KeywordDataset STATUS_CODES =
      KeywordDataset.staticDataset("OMIT", "BLNK", "ADHO", "PHOT", "PHIN", "SLID", "ROTC", "CUTR", "DRYD", "BUNK", "OOSV");
}
