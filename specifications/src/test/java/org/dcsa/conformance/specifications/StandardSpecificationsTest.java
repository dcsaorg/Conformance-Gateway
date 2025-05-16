package org.dcsa.conformance.specifications;

import org.dcsa.conformance.specifications.standards.an.v100.ANStandardSpecification;
import org.junit.jupiter.api.Test;

class StandardSpecificationsTest {
  @Test
  void testANStandardSpecification() {
    new ANStandardSpecification().generateArtifacts();
  }
}
