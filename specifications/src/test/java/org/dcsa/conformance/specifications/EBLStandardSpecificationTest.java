package org.dcsa.conformance.specifications;

import org.dcsa.conformance.specifications.standards.ebl.v300.EBLStandardSpecification;
import org.junit.jupiter.api.Test;

class EBLStandardSpecificationTest {
  @Test
  void testEBLStandardSpecification() {
    new EBLStandardSpecification().generateArtifacts();
  }
}
