package org.dcsa.conformance.standards.ebl.party;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TransportDocumentStatus {
  TD_ANY(null),
  TD_START(null),
  TD_DRAFT("DRAFT"),
  TD_APPROVED("APPROVED"),
  TD_ISSUED("ISSUED"),
  TD_PENDING_SURRENDER_FOR_AMENDMENT("PENDING_SURRENDER_FOR_AMENDMENT"),
  TD_SURRENDERED_FOR_AMENDMENT("SURRENDERED_FOR_AMENDMENT"),
  TD_PENDING_SURRENDER_FOR_DELIVERY("PENDING_SURRENDER_FOR_DELIVERY"),
  TD_SURRENDERED_FOR_DELIVERY("SURRENDERED_FOR_DELIVERY"),
  TD_VOIDED("VOIDED"),
  ;

  private final String wireName;

  private static final Map<String, TransportDocumentStatus> WIRENAME2STATUS = Arrays.stream(values())
    .filter(TransportDocumentStatus::hasWireName)
    .collect(Collectors.toMap(TransportDocumentStatus::wireName, Function.identity()));

  public String wireName() {
    if (!hasWireName()) {
      throw new IllegalArgumentException("State does not have a name visible in any transmission");
    }
    return wireName;
  }

  private boolean hasWireName() {
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
