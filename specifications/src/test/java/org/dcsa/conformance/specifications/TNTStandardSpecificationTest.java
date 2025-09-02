package org.dcsa.conformance.specifications;

import org.dcsa.conformance.specifications.standards.tnt.v300.TNTStandardSpecification;
import org.junit.jupiter.api.Test;

class TNTStandardSpecificationTest {
  @Test
  void testCTStandardSpecification() {
    TNTStandardSpecification tntStandardSpecification = new TNTStandardSpecification();
    tntStandardSpecification.generateArtifacts();

    StandardSpecificationTestToolkit.verifyTypeExport(
      "TNTEvent",
      "../specifications/generated-resources/standards/tnt/v300/tnt-v3.0.0-openapi.yaml",
      tntStandardSpecification);
  }
}
