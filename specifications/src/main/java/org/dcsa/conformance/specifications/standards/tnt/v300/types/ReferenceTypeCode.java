package org.dcsa.conformance.specifications.standards.tnt.v300.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dcsa.conformance.specifications.generator.EnumBase;

@Getter
@Schema(type = "string", example = "FF", description = "Code used to denote the type of a reference")
@AllArgsConstructor
public enum ReferenceTypeCode implements EnumBase {
  FF("Freight Forwarder’s Reference"),
  SI("Shipper’s Reference"),
  PO("Purchase Order Reference"),
  CR("Customer’s Reference"),
  AAO("Consignee’s Reference"),
  ECR("Empty container release reference"),
  CSI("Customer shipment ID"),
  BPR("Booking party reference number"),
  BID("Booking Request ID"),
  EQ("Equipment Reference"),
  RUC("Registro Único del Contribuyente"),
  DUE("Declaração Única de Exportação"),
  CER("Canadian Export Reporting System"),
  AES("Automated Export System");

  private final String valueDescription;
}
