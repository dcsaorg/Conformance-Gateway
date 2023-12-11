package org.dcsa.conformance.standards.booking.party;

import lombok.Getter;

@Getter
public enum BookingVariant {
    REGULAR("Regular"),
    REEFER("Reefer"),
    DG("Dangerous Goods");

    private final String value;

    BookingVariant(String value) {
      this.value = value;
    }

}
