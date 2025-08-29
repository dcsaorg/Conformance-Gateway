package org.dcsa.conformance.specifications;

import org.dcsa.conformance.specifications.standards.ct.v300.CTStandardSpecification;
import org.junit.jupiter.api.Test;

class CTStandardSpecificationTest {
  @Test
  void testCTStandardSpecification() {
    CTStandardSpecification ctStandardSpecification = new CTStandardSpecification();
    ctStandardSpecification.generateArtifacts();

    StandardSpecificationTestToolkit.verifyTypeExport(
      "ContainerTrackingEvent",
      "../specifications/generated-resources/standards/ct/v300/ct-v3.0.0-openapi.yaml",
      ctStandardSpecification);
  }
}
