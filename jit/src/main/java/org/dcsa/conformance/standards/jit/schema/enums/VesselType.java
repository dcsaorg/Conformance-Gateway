package org.dcsa.conformance.standards.jit.schema.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VesselType {
  GCGO("General cargo"),
  CONT("Container"),
  RORO("RoRo"),
  CARC("Car carrier"),
  PASS("Passenger"),
  FERY("Ferry"),
  BULK("Bulk"),
  TANK("Tanker"),
  LGTK("Liquefied gas tanker"),
  ASSI("Assistance"),
  PILO("Pilot boat");

  private final String fullName;
}
