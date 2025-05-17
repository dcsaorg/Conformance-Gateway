package org.dcsa.conformance.specifications.standards.dt.v100.types;

import io.swagger.v3.oas.annotations.media.Schema;
import org.dcsa.conformance.specifications.generator.StringPatterns;

@Schema(
  type = "string",
  pattern = StringPatterns.TRIMMED_NON_EMPTY_STRING,
  maxLength = 20,
  example = "XYZ1234",
  description = "Reference of a transport document")
public class TransportDocumentReference {}
