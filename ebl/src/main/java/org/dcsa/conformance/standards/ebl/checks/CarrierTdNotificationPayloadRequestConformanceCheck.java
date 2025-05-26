package org.dcsa.conformance.standards.ebl.checks;

import java.util.List;
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

  private static final String TRANSPORT_DOCUMENT_LABEL = "[Transport Document] ";

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
                "",
                DATA_PATH,
                () -> EBLChecks.getTdPayloadSimpleChecks(transportDocumentStatus, getTdrCheck())),
            buildChecks(
                TRANSPORT_DOCUMENT_LABEL,
                TRANSPORT_DOCUMENT_PATH,
                () ->
                    EBLChecks.getTdPayloadFullChecks(
                        standardVersion, transportDocumentStatus, cspSupplier, dspSupplier)))
        .flatMap(Function.identity());
  }

  private Stream<ConformanceCheck> buildChecks(
      String label, String jsonPath, Supplier<List<JsonContentCheck>> checksSupplier) {
    return checksSupplier.get().stream().map(check -> wrapWithSubCheck(label, jsonPath, check));
  }

  private ConformanceCheck wrapWithSubCheck(String label, String path, JsonContentCheck check) {
    return createSubCheck(label + check.description(), at(path, check::validate));
  }

  private JsonContentCheck getTdrCheck() {
    return Boolean.TRUE.equals(tdrIsKnown)
        ? EBLChecks.tdrInNotificationMustMatchDSP(dspSupplier)
        : EBLChecks.TDR_REQUIRED_IN_NOTIFICATION;
  }
}
