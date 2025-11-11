package org.dcsa.conformance.standards.ebl.checks;

import lombok.NoArgsConstructor;
import org.dcsa.conformance.core.check.KeywordDataset;

@NoArgsConstructor
public class EblDatasets {

  public static final KeywordDataset EBL_PLATFORMS_DATASET =
      KeywordDataset.staticDataset(
          "WAVE", "CARX", "ESSD", "IDT", "BOLE", "EDOX", "IQAX", "SECR", "TRGO", "ETEU", "TRAC",
          "BRIT");

  public static final KeywordDataset CARGO_MOVEMENT_TYPE =
      KeywordDataset.staticDataset("FCL", "LCL");
  public static final KeywordDataset REFERENCE_TYPE = KeywordDataset.staticDataset("CR", "AKG");

  public static final KeywordDataset CONSIGNMENT_ITEMS_REFERENCE_TYPE =
      KeywordDataset.staticDataset("CR", "AKG", "SPO", "CPO");

  public static final KeywordDataset METHOD_OF_PAYMENT_SET =
      KeywordDataset.staticDataset("A", "B", "C", "D", "H", "Y", "Z");

  public static final KeywordDataset TYPE_OF_PERSON_SET =
      KeywordDataset.staticDataset("NATURAL_PERSON", "LEGAL_PERSON", "ASSOCIATION_OF_PERSONS");

  public static final KeywordDataset EXEMPT_PACKAGE_CODES =
      KeywordDataset.staticDataset("VY", "VS", "VR", "VQ", "VO", "VL", "NG", "NF", "NE", "VG");

  public static final KeywordDataset DG_INHALATION_ZONES =
      KeywordDataset.fromCSV("/standards/ebl/datasets/inhalationzones.csv");
  public static final KeywordDataset DG_SEGREGATION_GROUPS =
      KeywordDataset.fromCSV("/standards/ebl/datasets/segregationgroups.csv");

  public static final KeywordDataset WOOD_DECLARATION_VALUES =
      KeywordDataset.staticDataset(
          "Not Applicable", "Not treated and not certified", "Processed", "Treated and certified");
  public static final KeywordDataset MODE_OF_TRANSPORT =
      KeywordDataset.staticDataset("VESSEL", "RAIL", "TRUCK", "BARGE", "MULTIMODAL");
  public static final KeywordDataset NATIONAL_COMMODITY_CODES_SET =
      KeywordDataset.staticDataset("NCM", "HTS", "Schedule B", "TARIC", "CN", "CUS");

  public static final KeywordDataset DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES =
      KeywordDataset.staticDataset(
          "WAVE", "CARX", "ESSD", "IDT", "BOLE", "EDOX", "IQAX", "SECR", "TRGO", "ETEU", "TRAC",
          "BRIT", "GSBN", "WISE", "GLEIF", "W3C", "DNB", "FMC", "DCSA", "ZZZ");

  public static final KeywordDataset REQUESTED_CARRIER_CLAUSES_SET =
      KeywordDataset.staticDataset(
          "CARGO_CARGOSPECIFICS",
          "VESSELCONVEYANCE_COUNTRYSPECIFIC",
          "CARGO_RETURNOFEMPTYCONTAINER",
          "CARGO_CARGOVALUE",
          "CARGO_REEFERTEMPERATURE",
          "CARGO_CONFLICTINGTEMPERATURES_MIXEDLOADS",
          "SHIPPERSLOADSTOWWEIGHTANDCOUNT",
          "INTRANSITCLAUSE");

  public static final KeywordDataset PARTY_FUNCTION_CODE =
      KeywordDataset.staticDataset("SCO", "DDR", "DDS", "COW", "COX", "CS", "MF", "WH");
  public static final KeywordDataset PARTY_FUNCTION_CODE_HBL =
      KeywordDataset.staticDataset("DDR", "DDS", "CS", "MF", "WH");

  public static final KeywordDataset FEEDBACKS_SEVERITY =
      KeywordDataset.staticDataset("INFO", "WARN", "ERROR");
  public static final KeywordDataset FEEDBACKS_CODE =
      KeywordDataset.staticDataset(
          "INFORMATIONAL_MESSAGE",
          "PROPERTY_WILL_BE_IGNORED",
          "PROPERTY_VALUE_MUST_CHANGE",
          "PROPERTY_VALUE_HAS_BEEN_CHANGED",
          "PROPERTY_VALUE_MAY_CHANGE",
          "PROPERTY_HAS_BEEN_DELETED");
}
