package org.dcsa.conformance.standards.tnt.v300.party;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum TntQueryParameters {
    CBR("carrierBookingReference"),
    TDR("transportDocumentReference"),
    ER("equipmentReference"),
    ET("eventTypes"),
    E_UDT_MIN("eventUpdatedDateTimeMin"),
    E_UDT_MAX("eventUpdatedDateTimeMax"),
    LIMIT("limit"),
    CURSOR("cursor");

    private final String parameterName;

    TntQueryParameters(String parameterName) {
        this.parameterName = parameterName;
    }

    public static TntQueryParameters fromParameterName(String parameterName) {
        return Arrays.stream(values())
                .filter(param -> param.parameterName.equals(parameterName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No TntQueryParameters with parameterName: " + parameterName));
    }
}
