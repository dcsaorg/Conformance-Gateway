package org.dcsa.conformance.core.traffic;

import java.util.Collection;
import java.util.Map;

public record ConformanceMessage(
    String sourcePartyName,
    String sourcePartyRole,
    String targetPartyName,
    String targetPartyRole,
    Map<String, ? extends Collection<String>> headers,
    ConformanceMessageBody body,
    long timestamp) {}
