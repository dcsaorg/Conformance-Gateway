package org.dcsa.conformance.standards.jit.schema.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(
    title = "Port Call",
    requiredProperties = "portCallID, UNLocationCode, vessel",
    description =
        "**Port Call** information. The `portCallID` uniquely identifies the **Port Call**. Any subsequent **Terminal Calls** linked to the same **Port Call** MUST use the same `portCallID`. An optional `portVisitReference` can be added in order to link the **Port Call** to external systems. It is mandatory to provide the **Vessel** information.")
@Data
@AllArgsConstructor
public class PortCall {

  @Schema(
      description =
          """
          Universal unique identifier for the **Port call**. The `portCallID` is created by the provider. The `portCallID` **MUST** only be created once per **
          Port Call**. To be used in all communication regarding the **Port Call**.
          """)
  private PortCallID portCallID;

  @Schema(
      description =
          "The unique reference that can be used to link different **Terminal Calls** to the same port visit. The reference is provided by the port to uniquely identify a **Port Call**.",
      example = "NLAMS1234589",
      maxLength = 50)
  private String portVisitReference;

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

  private IsFYI isFYI;
}
