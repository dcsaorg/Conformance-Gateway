package org.dcsa.conformance.standards.ebl.checks;

import com.fasterxml.jackson.core.JsonPointer;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

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

  private static final JsonPointer SI_REQUEST_INVOICE_PAYABLE_AT_UN_LOCATION_CODE = JsonPointer.compile("/invoicePayableAt/UNLocationCode");
  private static final JsonPointer SI_REQUEST_SEND_TO_PLATFORM = JsonPointer.compile("/sendToPlatform");

  public static JsonContentCheck SIR_REQUIRED_IN_REF_STATUS = JsonAttribute.mustBePresent(SI_REF_SIR_PTR);
  public static JsonContentCheck SIR_REQUIRED_IN_NOTIFICATION = JsonAttribute.mustBePresent(SI_NOTIFICATION_SIR_PTR);
  public static JsonContentCheck TDR_REQUIRED_IN_NOTIFICATION = JsonAttribute.mustBePresent(TD_NOTIFICATION_TDR_PTR);

  public static JsonContentCheck sirInRefStatusMustMatchDSP(Supplier<DynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.mustEqual(SI_REF_SIR_PTR, () -> dspSupplier.get().shippingInstructionsReference());
  }

  public static JsonContentCheck sirInNotificationMustMatchDSP(Supplier<DynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.mustEqual(SI_NOTIFICATION_SIR_PTR, () -> dspSupplier.get().shippingInstructionsReference());
  }

  public static JsonContentCheck tdrInNotificationMustMatchDSP(Supplier<DynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.mustEqual(TD_NOTIFICATION_TDR_PTR, () -> dspSupplier.get().transportDocumentReference());
  }


  public static ActionCheck siRequestContentChecks(UUID matched) {
    return JsonAttribute.contentChecks(
      EblRole::isShipper,
      matched,
      HttpMessageType.REQUEST,
      JsonAttribute.mustBeDatasetKeyword(
        SI_REQUEST_INVOICE_PAYABLE_AT_UN_LOCATION_CODE,
        EblDatasets.UN_LOCODE_DATASET
      ),
      JsonAttribute.mustBeDatasetKeyword(
        SI_REQUEST_SEND_TO_PLATFORM,
        EblDatasets.EBL_PLATFORMS_DATASET
      )
    );
  }

  public static Stream<ActionCheck> siRefStatusContentChecks(UUID matched, ShippingInstructionsStatus shippingInstructionsStatus, JsonContentCheck ... extraChecks) {
    return siRefStatusContentChecks(matched, shippingInstructionsStatus, null, extraChecks);
  }

  public static Stream<ActionCheck> siRefStatusContentChecks(UUID matched, ShippingInstructionsStatus shippingInstructionsStatus, ShippingInstructionsStatus updatedShippingInstructionsStatus, JsonContentCheck ... extraChecks) {
    var updatedStatusCheck = updatedShippingInstructionsStatus != null
      ? JsonAttribute.mustEqual(
      SI_REF_UPDATED_SI_STATUS_PTR,
      updatedShippingInstructionsStatus.wireName())
      : JsonAttribute.mustBeAbsent(SI_REF_UPDATED_SI_STATUS_PTR);
    var checks = new ArrayList<>(Arrays.asList(extraChecks));
    checks.add(JsonAttribute.mustEqual(
      SI_REF_SI_STATUS_PTR,
      shippingInstructionsStatus.wireName()
    ));
    checks.add(updatedStatusCheck);
    return Stream.of(
      JsonAttribute.contentChecks(
        EblRole::isCarrier,
        matched,
        HttpMessageType.RESPONSE,
        checks
      )
    );
  }

  public static ActionCheck tdRefStatusChecks(UUID matched, Supplier<DynamicScenarioParameters> dspSupplier, TransportDocumentStatus transportDocumentStatus) {
    return JsonAttribute.contentChecks(
      EblRole::isCarrier,
      matched,
      HttpMessageType.RESPONSE,
      JsonAttribute.mustEqual(
        TD_REF_TDR_PTR,
        () -> dspSupplier.get().transportDocumentReference()
      ),
      JsonAttribute.mustEqual(
        TD_REF_TD_STATUS_PTR,
        transportDocumentStatus.wireName()
      )
    );
  }

  public static ActionCheck siNotificationContentChecks(UUID matched, ShippingInstructionsStatus shippingInstructionsStatus, ShippingInstructionsStatus updatedShippingInstructionsStatus, JsonContentCheck ... extraChecks) {
    String titlePrefix = "[Notification]";
    var updatedStatusCheck = updatedShippingInstructionsStatus != null
      ? JsonAttribute.mustEqual(
      SI_NOTIFICATION_UPDATED_SI_STATUS_PTR,
      updatedShippingInstructionsStatus.wireName())
      : JsonAttribute.mustBeAbsent(SI_NOTIFICATION_UPDATED_SI_STATUS_PTR);
    List<JsonContentCheck> jsonContentChecks = new ArrayList<>(Arrays.asList(extraChecks));
    jsonContentChecks.add(JsonAttribute.mustEqual(
      SI_NOTIFICATION_SI_STATUS_PTR,
      shippingInstructionsStatus.wireName()
    ));
    jsonContentChecks.add(updatedStatusCheck);
    return JsonAttribute.contentChecks(
      titlePrefix,
      EblRole::isCarrier,
      matched,
      HttpMessageType.REQUEST,
      jsonContentChecks
    );
  }

  public static ActionCheck tdNotificationContentChecks(UUID matched, TransportDocumentStatus transportDocumentStatus, JsonContentCheck ... extraChecks) {
    String titlePrefix = "[Notification]";
    List<JsonContentCheck> jsonContentChecks = new ArrayList<>(Arrays.asList(extraChecks));
    jsonContentChecks.add(JsonAttribute.mustEqual(
      TD_NOTIFICATION_TD_STATUS_PTR,
      transportDocumentStatus.wireName()
    ));
    return JsonAttribute.contentChecks(
      titlePrefix,
      EblRole::isCarrier,
      matched,
      HttpMessageType.REQUEST,
      jsonContentChecks
    );
  }
}
