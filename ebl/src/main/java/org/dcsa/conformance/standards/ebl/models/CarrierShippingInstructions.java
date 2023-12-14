package org.dcsa.conformance.standards.ebl.models;

import static org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus.*;
import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.*;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

public class CarrierShippingInstructions {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Random RANDOM = new Random();

  private static final String SI_STATUS = "shippingInstructionsStatus";
  private static final String UPDATED_SI_STATUS = "updatedShippingInstructionsStatus";


  private static final String SHIPPING_INSTRUCTIONS_REFERENCE = "shippingInstructionsReference";
  private static final String TRANSPORT_DOCUMENT_REFERENCE = "transportDocumentReference";
  private static final String TRANSPORT_DOCUMENT_STATUS = "transportDocumentStatus";

  private static final String[] METADATA_FIELDS_TO_PRESERVE = {
    SHIPPING_INSTRUCTIONS_REFERENCE,
    TRANSPORT_DOCUMENT_REFERENCE,
    SI_STATUS,
    UPDATED_SI_STATUS,
  };

  private static final String[] COPY_SI_INTO_TD_FIELDS = {
    SHIPPING_INSTRUCTIONS_REFERENCE,
    "transportDocumentTypeCode",
    "freightPaymentTermCode",
    "originChargesPaymentTermCode",
    "destinationChargesPaymentTermCode",
    "isElectronic",
    "isToOrder",
    "numberOfCopiesWithCharges",
    "numberOfCopiesWithoutCharges",
    "numberOfOriginalsWithCharges",
    "numberOfOriginalsWithoutCharges",
    "displayedNameForPlaceOfReceipt",
    "displayedNameForPortOfLoad",
    "displayedNameForPortOfDischarge",
    "displayedNameForPlaceOfDelivery",
    "receiptTypeAtOrigin",
    "deliveryTypeAtDestination",
    "cargoMovementTypeAtOrigin",
    "cargoMovementTypeAtDestination",
    "invoicePayableAt",
    "partyContactDetails",
    "documentParties",
    "consignmentItems",
    "utilizedTransportEquipments",
    "references",
    "customsReferences",
    "isShippedOnBoardType",
  };

  private static JsonNode issuingCarrier(String name, String smdgCode) {
    var issuingCarrier = OBJECT_MAPPER.createObjectNode()
      .put("partyName", name);
    issuingCarrier
      .putArray("identifyingCodes")
      .addObject()
      .put("DCSAResponsibleAgencyCode", "SMDG")
      .put("codeListName", "LCL")
      .put("partyCode", smdgCode);
    return issuingCarrier;
  }

  // Randomize the issuing carrier to avoid favouring a particular carrier
  private static final JsonNode[] ISSUING_CARRIER_DEFINITIONS = {
    // Name is from the SMDG code list
    issuingCarrier("CMA CGM", "CMA"),
    issuingCarrier("Evergreen Marine Corporation", "EMC"),
    issuingCarrier("Hapag Lloyd", "HLC"),
    issuingCarrier("Hyundai", "HMM"),
    issuingCarrier("Maersk", "MSK"),
    issuingCarrier("Mediterranean Shipping Company", "MSC"),
    issuingCarrier("Ocean Network Express Pte. Ltd.", "ONE"),
    issuingCarrier("Yang Ming Line", "YML"),
    issuingCarrier("Zim Israel Navigation Company", "ZIM")
  };

  private static TDField initialFieldValue(String attribute, String value) {
    return initialFieldValue(attribute, (o, a) -> o.put(a, value));
  }

  private static TDField initialFieldValue(String attribute, Supplier<String> valueGenerator) {
    return initialFieldValue(attribute, (o, a) -> o.put(a, valueGenerator.get()));
  }

  private static TDField initialFieldValue(String attribute, BiConsumer<ObjectNode, String> valueSetter) {
    return new TDField(attribute, valueSetter, null);
  }

  private static TDField preserveIfPresent(String attribute) {
    return new TDField(attribute, null, null);
  }

  private static TDField field(String attribute, BiConsumer<ObjectNode, String> initializer, BiConsumer<ObjectNode, String> updater) {
    return new TDField(attribute, initializer, updater);
  }

  private static TDField issuingParty() {
    return initialFieldValue("issuingParty", (o, a) -> {
      int choiceNo = RANDOM.nextInt(ISSUING_CARRIER_DEFINITIONS.length);
      var choice = ISSUING_CARRIER_DEFINITIONS[choiceNo];
      o.set(a, choice.deepCopy());
    });
  }

