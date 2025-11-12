package org.dcsa.conformance.standards.vgm.checks;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum VgmQueryParameters {
  CBR("carrierBookingReference"),
  ER("equipmentReference"),
  TDR("transportDocumentReference"),
  DDT_MIN("declarationDateTimeMin"),
  DDT_MAX("declarationDateTimeMax"),
  LIMIT("limit"),
  CURSOR("cursor");

  private final String parameterName;

  VgmQueryParameters(String parameterName) {
    this.parameterName = parameterName;
  }

  public static VgmQueryParameters fromParameterName(String parameterName) {
    return Arrays.stream(values())
        .filter(param -> param.parameterName.equals(parameterName))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "No VgmQueryParameters with parameterName: " + parameterName));
  }
}
