package org.dcsa.conformance.standards.eblinterop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

  public static LinkedHashMap<String, PintScenarioListBuilder> createModuleScenarioListBuilders(
    String standardVersion, String sendingPlatformPartyName, String receivingPlatformPartyName) {
    STANDARD_VERSION.set(standardVersion);
    SENDING_PLATFORM_PARTY_NAME.set(sendingPlatformPartyName);
    RECEIVING_PLATFORM_PARTY_NAME.set(receivingPlatformPartyName);
    return Stream.of(
        Map.entry(
          "Transfer scenarios",
          noAction().thenEither(
            supplySenderTransferScenarioParameters(0).thenEither(
              receiverStateSetup(ScenarioClass.NO_ISSUES)
                .thenEither(
                  initiateAndCloseTransferAction(PintResponseCode.RECE).thenEither(
                    noAction(),
                    retryTransfer(PintResponseCode.DUPE, SenderTransmissionClass.VALID_TRANSFER),
                    resignLatestEntry().then(
                      retryTransfer(PintResponseCode.DUPE, SenderTransmissionClass.VALID_TRANSFER)
                    ),
                    manipulateLatestTransactionParameters().then(
                      retryTransfer(PintResponseCode.DISE, SenderTransmissionClass.VALID_TRANSFER)
                    )),
                  initiateAndCloseTransferAction(PintResponseCode.RECE, SenderTransmissionClass.VALID_TRANSFER).thenEither(
                    noAction(),
                    resignLatestEntry().then(
                      retryTransfer(PintResponseCode.DUPE, SenderTransmissionClass.VALID_TRANSFER)
                    ),
                    manipulateLatestTransactionParameters().then(
                      retryTransfer(PintResponseCode.DISE, SenderTransmissionClass.VALID_TRANSFER)
                    )
                  ),
                  initiateAndCloseTransferAction(PintResponseCode.BSIG, SenderTransmissionClass.SIGNATURE_ISSUE).then(
                    initiateAndCloseTransferAction(PintResponseCode.RECE)
                  ),
                  initiateAndCloseTransferAction(PintResponseCode.BENV, SenderTransmissionClass.WRONG_RECIPIENT_PLATFORM)
                ),
              receiverStateSetup(ScenarioClass.INVALID_RECIPIENT).then(
                initiateAndCloseTransferAction(PintResponseCode.BENV)
              ),
              receiverStateSetup(ScenarioClass.FAIL_W_503).then(
                initiateTransferUnsignedFailure(503, SenderTransmissionClass.VALID_TRANSFER).thenEither(
                  resetScenarioClass(ScenarioClass.NO_ISSUES).then(
                    initiateAndCloseTransferAction(PintResponseCode.RECE)),
                  initiateTransferUnsignedFailure(503, SenderTransmissionClass.VALID_TRANSFER).then(
                    resetScenarioClass(ScenarioClass.NO_ISSUES).then(
                      initiateAndCloseTransferAction(PintResponseCode.RECE)))
                ))
            ),
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
                      ),
                      resetScenarioClass(ScenarioClass.FAIL_W_503).then(
                        transferDocumentReceiverFailure().then(
                          resetScenarioClass(ScenarioClass.NO_ISSUES).thenEither(
                            transferDocument().then(closeTransferAction(PintResponseCode.RECE)),
                            retryTransfer(1, SenderTransmissionClass.VALID_TRANSFER).then(
                              transferDocument().thenEither(
                                closeTransferAction(PintResponseCode.RECE),
                                retryTransfer(PintResponseCode.RECE, SenderTransmissionClass.VALID_TRANSFER)
                              )
                            )
                          )
                        )
                      ),
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
          )),
      Map.entry("Receiver validation scenarios",
        supplyReceiverValidationScenarioParameters()
          .then(receiverValidation())
        ))
      .collect(
        Collectors.toMap(
          Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }


  private PintScenarioListBuilder(
      Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static PintScenarioListBuilder supplyReceiverValidationScenarioParameters() {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
      previousAction ->
        new SupplyValidationEndpointScenarioParametersAction(
          sendingPlatform,
          receivingPlatform,
          (PintAction) previousAction
        ));
  }

  private static PintScenarioListBuilder receiverValidation() {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
      previousAction ->
        new PintReceiverValidationAction(
          sendingPlatform,
          receivingPlatform,
          (PintAction) previousAction,
          200,
          resolveMessageSchemaValidator(RECEIVER_VALIDATION_RESPONSE)
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

  private static PintScenarioListBuilder manipulateLatestTransactionParameters() {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
      previousAction ->
        new ManipulateTransactionsAction(
          receivingPlatform,
          sendingPlatform,
          (PintAction) previousAction
        ));
  }

  private static PintScenarioListBuilder resignLatestEntry() {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
      previousAction ->
        new ResignLatestEntryAction(
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

  private static PintScenarioListBuilder resetScenarioClass(ScenarioClass scenarioClass) {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
      previousAction -> new ResetScenarioClassAction(
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


  private static PintScenarioListBuilder initiateTransferUnsignedFailure(int expectedStatus, SenderTransmissionClass senderTransmissionClass) {
    String sendingPlatform = SENDING_PLATFORM_PARTY_NAME.get();
    String receivingPlatform = RECEIVING_PLATFORM_PARTY_NAME.get();
    return new PintScenarioListBuilder(
        previousAction ->
            new PintInitiateTransferUnsignedErrorAction(
                receivingPlatform,
                sendingPlatform,
                (PintAction) previousAction,
                expectedStatus,
                senderTransmissionClass,
                resolveMessageSchemaValidator(ENVELOPE_REQUEST_SCHEMA),
                resolveMessageSchemaValidator(ENVELOPE_MANIFEST_SCHEMA),
                resolveMessageSchemaValidator(ENVELOPE_TRANSFER_CHAIN_ENTRY_SCHEMA),
                resolveMessageSchemaValidator(ISSUANCE_MANIFEST_SCHEMA)));
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
    String schemaFilePath = "/standards/pint/schemas/pint-%s.json".formatted(standardVersion);
    return JsonSchemaValidator.getInstance(schemaFilePath, schemaName);
  }
}
