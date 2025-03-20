package org.dcsa.conformance.standards.jit.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    format = "uuid",
    type = "string",
    description =
        "Universal unique identifier for the **Port Call Service**. The `portCallServiceID` is created by the **Service Provider**. To be used in all communication regarding the **Port Call Service** (i.e. sending a **Vessel Status** with the vessel-status endpoint).",
    example = "0342254a-5927-4856-b9c9-aa12e7c00563")
public class PortCallServiceID {}
