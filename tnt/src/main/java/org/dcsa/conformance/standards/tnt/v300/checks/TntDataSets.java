package org.dcsa.conformance.standards.tnt.v300.checks;

import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TntDataSets {

  protected static final Set<String> VALID_SHIPMENT_EVENT_TYPE_CODES =
      Set.of(
          "AMCF", "AMCN", "AMDC", "AMPR", "AMRE", "APPR", "CACF", "CADC", "CANC", "CARE", "CMPL",
          "CONF", "DECL", "DRFT", "HOLD", "ISSU", "PENA", "PENM", "PENU", "PSAM", "PSDL", "RECE",
          "REJE", "RELS", "REQS", "SUAM", "SUBM", "SUDL");

  protected static final Set<String> VALID_DOCUMENT_REFERENCE_TYPE_CODES =
      Set.of(
          "AMF", "ARN", "BKG", "CAS", "CBR", "CEA", "CEO", "CQU", "CRO", "CUC", "DEI", "DEO", "DGD",
          "FCE", "HCE", "ICE", "INV", "OOG", "PCE", "PFD", "SHI", "TRD", "TRO", "VCE", "VGM");

  protected static final Set<String> VALID_TRANSPORT_EVENT_TYPE_CODES = Set.of("ARRI", "DEPA");

  protected static final Set<String> VALID_EQUIPMENT_EVENT_TYPE_CODES =
      Set.of(
          "AVDO", "AVPU", "CROS", "CUSI", "CUSR", "CUSS", "DISC", "DROP", "GTIN", "GTOT", "INSP",
          "LOAD", "PICK", "RMVD", "RSEA", "STRP", "STUF");

  protected static final Set<String> VALID_IOT_EVENT_TYPE_CODES = Set.of("DETC");

  protected static final Set<String> VALID_IOT_EVENT_CODES = Set.of("DRC", "DRO", "LOC");

  protected static final Set<String> VALID_REEFER_EVENT_TYPE_CODES = Set.of("MEAS", "ADJU");
}
