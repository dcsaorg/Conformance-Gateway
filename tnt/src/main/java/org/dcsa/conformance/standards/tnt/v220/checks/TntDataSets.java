package org.dcsa.conformance.standards.tnt.v220.checks;

import lombok.NoArgsConstructor;
import org.dcsa.conformance.core.check.KeywordDataset;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class TntDataSets {

  public static final KeywordDataset VALID_EVENT_TYPES =
      KeywordDataset.staticDataset("TRANSPORT", "SHIPMENT", "EQUIPMENT", "IOT", "REEFER");

  public static final KeywordDataset VALID_SHIPMENT_EVENT_TYPES =
      KeywordDataset.staticDataset(
          "AMCF", "AMCN", "AMDC", "AMPR", "AMRE", "APPR", "CACF", "CADC", "CANC", "CARE", "CMPL",
          "CONF", "DECL", "DRFT", "HOLD", "ISSU", "PENA", "PENM", "PENU", "PSAM", "PSDL", "RECE",
          "REJE", "RELS", "REQS", "SUAM", "SUBM", "SUDL", "SURR", "UPCF", "UPCN", "UPDC", "UPDT",
          "UPRE", "VOID");

  public static final KeywordDataset VALID_DOCUMENT_TYPE_CODES =
      KeywordDataset.staticDataset(
          "AMF", "ARN", "BKG", "CAS", "CBR", "CEA", "CEO", "CQU", "CRO", "CUC", "DEI", "DEO", "DGD",
          "FCE", "HCE", "ICE", "INV", "OOG", "PCE", "PFD", "SHI", "TRD", "TRO", "VCE", "VGM");

  public static final KeywordDataset VALID_EQUIPMENT_EVENT_TYPES =
      KeywordDataset.staticDataset(
          "AVDO", "AVPU", "CROS", "CUSI", "CUSR", "CUSS", "DISC", "DROP", "GTIN", "GTOT", "INSP",
          "LOAD", "PICK", "RMVD", "RSEA", "STRP", "STUF");
}
