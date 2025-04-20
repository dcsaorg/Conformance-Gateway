package org.dcsa.conformance.specifications.an.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(
  type = "string",
  format = "date",
  pattern = "yyyy-MM-dd",
  example = "2025-01-23",
  description = "String representation of a date in yyyy-MM-dd format.")
public class FormattedDate {}
