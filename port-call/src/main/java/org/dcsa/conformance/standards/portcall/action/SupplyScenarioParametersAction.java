package org.dcsa.conformance.standards.portcall.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.core.toolkit.JsonToolkit;

public class SupplyScenarioParametersAction extends PortCallAction{

  public SupplyScenarioParametersAction(String publisherPartyName) {
    super(
      publisherPartyName,
      null,
      null,
      "SupplyScenarioParameters");
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Using the example format below, provide one or more query parameters"
      + " for which the sandbox can GET arrival notices from your system";
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return examplePrompt();
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) {
    JsonNode tdrsJsonNode = partyInput.get("input").get("transportDocumentReferences");
    if (!tdrsJsonNode.isArray()) {
      throw new UserFacingException(
        "The value of 'transportDocumentReferences' must be an array containing transport document reference strings");
    }
    ArrayNode tdrsArrayNode = ((ArrayNode) tdrsJsonNode);
    ArrayList<String> tdrs = new ArrayList<>();
    for (int index = 0; index < tdrsArrayNode.size(); ++index) {
      JsonNode elementJsonNode = tdrsArrayNode.get(index);
      if (!elementJsonNode.isTextual()) {
        throw new UserFacingException(
          "Element '%s' in the input is not a transport document reference string"
            .formatted(elementJsonNode.toString()));
      }
      String tdr = elementJsonNode.asText().trim();
      if (!tdr.equals(tdr.trim())) {
        throw new UserFacingException(
          "Element '%s' must be a transport document reference and therefore must not start or end with whitespace"
            .formatted(elementJsonNode.toString()));
      }
      if (tdr.isEmpty() || tdr.length() > 20) {
        throw new UserFacingException(
          "Element '%s' must be a transport document reference and therefore must have between 1 and 20 characters"
            .formatted(elementJsonNode.toString()));
      }
      tdrs.add(tdr);
    }
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  public static ObjectNode examplePrompt() {
    return JsonToolkit.OBJECT_MAPPER
      .createObjectNode()
      .set(
        "transportDocumentReferences",
        JsonToolkit.OBJECT_MAPPER.createArrayNode().add("HHL71800000"));
  }
}
