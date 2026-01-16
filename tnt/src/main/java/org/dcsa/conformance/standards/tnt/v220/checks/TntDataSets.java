package org.dcsa.conformance.standards.tnt.v220.checks;

import lombok.NoArgsConstructor;
import org.dcsa.conformance.core.check.KeywordDataset;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class TntDataSets {

  public static final KeywordDataset VALID_EVENT_TYPES =
    KeywordDataset.staticDataset("TRANSPORT", "SHIPMENT", "EQUIPMENT");

  public static final KeywordDataset VALID_SHIPMENT_EVENT_TYPES =
    KeywordDataset.staticDataset("RECE", "DRFT", "PENA", "PENU", "REJE", "APPR", "ISSU", "SURR",
      "SUBM", "VOID", "CONF", "REQS", "CMPL", "HOLD", "RELS");

  public static final KeywordDataset VALID_DOCUMENT_TYPE_CODES =
    KeywordDataset.staticDataset("CBR", "BKG", "SHI", "SRM", "TRD", "ARN", "VGM", "CAS", "CUS",
      "DGD", "OOG");

  public static final KeywordDataset VALID_EQUIPMENT_EVENT_TYPES =
    KeywordDataset.staticDataset("LOAD", "DISC", "GTIN", "GTOT", "STUF", "STRP", "PICK", "DROP",
      "INSP", "RSEA", "RMVD");
}
