package org.dcsa.conformance.standards.eblinterop.models;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.With;
import org.dcsa.conformance.core.party.ScenarioParameters;
import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerFactory;

@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReceiverScenarioParameters(
    JsonNode receiverParty, String receiversX509SigningCertificateInPEMFormat)
    implements ScenarioParameters {

  public static ReceiverScenarioParameters fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, ReceiverScenarioParameters.class);
  }

  public void validate() {
    PayloadSignerFactory.verifierFromPemEncodedCertificate(
        receiversX509SigningCertificateInPEMFormat, "receiversX509SigningCertificateInPEMFormat");
  }
}
