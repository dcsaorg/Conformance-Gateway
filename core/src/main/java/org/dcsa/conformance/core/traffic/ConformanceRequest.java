package org.dcsa.conformance.core.traffic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import org.dcsa.conformance.core.toolkit.JsonToolkit;

import static org.dcsa.conformance.core.Util.STATE_OBJECT_MAPPER;

public record ConformanceRequest(
    String method,
    String url,
    Map<String, ? extends Collection<String>> queryParams,
    ConformanceMessage message) {

  public ConformanceResponse createResponse(
      int statusCode,
      Map<String, ? extends Collection<String>> headers,
      ConformanceMessageBody body) {
    return new ConformanceResponse(
        statusCode,
        new ConformanceMessage(
            this.message.targetPartyName(),
            this.message.targetPartyRole(),
            this.message.sourcePartyName(),
            this.message.sourcePartyRole(),
            headers,
            body,
            System.currentTimeMillis()));
  }

  public ObjectNode toJson() {
    ObjectNode objectNode = STATE_OBJECT_MAPPER.createObjectNode();
    objectNode.put("method", method);
    objectNode.put("url", url);
    objectNode.set("queryParams", JsonToolkit.mapOfStringToStringCollectionToJson(queryParams));
    objectNode.set("message", message.toJson());
    return objectNode;
  }

  public URI toURI() throws MalformedURLException, URISyntaxException {
    if (this.queryParams.isEmpty()) {
      return new URL(url).toURI();
    }
    var b = new StringBuilder(url);
    b.append("?");
    boolean first = true;
    for (var queryParam : queryParams.entrySet()) {
      var encodedQueryParamName = URLEncoder.encode(queryParam.getKey(), StandardCharsets.UTF_8);
      for (var queryParamValue : queryParam.getValue()) {
        if (!first) {
          b.append("&");
        }
        first = false;
        b.append(encodedQueryParamName)
          .append("=")
          .append(URLEncoder.encode(queryParamValue, StandardCharsets.UTF_8));

      }
    }
    return new URL(b.toString()).toURI();
  }

  public static ConformanceRequest fromJson(ObjectNode objectNode) {
    return new ConformanceRequest(
        objectNode.get("method").asText(),
        objectNode.get("url").asText(),
        JsonToolkit.mapOfStringToStringCollectionFromJson((ArrayNode) objectNode.get("queryParams")),
        ConformanceMessage.fromJson((ObjectNode) objectNode.get("message")));
  }

}
