package org.dcsa.conformance.specifications;

import org.dcsa.conformance.specifications.standards.vgm.v100.VGMStandardSpecification;
import org.dcsa.conformance.specifications.standards.vgm.v100.model.VGM;
import org.junit.jupiter.api.Test;

class VGMStandardSpecificationTest {
  @Test
  void testVGMStandardSpecification() {
    VGMStandardSpecification vgmStandardSpecification = new VGMStandardSpecification();
    vgmStandardSpecification.generateArtifacts();

    StandardSpecificationTestToolkit.verifyTypeExport(
        VGM.class.getSimpleName(),
        "../specifications/generated-resources/standards/vgm/v100/vgm-v1.0.0-openapi.yaml",
        vgmStandardSpecification);
  }
}
