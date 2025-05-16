package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
    type = "string",
    format = "date-time",
    example = "2025-01-23T01:23:45Z",
    description =
"""
String representation of a timestamp in the [ISO 8601](https://www.iso.org/iso-8601-date-and-time-format.html) format
""")
public class FormattedDateTime {}
