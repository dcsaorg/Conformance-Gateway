package org.dcsa.conformance.standards.eblinterop.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import io.setl.json.Canonical;
import io.setl.json.jackson.Convert;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public class Checksums {

  @SneakyThrows
  public static String sha256CanonicalJson(JsonNode node) {
    var digester = MessageDigest.getInstance("SHA256");
    var digestBytes = digester.digest(((Canonical) Convert.toJson(node)).toCanonicalString().getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digestBytes);
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
