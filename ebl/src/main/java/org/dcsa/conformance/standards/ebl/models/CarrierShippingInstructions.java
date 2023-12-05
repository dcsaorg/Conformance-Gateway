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
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

public class CarrierShippingInstructions {

  private static final Set<ShippingInstructionsStatus> PENDING_UPDATE_PREREQUISITE_STATES = Set.of(
    SI_RECEIVED,
    SI_UPDATE_RECEIVED,
    SI_DECLINED
  );

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
  };

  private static final String[] PRESERVE_TD_FIELDS = {
    TRANSPORT_DOCUMENT_REFERENCE,
    TRANSPORT_DOCUMENT_STATUS,
    "termsAndConditions",
    "issueDate",
    "serviceContractReference",
    "contractQuotationReference",
    "declaredValue",
    "declaredValueCurrency",
    "carrierCode",
    "carrierCodeListProvider",
    "issuingParty",
    "carrierClauses",
    "numberOfRiderPages",
    "transports",
    "charges",
    "placeOfIssue",
    "isShippedOnBoardType",
    "shippedOnBoardDate",
    "receivedForShipmentDate",
  };

  private static final String SI_DATA_FIELD = "si";
  private static final String UPDATED_SI_DATA_FIELD = "updatedSi";

  private static final String TD_DATA_FIELD = "td";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
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

  public void cancelShippingInstructionsUpdate(String shippingInstructionsReference, String reason) {
    checkState(shippingInstructionsReference, getShippingInstructionsState(), s -> s == SI_UPDATE_RECEIVED);
    changeSIState(UPDATED_SI_STATUS, SI_CANCELLED);
    if (reason == null || reason.isBlank()) {
      reason = "Update cancelled by shipper (no reason given)";
    }
    setReason(reason);
  }

  public void requestChangesToShippingInstructions(String documentReference, Consumer<ArrayNode> requestedChangesGenerator) {
    checkState(documentReference, getShippingInstructionsState(), PENDING_UPDATE_PREREQUISITE_STATES::contains);
    clearUpdatedShippingInstructions();
    changeSIState(SI_STATUS, SI_PENDING_UPDATE);
    setReason(null);
    mutateShippingInstructionsAndUpdate(siData -> requestedChangesGenerator.accept(siData.putArray("requestedChanges")));
  }

  public void acceptUpdatedShippingInstructions(String documentReference) {
    checkState(documentReference, getShippingInstructionsState(), s -> s == SI_UPDATE_RECEIVED);
    var updated = getUpdatedShippingInstructions().orElseThrow();
    setShippingInstructions(updated);
    clearUpdatedShippingInstructions();
    setReason(null);
    mutateShippingInstructionsAndUpdate(siData -> siData.remove("requestedChanges"));
    changeSIState(SI_STATUS, SI_RECEIVED);
  }

  public void declineUpdatedShippingInstructions(String documentReference, String reason) {
    checkState(documentReference, getShippingInstructionsState(), s -> s == SI_UPDATE_RECEIVED);
    var updated = getUpdatedShippingInstructions().orElseThrow();
    setShippingInstructions(updated);
    clearUpdatedShippingInstructions();
    setReason(reason);
    mutateShippingInstructionsAndUpdate(siData -> siData.remove("requestedChanges"));
    changeSIState(UPDATED_SI_STATUS, SI_DECLINED);
  }

  public void publishDraftTransportDocument(String documentReference) {
    // SI_DECLINED only applies to the updated SI. UC1 -> UC3 -> UC4 (decline) -> UC6 is applicable and
    // in this case, the SI would be in RECEIVED with updated state SI_DECLINED.
    checkState(documentReference, getShippingInstructionsState(), s -> s == SI_RECEIVED || s == SI_DECLINED);
    this.generateTDFromSI();
    var tdData = getTransportDocument().orElseThrow();
    var tdr = tdData.required(TRANSPORT_DOCUMENT_REFERENCE).asText();
    mutateShippingInstructionsAndUpdate(si -> si.put(TRANSPORT_DOCUMENT_REFERENCE, tdr));
  }

  public void issueTransportDocument(String documentReference) {
    checkState(documentReference, getTransportDocumentState(), s -> s == TD_DRAFT || s == TD_APPROVED);
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

  private void copyFieldIfPresent(JsonNode source, ObjectNode dest, String field) {
    var data = source.get(field);
    if (data != null) {
      dest.set(field, data);
    }
  }

  private void copyFieldsWherePresent(JsonNode source, ObjectNode dest, String ... fields) {
    for (var field : fields) {
      copyFieldIfPresent(source, dest, field);
    }
  }

  private void generateTDFromSI() {
    var td = OBJECT_MAPPER.createObjectNode();
    var siData = getShippingInstructions();
    var existingTd = getTransportDocument().orElse(null);
    copyFieldsWherePresent(siData, td, COPY_SI_INTO_TD_FIELDS);
    if (existingTd == null) {
      var tdr = UUID.randomUUID().toString()
        .replace("-", "")
        .toUpperCase()
        .substring(0, 20);
      var isShippedOnBoardType = siData.path("isShippedOnBoardType").asBoolean(true);
      var date = LocalDate.now().toString();
      var shippedDateField = isShippedOnBoardType
        ? "shippedOnBoardDate"
        : "receivedForShipmentDate";
      existingTd =
          OBJECT_MAPPER.createObjectNode()
            .put(TRANSPORT_DOCUMENT_REFERENCE, tdr)
            .put(TRANSPORT_DOCUMENT_STATUS, TD_DRAFT.wireName())
            .put("isShippedOnBoardType", isShippedOnBoardType)
            .put(shippedDateField, date);
    }
    copyFieldsWherePresent(existingTd, td, PRESERVE_TD_FIELDS);
    state.set(TD_DATA_FIELD, td);
  }

  private void changeSIState(String attributeName, ShippingInstructionsStatus newState) {
    mutateShippingInstructionsAndUpdate(b -> b.put(attributeName, newState.wireName()));
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
      throw new IllegalArgumentException("Unknown CBRR: " + shippingInstructionsReference);
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
}
