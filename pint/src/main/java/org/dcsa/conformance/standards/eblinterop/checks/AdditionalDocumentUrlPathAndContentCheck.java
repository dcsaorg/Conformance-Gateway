package org.dcsa.conformance.standards.eblinterop.checks;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.crypto.Checksums;

public class AdditionalDocumentUrlPathAndContentCheck extends ActionCheck {

  private static final Pattern URL_PATTERN =
      Pattern.compile("/envelopes/([^/]++)/additional-documents/[0-9a-fA-F]{64}/?$");

  private final Supplier<String> envelopeReferenceSupplier;

  public AdditionalDocumentUrlPathAndContentCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      Supplier<String> envelopeReferenceSupplier) {
    this("", isRelevantForRoleName, matchedExchangeUuid, envelopeReferenceSupplier);
  }

  public AdditionalDocumentUrlPathAndContentCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      Supplier<String> envelopeReferenceSupplier) {
    super(
        titlePrefix,
        "Validate document checksum (body and URL) of the HTTP request is correct",
        isRelevantForRoleName,
        matchedExchangeUuid,
        HttpMessageType.REQUEST);
    this.envelopeReferenceSupplier = envelopeReferenceSupplier;
  }

  @Override
  protected ConformanceCheckResult performCheck(
      Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return ConformanceCheckResult.simple(Set.of());
    String requestUrl = exchange.getRequest().url();
    var m = URL_PATTERN.matcher(requestUrl);
    var issues = new LinkedHashSet<String>();
    var expectedReference = envelopeReferenceSupplier.get();
    var ok = false;
    if (expectedReference == null) {
      throw new AssertionError("Missing expected envelopeReference");
    }
    if (m.find()) {
      var reference = m.group(1);
      ok = expectedReference.equals(reference);
    }
    if (ok) {
      try {
        var bytes = exchange.getRequest().message().body().getJsonBody().binaryValue();
        var checksum = Checksums.sha256(bytes);
        var urlLc = requestUrl.toLowerCase().replaceAll("/++$", "");
        var idx = urlLc.lastIndexOf('/');
        var urlChecksum = urlLc.substring(idx + 1);
        if (!urlChecksum.equals(checksum)) {
          issues.add(
              "The decoded payload had checksum '%s' but according to the URL it should have had checksum '%s'"
                  .formatted(checksum, urlChecksum));
        }
      } catch (IOException e) {
        issues.add("Could not parse the payload as a base64 encoded byte sequence");
      }
    } else {
      issues.add(
          "Request URL '%s' does not match '/envelopes/%s/additional-documents/{sha256checksum}'"
              .formatted(requestUrl, expectedReference));
    }

    return ConformanceCheckResult.simple(issues);
  }
}
