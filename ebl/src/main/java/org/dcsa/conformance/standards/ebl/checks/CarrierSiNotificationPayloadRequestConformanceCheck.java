package org.dcsa.conformance.standards.ebl.checks;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;
import org.dcsa.conformance.standardscommons.party.EblDynamicScenarioParameters;

public class CarrierSiNotificationPayloadRequestConformanceCheck
    extends PayloadContentConformanceCheck {

  private static final String DATA_PATH = "/data";
  private static final String SHIPPING_INSTRUCTIONS_PATH = "/data/shippingInstructions";
  private static final String UPDATED_SHIPPING_INSTRUCTIONS_PATH =
      "/data/updatedShippingInstructions";

  private static final String ROOT_LABEL = "";
  private static final String SHIPPING_INSTRUCTIONS_LABEL = "[Shipping Instructions] ";
  private static final String UPDATED_SHIPPING_INSTRUCTIONS_LABEL =
      "[Updated Shipping Instructions] ";

  private final ShippingInstructionsStatus shippingInstructionsStatus;
  private final ShippingInstructionsStatus updatedShippingInstructionsStatus;
  private final Supplier<EblDynamicScenarioParameters> dspSupplier;
  private final List<JsonContentCheck> extraChecks;

  public CarrierSiNotificationPayloadRequestConformanceCheck(
      UUID matchedExchangeUuid,
      ShippingInstructionsStatus shippingInstructionsStatus,
      ShippingInstructionsStatus updatedShippingInstructionsStatus,
      Supplier<EblDynamicScenarioParameters> dspSupplier,
      JsonContentCheck... extraChecks) {
    super(EblRole::isCarrier, matchedExchangeUuid, HttpMessageType.REQUEST);
    this.shippingInstructionsStatus = shippingInstructionsStatus;
    this.updatedShippingInstructionsStatus = updatedShippingInstructionsStatus;
    this.dspSupplier = dspSupplier;
    this.extraChecks = List.of(extraChecks);
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    return Stream.of(
            buildChecks(
                ROOT_LABEL,
                DATA_PATH,
                () ->
                    EblChecks.getSiNotificationChecks(
                        shippingInstructionsStatus,
                        updatedShippingInstructionsStatus,
                        extraChecks)),
            buildChecks(
                SHIPPING_INSTRUCTIONS_LABEL,
                SHIPPING_INSTRUCTIONS_PATH,
                () ->
                    EblChecks.getSiPayloadChecks(
                        shippingInstructionsStatus,
                        updatedShippingInstructionsStatus,
                        dspSupplier)),
            buildChecksWithCondition(
                UPDATED_SHIPPING_INSTRUCTIONS_LABEL,
                UPDATED_SHIPPING_INSTRUCTIONS_PATH,
                updatedShippingInstructionsStatus != null,
                () ->
                    EblChecks.getSiPayloadChecks(
                        shippingInstructionsStatus,
                        updatedShippingInstructionsStatus,
                        dspSupplier)))
        .flatMap(Function.identity());
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
}
