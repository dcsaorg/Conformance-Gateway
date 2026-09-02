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
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;
import org.dcsa.conformance.standardscommons.party.EblDynamicScenarioParameters;

public class CarrierTdNotificationPayloadRequestConformanceCheck
    extends PayloadContentConformanceCheck {

  private static final String DATA_PATH = "/data";
  private static final String TRANSPORT_DOCUMENT_PATH = "/data/transportDocument";
  private static final String AMENDED_TRANSPORT_DOCUMENT_PATH = "/data/amendedTransportDocument";

  private static final String ROOT_LABEL = "";
  private static final String TRANSPORT_DOCUMENT_LABEL = "[Transport Document]";
  private static final String AMENDED_TRANSPORT_DOCUMENT_LABEL = "[Amended Transport Document]";

  private final TransportDocumentStatusScenario statusScenario;
  private final Boolean tdrIsKnown;
  private final Supplier<EblDynamicScenarioParameters> dspSupplier;

  public CarrierTdNotificationPayloadRequestConformanceCheck(
      UUID matchedExchangeUuid,
      List<TransportDocumentStatus> transportDocumentStatus,
      Boolean tdrIsKnown,
      Supplier<EblDynamicScenarioParameters> dspSupplier) {
    this(
        matchedExchangeUuid,
        TransportDocumentStatusScenario.primaryStatusesOnly(
            new java.util.LinkedHashSet<>(transportDocumentStatus)),
        tdrIsKnown,
        dspSupplier);
  }

  public CarrierTdNotificationPayloadRequestConformanceCheck(
      UUID matchedExchangeUuid,
      TransportDocumentStatusScenario statusScenario,
      Boolean tdrIsKnown,
      Supplier<EblDynamicScenarioParameters> dspSupplier) {

    super(EblRole::isCarrier, matchedExchangeUuid, HttpMessageType.REQUEST);
    this.statusScenario = statusScenario;
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
                      new ArrayList<>(EblChecks.getTdNotificationChecks(statusScenario));
                  getTdrCheck().ifPresent(checks::add);
                  return checks;
                }),
            buildChecks(
                TRANSPORT_DOCUMENT_LABEL,
                TRANSPORT_DOCUMENT_PATH,
                () -> {
                  List<JsonContentCheck> checks = new ArrayList<>();
                  getTdrCheck().ifPresent(checks::add);
                  checks.addAll(
                      EblChecks.getTdPayloadChecks(
                          List.copyOf(statusScenario.transportDocumentStatuses()),
                          dspSupplier,
                          EblChecks.TdPayloadContext.TRANSPORT_DOCUMENT_NOTIFICATION));
                  return checks;
                }),
            buildChecks(
                AMENDED_TRANSPORT_DOCUMENT_LABEL,
                AMENDED_TRANSPORT_DOCUMENT_PATH,
                () -> {
                  List<JsonContentCheck> checks = new ArrayList<>();
                  getTdrCheck().ifPresent(checks::add);
                  checks.addAll(
                      EblChecks.getTdPayloadChecks(
                          List.copyOf(statusScenario.transportDocumentStatuses()),
                          dspSupplier,
                          EblChecks.TdPayloadContext.AMENDED_TRANSPORT_DOCUMENT_NOTIFICATION));
                  return checks;
                }))
        .flatMap(Function.identity());
  }

  private Optional<JsonContentCheck> getTdrCheck() {
    if (Boolean.TRUE.equals(tdrIsKnown)) {
      return Optional.of(EblChecks.tdrInNotificationMustMatchDSP(dspSupplier));
    }
    return Optional.empty();
  }
}
