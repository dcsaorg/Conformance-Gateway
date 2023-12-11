package org.dcsa.conformance.standards.ebl.checks;

import com.fasterxml.jackson.core.JsonPointer;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttributePresenceCheck;
import org.dcsa.conformance.core.check.JsonAttributeCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@UtilityClass
public class EBLChecks {

  private static final JsonPointer SI_REF_SIR_PTR = JsonPointer.compile("/shippingInstructionsReference");
  private static final JsonPointer SI_REF_SI_STATUS_PTR = JsonPointer.compile("/shippingInstructionsStatus");
  private static final JsonPointer SI_REF_UPDATED_SI_STATUS_PTR = JsonPointer.compile("/updatedShippingInstructionsStatus");
  private static final JsonPointer SI_NOTIFICATION_SIR_PTR = JsonPointer.compile("/data/shippingInstructionsReference");
  private static final JsonPointer SI_NOTIFICATION_SI_STATUS_PTR = JsonPointer.compile("/data/shippingInstructionsStatus");
  private static final JsonPointer SI_NOTIFICATION_UPDATED_SI_STATUS_PTR = JsonPointer.compile("/data/updatedShippingInstructionsStatus");

  private static final JsonPointer TD_REF_TDR_PTR = JsonPointer.compile("/transportDocumentReference");
  private static final JsonPointer TD_REF_TD_STATUS_PTR = JsonPointer.compile("/transportDocumentStatus");
  private static final JsonPointer TD_NOTIFICATION_TDR_PTR = JsonPointer.compile("/data/transportDocumentReference");
  private static final JsonPointer TD_NOTIFICATION_TD_STATUS_PTR = JsonPointer.compile("/data/transportDocumentStatus");


  public static Stream<ActionCheck> siRefSIRIsPresent(UUID matched) {
    return Stream.of(
      JsonAttributePresenceCheck.jsonAttributeMustBePresent(
        EblRole::isCarrier,
        matched,
        HttpMessageType.RESPONSE,
        SI_REF_SIR_PTR
      )
    );
  }

  public static Stream<ActionCheck> siRefSIR(UUID matched, String reference) {
    return Stream.of(
      new JsonAttributeCheck(
        EblRole::isCarrier,
        matched,
        HttpMessageType.RESPONSE,
        SI_REF_SIR_PTR,
        // The reference usually comes from the DSP where the value is null until relevant actions
        // have run.  To avoid NPEs, we "forgive" null here
        Objects.requireNonNullElse(reference, "<DSP-MISSING-SHIPPING-INSTRUCTIONS-REFERENCE>")
      )
    );
  }


  public static Stream<ActionCheck> siRefStatusChecks(UUID matched, ShippingInstructionsStatus shippingInstructionsStatus) {
    return siRefStatusChecks(matched, shippingInstructionsStatus, null);
  }

  public static Stream<ActionCheck> siRefStatusChecks(UUID matched, ShippingInstructionsStatus shippingInstructionsStatus, ShippingInstructionsStatus updatedShippingInstructionsStatus) {
    var updatedStatusCheck = updatedShippingInstructionsStatus != null
      ? new JsonAttributeCheck(
      EblRole::isCarrier,
      matched,
      HttpMessageType.RESPONSE,
      SI_REF_UPDATED_SI_STATUS_PTR,
      updatedShippingInstructionsStatus.wireName())
      : JsonAttributePresenceCheck.jsonAttributeMustBeAbsent(
      EblRole::isCarrier,
      matched,
      HttpMessageType.RESPONSE,
      SI_REF_UPDATED_SI_STATUS_PTR);
    return Stream.of(
      new JsonAttributeCheck(
        EblRole::isCarrier,
        matched,
        HttpMessageType.RESPONSE,
        SI_REF_SI_STATUS_PTR,
        shippingInstructionsStatus.wireName()
      ),
      updatedStatusCheck
    );
  }

  public static Stream<ActionCheck> siNotificationStatusChecks(UUID matched, ShippingInstructionsStatus shippingInstructionsStatus) {
    return siNotificationStatusChecks(matched, shippingInstructionsStatus, null);
  }

