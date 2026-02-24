package org.dcsa.conformance.standards.ebl.party;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TransportDocumentStatus {
  TD_ANY(null, null),
  TD_START(null, null),
  TD_DRAFT("DRAFT", "DRAFT"),
  TD_APPROVED("APPROVED", "APPROVED"),
  TD_ISSUED("ISSUED", "ISSUED"),
  TD_PENDING_SURRENDER_FOR_AMENDMENT(
      "PENDING_SURRENDER_FOR_AMENDMENT", "PENDING_SURRENDER_FOR_AMENDMENT"),
  TD_SURRENDERED_FOR_AMENDMENT("SURRENDERED_FOR_AMENDMENT", "SURRENDER_FOR_AMENDMENT"),
  TD_PENDING_SURRENDER_FOR_DELIVERY(
      "PENDING_SURRENDER_FOR_DELIVERY", "PENDING_SURRENDER_FOR_DELIVERY"),
  TD_SURRENDERED_FOR_DELIVERY("SURRENDERED_FOR_DELIVERY", "SURRENDER_FOR_DELIVERY"),
  TD_VOIDED("VOIDED", "VOID"),
  ;

  private final String wireName;
  // Due to a mistake in the API schema, the notification wire names are different for some statuses
  private final String notificationWireName;

  private static final Map<String, TransportDocumentStatus> WIRENAME2STATUS = Arrays.stream(values())
    .filter(TransportDocumentStatus::hasWireName)
    .collect(Collectors.toMap(TransportDocumentStatus::wireName, Function.identity()));

  public String wireName() {
    if (!hasWireName()) {
      throw new IllegalArgumentException("State does not have a name visible in any transmission");
    }
    return wireName;
  }

  /**
   * Returns the wire name used in notification payloads. Due to a mistake in the API schema, the
   * notification wire names are different for some statuses: SURRENDERED_FOR_AMENDMENT ->
   * SURRENDER_FOR_AMENDMENT SURRENDERED_FOR_DELIVERY -> SURRENDER_FOR_DELIVERY VOIDED -> VOID
   */
  public String notificationWireName() {
    if (!hasWireName()) {
      throw new IllegalArgumentException("State does not have a name visible in any transmission");
    }
    return notificationWireName;
  }

  public boolean hasWireName() {
    return wireName != null;
  }

  public static TransportDocumentStatus fromWireName(String wireName) {
    var v = WIRENAME2STATUS.get(wireName);
    if (v == null) {
      throw new IllegalArgumentException("Unknown TD status or the status does not have a wireName");
    }
    return v;
  }
}
