package org.dcsa.conformance.standards.jit.party;

import static org.dcsa.conformance.core.party.ConformanceParty.API_VERSION;
import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.jit.checks.JitChecks;
import org.dcsa.conformance.standards.jit.model.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JitPartyHelper {

  static ConformanceResponse handleGetRequest(
      ConformanceRequest request,
      JsonNodeMap persistentMap,
      String apiVersion,
      ConformanceParty jitParty) {
    ArrayNode response = OBJECT_MAPPER.createArrayNode();
    if (request.url().contains(JitGetType.PORT_CALLS.getUrlPath())) {
      ObjectNode portCall = (ObjectNode) persistentMap.load(JitGetType.PORT_CALLS.name());
      portCall.remove(JitProvider.IS_FYI);
      response.add(portCall);
      jitParty.addOperatorLogEntry("Handled GET Port Calls request accepted.");
    } else if (request.url().contains(JitGetType.TERMINAL_CALLS.getUrlPath())) {
      ObjectNode terminalCall = (ObjectNode) persistentMap.load(JitGetType.TERMINAL_CALLS.name());
      terminalCall.remove(JitProvider.IS_FYI);
      response.add(terminalCall);
      jitParty.addOperatorLogEntry("Handled GET Terminal Calls request accepted.");
    } else if (request.url().contains(JitGetType.PORT_CALL_SERVICES.getUrlPath())) {
      ObjectNode portServiceCall =
          (ObjectNode) persistentMap.load(JitGetType.PORT_CALL_SERVICES.name());
      portServiceCall.remove(JitProvider.IS_FYI);
      response.add(portServiceCall);
      jitParty.addOperatorLogEntry("Handled GET Port Service Calls request accepted.");
    } else if (request.url().contains(JitGetType.VESSEL_STATUSES.getUrlPath())) {
      ObjectNode vesselStatusCall =
          (ObjectNode) persistentMap.load(JitGetType.VESSEL_STATUSES.name());
      vesselStatusCall.remove(JitProvider.IS_FYI);
      response.add(vesselStatusCall);
      jitParty.addOperatorLogEntry("Handled GET Vessel Status Calls request accepted.");
    } else if (request.url().contains(JitGetType.TIMESTAMPS.getUrlPath())) {
      ArrayNode timestampCalls = (ArrayNode) persistentMap.load(JitGetType.TIMESTAMPS.name());
      response.addAll(getMatchingURLParamsWithTimestamps(request, timestampCalls));

      jitParty.addOperatorLogEntry(
          "Handled GET Timestamp Calls request accepted. Return %s timestamp objects."
              .formatted(response.size()));
    } else {
      jitParty.addOperatorLogEntry("Unhandled GET request!");
    }

    return request.createResponse(
        200, Map.of(API_VERSION, List.of(apiVersion)), new ConformanceMessageBody(response));
  }

  private static ArrayNode getMatchingURLParamsWithTimestamps(
      ConformanceRequest request, ArrayNode timestampCalls) {
    ArrayNode results = OBJECT_MAPPER.createArrayNode();
    Set<String> queryParams = request.queryParams().keySet();
    // Timestamp URL parameter section.
    if (queryParams.contains(JitChecks.TIMESTAMP_ID)) {
      request.queryParams().get(JitChecks.TIMESTAMP_ID).stream()
          .findFirst()
          .ifPresent(
              timestampID -> {
                ObjectNode timestampCall =
                    (ObjectNode) getTimestampBy(null, timestampID, timestampCalls);
                if (timestampCall != null) {
                  results.add(timestampCall);
                }
              });
    }
    // PortCallServiceID URL parameter section.
    if (queryParams.contains(JitChecks.PORT_CALL_SERVICE_ID)) {
      request.queryParams().get(JitChecks.PORT_CALL_SERVICE_ID).stream()
          .findFirst()
          .ifPresent(
              portCallServiceID ->
                  results.addAll(
                      getTimestampsByPortCallServiceID(portCallServiceID, timestampCalls)));
    }

    // ClassifierCode URL parameter section.
    if (queryParams.contains(JitChecks.CLASSIFIER_CODE)) {
      request.queryParams().get(JitChecks.CLASSIFIER_CODE).stream()
          .findFirst()
          .ifPresent(
              classifierCode ->
                  results.add(
                      getTimestampBy(
                          JitClassifierCode.valueOf(classifierCode), null, timestampCalls)));
    }
    // TODO: filter by multiple parameters.
    return results;
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

  static void createParamsForTerminalCall(
      JsonNodeMap persistentMap,
      JitGetType getType,
      List<String> filters,
      Map<String, List<String>> queryParams) {
    if (getType != JitGetType.TERMINAL_CALLS) return;

    JsonNode terminalCall = persistentMap.load(JitGetType.TERMINAL_CALLS.name());
    for (int i = 0; i < JitGetTerminalCallFilters.props().size(); i++) {
      String propertyName = JitGetTerminalCallFilters.props().get(i);
      if (filters.contains(propertyName)) {
        queryParams.put(propertyName, List.of(terminalCall.get(propertyName).asText()));
      }
    }
  }

  static void createParamsForPortServiceCall(
      JsonNodeMap persistentMap,
      JitGetType getType,
      List<String> filters,
      Map<String, List<String>> queryParams) {
    if (getType != JitGetType.PORT_CALL_SERVICES) return;

    JsonNode portServiceCall = persistentMap.load(JitGetType.PORT_CALL_SERVICES.name());
    for (int i = 0; i < JitGetPortServiceCallFilters.props().size(); i++) {
      String propertyName = JitGetPortServiceCallFilters.props().get(i);
      if (filters.contains(propertyName)) {
        queryParams.put(propertyName, List.of(portServiceCall.get(propertyName).asText()));
      }
    }
  }

  public static void createParamsForVesselStatusCall(
      JsonNodeMap persistentMap,
      JitGetType getType,
      List<String> filters,
      Map<String, List<String>> queryParams) {
    if (getType != JitGetType.VESSEL_STATUSES) return;

    JsonNode vesselStatus = persistentMap.load(JitGetType.VESSEL_STATUSES.name());
    String propertyName = "portCallServiceID"; // Only one property exists for vessel status.
    if (filters.contains(propertyName)) {
      queryParams.put(propertyName, List.of(vesselStatus.get(propertyName).asText()));
    }
  }

  public static void createParamsForTimestampCall(
      JsonNodeMap persistentMap,
      JitGetType getType,
      List<String> filters,
      Map<String, List<String>> queryParams) {
    if (getType != JitGetType.TIMESTAMPS) return;

    ArrayNode timestampCalls = (ArrayNode) persistentMap.load(JitGetType.TIMESTAMPS.name());
    Optional<String> timestampFilter =
        filters.stream().filter(s -> s.startsWith(JitChecks.TIMESTAMP_ID)).findFirst();
    if (timestampFilter.isPresent()) {
      JsonNode timestamp;
      timestamp =
          switch (timestampFilter.get()) {
            case "timestampID_Estimated" ->
                getTimestampBy(JitClassifierCode.EST, null, timestampCalls);
            case "timestampID_Requested" ->
                getTimestampBy(JitClassifierCode.REQ, null, timestampCalls);
            case "timestampID_Planned" ->
                getTimestampBy(JitClassifierCode.PLN, null, timestampCalls);
            case "timestampID_Actual" ->
                getTimestampBy(JitClassifierCode.ACT, null, timestampCalls);
            default ->
                throw new IllegalArgumentException(
                    "Unknown timestamp ID: " + timestampFilter.get());
          };
      queryParams.put(
          JitChecks.TIMESTAMP_ID, List.of(timestamp.get(JitChecks.TIMESTAMP_ID).asText()));
    }
    if (filters.contains(JitChecks.PORT_CALL_SERVICE_ID)) {
      // The previous Planned timestamp has the same portCallServiceID.
      JsonNode timestampNode = getTimestampBy(JitClassifierCode.PLN, null, timestampCalls);
      queryParams.put(
          JitChecks.PORT_CALL_SERVICE_ID,
          List.of(timestampNode.get(JitChecks.PORT_CALL_SERVICE_ID).asText()));
    }
    if (filters.contains(JitChecks.CLASSIFIER_CODE)) {
      // Filter by classifier code PLN.
      JsonNode timestampNode = getTimestampBy(JitClassifierCode.PLN, null, timestampCalls);
      queryParams.put(
          JitChecks.CLASSIFIER_CODE,
          List.of(timestampNode.get(JitChecks.CLASSIFIER_CODE).asText()));
    }
  }

  static JsonNode getTimestampBy(
      JitClassifierCode classifierCode, String timestampID, ArrayNode timestampCalls) {
    for (JsonNode jsonNode : timestampCalls) {
      if (classifierCode != null
          && jsonNode
              .get(JitChecks.CLASSIFIER_CODE)
              .asText()
              .equalsIgnoreCase(classifierCode.name())) {
        return jsonNode;
      }
      if (timestampID != null
          && jsonNode.path(JitChecks.TIMESTAMP_ID).asText("").equals(timestampID)) {
        return jsonNode;
      }
    }
    return null;
  }

  static ArrayNode getTimestampsByPortCallServiceID(
      String portCallServiceID, ArrayNode timestampCalls) {
    ArrayNode response = OBJECT_MAPPER.createArrayNode();
    for (JsonNode jsonNode : timestampCalls) {
      if (portCallServiceID.equals(jsonNode.get(JitChecks.PORT_CALL_SERVICE_ID).asText(null))) {
        response.add(jsonNode);
      }
    }
    return response;
  }

  // Save the response for generating GET requests. Add it to the list of timestamps.
  static void storeTimestamp(JsonNodeMap persistentMap, JitTimestamp timestamp) {
    ArrayNode timestamps = (ArrayNode) persistentMap.load(JitGetType.TIMESTAMPS.name());
    if (timestamps == null) {
      timestamps = OBJECT_MAPPER.createArrayNode();
    }
    ObjectNode timestampNode = timestamp.toJson();
    timestampNode.remove(JitProvider.IS_FYI);
    timestamps.add(timestampNode);
    persistentMap.save(JitGetType.TIMESTAMPS.name(), timestamps);
    // TODO: deal with previous received timestamps with the same type.
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
