package org.dcsa.conformance.standards.ebl.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import lombok.SneakyThrows;
import org.erdtman.jcs.JsonCanonicalizer;

public class Checksums {

  @SneakyThrows
  public static String sha256CanonicalJson(JsonNode node) {
    return sha256(new JsonCanonicalizer(node.toString()).getEncodedString());
  }

  @SneakyThrows
  public static String sha256(String text) {
    return sha256(text.getBytes(StandardCharsets.UTF_8));
  }

  @SneakyThrows
  public static String sha256(byte[] data) {
    var digester = MessageDigest.getInstance("SHA256");
    var digestBytes = digester.digest(data);
    return HexFormat.of().formatHex(digestBytes);
  }

}
