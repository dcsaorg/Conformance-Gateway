package org.dcsa.conformance.standards.eblinterop.models;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.With;
import org.dcsa.conformance.core.party.ScenarioParameters;
import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerFactory;

@With
public record SenderScenarioParameters(
    String transportDocumentReference,
    String eblPlatform,
    String sendersX509SigningCertificateInPEMFormat,
    String carriersX509SigningCertificateInPEMFormat)
    implements ScenarioParameters {

  public static SenderScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, SenderScenarioParameters.class);
  }

  public void validate() {
    PayloadSignerFactory.verifierFromPemEncodedCertificate(
      carriersX509SigningCertificateInPEMFormat,
      "carriersX509SigningCertificateInPEMFormat"
    );
    PayloadSignerFactory.verifierFromPemEncodedCertificate(
      sendersX509SigningCertificateInPEMFormat,
      "sendersX509SigningCertificateInPEMFormat"
    );
  }
}
