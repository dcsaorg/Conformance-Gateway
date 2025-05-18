package org.dcsa.conformance.specifications.standards.ebl.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.ebl.v300.types.UnspecifiedType;

import java.util.List;

@Data
@Schema()
public class Transports {
  private UnspecifiedType plannedArrivalDate;

  private UnspecifiedType plannedDepartureDate;

  private UnspecifiedType preCarriageBy;

  private UnspecifiedType onCarriageBy;

  private PlaceOfReceipt placeOfReceipt;

  private PortOfLoading portOfLoading;

  private PortOfDischarge portOfDischarge;

  private PlaceOfDelivery placeOfDelivery;

  private OnwardInlandRouting onwardInlandRouting;

  private List<VesselVoyage> vesselVoyages;
}
