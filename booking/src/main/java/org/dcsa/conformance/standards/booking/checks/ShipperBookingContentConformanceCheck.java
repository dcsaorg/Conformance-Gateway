package org.dcsa.conformance.standards.booking.checks;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingRole;

import java.util.*;
import java.util.stream.Stream;

public class ShipperBookingContentConformanceCheck extends PayloadContentConformanceCheck {

  private static final String IS_NOR_FIELD = "isNonOperatingReefer";
  private static final String ACTIVE_REEFER_SETTINGS_FIELD = "activeReeferSettings";

  public ShipperBookingContentConformanceCheck(
    UUID matchedExchangeUuid
  ) {
    super(
      "Validate the shipper request",
      BookingRole::isShipper,
      matchedExchangeUuid,
      HttpMessageType.REQUEST
    );
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return Stream.of(
      createSubCheck("Reefer Container checks", this::reeferChecks)
    );
  }

  private boolean isReeferContainerSizeTypeCode(String isoEquipmentCode) {
    // DT-437
    var codeChar = isoEquipmentCode.charAt(2);
    return codeChar == 'R' || codeChar == 'H';
  }

  protected Set<String> reeferChecks(JsonNode payload) {
    var requestedEquipments = payload.path("requestedEquipments");
    var issues = new LinkedHashSet<String>();
    int index = -1;
    for (var requestedEquipment : requestedEquipments) {
      var code = requestedEquipment.path("ISOEquipmentCode").asText(null);
      index++;
      var prefix = "requestedEquipments[" + index + "].";
      if (code == null || code.length() < 4) {
        // Schema validation should catch this case.
        continue;
      }
      if (isReeferContainerSizeTypeCode(code)) {
        var node = fieldRequired(
          requestedEquipment,
          IS_NOR_FIELD,
          issues,
          prefix,
          "the ISOEquipmentCode implies that the container is a reefer container"
        );
        if (node != null && node.isBoolean()) {
          var isNOR = node.asBoolean();
          if (isNOR) {
            fieldRequired(
              requestedEquipment,
              ACTIVE_REEFER_SETTINGS_FIELD,
              issues,
              prefix,
              "the ISOEquipmentCode implies that the container is a reefer container" +
                " and isNonOperatingReefer was false"
            );
          } else {
            fieldOmitted(
              requestedEquipment,
              ACTIVE_REEFER_SETTINGS_FIELD,
              issues,
              prefix,
              "the isNonOperatingReefer was true"
            );
          }
        }
      } else {
        fieldOmitted(
          requestedEquipment,
          IS_NOR_FIELD,
          issues,
          prefix,
          "the ISOEquipmentCode implies that the container is not a reefer container"
        );
        fieldOmitted(
          requestedEquipment,
          ACTIVE_REEFER_SETTINGS_FIELD,
          issues,
          prefix,
          "the ISOEquipmentCode implies that the container is not a reefer container"
        );
      }
    }
    return issues;
  }


  private void fieldOmitted(JsonNode object, String attributeName, Set<String> issues, String prefix, String reason) {
    if (object.has(attributeName)) {
      issues.add("The field '%s%s' must be omitted because %s".formatted(prefix, attributeName, reason));
    }
  }

  private JsonNode fieldRequired(JsonNode object, String attributeName, Set<String> issues, String prefix, String reason) {
    var field = object.path(attributeName);
    if (isNonEmptyNode(field)) {
      return field;
    }
    issues.add("The field '%s%s' must be present and non-empty because %s".formatted(prefix, attributeName, reason));
    return null;
  }
}
