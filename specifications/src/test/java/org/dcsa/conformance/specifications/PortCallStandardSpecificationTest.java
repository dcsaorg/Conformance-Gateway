package org.dcsa.conformance.specifications;

import org.dcsa.conformance.specifications.standards.portcall.v200.PortCallStandardSpecification;
import org.dcsa.conformance.specifications.standards.portcall.v200.model.Event;
import org.junit.jupiter.api.Test;

class PortCallStandardSpecificationTest {
  @Test
  void testPortCallStandardSpecification() {
    PortCallStandardSpecification portCallStandardSpecificationStandardSpecification = new PortCallStandardSpecification();
    portCallStandardSpecificationStandardSpecification.generateArtifacts();

    StandardSpecificationTestToolkit.verifyTypeExport(
        Event.class.getSimpleName(),
        "../specifications/generated-resources/standards/portcall/v200/port-call-v2.0.0-openapi.yaml",
        portCallStandardSpecificationStandardSpecification);
  }
}
