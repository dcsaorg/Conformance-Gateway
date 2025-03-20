package org.dcsa.conformance.standards.jit.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    format = "uuid",
    type = "string",
    description =
        "Universal unique identifier for the **Terminal Call**. The `terminalCallID` is created by the **Service Provider**. The `terminalCallID` **MUST** only be created once per **Terminal Call**. To be used in all communication regarding the **Terminal Call**.",
    example = "0342254a-5927-4856-b9c9-aa12e7c00563")
public class TerminalCallID {}
