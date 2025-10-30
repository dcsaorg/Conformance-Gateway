package org.dcsa.conformance.standards.portcall.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(
    value = {"facility", "geoCoordinate"}) // Ignore some fields which doesn't need checking
public record PortCallServiceLocationTimestamp(String locationName, String UNLocationCode) {}
