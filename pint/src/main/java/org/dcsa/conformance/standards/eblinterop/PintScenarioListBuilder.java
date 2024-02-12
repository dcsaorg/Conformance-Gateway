package org.dcsa.conformance.standards.eblinterop;

import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.eblinterop.action.*;

@Slf4j
public class PintScenarioListBuilder
    extends ScenarioListBuilder<PintScenarioListBuilder> {

  private static final ThreadLocal<String> STANDARD_VERSION = new ThreadLocal<>();
  private static final ThreadLocal<String> SENDING_PLATFORM_PARTY_NAME = new ThreadLocal<>();
  private static final ThreadLocal<String> RECEIVING_PLATFORM_PARTY_NAME = new ThreadLocal<>();

  private static final String ENVELOPE_REQUEST_SCHEMA = "EblEnvelope";
  private static final String TRANSFER_FINISHED_SIGNED_RESPONSE_SCHEMA = "EnvelopeTransferFinishedResponseSignedContent";

  private static final String TRANSFER_STARTED_UNSIGNED_RESPONSE_SCHEMA = "EnvelopeTransferStartedResponse";

  private static final String ENVELOPE_MANIFEST_SCHEMA = "EnvelopeManifest";
  private static final String ENVELOPE_TRANSFER_CHAIN_ENTRY_SCHEMA = "EnvelopeTransferChainEntry";

  public static PintScenarioListBuilder buildTree(
      String standardVersion,
      String sendingPlatformPartyName,
      String receivingPlatformPartyName) {
    STANDARD_VERSION.set(standardVersion);
    SENDING_PLATFORM_PARTY_NAME.set(sendingPlatformPartyName);
    RECEIVING_PLATFORM_PARTY_NAME.set(receivingPlatformPartyName);
    return supplyScenarioParameters().thenEither(
      receiverStateSetup(ScenarioClass.NO_ISSUES)
        .then(initiateTransferRequest(PintResponseCode.RECE).thenEither(
          noAction(),
          initiateTransferRequest(PintResponseCode.DUPE)
        )),

      receiverStateSetup(ScenarioClass.INVALID_RECIPIENT).then(
        initiateTransferRequest(PintResponseCode.BENV)
      )/*,
      scenarioType(ScenarioClass.FINISH_IMMEDIATELY, PintResponseCode.BENV)*/
    );
  }


  private PintScenarioListBuilder(
      Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }


  private static PintScenarioListBuilder supplyScenarioParameters() {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
        previousAction ->
            new SenderSupplyScenarioParametersAction(
                receivingPlatform,
                sendingPlatform,
                (PintAction) previousAction
            ));
  }

  private static PintScenarioListBuilder receiverStateSetup(ScenarioClass scenarioClass) {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
        previousAction -> new ReceiverSupplyScenarioParametersAndStateSetupAction(
            receivingPlatform,
            sendingPlatform,
            (PintAction) previousAction,
            scenarioClass));
  }

  private static PintScenarioListBuilder noAction() {
    return new PintScenarioListBuilder(null);
  }

  private static PintScenarioListBuilder initiateTransferRequest(PintResponseCode signedResponseCode) {
    return _issuanceRequest(signedResponseCode);
  }

  private static PintScenarioListBuilder _issuanceRequest(
    PintResponseCode signedResponseCode
  ) {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
        previousAction ->
            new PintInitiateTransferAction(
                receivingPlatform,
                sendingPlatform,
                (PintAction) previousAction,
                signedResponseCode,
                resolveMessageSchemaValidator(ENVELOPE_REQUEST_SCHEMA),
                resolveMessageSchemaValidator(TRANSFER_FINISHED_SIGNED_RESPONSE_SCHEMA),
                resolveMessageSchemaValidator(ENVELOPE_MANIFEST_SCHEMA),
                resolveMessageSchemaValidator(ENVELOPE_TRANSFER_CHAIN_ENTRY_SCHEMA)
              ));
  }


  private static JsonSchemaValidator resolveMessageSchemaValidator(String schemaName) {
    var standardVersion = STANDARD_VERSION.get();
    String schemaFilePath = "/standards/pint/schemas/pint-%s.json".formatted(standardVersion);
    return JsonSchemaValidator.getInstance(schemaFilePath, schemaName);
  }
}
