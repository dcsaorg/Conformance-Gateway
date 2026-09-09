package org.dcsa.conformance.standards.ebl.action;

import java.util.Map;
import java.util.Set;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;

public class ShipperGetShippingInstructionsSkippableAction
    extends Shipper_GetShippingInstructionsAction {

  public ShipperGetShippingInstructionsSkippableAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      ShippingInstructionsStatus expectedSiStatus,
      ShippingInstructionsStatus expectedAmendedSiStatus,
      JsonSchemaValidator responseSchemaValidator,
      boolean requestAmendedStatus,
      boolean recordTDR,
      boolean useBothRef) {
    super(
        carrierPartyName,
        shipperPartyName,
        previousAction,
        expectedSiStatus,
        expectedAmendedSiStatus,
        responseSchemaValidator,
        requestAmendedStatus,
        recordTDR,
        useBothRef);
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
            Map.of("REFERENCE", getDspSupplier().get().shippingInstructionsReference(),
                "ORIGINAL_OR_AMENDED_PLACEHOLDER", requestAmendedStatus ? "AMENDED" : "ORIGINAL"),
            "prompt-shipper-get.md",
            "prompt-shipper-refresh-skippable-complete.md");
  }

  @Override
  public Set<String> skippableForRoles() {
    return Set.of(EblRole.SHIPPER.getConfigName());
  }
}

