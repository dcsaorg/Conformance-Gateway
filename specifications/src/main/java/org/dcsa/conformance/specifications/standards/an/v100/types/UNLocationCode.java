package org.dcsa.conformance.specifications.standards.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    type = "string",
    pattern = "^[A-Z]{2}[A-Z2-9]{3}$",
    minLength = 5,
    maxLength = 5,
    example = "NLAMS",
    description =
"""
The UN Location code identifying a certain location. The pattern used must be:
* 2 characters for the country code using ISO 3166-1 alpha-2
* 3 characters to code a location within that country (using letters A-Z and numbers from 2-9)

Additional information can be found [here](https://unece.org/trade/cefact/UNLOCODE-Download).
""")
public class UNLocationCode {}
