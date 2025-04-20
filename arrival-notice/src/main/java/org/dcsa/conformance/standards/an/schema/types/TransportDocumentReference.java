package org.dcsa.conformance.standards.an.schema.types;

import io.swagger.v3.oas.annotations.media.Schema;
import org.dcsa.conformance.standards.an.schema.StringPatterns;

@Schema(
  pattern = StringPatterns.TRIMMED_NON_EMPTY_STRING,
  maxLength = 20,
  example = "XYZ1234",
  description = "Reference of a transport document")
public class TransportDocumentReference {}
