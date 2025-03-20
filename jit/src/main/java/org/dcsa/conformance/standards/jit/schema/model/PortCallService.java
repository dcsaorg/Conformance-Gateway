package org.dcsa.conformance.standards.jit.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.dcsa.conformance.standards.jit.schema.DescriptionOverride;

@Schema(
    title = "Port Call Service",
    requiredProperties = "terminalCallID, portCallServiceID",
    description =
        "The **Port Call Service** contains all information about the service being provided.")
@Data
@AllArgsConstructor
public class PortCallService {

  private TerminalCallID terminalCallID;

  @DescriptionOverride(
      "Universal unique identifier for the **Port Call Service**. The `portCallServiceID` is created by the **Service Provider**. To be used in all communication regarding the **Port Call Service** (i.e. sending a **Timestamp** with the timestamps endpoint).")
  private PortCallServiceID portCallServiceID;

  private IsFYI isFYI;
}
