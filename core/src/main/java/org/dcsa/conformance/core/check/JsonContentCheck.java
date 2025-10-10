package org.dcsa.conformance.core.check;


public interface JsonContentCheck extends JsonContentValidation {
  String description();
  boolean isApplicable();
}
