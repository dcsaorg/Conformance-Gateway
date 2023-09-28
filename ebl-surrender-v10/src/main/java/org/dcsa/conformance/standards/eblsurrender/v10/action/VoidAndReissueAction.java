package org.dcsa.conformance.standards.eblsurrender.v10.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.Getter;
import org.dcsa.conformance.core.scenario.ConformanceAction;

@Getter
public class VoidAndReissueAction extends TdrAction {

  private final Consumer<String> tdrConsumer =
      tdr -> {
        if (!Objects.equals(tdr, tdrSupplier.get())) {
          throw new UnsupportedOperationException(
              "Changing the transportDocumentReference during void/reissue is currently not supported");
        }
      };

  public VoidAndReissueAction(
      String carrierPartyName, String platformPartyName, ConformanceAction previousAction) {
    super(carrierPartyName, platformPartyName, -1, previousAction, "Void&Reissue");
  }

  @Override
  public ObjectNode exportJsonState() {
    return super.exportJsonState();
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Void and reissue the eBL with transport document reference '%s'".formatted(tdrSupplier.get());
  }

  @Override
  public boolean isConfirmationRequired() {
    return true;
  }

  @Override
  public Supplier<String> getSrrSupplier() {
    return null;
  }
}
