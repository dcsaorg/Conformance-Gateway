package org.dcsa.conformance.standards.eblinterop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.eblinterop.action.*;

@Slf4j
class PintScenarioListBuilder extends ScenarioListBuilder<PintScenarioListBuilder> {

  private static final ThreadLocal<String> STANDARD_VERSION = new ThreadLocal<>();
  private static final ThreadLocal<String> SENDING_PLATFORM_PARTY_NAME = new ThreadLocal<>();
  private static final ThreadLocal<String> RECEIVING_PLATFORM_PARTY_NAME = new ThreadLocal<>();

  private static final String ENVELOPE_REQUEST_SCHEMA = "EblEnvelope";
  private static final String TRANSFER_FINISHED_SIGNED_RESPONSE_SCHEMA = "EnvelopeTransferFinishedResponseSignedContent";

  private static final String TRANSFER_STARTED_UNSIGNED_RESPONSE_SCHEMA = "EnvelopeTransferStartedResponse";

  private static final String ENVELOPE_MANIFEST_SCHEMA = "EnvelopeManifest";
  private static final String ENVELOPE_TRANSFER_CHAIN_ENTRY_SCHEMA = "EnvelopeTransferChainEntry";
  private static final String ISSUANCE_MANIFEST_SCHEMA = "IssuanceManifest";
  private static final String RECEIVER_VALIDATION_RESPONSE = "ReceiverValidationResponse";
  private static final String RECEIVER_VALIDATION_REQUEST = "IdentifyingCode";

  private static final String ERROR_RESPONSE = "ErrorResponse";

  public static LinkedHashMap<String, PintScenarioListBuilder> createModuleScenarioListBuilders(
    String standardVersion, String sendingPlatformPartyName, String receivingPlatformPartyName) {
    STANDARD_VERSION.set(standardVersion);
    SENDING_PLATFORM_PARTY_NAME.set(sendingPlatformPartyName);
    RECEIVING_PLATFORM_PARTY_NAME.set(receivingPlatformPartyName);
    var scenarios = new LinkedHashMap<String, PintScenarioListBuilder>();
    transferScenarios(scenarios);
    receiverValidationScenarios(scenarios);
    errorScenarios(scenarios);
    return scenarios;
  }

  private static void transferScenarios(Map<String, PintScenarioListBuilder> scenarios) {
    scenarios.put("Transfer scenarios: No additional documents",
      supplySenderTransferScenarioParameters(0).thenEither(
        receiverStateSetup(ScenarioClass.NO_ISSUES)
          .thenEither(
            initiateAndCloseTransferAction(PintResponseCode.RECE).thenEither(
              noAction(),
              retryTransfer(PintResponseCode.DUPE, SenderTransmissionClass.VALID_TRANSFER),
              retryTransfer(PintResponseCode.DUPE, SenderTransmissionClass.VALID_TRANSFER, RetryType.RESIGN)),
            initiateAndCloseTransferAction(PintResponseCode.RECE, SenderTransmissionClass.VALID_TRANSFER).thenEither(
              noAction(),
              retryTransfer(PintResponseCode.DUPE, SenderTransmissionClass.VALID_TRANSFER, RetryType.RESIGN)
            )
          )));
    scenarios.put("Transfer scenarios: With additional documents",
        supplySenderTransferScenarioParameters(2).thenEither(
          receiverStateSetup(ScenarioClass.NO_ISSUES)
            .then(
              initiateTransfer(2, SenderTransmissionClass.VALID_TRANSFER).thenEither(
                transferDocument().thenEither(
                  transferDocument().then(closeTransferAction(PintResponseCode.RECE)),
                  retryTransfer(1, SenderTransmissionClass.VALID_TRANSFER).then(
                    transferDocument().thenEither(
                      closeTransferAction(PintResponseCode.RECE),
                      retryTransfer(PintResponseCode.RECE, SenderTransmissionClass.VALID_TRANSFER)
                    )
                  )
                )
              )))
      );
  }

  private static void receiverValidationScenarios(Map<String, PintScenarioListBuilder> scenarios) {
    scenarios.put("Receiver validation scenarios", noAction().thenEither(
        supplyReceiverValidationScenarioParameters().then(receiverValidation(true)),
        supplyReceiverValidationScenarioParametersForUnknownParty().then(receiverValidation(false))
    ));
  }

