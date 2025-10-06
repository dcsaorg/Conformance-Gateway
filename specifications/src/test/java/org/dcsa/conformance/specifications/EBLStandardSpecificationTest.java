package org.dcsa.conformance.specifications;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.specifications.standards.ebl.v300.EBLStandardSpecification;
import org.junit.jupiter.api.Test;

@Slf4j
class EBLStandardSpecificationTest {
  @Test
  void testEBLStandardSpecification() {
    EBLStandardSpecification eblStandardSpecification = new EBLStandardSpecification();

    StandardSpecificationTestToolkit.verifyTypeExport(
        "TransportDocument",
        "../ebl/src/main/resources/standards/ebl/schemas/EBL_v3.0.0.yaml",
        eblStandardSpecification);

    eblStandardSpecification.generateArtifacts();
  }

}
