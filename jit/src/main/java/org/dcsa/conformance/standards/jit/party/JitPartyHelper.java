package org.dcsa.conformance.standards.jit.party;

import static org.dcsa.conformance.core.party.ConformanceParty.API_VERSION;
import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.jit.model.JitGetPortCallFilters;
import org.dcsa.conformance.standards.jit.model.JitGetType;
import org.dcsa.conformance.standards.jit.model.PortCallPhaseTypeCode;
import org.dcsa.conformance.standards.jit.model.PortCallServiceEventTypeCode;
import org.dcsa.conformance.standards.jit.model.PortCallServiceType;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JitPartyHelper {

  static ConformanceResponse handleGetRequest(
      ConformanceRequest request,
      JsonNodeMap persistentMap,
      String apiVersion,
      ConformanceParty jitConsumer) {
    JsonNode response;
    if (request.url().contains(JitGetType.PORT_CALLS.getUrlPath())) {
      ObjectNode portCall = (ObjectNode) persistentMap.load(JitGetType.PORT_CALLS.name());
      portCall.remove(JitProvider.IS_FYI);
      response = OBJECT_MAPPER.createArrayNode().add(portCall);
      jitConsumer.addOperatorLogEntry("Handled Port Call Service request accepted.");
    } else {
      response = OBJECT_MAPPER.createArrayNode();
    }

    return request.createResponse(
        200, Map.of(API_VERSION, List.of(apiVersion)), new ConformanceMessageBody(response));
  }

  static void createParamsForPortCall(
      JsonNodeMap persistentMap,
      JitGetType getType,
      List<String> filters,
      Map<String, List<String>> queryParams) {
    if (getType != JitGetType.PORT_CALLS) return;

    JsonNode portCall = persistentMap.load(JitGetType.PORT_CALLS.name());
    JsonNode vessel = portCall.path("vessel");
    for (int i = 0; i < JitGetPortCallFilters.props().size(); i++) {
      String propertyName = JitGetPortCallFilters.props().get(i);
      if (filters.contains(propertyName)) {
        // The first 3 properties are in the portCall.
        if (i < 3) queryParams.put(propertyName, List.of(portCall.get(propertyName).asText()));
        else { // The rest are in the vessel.
          String propertyValue;
          if (propertyName.equals("vesselName"))
            propertyValue = vessel.get("name").asText(); // property has different filter name.
          else propertyValue = vessel.get(propertyName).asText();
          queryParams.put(propertyName, List.of(propertyValue));
        }
      }
    }
  }

  static JsonNode replacePlaceHolders(String fileType, DynamicScenarioParameters dsp) {
    PortCallServiceType serviceType = dsp.portCallServiceType();
    String portCallPhaseTypeCode = "";
    String portCallServiceEventTypeCode = "";
    if (serviceType != null) {
      portCallPhaseTypeCode = calculatePortCallPhaseTypeCode(serviceType.name());
      portCallServiceEventTypeCode =
          PortCallServiceEventTypeCode.getCodesForPortCallServiceType(serviceType.name())
              .getFirst()
              .name();
    }

    JsonNode jsonNode =
        JsonToolkit.templateFileToJsonNode(
            "/standards/jit/messages/jit-200-%s-request.json".formatted(fileType),
            Map.of(
                "PORT_CALL_ID_PLACEHOLDER",
                Objects.requireNonNullElse(dsp.portCallID(), ""),
                "TERMINAL_CALL_ID_PLACEHOLDER",
                Objects.requireNonNullElse(dsp.terminalCallID(), ""),
                "PORT_CALL_SERVICE_TYPE_PLACEHOLDER",
                serviceType != null ? serviceType.name() : "",
                "PORT_CALL_SERVICE_ID_PLACEHOLDER",
                Objects.requireNonNullElse(dsp.portCallServiceID(), ""),
                "PORT_CALL_SERVICE_EVENT_TYPE_CODE_PLACEHOLDER",
                portCallServiceEventTypeCode,
                "PORT_CALL_PHASE_TYPE_CODE_PLACEHOLDER",
                portCallPhaseTypeCode,
                "IS_FYI_PLACEHOLDER",
                Boolean.toString(dsp.isFYI())));
    // Some serviceType do not have a portCallPhaseTypeCode; remove it, since it is an enum.
    if (serviceType != null && portCallPhaseTypeCode.isEmpty())
      ((ObjectNode) jsonNode).remove("portCallPhaseTypeCode");
    return jsonNode;
  }

  private static String calculatePortCallPhaseTypeCode(String serviceType) {
    List<PortCallPhaseTypeCode> typeCodes =
        PortCallPhaseTypeCode.getCodesForPortCallServiceType(serviceType);
    if (typeCodes.isEmpty()) {
      return "";
    }
    return typeCodes.getFirst().name();
  }
}