  private static void errorScenarios(Map<String, PintScenarioListBuilder> scenarios) {
    scenarios.put("Error scenarios: Signature issues", supplySenderTransferScenarioParameters(0).thenEither(
      receiverStateSetup(ScenarioClass.NO_ISSUES)
        .then(
          initiateAndCloseTransferAction(PintResponseCode.BSIG, SenderTransmissionClass.SIGNATURE_ISSUE).then(
            initiateAndCloseTransferAction(PintResponseCode.RECE)
          ))
    ));
    scenarios.put("Error scenarios: Incorrect payloads issues", supplySenderTransferScenarioParameters(0).thenEither(
      receiverStateSetup(ScenarioClass.NO_ISSUES)
        .thenEither(
          initiateAndCloseTransferAction(PintResponseCode.BENV, SenderTransmissionClass.WRONG_RECIPIENT_PLATFORM),
          receiverStateSetup(ScenarioClass.INVALID_RECIPIENT).then(
            initiateAndCloseTransferAction(PintResponseCode.BENV)
          ),
          initiateAndCloseTransferAction(PintResponseCode.BENV, SenderTransmissionClass.INVALID_PAYLOAD)
        )));
    scenarios.put("Error scenarios: Dispute errors", supplySenderTransferScenarioParameters(0).then(
      receiverStateSetup(ScenarioClass.NO_ISSUES)
        .thenEither(
          initiateAndCloseTransferAction(PintResponseCode.RECE).then(
            retryTransfer(PintResponseCode.DISE, SenderTransmissionClass.VALID_TRANSFER, RetryType.MANIPULATE)),
          initiateAndCloseTransferAction(PintResponseCode.RECE, SenderTransmissionClass.VALID_TRANSFER).then(
            retryTransfer(PintResponseCode.DISE, SenderTransmissionClass.VALID_TRANSFER, RetryType.MANIPULATE)
          )
        )));
    scenarios.put("Error scenarios: Document transfer issue", supplySenderTransferScenarioParameters(2).thenEither(
      receiverStateSetup(ScenarioClass.NO_ISSUES)
        .then(
          initiateTransfer(2, SenderTransmissionClass.VALID_TRANSFER).thenEither(
            transferDocument().thenEither(
              transferDocument(SenderDocumentTransmissionTypeCode.CORRUPTED_DOCUMENT).then(
                retryTransfer(1, SenderTransmissionClass.VALID_TRANSFER).then(transferDocument().then(closeTransferAction(PintResponseCode.RECE)))
              ),
              transferDocument(SenderDocumentTransmissionTypeCode.UNRELATED_DOCUMENT).then(
                retryTransfer(1, SenderTransmissionClass.VALID_TRANSFER).then(transferDocument().then(closeTransferAction(PintResponseCode.RECE)))
              ),
              closeTransferAction(PintResponseCode.MDOC).then(
                retryTransfer(1, SenderTransmissionClass.VALID_TRANSFER).then(
                  transferDocument().thenEither(
                    closeTransferAction(PintResponseCode.RECE),
                    retryTransfer(PintResponseCode.RECE, SenderTransmissionClass.VALID_TRANSFER)
                  )
                )
              )
            )
          )))
    );
  }


  private PintScenarioListBuilder(
      Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static PintScenarioListBuilder supplyReceiverValidationScenarioParameters() {
    return supplyReceiverValidationScenarioParameters(true);
  }

  private static PintScenarioListBuilder supplyReceiverValidationScenarioParametersForUnknownParty() {
    return supplyReceiverValidationScenarioParameters(
      false
    );
  }

  private static PintScenarioListBuilder supplyReceiverValidationScenarioParameters(boolean isValid) {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
      previousAction ->
        new SupplyValidationEndpointScenarioParametersAction(
          sendingPlatform,
          receivingPlatform,
          (PintAction) previousAction,
          isValid
        ));
  }