  public static Stream<ActionCheck> siNotificationStatusChecks(UUID matched, ShippingInstructionsStatus shippingInstructionsStatus, ShippingInstructionsStatus updatedShippingInstructionsStatus) {
    String titlePrefix = "[Notification]";
    var updatedStatusCheck = updatedShippingInstructionsStatus != null
      ? new JsonAttributeCheck(
      titlePrefix,
      EblRole::isCarrier,
      matched,
      HttpMessageType.REQUEST,
      SI_NOTIFICATION_UPDATED_SI_STATUS_PTR,
      updatedShippingInstructionsStatus.wireName())
      : JsonAttributePresenceCheck.jsonAttributeMustBeAbsent(
      titlePrefix,
      EblRole::isCarrier,
      matched,
      HttpMessageType.REQUEST,
      SI_NOTIFICATION_UPDATED_SI_STATUS_PTR);
    return Stream.of(
      new JsonAttributeCheck(
        titlePrefix,
        EblRole::isCarrier,
        matched,
        HttpMessageType.REQUEST,
        SI_NOTIFICATION_SI_STATUS_PTR,
        shippingInstructionsStatus.wireName()
      ),
      updatedStatusCheck
    );
  }

  public static Stream<ActionCheck> siNotificationSIRIsPresent(UUID matched) {
    String titlePrefix = "[Notification]";
    return Stream.of(
      JsonAttributePresenceCheck.jsonAttributeMustBePresent(
        titlePrefix,
        EblRole::isCarrier,
        matched,
        HttpMessageType.REQUEST,
        SI_NOTIFICATION_SIR_PTR
      )
    );
  }

  public static Stream<ActionCheck> siNotificationSIR(UUID matched, String reference) {
    String titlePrefix = "[Notification]";
    return Stream.of(
      new JsonAttributeCheck(
        titlePrefix,
        EblRole::isCarrier,
        matched,
        HttpMessageType.REQUEST,
        SI_NOTIFICATION_SIR_PTR,
        // The reference usually comes from the DSP where the value is null until relevant actions
        // have run.  To avoid NPEs, we "forgive" null here
        Objects.requireNonNullElse(reference, "<DSP-MISSING-SHIPPING-INSTRUCTIONS-REFERENCE>")
      )
    );
  }


  public static Stream<ActionCheck> tdRefStatusChecks(UUID matched, TransportDocumentStatus transportDocumentStatus) {
    return Stream.of(
      new JsonAttributeCheck(
        EblRole::isCarrier,
        matched,
        HttpMessageType.RESPONSE,
        TD_REF_TD_STATUS_PTR,
        transportDocumentStatus.wireName()
      )
    );
  }

  public static Stream<ActionCheck> tdNotificationTDRIsPresent(UUID matched) {
    String titlePrefix = "[Notification]";
    return Stream.of(
      JsonAttributePresenceCheck.jsonAttributeMustBePresent(
        titlePrefix,
        EblRole::isCarrier,
        matched,
        HttpMessageType.REQUEST,
        TD_NOTIFICATION_TDR_PTR
      )
    );
  }


  public static Stream<ActionCheck> tdRefTDR(UUID matched, String reference) {
    return Stream.of(
      new JsonAttributeCheck(
        EblRole::isCarrier,
        matched,
        HttpMessageType.RESPONSE,
        TD_REF_TDR_PTR,
        // The reference usually comes from the DSP where the value is null until relevant actions
        // have run.  To avoid NPEs, we "forgive" null here
        Objects.requireNonNullElse(reference, "<DSP-MISSING-TRANSPORT-DOCUMENT-REFERENCE>")
      )
    );
  }

  public static Stream<ActionCheck> tdNotificationTDR(UUID matched, String reference) {
    String titlePrefix = "[Notification]";
    return Stream.of(
      new JsonAttributeCheck(
        titlePrefix,
        EblRole::isCarrier,
        matched,
        HttpMessageType.REQUEST,
        TD_NOTIFICATION_TDR_PTR,
        // The reference usually comes from the DSP where the value is null until relevant actions
        // have run.  To avoid NPEs, we "forgive" null here
        Objects.requireNonNullElse(reference, "<DSP-MISSING-TRANSPORT-DOCUMENT-REFERENCE>")
      )
    );
  }

  public static Stream<ActionCheck> tdNotificationStatusChecks(UUID matched, TransportDocumentStatus transportDocumentStatus) {
    String titlePrefix = "[Notification]";
    return Stream.of(
      new JsonAttributeCheck(
        titlePrefix,
        EblRole::isCarrier,
        matched,
        HttpMessageType.REQUEST,
        TD_NOTIFICATION_TD_STATUS_PTR,
        transportDocumentStatus.wireName()
      )
    );
  }
}
