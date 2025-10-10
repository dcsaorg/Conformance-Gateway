package org.dcsa.conformance.specifications;

import org.dcsa.conformance.specifications.standards.jit.v200.JITStandardSpecification;
import org.dcsa.conformance.specifications.standards.jit.v200.model.Event;
import org.junit.jupiter.api.Test;

class JITStandardSpecificationTest {
  @Test
  void testJITStandardSpecification() {
    JITStandardSpecification jitStandardSpecification = new JITStandardSpecification();
    jitStandardSpecification.generateArtifacts();

    StandardSpecificationTestToolkit.verifyTypeExport(
        Event.class.getSimpleName(),
        "../specifications/generated-resources/standards/jit/v200/jit-v2.0.0-openapi.yaml",
        jitStandardSpecification);
  }
}
