package org.dcsa.conformance.standards.ebl.checks;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.party.CarrierScenarioParameters;
import org.dcsa.conformance.standards.ebl.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;

public class CarrierSiNotificationPayloadRequestConformanceCheck
    extends PayloadContentConformanceCheck {

  private static final String DATA_PATH = "/data";
  private static final String SHIPPING_INSTRUCTIONS_PATH = "/data/shippingInstructions";
  private static final String UPDATED_SHIPPING_INSTRUCTIONS_PATH =
      "/data/updatedShippingInstructions";

  private static final String NOTIFICATION_LABEL = "[Notification] ";
  private static final String SHIPPING_INSTRUCTIONS_LABEL = "[Shipping Instructions] ";
  private static final String UPDATED_SHIPPING_INSTRUCTIONS_LABEL =
      "[Updated Shipping Instructions] ";

  private final String standardsVersion;
  private final ShippingInstructionsStatus shippingInstructionsStatus;
  private final ShippingInstructionsStatus updatedShippingInstructionsStatus;
  private final Supplier<CarrierScenarioParameters> cspSupplier;
  private final Supplier<DynamicScenarioParameters> dspSupplier;
  private final List<JsonContentCheck> extraChecks;

  public CarrierSiNotificationPayloadRequestConformanceCheck(
      UUID matchedExchangeUuid,
      String standardsVersion,
      ShippingInstructionsStatus shippingInstructionsStatus,
      ShippingInstructionsStatus updatedShippingInstructionsStatus,
      Supplier<CarrierScenarioParameters> cspSupplier,
      Supplier<DynamicScenarioParameters> dspSupplier,
      JsonContentCheck... extraChecks) {
    super(EblRole::isCarrier, matchedExchangeUuid, HttpMessageType.REQUEST);
    this.standardsVersion = standardsVersion;
    this.shippingInstructionsStatus = shippingInstructionsStatus;
    this.updatedShippingInstructionsStatus = updatedShippingInstructionsStatus;
    this.cspSupplier = cspSupplier;
    this.dspSupplier = dspSupplier;
    this.extraChecks = List.of(extraChecks);
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return Stream.of(
            buildChecks(
                NOTIFICATION_LABEL,
                DATA_PATH,
                () ->
                    EBLChecks.getSiPayloadSimpleChecks(
                        shippingInstructionsStatus,
                        updatedShippingInstructionsStatus,
                        extraChecks)),
            buildChecks(
                SHIPPING_INSTRUCTIONS_LABEL,
                SHIPPING_INSTRUCTIONS_PATH,
                () ->
                    EBLChecks.getSiPayloadFullChecks(
                        standardsVersion,
                        shippingInstructionsStatus,
                        updatedShippingInstructionsStatus,
                        cspSupplier,
                        dspSupplier)),
            buildChecksWithCondition(
                UPDATED_SHIPPING_INSTRUCTIONS_LABEL,
                UPDATED_SHIPPING_INSTRUCTIONS_PATH,
                updatedShippingInstructionsStatus != null,
                () ->
                    EBLChecks.getSiPayloadFullChecks(
                        standardsVersion,
                        shippingInstructionsStatus,
                        updatedShippingInstructionsStatus,
                        cspSupplier,
                        dspSupplier)))
        .flatMap(Function.identity());
  }

  private Stream<ConformanceCheck> buildChecks(
      String label, String jsonPath, Supplier<List<JsonContentCheck>> checksSupplier) {
    return checksSupplier.get().stream().map(check -> wrapWithSubCheck(label, jsonPath, check));
  }

  private Stream<ConformanceCheck> buildChecksWithCondition(
      String label,
      String jsonPath,
      boolean condition,
      Supplier<List<JsonContentCheck>> checksSupplier) {
    return condition
        ? checksSupplier.get().stream().map(check -> wrapWithSubCheck(label, jsonPath, check))
        : Stream.empty();
  }

  private ConformanceCheck wrapWithSubCheck(String label, String path, JsonContentCheck check) {
    return createSubCheck(label + check.description(), at(path, check::validate));
  }
}
