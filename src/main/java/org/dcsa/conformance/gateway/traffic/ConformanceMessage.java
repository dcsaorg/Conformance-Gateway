package org.dcsa.conformance.gateway.traffic;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.util.MultiValueMap;

public record ConformanceMessage(
    String sourcePartyName,
    String sourcePartyRole,
    String targetPartyName,
    String targetPartyRole,
    MultiValueMap<String, String> headers,
    String stringBody,
    JsonNode jsonBody,
    long timestamp) {}
