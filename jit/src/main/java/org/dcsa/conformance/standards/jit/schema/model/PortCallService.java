package org.dcsa.conformance.standards.jit.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(
    title = "Port Call Service",
    requiredProperties = "terminalCallID, portCallServiceID",
    description =
        "The **Port Call Service** contains all information about the service being provided.")
@Data
@AllArgsConstructor
public class PortCallService {

  @Schema(
      description =
          "Universal unique identifier for the **Terminal Call**. The `terminalCallID` is created by the provider. The `terminalCallID` **MUST** only be created once per **Terminal Call**. To be used in all communication regarding the **Terminal Call**.")
  private TerminalCallID terminalCallID;

  @Schema(
      description =
          "Universal unique identifier for the **Port Call Service**. The `portCallServiceID` is created by the provider. To be used in all communication regarding the **Port Call Service** (i.e. sending a timestamp with the timestamps endpoint).")
  private PortCallServiceID portCallServiceID;

  private IsFYI isFYI;
}
