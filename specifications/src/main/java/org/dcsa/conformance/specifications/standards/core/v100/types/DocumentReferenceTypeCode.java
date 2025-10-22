package org.dcsa.conformance.specifications.standards.core.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(
    type = "string",
    example = "BKG",
    description = "Code used to denote the type of a document reference")
@AllArgsConstructor
public enum DocumentReferenceTypeCode implements EnumBase {
  AMF("Advance Manifest Filing (of any subtype)"),
  ARN("Arrival Notice"),
  BKG("Booking"),
  CAS("Cargo Survey"),
  CBR("Carrier Booking Request"),
  CEA("Certificate of Analysis"),
  CEO("Certificate of Origin"),
  CQU("Contract Quotation"),
  CRO("Container Release Order"),
  CUC("Customs Clearance"),
  DEI("Delivery Instructions"),
  DEO("Delivery Order"),
  DGD("Dangerous Goods Declaration"),
  FCE("Fumigation Certificate"),
  HCE("Health Certificate"),
  ICE("Inspection Certificate"),
  INV("Invoice"),
  OOG("Out of Gauge"),
  PCE("Phytosanitary Certificate"),
  PFD("Proof of Delivery"),
  SHI("Shipping Instruction"),
  TRD("Transport Document"),
  TRO("Transport Order"),
  VCE("Veterinary Certificate"),
  VGM("Verified Gross Mass");

  private final String valueDescription;
}
