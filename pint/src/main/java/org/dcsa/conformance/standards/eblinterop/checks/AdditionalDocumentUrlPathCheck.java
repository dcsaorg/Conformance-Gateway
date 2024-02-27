package org.dcsa.conformance.standards.eblinterop.checks;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class AdditionalDocumentUrlPathCheck extends ActionCheck {

  private static final Pattern URL_PATTERN = Pattern.compile("/envelopes/([^/]++)/additional-documents/[0-9a-fA-F]{64}/?$");

  private final Supplier<String> envelopeReferenceSupplier;

  public AdditionalDocumentUrlPathCheck(
    Predicate<String> isRelevantForRoleName, UUID matchedExchangeUuid, Supplier<String> envelopeReferenceSupplier) {
    this("", isRelevantForRoleName, matchedExchangeUuid, envelopeReferenceSupplier);
  }

  public AdditionalDocumentUrlPathCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      Supplier<String> envelopeReferenceSupplier) {
    super(
        titlePrefix,
        "The URL path of the HTTP request is correct",
        isRelevantForRoleName,
        matchedExchangeUuid,
        HttpMessageType.REQUEST);
    this.envelopeReferenceSupplier = envelopeReferenceSupplier;
  }

  @Override
  protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return Set.of();
    String requestUrl = exchange.getRequest().url();
    var m = URL_PATTERN.matcher(requestUrl);
    var ok = false;
    var expectedReference = envelopeReferenceSupplier.get();
    if (expectedReference == null) {
      throw new AssertionError("Missing expected envelopeReference");
    }
    if (m.find()) {
      var reference = m.group(1);
      ok = expectedReference.equals(reference);
    }

    return ok
        ? Collections.emptySet()
        : Set.of("Request URL '%s' does not match '/envelopes/%s/additional-documents/{sha256checksum}'".formatted(
          requestUrl, expectedReference));
  }
}
