package org.dcsa.conformance.standards.booking.checks;

import org.dcsa.conformance.core.check.KeywordDataset;

public class BookingDataSets {

  public static final KeywordDataset CARGO_MOVEMENT_TYPE =
    KeywordDataset.staticDataset("FCL", "LCL");

  public static final KeywordDataset NATIONAL_COMMODITY_TYPE_CODES =
    KeywordDataset.staticDataset("NCM", "HTS", "SCHEDULE_B", "TARIC", "CN", "CUS");

  public static final KeywordDataset CUTOFF_DATE_TIME_CODES =
    KeywordDataset.staticDataset("DCO", "VCO", "FCO", "LCO", "EFC");

  public static final KeywordDataset SHIPPER_REFERENCE_TYPES =
    KeywordDataset.staticDataset("CR", "AKG", "AEF");

  public static final KeywordDataset CARRIER_REFERENCE_TYPES =
    KeywordDataset.staticDataset("CR", "ECR", "AKG", "AEF");

  public static final KeywordDataset DG_SEGREGATION_GROUPS =
    KeywordDataset.fromCSV("/standards/booking/datasets/segregationgroups.csv");

  public static final KeywordDataset INHALATION_ZONE_CODE =
    KeywordDataset.staticDataset("A", "B", "C", "D");

  public static final KeywordDataset MODE_OF_TRANSPORT =
    KeywordDataset.staticDataset(
      "VESSEL",
      "RAIL",
      "TRUCK",
      "BARGE",
      "RAIL_TRUCK",
      "BARGE_TRUCK",
      "BARGE_RAIL",
      "MULTIMODAL");

  public static final KeywordDataset SHIPMENT_LOCATION_TYPES =
    KeywordDataset.staticDataset(
      "PRE", "POL", "POD", "PDE", "PCF", "OIR", "ORI", "IEL", "PTP", "RTP", "FCD", "ROU");

  public static final KeywordDataset OTHER_PARTY_FUNCTION_CODES =
    KeywordDataset.staticDataset("DDR", "DDS", "COW", "COX", "N1", "N2", "NI", "NAC", "CSR");

  public static final KeywordDataset CODE_LIST_PROVIDER_CODES =
    KeywordDataset.staticDataset(
      "WAVE", "CARX", "ESSD", "IDT", "BOLE", "EDOX", "IQAX", "SECR", "TRGO", "ETEU", "TRAC",
      "BRIT", "COVA", "ETIT", "KTNE", "CRED", "BLOC", "DOCU", "AEOT", "SGTD", "GSBN", "WISE",
      "GLEIF", "W3C", "DNB", "FMC", "DCSA", "ZZZ");

  public static final KeywordDataset BOOKING_STATUS =
    KeywordDataset.staticDataset(
      "RECEIVED",
      "PENDING_UPDATE",
      "UPDATE_RECEIVED",
      "CONFIRMED",
      "PENDING_AMENDMENT",
      "REJECTED",
      "DECLINED",
      "CANCELLED",
      "COMPLETED");

  public static final KeywordDataset CARRIER_BOOKING_REFERENCE_OPTIONAL_STATES =
    KeywordDataset.staticDataset(
      "RECEIVED", "REJECTED", "PENDING_UPDATE", "UPDATE_RECEIVED", "CANCELLED");

  public static final KeywordDataset AMENDED_BOOKING_STATUS =
    KeywordDataset.staticDataset(
      "AMENDMENT_RECEIVED",
      "AMENDMENT_CONFIRMED",
      "AMENDMENT_DECLINED",
      "AMENDMENT_CANCELLED");

  public static final KeywordDataset BOOKING_CANCELLATION_STATUS =
    KeywordDataset.staticDataset(
      "CANCELLATION_RECEIVED", "CANCELLATION_DECLINED", "CANCELLATION_CONFIRMED");

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