  private static PintScenarioListBuilder receiverValidation(boolean isValid) {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
      previousAction ->
        new PintReceiverValidationAction(
          sendingPlatform,
          receivingPlatform,
          (PintAction) previousAction,
          isValid ? 200 : 404,
          resolveMessageSchemaValidator(RECEIVER_VALIDATION_REQUEST),
          resolveMessageSchemaValidator(isValid ? RECEIVER_VALIDATION_RESPONSE : ERROR_RESPONSE)
        ));
  }

  private static PintScenarioListBuilder supplySenderTransferScenarioParameters(int documentCount) {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
        previousAction ->
            new SenderSupplyScenarioParametersAction(
                receivingPlatform,
                sendingPlatform,
                (PintAction) previousAction,
                documentCount
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

  private static PintScenarioListBuilder transferDocument() {
    return transferDocument(SenderDocumentTransmissionTypeCode.VALID_DOCUMENT);
  }

  private static PintScenarioListBuilder transferDocument(SenderDocumentTransmissionTypeCode senderDocumentTransmissionTypeCode) {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
      previousAction -> new PintTransferAdditionalDocumentAction(
        receivingPlatform,
        sendingPlatform,
        (PintAction) previousAction,
        senderDocumentTransmissionTypeCode,
        resolveMessageSchemaValidator(TRANSFER_FINISHED_SIGNED_RESPONSE_SCHEMA)
      ));
  }


  private static PintScenarioListBuilder transferDocumentReceiverFailure() {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
      previousAction -> new PintTransferAdditionalDocumentFailureAction(
        receivingPlatform,
        sendingPlatform,
        (PintAction) previousAction,
        503
      ));
  }

  private static PintScenarioListBuilder noAction() {
    return new PintScenarioListBuilder(null);
  }

  private static PintScenarioListBuilder initiateTransfer(int expectedMissingDocumentCount, SenderTransmissionClass senderTransmissionClass) {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
        previousAction ->
            new PintInitiateTransferAction(
                receivingPlatform,
                sendingPlatform,
                (PintAction) previousAction,
                expectedMissingDocumentCount,
                senderTransmissionClass,
                resolveMessageSchemaValidator(ENVELOPE_REQUEST_SCHEMA),
                resolveMessageSchemaValidator(ENVELOPE_MANIFEST_SCHEMA),
                resolveMessageSchemaValidator(ENVELOPE_TRANSFER_CHAIN_ENTRY_SCHEMA),
                resolveMessageSchemaValidator(ISSUANCE_MANIFEST_SCHEMA),
                resolveMessageSchemaValidator(TRANSFER_STARTED_UNSIGNED_RESPONSE_SCHEMA)
                ));
  }

  private static PintScenarioListBuilder retryTransfer(int expectedMissingDocumentCount, SenderTransmissionClass senderTransmissionClass) {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
        previousAction ->
            new PintRetryTransferAction(
                receivingPlatform,
                sendingPlatform,
                (PintAction) previousAction,
                expectedMissingDocumentCount,
                senderTransmissionClass,
                resolveMessageSchemaValidator(ENVELOPE_REQUEST_SCHEMA),
                resolveMessageSchemaValidator(ENVELOPE_MANIFEST_SCHEMA),
                resolveMessageSchemaValidator(ENVELOPE_TRANSFER_CHAIN_ENTRY_SCHEMA),
                resolveMessageSchemaValidator(ISSUANCE_MANIFEST_SCHEMA),
                resolveMessageSchemaValidator(TRANSFER_STARTED_UNSIGNED_RESPONSE_SCHEMA)));
  }

  private static PintScenarioListBuilder retryTransfer(PintResponseCode pintResponseCode, SenderTransmissionClass senderTransmissionClass) {
    return retryTransfer(pintResponseCode, senderTransmissionClass, RetryType.NO_CHANGE);
  }

  private static PintScenarioListBuilder retryTransfer(PintResponseCode pintResponseCode, SenderTransmissionClass senderTransmissionClass, RetryType retryType) {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
        previousAction ->
            new PintRetryTransferAndCloseAction(
                receivingPlatform,
                sendingPlatform,
                (PintAction) previousAction,
                pintResponseCode,
                senderTransmissionClass,
                retryType,
                resolveMessageSchemaValidator(ENVELOPE_REQUEST_SCHEMA),
                resolveMessageSchemaValidator(ENVELOPE_MANIFEST_SCHEMA),
                resolveMessageSchemaValidator(ENVELOPE_TRANSFER_CHAIN_ENTRY_SCHEMA),
                resolveMessageSchemaValidator(ISSUANCE_MANIFEST_SCHEMA),
                resolveMessageSchemaValidator(TRANSFER_FINISHED_SIGNED_RESPONSE_SCHEMA)));
  }


  private static PintScenarioListBuilder initiateAndCloseTransferAction(PintResponseCode signedResponseCode) {
    return initiateAndCloseTransferAction(signedResponseCode, SenderTransmissionClass.VALID_ISSUANCE);
  }

  private static PintScenarioListBuilder initiateAndCloseTransferAction(PintResponseCode signedResponseCode, SenderTransmissionClass senderTransmissionClass) {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
        previousAction ->
            new PintInitiateAndCloseTransferAction(
                receivingPlatform,
                sendingPlatform,
                (PintAction) previousAction,
                signedResponseCode,
                senderTransmissionClass,
                resolveMessageSchemaValidator(ENVELOPE_REQUEST_SCHEMA),
                resolveMessageSchemaValidator(ENVELOPE_MANIFEST_SCHEMA),
                resolveMessageSchemaValidator(ENVELOPE_TRANSFER_CHAIN_ENTRY_SCHEMA),
                resolveMessageSchemaValidator(ISSUANCE_MANIFEST_SCHEMA),
                resolveMessageSchemaValidator(TRANSFER_FINISHED_SIGNED_RESPONSE_SCHEMA)
              ));
  }


  private static PintScenarioListBuilder closeTransferAction(PintResponseCode signedResponseCode) {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
        previousAction ->
            new PintCloseTransferAction(
                receivingPlatform,
                sendingPlatform,
                (PintAction) previousAction,
                signedResponseCode,
                resolveMessageSchemaValidator(TRANSFER_FINISHED_SIGNED_RESPONSE_SCHEMA)));
  }


  private static JsonSchemaValidator resolveMessageSchemaValidator(String schemaName) {
    var standardVersion = STANDARD_VERSION.get();
    String schemaFilePath = "/standards/pint/schemas/EBL_PINT_v%s.yaml".formatted(standardVersion);
    return JsonSchemaValidator.getInstance(schemaFilePath, schemaName);
  }
}
