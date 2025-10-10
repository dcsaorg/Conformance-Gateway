package org.dcsa.conformance.standards.an.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.an.party.DynamicScenarioParameters;

public class ANAction extends ConformanceAction {

  private final OverwritingReference<DynamicScenarioParameters> dsp;

  protected ANAction(
      String sourcePartyName, String targetPartyName, ANAction previousAction, String actionTitle) {
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

  @Override
  public void reset() {
    super.reset();
    if (previousAction != null) {
      this.dsp.set(null);
    }
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);

    if (this instanceof SubscriberGetANAction) {
      return;
    }

    DynamicScenarioParameters dspReference = getDspSupplier().get();

    var updatedDsp = dspReference;

    JsonNode jsonBody = exchange.getRequest().message().body().getJsonBody();

    List<String> transportDocumentReferences = new ArrayList<>();
    JsonNode arrivalNotices = null;
    if (this instanceof PublisherPostANNotificationAction) {
      arrivalNotices = jsonBody.get("arrivalNoticeNotifications");
    } else {
      arrivalNotices = jsonBody.get("arrivalNotices");
    }
    assert arrivalNotices != null;
    for (JsonNode arrivalNotice : arrivalNotices) {
      JsonNode tdr = arrivalNotice.get("transportDocumentReference");
        if (tdr != null && tdr.isTextual()) {
          transportDocumentReferences.add(tdr.asText());
        }
      }

    updatedDsp =
        getDspSupplier().get().withTransportDocumentReferences(transportDocumentReferences);

    if (!dspReference.equals(updatedDsp)) {
      dsp.set(updatedDsp);
    }
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (dsp.hasCurrentValue()) {
      jsonState.set("currentDsp", dsp.get().toJson());
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode dspNode = jsonState.get("currentDsp");
    if (dspNode != null) {
      dsp.set(DynamicScenarioParameters.fromJson(dspNode));
    }
  }
}
