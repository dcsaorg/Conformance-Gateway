package org.dcsa.conformance.standards.an.action;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.an.party.DynamicScenarioParameters;

public class AnAction extends ConformanceAction {

  private final OverwritingReference<DynamicScenarioParameters> dsp;

  protected AnAction(String sourcePartyName, String targetPartyName, AnAction previousAction, String actionTitle) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.dsp =
      previousAction == null
      ? new OverwritingReference<>(null, new DynamicScenarioParameters(null))
      : new OverwritingReference<>(previousAction.dsp, null);
  }

  @Override
  public String getHumanReadablePrompt() {
    return "";
  }

  protected Supplier<DynamicScenarioParameters> getDspSupplier() {
    return dsp::get;
  }

  protected Consumer<DynamicScenarioParameters> getDspConsumer() {
    return dsp::set;
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);

    JsonNode jsonBody = exchange.getRequest().message().body().getJsonBody();

    List<String> transportDocumentReferences = new ArrayList<>();
    if (jsonBody.isArray()) {
      for (JsonNode item : jsonBody) {
        JsonNode tdr = item.get("transportDocumentReference");
        if (tdr != null && tdr.isTextual()) {
          transportDocumentReferences.add(tdr.asText());
        }
      }
    }
    getDspConsumer().accept(
        getDspSupplier()
            .get()
            .withTransportDocumentReferences(transportDocumentReferences));
  }

}
