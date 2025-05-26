package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    type = "string",
    pattern = "^\\d{7,8}$",
    minLength = 7,
    maxLength = 8,
    example = "12345678",
    description =
"""
The unique reference for a registered vessel, which remains unchanged throughout the entire lifetime of the vessel.
The reference is the International Maritime Organisation (IMO) number,
also sometimes known as the Lloyd's register code.
""")
public class VesselIMONumber {}
