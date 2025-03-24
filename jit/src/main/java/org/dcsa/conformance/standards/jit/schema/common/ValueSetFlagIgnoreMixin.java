package org.dcsa.conformance.standards.jit.schema.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Example objects can be converted to YAML with an extra attribute: valueSetFlag: true. This is the
 * easiest way to remove that extra attribute.
 */
@JsonIgnoreProperties("valueSetFlag")
public abstract class ValueSetFlagIgnoreMixin {}
