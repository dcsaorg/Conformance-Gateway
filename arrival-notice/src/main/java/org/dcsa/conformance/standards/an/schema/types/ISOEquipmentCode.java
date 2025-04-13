package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    type = "string",
    pattern = "^\\S(?:.*\\S)?$",
    maxLength = 4,
    example = "22GP",
    description =
"""
Unique code identifying the equipment size and type used to transport commodities.
 The code can refer to either the ISO size type (e.g. 22G1) or the ISO type group (e.g. 22GP)
 following the [ISO 6346](https://en.wikipedia.org/wiki/ISO_6346) standard.
""")
public class ISOEquipmentCode {}
