package org.dcsa.conformance.standards.jit.schema.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    accessMode = Schema.AccessMode.WRITE_ONLY,
    defaultValue = "false", // This is somehow ignored by the generator
    type = "boolean",
    name = "isFYI",
    description =
        "If set to `true` it indicates that this message is primarily meant for another party - but is sent as a FYI (for your information).",
    example = "true")
public class IsFYI {}
