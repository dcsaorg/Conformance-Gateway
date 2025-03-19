package org.dcsa.conformance.standards.jit.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(
    title = "Terminal Call",
    requiredProperties = "terminalCallID, portCallID",
    description =
        "**Terminal Call** information. The `terminalCallID` uniquely identifies the **Terminal Call**. Any subsequent **Port Call Services** linked to the same **Terminal Call** MUST use the same `terminalCallID`. An optional `terminalCallReference` can be added in order to link the **Terminal Call** to external systems e.g Operational Vessel Schedules (**OVS**).")
@Data
@AllArgsConstructor
public class TerminalCall {

  @Schema(
      description =
          "Universal unique identifier for the **Terminal Call**. The `terminalCallID` is created by the provider. The `terminalCallID` **MUST** only be created once per **Terminal Call**. To be used in all communication regarding the **Terminal Call**.")
  private TerminalCallID terminalCallID;

  @Schema(
      description =
          "Universal unique identifier for the **Port call**. The `portCallID` is created by the provider. The `portCallID` **MUST** only be created once per **Port Call**. To be used in all communication regarding the **Port Call**.")
  private PortCallID portCallID;

  @Schema(
      description =
          """
          The unique reference for a **Terminal Call**. It's the vessel operator's responsibility to provide the **Terminal Call Reference**, other parties are obliged to pick it up and use it.

          In **Operational Vessel Schedules (OVS) 3.0** this field can be found as `transportCallReference`.""",
      example = "15063401",
      maxLength = 100)
  private String terminalCallReference;

  @Schema(
      description =
          """
The UN Location code specifying where the place is located. The pattern used must be

            - 2 characters for the country code using [ISO 3166-1 alpha-2](https://www.iso.org/obp/ui/#iso:pub:PUB500001:en)
            - 3 characters to code a location within that country. Letters A-Z and numbers from 2-9 can be used

            More info can be found here: [UN/LOCODE](https://unece.org/trade/cefact/UNLOCODE-Download).
""",
      pattern = "^[A-Z]{2}[A-Z2-9]{3}$",
      example = "NLAMS",
      minLength = 5,
      maxLength = 5)
  @JsonProperty("UNLocationCode")
  private String unLocationCode;

  @Schema(ref = "#/components/schemas/Vessel")
  private String vessel;

  @Schema(
      accessMode = Schema.AccessMode.READ_ONLY,
      defaultValue = "false",
      description =
          "If set to `true` it indicates that the **Port Call** has been omitted by the carrier.",
      example = "false")
  private boolean omitted;

  @Schema(
      accessMode = Schema.AccessMode.READ_ONLY,
      defaultValue = "false",
      description =
          "If set to `true` it indicates that this message is primarily meant for another party - but is sent as a FYI (for your information).",
      example = "true")
  @JsonProperty("isFYI")
  private boolean isFYI;
}
