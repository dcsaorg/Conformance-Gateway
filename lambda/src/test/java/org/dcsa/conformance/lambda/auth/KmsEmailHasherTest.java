package org.dcsa.conformance.lambda.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.GenerateMacRequest;
import software.amazon.awssdk.services.kms.model.GenerateMacResponse;
import software.amazon.awssdk.services.kms.model.MacAlgorithmSpec;

class KmsEmailHasherTest {
  @Test
  void usesTheInventoryHmacContract() {
    var kms = mock(KmsClient.class);
    when(kms.generateMac(any(GenerateMacRequest.class)))
        .thenReturn(
            GenerateMacResponse.builder()
                .mac(SdkBytes.fromByteArray(new byte[] {0, 15, (byte) 255}))
                .build());
    assertEquals(
        "000fff", new KmsEmailHasher(kms, "deployment-key").apply("alice+tag@example.com"));
    var request = ArgumentCaptor.forClass(GenerateMacRequest.class);
    verify(kms).generateMac(request.capture());
    assertEquals("deployment-key", request.getValue().keyId());
    assertEquals(MacAlgorithmSpec.HMAC_SHA_256, request.getValue().macAlgorithm());
    assertEquals("alice+tag@example.com", request.getValue().message().asUtf8String());
  }
}
