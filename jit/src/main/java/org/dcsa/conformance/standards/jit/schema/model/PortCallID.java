package org.dcsa.conformance.standards.jit.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
    format = "uuid",
    type = "string",
    example = "0342254a-5927-4856-b9c9-aa12e7c00563",
    description =
        """
          Universal unique identifier for the **Port call**. The `portCallID` is created by the **Service Provider**. The `portCallID` **MUST** only be created once per **Port Call**. To be used in all communication regarding the **Port Call**.
          """)
public class PortCallID {
  String value;
}
