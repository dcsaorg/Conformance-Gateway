package org.dcsa.conformance.specifications;

import org.dcsa.conformance.specifications.standards.an.v100.ANStandardSpecification;
import org.junit.jupiter.api.Test;

class ANStandardSpecificationTest {
  @Test
  void testANStandardSpecification() {
    new ANStandardSpecification().generateArtifacts();
  }
}
