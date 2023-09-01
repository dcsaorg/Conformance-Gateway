package org.dcsa.conformance.gateway.traffic;

import org.springframework.util.MultiValueMap;

public record ConformanceRequest(
    String method,
    String path,
    MultiValueMap<String, String> queryParams,
    ConformanceMessage message) {}
