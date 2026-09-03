package org.dcsa.conformance.standards.ebl.checks;

import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.TD_APPROVED;
import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.TD_DRAFT;
import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.TD_ISSUED;
import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.TD_PENDING_SURRENDER_FOR_AMENDMENT;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentMatchedValidation;
import org.dcsa.conformance.core.check.JsonRebasableContentCheck;
import org.dcsa.conformance.standards.ebl.party.AmendedTransportDocumentStatus;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

public record TransportDocumentStatusScenario(
    String useCase,
    Set<TransportDocumentStatus> transportDocumentStatuses,
    AmendedTransportDocumentStatus amendedTransportDocumentStatus,
    boolean validateAmendedTransportDocumentStatus) {

  private static final String TRANSPORT_DOCUMENT_STATUS = "transportDocumentStatus";
  private static final String AMENDED_TRANSPORT_DOCUMENT_STATUS = "amendedTransportDocumentStatus";
  private static final Set<TransportDocumentStatus> DIRECT_AMENDMENT_PRIMARY_STATUSES =
      Set.of(TD_DRAFT, TD_ISSUED, TD_PENDING_SURRENDER_FOR_AMENDMENT);

  public TransportDocumentStatusScenario {
    transportDocumentStatuses = Set.copyOf(transportDocumentStatuses);
    if (transportDocumentStatuses.isEmpty()) {
      throw new IllegalArgumentException("At least one transport document status is required");
    }
  }

  public static TransportDocumentStatusScenario uc6() {
    return primaryStatusOnly("UC6", TD_DRAFT);
  }

  public static TransportDocumentStatusScenario uc7() {
    return primaryStatusOnly("UC7", TD_APPROVED);
  }

  public static TransportDocumentStatusScenario uc8() {
    return primaryStatusOnly("UC8", TD_ISSUED);
  }

  public static TransportDocumentStatusScenario uc17() {
    return new TransportDocumentStatusScenario(
        "UC17",
        DIRECT_AMENDMENT_PRIMARY_STATUSES,
        AmendedTransportDocumentStatus.AMENDMENT_RECEIVED,
        true);
  }

  public static TransportDocumentStatusScenario uc18() {
    return new TransportDocumentStatusScenario(
        "UC18",
        DIRECT_AMENDMENT_PRIMARY_STATUSES,
        AmendedTransportDocumentStatus.AMENDMENT_CANCELLED,
        true);
  }

  public static TransportDocumentStatusScenario uc19(boolean confirm) {
    return new TransportDocumentStatusScenario(
        "UC19",
        DIRECT_AMENDMENT_PRIMARY_STATUSES,
        confirm
            ? AmendedTransportDocumentStatus.AMENDMENT_CONFIRMED
            : AmendedTransportDocumentStatus.AMENDMENT_DECLINED,
        true);
  }

  public static TransportDocumentStatusScenario primaryStatusOnly(
      String useCase, TransportDocumentStatus transportDocumentStatus) {
    return new TransportDocumentStatusScenario(
        useCase, Set.of(transportDocumentStatus), null, true);
  }

  public static TransportDocumentStatusScenario primaryStatusesOnly(
      Set<TransportDocumentStatus> transportDocumentStatuses) {
    return new TransportDocumentStatusScenario(
        null, transportDocumentStatuses, null, false);
  }

  public List<JsonRebasableContentCheck> checks(boolean notification) {
    var checks = new java.util.ArrayList<JsonRebasableContentCheck>();
    Set<String> primaryStatuses =
        transportDocumentStatuses.stream()
            .map(notification
                ? TransportDocumentStatus::notificationWireName
                : TransportDocumentStatus::wireName)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    checks.add(
        JsonAttribute.customValidator(
            primaryStatusDescription(primaryStatuses),
            (JsonContentMatchedValidation)
                JsonAttribute.mustBeOneOf(
                        com.fasterxml.jackson.core.JsonPointer.compile(
                            "/" + TRANSPORT_DOCUMENT_STATUS),
                        primaryStatuses)
                    ::validate));
    if (validateAmendedTransportDocumentStatus) {
      checks.add(
          amendedTransportDocumentStatus == null
              ? JsonAttribute.customValidator(
                  "After %s, the '%s' must be absent."
                      .formatted(useCase, AMENDED_TRANSPORT_DOCUMENT_STATUS),
                  (JsonContentMatchedValidation)
                      JsonAttribute.mustBeAbsent(
                              com.fasterxml.jackson.core.JsonPointer.compile(
                                  "/" + AMENDED_TRANSPORT_DOCUMENT_STATUS))
                          ::validate)
              : JsonAttribute.customValidator(
                  amendedStatusDescription(),
                  JsonAttribute.path(
                      AMENDED_TRANSPORT_DOCUMENT_STATUS,
                      JsonAttribute.matchedMustEqual(
                          amendedTransportDocumentStatus::name))));
    }
    return List.copyOf(checks);
  }

  private String primaryStatusDescription(Set<String> statuses) {
    String expected = statuses.stream().map("'%s'"::formatted).collect(Collectors.joining(" or "));
    return useCase == null
        ? "The '%s' must equal %s.".formatted(TRANSPORT_DOCUMENT_STATUS, expected)
        : "After %s, the '%s' must equal %s."
            .formatted(useCase, TRANSPORT_DOCUMENT_STATUS, expected);
  }

  private String amendedStatusDescription() {
    return "After %s, the '%s' must equal '%s'."
        .formatted(
            useCase, AMENDED_TRANSPORT_DOCUMENT_STATUS, amendedTransportDocumentStatus.name());
  }
}


