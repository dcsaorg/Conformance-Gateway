package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.booking.checks.CarrierStatusScenario;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.stream.Stream;

@Getter
public class UC14CarrierProcessBookingCancellationAction extends CarrierNotificationBookingAction {

  private final JsonSchemaValidator requestSchemaValidator;
  private final boolean confirmCancellation;

  public UC14CarrierProcessBookingCancellationAction(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    JsonSchemaValidator requestSchemaValidator,
    boolean isWithNotifications) {
    this(
      carrierPartyName,
      shipperPartyName,
      previousAction,
      requestSchemaValidator,
      isWithNotifications,
      true);
  }

  public UC14CarrierProcessBookingCancellationAction(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    JsonSchemaValidator requestSchemaValidator,
    boolean isWithNotifications,
    boolean confirmCancellation) {
    super(
      carrierPartyName,
      shipperPartyName,
      previousAction,
      confirmCancellation ? "UC14 (Confirm)" : "UC14 (Decline)",
      204,
      isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
    this.confirmCancellation = confirmCancellation;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
      confirmCancellation ? "prompt-carrier-uc14.md" : "prompt-carrier-uc14-decline.md",
      "prompt-carrier-notification.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    var dsp = getDspSupplier().get();
    return jsonNode
      .put("cbr", dsp.carrierBookingReference())
      .put("confirmCancellation", confirmCancellation);
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return getSimpleNotificationChecks(
          expectedApiVersion,
          requestSchemaValidator,
          CarrierStatusScenario.from(
            confirmCancellation ? BookingState.CANCELLED : BookingState.CONFIRMED,
            null,
            confirmCancellation ? BookingCancellationState.CANCELLATION_CONFIRMED : BookingCancellationState.CANCELLATION_DECLINED
          ));
      }
    };
  }
}
