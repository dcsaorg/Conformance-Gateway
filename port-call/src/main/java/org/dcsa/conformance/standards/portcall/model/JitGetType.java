package org.dcsa.conformance.standards.portcall.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dcsa.conformance.standards.portcall.JitStandard;

@Getter
@RequiredArgsConstructor
public enum JitGetType {
  PORT_CALLS(
      "Port Calls",
      JitSchema.PORT_CALLS,
      JitStandard.PORT_CALL_URL.substring(0, JitStandard.PORT_CALL_URL.length() - 1)),
  TERMINAL_CALLS(
      "Terminal Calls",
      JitSchema.TERMINAL_CALLS,
      JitStandard.TERMINAL_CALL_URL.substring(0, JitStandard.TERMINAL_CALL_URL.length() - 1)),
  PORT_CALL_SERVICES(
      "Port Call Services",
      JitSchema.PORT_CALL_SERVICES,
      JitStandard.PORT_CALL_SERVICES_URL.substring(
          0, JitStandard.PORT_CALL_SERVICES_URL.length() - 1)),
  TIMESTAMPS(
      "Timestamps",
      JitSchema.TIMESTAMPS,
      JitStandard.TIMESTAMP_URL.substring(0, JitStandard.TIMESTAMP_URL.length() - 1)),
  VESSEL_STATUSES(
      "Vessel Statuses",
      JitSchema.VESSEL_STATUSES,
      JitStandard.VESSEL_STATUS_URL.substring(0, JitStandard.VESSEL_STATUS_URL.length() - 1));

  private final String name;
  private final JitSchema jitSchema;
  private final String urlPath;
}
