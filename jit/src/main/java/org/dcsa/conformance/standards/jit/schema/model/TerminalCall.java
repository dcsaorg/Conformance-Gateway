package org.dcsa.conformance.standards.jit.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.dcsa.conformance.standards.jit.schema.SchemaOverride;

@Schema(
    title = "Terminal Call",
    requiredProperties = {"terminalCallID", "portCallID"},
    description =
        "**Terminal Call** information. The `terminalCallID` uniquely identifies the **Terminal Call**. Any subsequent **Port Call Services** linked to the same **Terminal Call** MUST use the same `terminalCallID`. An optional `terminalCallReference` can be added in order to link the **Terminal Call** to external systems e.g Operational Vessel Schedules (**OVS**).")
@Data
@AllArgsConstructor
public class TerminalCall {

  @SchemaOverride(
      description =
          "Universal unique identifier for the **Terminal Call**. The `terminalCallID` is created by the **Service Provider**. The `terminalCallID` **MUST** only be created once per **Terminal Call**. To be used in all communication regarding the **Terminal Call**.")
  private TerminalCallID terminalCallID;

  @Schema
  @SchemaOverride(example = "9d4b83fa-cf48-413e-aa79-8d01a17ee201")
  private PortCallID portCallID;

  @Schema(
      description =
          """
          The unique reference for a **Terminal Call**. It's the vessel operator's responsibility to provide the **Terminal Call Reference**, other parties are obliged to pick it up and use it.

          In **Operational Vessel Schedules (OVS) 3.0** this field can be found as `transportCallReference`.""",
      example = "15063401",
      maxLength = 100)
  private String terminalCallReference;

  @Schema(ref = "#/components/schemas/Vessel")
  private String vessel;

  @Schema(
      accessMode = Schema.AccessMode.READ_ONLY,
      defaultValue = "false",
      description =
          "If set to `true` it indicates that the **Port Call** has been omitted by the carrier.",
      example = "false")
  private boolean omitted;

  private IsFYI isFYI;
}
