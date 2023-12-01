package org.dcsa.conformance.standards.ebl.party;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public enum ShippingInstructionsStatus {
  SI_ANY(null),
  SI_START(null),
  SI_RECEIVED("RECEIVED"),
  SI_PENDING_UPDATE("PENDING UDPATE"),
  SI_UPDATE_RECEIVED("UPDATE RECEIVED"),
  SI_CANCELLED("CANCELLED"),
  SI_DECLINED("DECLINED"),
  SI_COMPLETED("COMPLETED"),
  ;

  private final String wireName;

  private static final Map<String, ShippingInstructionsStatus> WIRENAME2STATUS = Arrays.stream(values())
    .filter(ShippingInstructionsStatus::hasWireName)
    .collect(Collectors.toMap(ShippingInstructionsStatus::wireName, Function.identity()));

  public String wireName() {
    if (!hasWireName()) {
      throw new IllegalArgumentException("State " + this.name() + " does not have a name visible in any transmission");
    }
    return wireName;
  }

  private boolean hasWireName() {
    return wireName != null;
  }

  public static ShippingInstructionsStatus fromWireName(String wireName) {
    var v = WIRENAME2STATUS.get(wireName);
    if (v == null) {
      throw new IllegalArgumentException("Unknown SI status or the status does not have a wireName");
    }
    return v;
  }
}
