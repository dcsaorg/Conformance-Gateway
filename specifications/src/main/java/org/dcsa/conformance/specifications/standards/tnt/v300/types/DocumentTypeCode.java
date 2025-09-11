package org.dcsa.conformance.specifications.standards.tnt.v300.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    example = "BKG",
    description = "Code used to denote the type of a document")
@AllArgsConstructor
public enum DocumentTypeCode implements EnumBase {
  CBR("Carrier Booking Request"),
  BKG("Booking"),
  SHI("Shipping Instruction"),
  TRD("Transport Document"),
  DEI("Delivery Instructions"),
  DEO("Delivery Order"),
  TRO("Transport Order"),
  CRO("Container Release Order"),
  ARN("Arrival Notice"),
  VGM("Verified Gross Mass"),
  CAS("Cargo Survey"),
  CUC("Customs Clearance"),
  DGD("Dangerous Goods Declaration"),
  OOG("Out of Gauge"),
  CQU("Contract Quotation"),
  INV("Invoice"),
  HCE("Health Certificate"),
  PCE("Phytosanitary Certificate"),
  VCE("Veterinary Certificate"),
  FCE("Fumigation Certificate"),
  ICE("Inspection Certificate"),
  CEA("Certificate of Analysis"),
  CEO("Certificate of Origin");

  private final String valueDescription;
}
