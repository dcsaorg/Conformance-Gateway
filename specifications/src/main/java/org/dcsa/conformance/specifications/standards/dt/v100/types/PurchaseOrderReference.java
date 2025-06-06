package org.dcsa.conformance.specifications.standards.dt.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    type = "string",
    description = "A purchase order reference linked to the `Shipper`.",
    example = "HHL007",
    maxLength = 35,
    pattern = "^\\S(?:.*\\S)?$")
public class PurchaseOrderReference {}
