package org.dcsa.conformance.core.check;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;

public class ResponseStatusCheck extends ActionCheck {

  private final Set<Integer> expectedResponseStatus;
  private final boolean anySuccessfulResponse;

  public static ResponseStatusCheck forSuccessfulResponse(Predicate<String> isRelevantForRoleName, UUID matchedExchangeUuid) {
    return forSuccessfulResponse("", isRelevantForRoleName, matchedExchangeUuid);
  }

  public static ResponseStatusCheck forSuccessfulResponse(String titlePrefix, Predicate<String> isRelevantForRoleName, UUID matchedExchangeUuid) {
    return new ResponseStatusCheck(titlePrefix, isRelevantForRoleName, matchedExchangeUuid, Collections.emptySet(), true);
  }

  public ResponseStatusCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      int expectedResponseStatus) {
    this("", isRelevantForRoleName, matchedExchangeUuid, expectedResponseStatus);
  }

  public ResponseStatusCheck(
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    Set<Integer> expectedResponseStatus) {
    this("", isRelevantForRoleName, matchedExchangeUuid, expectedResponseStatus);
  }

  public ResponseStatusCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      Set<Integer> expectedResponseStatus) {
    this(titlePrefix, isRelevantForRoleName, matchedExchangeUuid, expectedResponseStatus, false);
  }

  private ResponseStatusCheck(
      String titlePrefix,
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      Set<Integer> expectedResponseStatus,
      boolean anySuccessfulResponse) {
    super(
        titlePrefix,
        "The HTTP response status is correct",
        isRelevantForRoleName,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE);
    this.expectedResponseStatus = Set.copyOf(expectedResponseStatus);
    this.anySuccessfulResponse = anySuccessfulResponse;
  }

  public ResponseStatusCheck(
    String titlePrefix,
    Predicate<String> isRelevantForRoleName,
    UUID matchedExchangeUuid,
    int expectedResponseStatus) {
    super(
      titlePrefix,
      "The HTTP response status is correct",
      isRelevantForRoleName,
      matchedExchangeUuid,
      HttpMessageType.RESPONSE);
    this.expectedResponseStatus = Set.of(expectedResponseStatus);
    this.anySuccessfulResponse = false;
  }

  private String validationErrorMessage(int actualResponseStatus) {
    if (anySuccessfulResponse) {
      return "Response status '%d' is not a successful 2xx status".formatted(actualResponseStatus);
    }
    if (expectedResponseStatus.size() == 1) {
      var expectedResponseStatus = this.expectedResponseStatus.iterator().next();
      return "Response status '%d' does not match the expected value '%d'"
        .formatted(actualResponseStatus, expectedResponseStatus);
    }
    var values = expectedResponseStatus.stream().sorted()
      .map(String::valueOf)
      .collect(Collectors.joining(", ", "'", "'"));
    return "Response status '%d' does not match one of the expected values: %s"
      .formatted(actualResponseStatus, values);
  }

  @Override
  protected ConformanceCheckResult performCheck(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return ConformanceCheckResult.simple(Collections.emptySet());
    int responseStatus = exchange.getResponse().statusCode();
    boolean statusMatches = anySuccessfulResponse
      ? responseStatus >= 200 && responseStatus < 300
      : expectedResponseStatus.contains(responseStatus);
    return ConformanceCheckResult.simple(statusMatches
        ? Collections.emptySet()
        : Set.of(validationErrorMessage(responseStatus)));
  }
}
