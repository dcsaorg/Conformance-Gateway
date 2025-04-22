package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    type = "string",
    pattern = "^\\d{6,10}$",
    minLength = 6,
    maxLength = 10,
    example = "12345678",
    description =
"""
Code used by customs to classify the product being shipped.
The type of HS code depends on country and customs requirements. More information can be found
in the [HS Nomenclature](https://www.wcoomd.org/en/topics/nomenclature/instrument-and-tools)
""")
public class HSCode {}
