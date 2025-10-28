package org.dcsa.conformance.core.scenario;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.ConformanceResult;
import org.dcsa.conformance.core.state.StatefulEntity;
import org.dcsa.conformance.core.traffic.ConformanceExchange;

@Getter
@Slf4j
public abstract class ConformanceAction implements StatefulEntity {

  private final String sourcePartyName;
  private final String targetPartyName;
  protected final ConformanceAction previousAction;
  private String actionPath;
  private String actionTitle;

  protected volatile UUID id = UUID.randomUUID();
  private volatile UUID matchedExchangeUuid;
  private volatile UUID matchedNotificationExchangeUuid;
  private volatile String exchangeHandlingExceptionMessage;
  private volatile String notificationHandlingExceptionMessage;

  protected ConformanceAction(
      String sourcePartyName,
      String targetPartyName,
      ConformanceAction previousAction,
      String actionTitle) {
    this.sourcePartyName = sourcePartyName;
    this.targetPartyName = targetPartyName;
    this.previousAction = previousAction;
    this.actionTitle = actionTitle;
    this.actionPath =
        (previousAction == null ? "" : previousAction.actionPath + " - ") + this.actionTitle;
  }

  public void reset() {
    id = UUID.randomUUID();
    matchedExchangeUuid = null;
    matchedNotificationExchangeUuid = null;
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = OBJECT_MAPPER.createObjectNode();
    jsonState.put("id", id.toString());
    if (matchedExchangeUuid != null) {
      jsonState.put("matchedExchangeUuid", matchedExchangeUuid.toString());
    }
    if (matchedNotificationExchangeUuid != null) {
      jsonState.put("matchedNotificationExchangeUuid", matchedNotificationExchangeUuid.toString());
    }
    if (exchangeHandlingExceptionMessage != null) {
      jsonState.put("exchangeHandlingExceptionMessage", exchangeHandlingExceptionMessage);
    }
    if (notificationHandlingExceptionMessage != null) {
      jsonState.put("notificationHandlingExceptionMessage", notificationHandlingExceptionMessage);
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    id = UUID.fromString(jsonState.get("id").asText());
    if (jsonState.has("matchedExchangeUuid")) {
      this.matchedExchangeUuid = UUID.fromString(jsonState.get("matchedExchangeUuid").asText());
    }
    if (jsonState.has("matchedNotificationExchangeUuid")) {
      this.matchedNotificationExchangeUuid =
          UUID.fromString(jsonState.get("matchedNotificationExchangeUuid").asText());
    }
    if (jsonState.has("exchangeHandlingExceptionMessage")) {
      this.exchangeHandlingExceptionMessage =
          jsonState.get("exchangeHandlingExceptionMessage").asText();
    }
    if (jsonState.has("notificationHandlingExceptionMessage")) {
      this.notificationHandlingExceptionMessage =
          jsonState.get("notificationHandlingExceptionMessage").asText();
    }
  }

  public abstract String getHumanReadablePrompt();

  public JsonNode getJsonForHumanReadablePrompt() {
    return null;
  }

  public boolean isConfirmationRequired() {
    return false;
  }

  /**
   * Whether the action requires input from the user. Overwrite this method to return true if required.
   *
   * @return true if the action requires input.
   */
  public boolean isInputRequired() {
    return false;
  }

  public boolean isMissingMatchedExchange() {
    return matchedExchangeUuid == null;
  }

  /**
   * Saves the matched UUIDs and captures dynamic parameters like document references.
   *
   * @param exchange the regular or notification exchange to handle
   * @return true if all expected exchanges were handled, false otherwise
   */
  public final boolean handleExchange(ConformanceExchange exchange) {
    String exchangeSourcePartyName = exchange.getRequest().message().sourcePartyName();
    if (Objects.equals(exchangeSourcePartyName, sourcePartyName)) {
      log.info(
          "ConformanceAction.handleExchange() %s '%s' handling regular exchange: %s"
              .formatted(
                  getClass().getSimpleName(),
                  getActionTitle(),
                  exchange.toJson().toPrettyString()));
      matchedExchangeUuid = exchange.getUuid();
      try {
        this.exchangeHandlingExceptionMessage = null;
        doHandleExchange(exchange);
      } catch (RuntimeException e) {
        this.exchangeHandlingExceptionMessage = e.getMessage();
        log.warn("Unhandled doHandleExchange() exception: {}", e, e);
      }
    } else if (Objects.equals(exchangeSourcePartyName, targetPartyName)) {
      log.info(
          "ConformanceAction.handleExchange() %s '%s' handling notification exchange: %s"
              .formatted(
                  getClass().getSimpleName(),
                  getActionTitle(),
                  exchange.toJson().toPrettyString()));
      matchedNotificationExchangeUuid = exchange.getUuid();
      try {
        this.notificationHandlingExceptionMessage = null;
        doHandleNotificationExchange(exchange);
      } catch (RuntimeException e) {
        this.notificationHandlingExceptionMessage = e.getMessage();
        log.warn("Unhandled doHandleNotificationExchange() exception: {}", e, e);
      }
    } else {
      log.info(
          "ConformanceAction.handleExchange() %s '%s' ignoring exchange: %s"
              .formatted(
                  getClass().getSimpleName(),
                  getActionTitle(),
                  exchange.toJson().toPrettyString()));
    }

    boolean allHandled =
        matchedExchangeUuid != null
            && (matchedNotificationExchangeUuid != null || !expectsNotificationExchange());
    log.info(
      "ConformanceAction.handleExchange() %s '%s' matchedExchangeUuid='%s' matchedNotificationExchangeUuid='%s' expectsNotificationExchange='%s' allHandled='%s'"
        .formatted(
          getClass().getSimpleName(),
          getActionTitle(),
          matchedExchangeUuid,
          matchedNotificationExchangeUuid,
          expectsNotificationExchange(),
          allHandled));
    return allHandled;
  }

  protected void doHandleExchange(ConformanceExchange exchange) {}

  protected boolean expectsNotificationExchange() {
    return false;
  }

  @SuppressWarnings("unused")
  protected void doHandleNotificationExchange(ConformanceExchange exchange) {}

  public final ConformanceCheck createFullCheck(String expectedApiVersion) {
    if (this.exchangeHandlingExceptionMessage == null
        && this.notificationHandlingExceptionMessage == null) {
      return createCheck(expectedApiVersion);
    }
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        // Not filtering by role: even when it's not the adopter system's fault,
        // the operator must still see that there was an error.
        return Stream.of(
                exchangeHandlingExceptionMessage == null
                    ? null
                    : new ConformanceCheck("Exchange handling exception") {
                      @Override
                      protected void doCheck(
                          Function<UUID, ConformanceExchange> getExchangeByUuid) {
                        addResult(
                            ConformanceResult.forSourceParty(
                                Set.of(exchangeHandlingExceptionMessage)));
                      }
                    },
                notificationHandlingExceptionMessage == null
                    ? null
                    : new ConformanceCheck("Notification handling exception") {
                      @Override
                      protected void doCheck(
                          Function<UUID, ConformanceExchange> getExchangeByUuid) {
                        addResult(
                            ConformanceResult.forTargetParty(
                                Set.of(notificationHandlingExceptionMessage)));
                      }
                    },
                createCheck(expectedApiVersion))
            .filter(Objects::nonNull);
      }
    };
  }

  public ConformanceCheck createCheck(String expectedApiVersion) {
    return null;
  }

  public void handlePartyInput(JsonNode partyInput) throws UserFacingException {
    JsonNode jsonForHumanReadablePrompt = getJsonForHumanReadablePrompt();
    if (jsonForHumanReadablePrompt != null) {
      Set<String> expectedAttributes =
          StreamSupport.stream(
                  Spliterators.spliteratorUnknownSize(
                      jsonForHumanReadablePrompt.fieldNames(), Spliterator.ORDERED),
                  false)
              .collect(Collectors.toSet());
      if (!expectedAttributes.isEmpty()) { // because "empty" means "any input is allowed"
        Set<String> providedAttributes =
            StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(
                        partyInput.get("input").fieldNames(), Spliterator.ORDERED),
                    false)
                .collect(Collectors.toSet());
        Set<String> missingAttributes = new HashSet<>(expectedAttributes);
        missingAttributes.removeAll(providedAttributes);
        if (!missingAttributes.isEmpty()) {
          throw new UserFacingException(
            "The input must contain: %s".formatted(String.join(", ", missingAttributes)));
        }
        Set<String> unexpectedAttributes = new HashSet<>(providedAttributes);
        unexpectedAttributes.removeAll(expectedAttributes);
        if (!unexpectedAttributes.isEmpty()) {
          throw new UserFacingException(
            "The input may not contain: %s".formatted(String.join(", ", unexpectedAttributes)));
        }
      }
    }
    doHandlePartyInput(partyInput);
  }

  protected void doHandlePartyInput(JsonNode partyInput) throws UserFacingException {}

  public ObjectNode asJsonNode() {
    return OBJECT_MAPPER
        .createObjectNode()
        .put("actionId", id.toString())
        .put("actionType", getClass().getCanonicalName())
        .put("actionPath", actionPath);
  }

  public Set<String> skippableForRoles(){
    return Collections.emptySet();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ConformanceAction that = (ConformanceAction) o;
    return Objects.equals(id, that.id);
  }

  public String[] buildFullUris(String uri, String... uriReference) {
    if (uriReference == null || uriReference.length == 0) {
      return new String[] {uri};
    }
    return Arrays.stream(uriReference)
      .filter(Objects::nonNull)
      .filter(Predicate.not(String::isBlank))
      .map(ref -> uri + ref)
      .toArray(String[]::new);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public ConformanceAction withTitleComplement(String complement) {
    this.actionTitle = complement + " " + this.actionTitle;
    int lastDashIndex = this.actionPath.lastIndexOf(" - ");
    this.actionPath =
        this.actionPath.substring(0, lastDashIndex)
            + " - "
            + complement
            + " "
            + this.actionPath.substring(lastDashIndex + 3);
    return this;
  }
}
