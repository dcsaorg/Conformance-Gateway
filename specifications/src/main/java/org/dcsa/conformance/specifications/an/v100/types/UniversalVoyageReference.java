package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    type = "string",
    pattern = "^\\d{2}[0-9A-Z]{2}[NEWSR]$",
    minLength = 5,
    maxLength = 5,
    example = "2301W",
    description =
"""
A global unique voyage reference for the import Voyage, as per DCSA standard, agreed by VSA partners for the voyage.
 * last 2 digits of the year
 * 2 alphanumeric characters for the sequence number of the voyage
 * 1 character for the initial of the direction/haul (North, East, West, South or Roundtrip).
""")
public class UniversalVoyageReference {}
