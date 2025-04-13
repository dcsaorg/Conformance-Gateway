package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
  type = "string",
  pattern = "^\\d+(\\.\\d{1,2})?$",
  example = "123.45",
  description = "A monetary amount expressed with a maximum of 2 decimal digits")
public class CurrencyAmount {}
