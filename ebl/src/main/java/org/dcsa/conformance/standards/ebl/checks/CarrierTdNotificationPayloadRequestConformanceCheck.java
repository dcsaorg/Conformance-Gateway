package org.dcsa.conformance.standards.ebl.checks;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.party.CarrierScenarioParameters;
import org.dcsa.conformance.standards.ebl.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

public class CarrierTdNotificationPayloadRequestConformanceCheck
    extends PayloadContentConformanceCheck {

  private static final String DATA_PATH = "/data";
  private static final String TRANSPORT_DOCUMENT_PATH = "/data/transportDocument";

  private static final String ROOT_LABEL = "";
  private static final String TRANSPORT_DOCUMENT_LABEL = "[Transport Document]";

  private final String standardVersion;
  private final TransportDocumentStatus transportDocumentStatus;
  private final Boolean tdrIsKnown;
  private final Supplier<CarrierScenarioParameters> cspSupplier;
  private final Supplier<DynamicScenarioParameters> dspSupplier;

  public CarrierTdNotificationPayloadRequestConformanceCheck(
      UUID matchedExchangeUuid,
      String standardVersion,
      TransportDocumentStatus transportDocumentStatus,
      Boolean tdrIsKnown,
      Supplier<CarrierScenarioParameters> cspSupplier,
      Supplier<DynamicScenarioParameters> dspSupplier) {

    super(EblRole::isCarrier, matchedExchangeUuid, HttpMessageType.REQUEST);
    this.standardVersion = standardVersion;
    this.transportDocumentStatus = transportDocumentStatus;
    this.tdrIsKnown = Boolean.TRUE.equals(tdrIsKnown);
    this.cspSupplier = cspSupplier;
    this.dspSupplier = dspSupplier;
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return Stream.of(
            buildChecks(
                ROOT_LABEL,
                DATA_PATH,
                () -> EBLChecks.getTdNotificationChecks(transportDocumentStatus, getTdrCheck())),
            buildChecks(
                TRANSPORT_DOCUMENT_LABEL,
                TRANSPORT_DOCUMENT_PATH,
                () ->
                    EBLChecks.getTdNotificationPayloadChecks(
                        standardVersion, transportDocumentStatus, cspSupplier, dspSupplier)))
        .flatMap(Function.identity());
  }

  private JsonContentCheck getTdrCheck() {
    return Boolean.TRUE.equals(tdrIsKnown)
        ? EBLChecks.tdrInNotificationMustMatchDSP(dspSupplier)
        : EBLChecks.TDR_REQUIRED_IN_NOTIFICATION;
  }
}
