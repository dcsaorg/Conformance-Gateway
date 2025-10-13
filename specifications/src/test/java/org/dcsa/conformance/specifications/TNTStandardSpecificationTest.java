package org.dcsa.conformance.specifications;

import org.dcsa.conformance.specifications.standards.tnt.v300.TNTStandardSpecification;
import org.dcsa.conformance.specifications.standards.tnt.v300.model.Event;
import org.junit.jupiter.api.Test;

class TNTStandardSpecificationTest {
  @Test
  void testTNTStandardSpecification() {
    TNTStandardSpecification tntStandardSpecification = new TNTStandardSpecification();
    tntStandardSpecification.generateArtifacts();

    StandardSpecificationTestToolkit.verifyTypeExport(
        Event.class.getSimpleName(),
        "../specifications/generated-resources/standards/tnt/v300/tnt-v3.0.0-openapi.yaml",
        tntStandardSpecification);
  }
}
