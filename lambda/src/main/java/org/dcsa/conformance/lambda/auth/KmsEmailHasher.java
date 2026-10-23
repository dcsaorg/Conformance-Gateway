package org.dcsa.conformance.lambda.auth;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.GenerateMacRequest;
import software.amazon.awssdk.services.kms.model.MacAlgorithmSpec;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.function.UnaryOperator;

/**
 * Produces the same non-exportable-key HMAC used by the migration inventory tool.
 */
public final class KmsEmailHasher implements UnaryOperator<String> {

  private final KmsClient kms;
  private final String keyArn;

  public KmsEmailHasher(KmsClient kms, String keyArn) {
    this.kms = kms;
    this.keyArn = keyArn;
  }

  @Override
  public String apply(String normalizedEmail) {
    var request = GenerateMacRequest.builder()
      .keyId(keyArn)
      .macAlgorithm(MacAlgorithmSpec.HMAC_SHA_256)
      .message(SdkBytes.fromByteArray(normalizedEmail.getBytes(StandardCharsets.UTF_8)))
      .build();
    return HexFormat.of().formatHex(kms.generateMac(request).mac().asByteArray());
  }
}
