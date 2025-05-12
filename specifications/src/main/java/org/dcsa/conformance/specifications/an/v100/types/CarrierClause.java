package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    type = "string",
    maxLength = 20000,
    example = "It is not allowed to...",
    description =
"""
Clause for a specific shipment added by the carrier, subject to local rules / guidelines
or certain mandatory information required to be shared with the customer.
""")
public class CarrierClause {}
