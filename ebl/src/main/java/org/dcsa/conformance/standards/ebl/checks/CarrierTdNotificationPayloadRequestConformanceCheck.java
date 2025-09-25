package org.dcsa.conformance.standards.ebl.checks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

public class CarrierTdNotificationPayloadRequestConformanceCheck
    extends PayloadContentConformanceCheck {

  private static final String DATA_PATH = "/data";
  private static final String TRANSPORT_DOCUMENT_PATH = "/data/transportDocument";

  private static final String ROOT_LABEL = "";
  private static final String TRANSPORT_DOCUMENT_LABEL = "[Transport Document]";

  private final TransportDocumentStatus transportDocumentStatus;
  private final Boolean tdrIsKnown;
  private final Supplier<DynamicScenarioParameters> dspSupplier;

  public CarrierTdNotificationPayloadRequestConformanceCheck(
      UUID matchedExchangeUuid,
      TransportDocumentStatus transportDocumentStatus,
      Boolean tdrIsKnown,
      Supplier<DynamicScenarioParameters> dspSupplier) {

    super(EblRole::isCarrier, matchedExchangeUuid, HttpMessageType.REQUEST);
    this.transportDocumentStatus = transportDocumentStatus;
    this.tdrIsKnown = Boolean.TRUE.equals(tdrIsKnown);
    this.dspSupplier = dspSupplier;
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return Stream.of(
            buildChecks(
                ROOT_LABEL,
                DATA_PATH,
                () -> {
                  List<JsonContentCheck> checks =
                      new ArrayList<>(EblChecks.getTdNotificationChecks(transportDocumentStatus));
                  getTdrCheck().ifPresent(checks::add);
                  return checks;
                }),
            buildChecks(
                TRANSPORT_DOCUMENT_LABEL,
                TRANSPORT_DOCUMENT_PATH,
                () -> EblChecks.getTdPayloadChecks(transportDocumentStatus, dspSupplier)))
        .flatMap(Function.identity());
  }

  private Optional<JsonContentCheck> getTdrCheck() {
    if (Boolean.TRUE.equals(tdrIsKnown)) {
      return Optional.of(EblChecks.tdrInNotificationMustMatchDSP(dspSupplier));
    }
    return Optional.empty();
  }
}