  private record TDField(
    String attribute,
    BiConsumer<ObjectNode, String> initializer,
    BiConsumer<ObjectNode, String> updater
  ) {

    public void provideField(JsonNode source, ObjectNode dest) {
      var data = source != null ? source.get(attribute) : null;
      if (data != null) {
        dest.set(attribute, data.deepCopy());
        if (updater != null) {
          updater.accept(dest, attribute);
        }
      } else if (initializer != null) {
        initializer.accept(dest, attribute);
      }
    }
  }

  private static final TDField[] CARRIER_PROVIDED_TD_FIELDS = {
    initialFieldValue(
        "transportDocumentReference",
        () -> UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 20)),
    initialFieldValue("transportDocumentStatus", TD_DRAFT.wireName()),
    initialFieldValue("cargoMovementTypeAtOrigin", "FCL"),
    initialFieldValue("cargoMovementTypeAtDestination", "FCL"),
    initialFieldValue("receiptTypeAtOrigin", "CY"),
    initialFieldValue("deliveryTypeAtDestination", "CY"),
    initialFieldValue(
        "shippedOnBoardDate",
        (o, a) -> {
          if (o.path("isShippedOnBoardType").asBoolean(true)) {
            o.put(a, LocalDate.now().toString());
          }
        }),
    initialFieldValue(
        "receivedForShipmentDate",
        (o, a) -> {
          if (!o.path("isShippedOnBoardType").asBoolean(true)) {
            o.put(a, LocalDate.now().toString());
          }
        }),
    initialFieldValue("termsAndConditions", termsAndConditions()),
    preserveIfPresent("issueDate"),
    preserveIfPresent("declaredValue"),
    preserveIfPresent("declaredValueCurrency"),
    preserveIfPresent("serviceContractReference"),
    preserveIfPresent("contractQuotationReference"),
    issuingParty(),
    initialFieldValue(
        "carrierCode",
        (o, a) -> {
          var identifyingPartyCode = o.path("issuingParty").path("identifyingCodes").path(0);
          assert Objects.equals(
              identifyingPartyCode.path("DCSAResponsibleAgencyCode").asText(), "SMDG");
          assert Objects.equals(identifyingPartyCode.path("codeListProvider").asText(), "LCL");
          var result = identifyingPartyCode.path("partyCode");
          assert result.isTextual();
          o.set(a, result);
        }),
    initialFieldValue("carrierCodeListProvider", "SMDG"),
    // We always add a charge per amendment to ensure an amendment is never identical to the document
    // it replaces (which could cause issues with the issuance API). In reality, the carrier would also
    // change some other detail (like the requested change or the issuance date). But in the
    // conformance tests every thing happens in the same day and when the conformance toolkit is
    // the carrier, booking amendments are "zero-change" amendments.
    field("charges", (o, a) -> addCharge(o.putArray(a)), (o, a) -> addCharge(o.path(a)))
  };

  private static void addCharge(JsonNode chargesArray) {
    if (!chargesArray.isArray()) {
      return;
    }
    var charges = (ArrayNode)chargesArray;
    if (charges.isEmpty()) {
      charges
        .addObject()
        .put("chargeName", "Fictive transport document fee")
        .put("currencyAmount", 1f)
        .put("currencyCode", "EUR")
        .put("paymentTermCode", "COL")
        .put("calculationBasis", "For the concrete transport document")
        .put("unitPrice", 1f)
        .put("quantity", 1);
    } else {
      charges
        .addObject()
        .put("chargeName", "Fictive amendment fee")
        .put("currencyAmount", 1f)
        .put("currencyCode", "EUR")
        .put("paymentTermCode", "PRE")
        .put("calculationBasis", "For the entire amendment")
        .put("unitPrice", 1f)
        .put("quantity", 1);
    }
  }

  private static final String SI_DATA_FIELD = "si";
  private static final String UPDATED_SI_DATA_FIELD = "updatedSi";

  private static final String TD_DATA_FIELD = "td";

  private final ObjectNode state;

  private CarrierShippingInstructions(ObjectNode state) {
    this.state = state;
  }

  public String getShippingInstructionsReference() {
    return getShippingInstructions().required(SHIPPING_INSTRUCTIONS_REFERENCE).asText();
  }

  public String getTransportDocumentReference() {
    return getShippingInstructions().path(TRANSPORT_DOCUMENT_REFERENCE).asText(null);
  }

  public ObjectNode getShippingInstructions() {
    return (ObjectNode) state.required(SI_DATA_FIELD);
  }

  private void setShippingInstructions(ObjectNode node) {
    state.set(SI_DATA_FIELD, node);
  }

  public Optional<ObjectNode> getTransportDocument() {
    return Optional.ofNullable((ObjectNode) state.get(TD_DATA_FIELD));
  }

  public Optional<ObjectNode> getUpdatedShippingInstructions() {
    return Optional.ofNullable((ObjectNode)state.get(UPDATED_SI_DATA_FIELD));
  }

  private void setUpdatedShippingInstructions(ObjectNode node) {
    state.set(UPDATED_SI_DATA_FIELD, node);
  }

  private void clearUpdatedShippingInstructions() {
    state.remove(UPDATED_SI_DATA_FIELD);
    mutateShippingInstructionsAndUpdate(siData -> siData.remove(UPDATED_SI_STATUS));
  }


  private void setReason(String reason) {
    if (reason != null) {
      mutateShippingInstructionsAndUpdate(b -> b.put("reason", reason));
    } else {
      mutateShippingInstructionsAndUpdate(b -> b.remove("reason"));
    }
  }

  public void cancelShippingInstructionsUpdate(String shippingInstructionsReference) {
    checkState(shippingInstructionsReference, getShippingInstructionsState(), s -> s == SI_UPDATE_RECEIVED);
    changeSIState(UPDATED_SI_STATUS, SI_UPDATE_CANCELLED);
    setReason(null);
  }

  public void requestChangesToShippingInstructions(String documentReference, Consumer<ArrayNode> requestedChangesGenerator) {
    checkState(documentReference, getShippingInstructionsState(), s -> s != SI_PENDING_UPDATE && s != SI_COMPLETED );
    clearUpdatedShippingInstructions();
    changeSIState(SI_STATUS, SI_PENDING_UPDATE);
    setReason(null);
    mutateShippingInstructionsAndUpdate(siData -> requestedChangesGenerator.accept(siData.putArray("requestedChanges")));
  }

  public void acceptUpdatedShippingInstructions(String documentReference) {
    checkState(documentReference, getShippingInstructionsState(), s -> s == SI_UPDATE_RECEIVED);
    var updated = getUpdatedShippingInstructions().orElseThrow();
    setShippingInstructions(updated);
    setReason(null);
    mutateShippingInstructionsAndUpdate(siData -> siData.remove("requestedChanges"));
    changeSIState(SI_STATUS, SI_RECEIVED);
    changeSIState(UPDATED_SI_STATUS, SI_UPDATE_CONFIRMED);
  }

  public void declineUpdatedShippingInstructions(String documentReference, String reason) {
    checkState(documentReference, getShippingInstructionsState(), s -> s == SI_UPDATE_RECEIVED);
    var updated = getUpdatedShippingInstructions().orElseThrow();
    setShippingInstructions(updated);
    clearUpdatedShippingInstructions();
    setReason(reason);
    mutateShippingInstructionsAndUpdate(siData -> siData.remove("requestedChanges"));
    changeSIState(UPDATED_SI_STATUS, SI_UPDATE_DECLINED);
  }

  public void confirmShippingInstructionsComplete(String documentReference) {
    checkState(documentReference, getOriginalShippingInstructionState(), s -> s == SI_RECEIVED);
    clearUpdatedShippingInstructions();
    setReason(null);
    changeSIState(UPDATED_SI_STATUS, null);
    changeSIState(SI_STATUS, SI_COMPLETED);
  }

  public void approveDraftTransportDocument(String documentReference) {
    checkState(documentReference, getTransportDocumentState(), s -> s == TD_DRAFT);
    var td = getTransportDocument().orElseThrow();
    td.put(TRANSPORT_DOCUMENT_STATUS, TD_APPROVED.wireName());
  }

  public void acceptSurrenderForAmendment(String documentReference) {
    checkState(documentReference, getTransportDocumentState(), s -> s == TD_PENDING_SURRENDER_FOR_AMENDMENT);
    var td = getTransportDocument().orElseThrow();
    td.put(TRANSPORT_DOCUMENT_STATUS, TD_SURRENDERED_FOR_AMENDMENT.wireName());
  }

  public void voidTransportDocument(String documentReference) {
    checkState(documentReference, getTransportDocumentState(), s -> s == TD_SURRENDERED_FOR_AMENDMENT);
    var td = getTransportDocument().orElseThrow();
    td.put(TRANSPORT_DOCUMENT_STATUS, TD_VOIDED.wireName());
  }

  public void issueAmendedTransportDocument(String documentReference, ScenarioType scenarioType) {
    checkState(documentReference, getTransportDocumentState(), s -> s == TD_VOIDED);
    this.generateDraftTD(scenarioType);
    updateTDForIssuance();
    var tdData = getTransportDocument().orElseThrow();
    var tdr = tdData.required(TRANSPORT_DOCUMENT_REFERENCE).asText();
    mutateShippingInstructionsAndUpdate(si -> si.put(TRANSPORT_DOCUMENT_REFERENCE, tdr));
  }

  public void rejectSurrenderForAmendment(String documentReference) {
    checkState(documentReference, getTransportDocumentState(), s -> s == TD_PENDING_SURRENDER_FOR_AMENDMENT);
    var td = getTransportDocument().orElseThrow();
    td.put(TRANSPORT_DOCUMENT_STATUS, TD_ISSUED.wireName());
  }

  public void acceptSurrenderForDelivery(String documentReference) {
    checkState(documentReference, getTransportDocumentState(), s -> s == TD_PENDING_SURRENDER_FOR_DELIVERY);
    var td = getTransportDocument().orElseThrow();
    td.put(TRANSPORT_DOCUMENT_STATUS, TD_SURRENDERED_FOR_DELIVERY.wireName());
  }

  public void rejectSurrenderForDelivery(String documentReference) {
    checkState(documentReference, getTransportDocumentState(), s -> s == TD_PENDING_SURRENDER_FOR_DELIVERY);
    var td = getTransportDocument().orElseThrow();
    td.put(TRANSPORT_DOCUMENT_STATUS, TD_ISSUED.wireName());
  }

  public void publishDraftTransportDocument(String documentReference, ScenarioType scenarioType) {
    // We allow draft when:
    //  1) The original ("black") state is RECEIVED, *and*
    //  2) There is no update received (that is "grey" is not UPDATE_RECEIVED)
    checkState(documentReference, getOriginalShippingInstructionState(), s -> s == SI_RECEIVED);
    checkState(documentReference, getOriginalShippingInstructionState(), s -> s != SI_UPDATE_RECEIVED);
    this.generateDraftTD(scenarioType);
    var tdData = getTransportDocument().orElseThrow();
    var tdr = tdData.required(TRANSPORT_DOCUMENT_REFERENCE).asText();
    mutateShippingInstructionsAndUpdate(si -> si.put(TRANSPORT_DOCUMENT_REFERENCE, tdr));
  }

  public void issueTransportDocument(String documentReference) {
    checkState(documentReference, getTransportDocumentState(), s -> s == TD_DRAFT || s == TD_APPROVED);
    updateTDForIssuance();
  }

  private void updateTDForIssuance() {
    var td = getTransportDocument().orElseThrow();
    var date = LocalDate.now().toString();
    var shippedDateField = td.path("isShippedOnBoardType").asBoolean(true)
      ? "shippedOnBoardDate"
      : "receivedForShipmentDate";
    td.put(TRANSPORT_DOCUMENT_STATUS, TD_ISSUED.wireName())
      .put("issueDate", date)
      // Reset the shippedOnBoardDate as it generally cannot happen before the issueDate.
      // It is less clear whether we should do it for receivedForShipmentDate but ¯\_(ツ)_/¯
      .put(shippedDateField, date);
  }

  public void surrenderForAmendmentRequest(String documentReference) {
    checkState(documentReference, getTransportDocumentState(), s -> s == TD_ISSUED);
    var td = getTransportDocument().orElseThrow();
    td.put(TRANSPORT_DOCUMENT_STATUS, TD_PENDING_SURRENDER_FOR_AMENDMENT.wireName());
  }

  public void surrenderForDeliveryRequest(String documentReference) {
    checkState(documentReference, getTransportDocumentState(), s -> s == TD_ISSUED);
    var td = getTransportDocument().orElseThrow();
    td.put(TRANSPORT_DOCUMENT_STATUS, TD_PENDING_SURRENDER_FOR_DELIVERY.wireName());
  }

  private void copyFieldsWherePresent(JsonNode source, ObjectNode dest, String ... fields) {
    for (var field : fields) {
      var data = source.get(field);
      if (data != null) {
        dest.set(field, data.deepCopy());
      }
    }
  }

  private void preserveOrGenerateCarrierFields(JsonNode source, ObjectNode dest) {
    for (var entry : CARRIER_PROVIDED_TD_FIELDS) {
      entry.provideField(source, dest);
    }
  }

  private void fixupConsignmentItems(ObjectNode transportDocument) {
    for (var consignmentItemNode : transportDocument.path("consignmentItems")) {
      for (var cargoItemNode : consignmentItemNode.path("cargoItems")) {
        var outerPackagingNode = cargoItemNode.path("outerPackaging");
        if (!outerPackagingNode.isObject() || !outerPackagingNode.path("description").isMissingNode()) {
          continue;
        }
        ObjectNode outerPackaging = (ObjectNode)outerPackagingNode;
        // The packaging code has to be aligned with the description. To simplify things, we replace
        // the packageCode to ensure they are aligned. Which is not perfect, but better than
        // inconsistent data.
        //
        // The alternative is having a look-up table of all known packageCode's and their relevant
        // description.
        outerPackaging.put("packageCode", "1A")
          .put("description", "Drum, Steel");

      }
    }
  }

  private void fixupUtilizedTransportEquipments(ObjectNode transportDocument, ScenarioType scenarioType) {
    var containerISOEquipmentCode = switch (scenarioType) {
      case REEFER -> "22RB";
      case REGULAR -> "22G1";
    };
    for (JsonNode node : transportDocument.path("utilizedTransportEquipments")) {
      if (!node.isObject()) {
        continue;
      }
      ObjectNode ute = (ObjectNode)node;
      var ref = ute.path("equipmentReference");
      ute.putObject("equipment")
        .put("ISOEquipmentCode", containerISOEquipmentCode)
        .set("equipmentReference", ref);
      ute.remove("equipmentReference");
      if (scenarioType == ScenarioType.REEFER) {
        ute.put("isNonOperatingReefer", false)
          .putObject("activeReeferSettings")
          .put("temperatureSetpoint", -18)
          .put("temperatureUnit", "CEL");
      }
    }
  }

  private void generateDraftTD(ScenarioType scenarioType) {
    var td = OBJECT_MAPPER.createObjectNode();
    var siData = getShippingInstructions();
    var existingTd = getTransportDocument().orElse(null);
    copyFieldsWherePresent(siData, td, COPY_SI_INTO_TD_FIELDS);
    preserveOrGenerateCarrierFields(existingTd, td);
    fixupUtilizedTransportEquipments(td, scenarioType);
    fixupConsignmentItems(td);
    state.set(TD_DATA_FIELD, td);
  }

  private void changeSIState(String attributeName, ShippingInstructionsStatus newState) {
    if (newState == null && attributeName.equals(SI_STATUS)) {
      throw new IllegalArgumentException("The attribute " + SI_STATUS + " is mandatory");
    }
    mutateShippingInstructionsAndUpdate(b -> {
      if (newState != null) {
        b.put(attributeName, newState.wireName());
      } else {
        b.remove(attributeName);
      }
    });
  }

  private void mutateShippingInstructionsAndUpdate(Consumer<ObjectNode> mutator) {
    mutator.accept(getShippingInstructions());
    getUpdatedShippingInstructions().ifPresent(mutator);
  }

  private static void checkState(
    String reference, ShippingInstructionsStatus currentState, Predicate<ShippingInstructionsStatus> expectedState) {
    if (!expectedState.test(currentState)) {
      throw new IllegalStateException(
        "SI '%s' is in state '%s'".formatted(reference, currentState));
    }
  }

  private static void checkState(
    String reference, TransportDocumentStatus currentState, Predicate<TransportDocumentStatus> expectedState) {
    if (!expectedState.test(currentState)) {
      throw new IllegalStateException(
        "TD '%s' is in state '%s'".formatted(reference, currentState));
    }
  }

  private void removeRequestedChanges() {
    getShippingInstructions().remove("requestedChanges");
    getUpdatedShippingInstructions().ifPresent(amendedBooking -> amendedBooking.remove("requestedChanges"));
  }

  public void putShippingInstructions(String documentReference, ObjectNode newShippingInstructionData) {
    var currentState = getShippingInstructionsState();

    checkState(
      documentReference,
      currentState,
      s -> s != SI_COMPLETED
    );
    changeSIState(UPDATED_SI_STATUS, SI_UPDATE_RECEIVED);
    copyMetadataFields(getShippingInstructions(), newShippingInstructionData);
    setUpdatedShippingInstructions(newShippingInstructionData);
    removeRequestedChanges();
  }

  public ShippingInstructionsStatus getOriginalShippingInstructionState() {
    var siData = getShippingInstructions();
    var s = siData.required(SI_STATUS);
    return ShippingInstructionsStatus.fromWireName(s.asText());
  }

  public ShippingInstructionsStatus getShippingInstructionsState() {
    var siData = getShippingInstructions();
    var s = siData.path(UPDATED_SI_STATUS);
    if (s.isTextual()) {
      return ShippingInstructionsStatus.fromWireName(s.asText());
    }
    return ShippingInstructionsStatus.fromWireName(siData.required(SI_STATUS).asText());
  }


  public TransportDocumentStatus getTransportDocumentState() {
    var tdData = getTransportDocument().orElse(null);
    if (tdData == null) {
      return TD_START;
    }
    var s = tdData.required(TRANSPORT_DOCUMENT_STATUS);
    if (s.isTextual()) {
      return TransportDocumentStatus.fromWireName(s.asText());
    }
    return TD_START;
  }

  public static CarrierShippingInstructions initializeFromShippingInstructionsRequest(ObjectNode bookingRequest) {
    String sir = UUID.randomUUID().toString();
    bookingRequest.put(SHIPPING_INSTRUCTIONS_REFERENCE, sir)
      .put(SI_STATUS, SI_RECEIVED.wireName());
    var state = OBJECT_MAPPER.createObjectNode();
    state.set(SI_DATA_FIELD, bookingRequest);
    return new CarrierShippingInstructions(state);
  }

  public static CarrierShippingInstructions fromPersistentStore(JsonNode state) {
    return new CarrierShippingInstructions((ObjectNode) state);
  }

  public JsonNode asPersistentState() {
    return this.state;
  }

  public static CarrierShippingInstructions fromPersistentStore(JsonNodeMap jsonNodeMap, String shippingInstructionsReference) {
    var data = jsonNodeMap.load(shippingInstructionsReference);
    if (data == null) {
      throw new IllegalArgumentException("Unknown SI Reference: " + shippingInstructionsReference);
    }
    return fromPersistentStore(data);
  }

  public void save(JsonNodeMap jsonNodeMap) {
    jsonNodeMap.save(getShippingInstructionsReference(), asPersistentState());
  }

  private void copyMetadataFields(JsonNode originalBooking, ObjectNode updatedBooking) {
    for (String field : METADATA_FIELDS_TO_PRESERVE) {
      var previousValue = originalBooking.path(field);
      if (previousValue != null && previousValue.isTextual()){
        updatedBooking.put(field, previousValue.asText());
      } else {
        updatedBooking.remove(field);
      }
    }
  }

  private static String termsAndConditions() {
    return """
            You agree that this transport document exist is name only for the sake of
            testing your conformance with the DCSA EBL API. This transport document is NOT backed
            by a real shipment with ANY carrier and NONE of the requested services will be
            carried out in real life.

            Unless required by applicable law or agreed to in writing, DCSA provides
            this JSON data on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
            ANY KIND, either express or implied, including, without limitation, any
            warranties or conditions of TITLE, NON-INFRINGEMENT, MERCHANTABILITY,
            or FITNESS FOR A PARTICULAR PURPOSE. You are solely responsible for
            determining the appropriateness of using or redistributing this JSON
            data and assume any risks associated with Your usage of this data.

            In no event and under no legal theory, whether in tort (including negligence),
            contract, or otherwise, unless required by applicable law (such as deliberate
            and grossly negligent acts) or agreed to in writing, shall DCSA be liable to
            You for damages, including any direct, indirect, special, incidental, or
            consequential damages of any character arising as a result of this terms or conditions
            or out of the use or inability to use the provided JSON data (including but not limited
            to damages for loss of goodwill, work stoppage, computer failure or malfunction, or any
            and all other commercial damages or losses), even if DCSA has been advised of the
            possibility of such damages.
            """;
  }
}
