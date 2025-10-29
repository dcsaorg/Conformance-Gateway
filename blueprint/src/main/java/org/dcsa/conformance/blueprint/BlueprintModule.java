package org.dcsa.conformance.blueprint;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Activity;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.Collaboration;
import org.camunda.bpm.model.bpmn.instance.DataInput;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataObject;
import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.DataOutput;
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataStore;
import org.camunda.bpm.model.bpmn.instance.DataStoreReference;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.IoSpecification;
import org.camunda.bpm.model.bpmn.instance.InputSet;
import org.camunda.bpm.model.bpmn.instance.Message;
import org.camunda.bpm.model.bpmn.instance.MessageFlow;
import org.camunda.bpm.model.bpmn.instance.OutputSet;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Blueprint module for creating BPMN processes using Camunda BPMN Model library.
 * Convert .bpmn to .png: npx bpmn-to-image example-process.bpmn;example-process.png
 * ...after installing: npm i -g bpmn-to-image
 */
public class BlueprintModule {

  /**
   * Creates an example BPMN process with a start event, user task, and end event.
   *
   * @return the created BPMN model instance
   */
  public BpmnModelInstance createExampleProcess() {
    return Bpmn.createExecutableProcess("ValidatePlanConfirmBooking")
        .name("1.2 Validate, plan and confirm booking request")

        // --- Start ---
        .startEvent("bookingRegistered")
        .name("Booking request registered")

        // --- Pricing decision ---
        .exclusiveGateway("gw_pricingRef")
        .name("Pricing reference available?")
        // Pre-create the join after pricing so branches can connect to it
        .exclusiveGateway("gw_afterPricingJoin")
        .name("After pricing")
        .moveToNode("gw_pricingRef")
        .condition("Yes", "${pricingRef == true}")
        .serviceTask("applyQuoteOrContractRate")
        .name("Apply quote or contract rate")
        .connectTo("gw_afterPricingJoin")
        .moveToNode("gw_pricingRef")
        .condition("No", "${pricingRef == false}")
        .serviceTask("applyTariffRate")
        .name("Apply tariff rate")
        .connectTo("gw_afterPricingJoin")

        // --- DG check ---
        .moveToNode("gw_afterPricingJoin")
        .exclusiveGateway("gw_isDG")
        .name("Is it DG?")
        .exclusiveGateway("gw_afterDGJoin")
        .name("After DG")
        .moveToNode("gw_isDG")
        .condition("Yes", "${isDG == true}")
        .callActivity("bookingPreValidation")
        .name("DS booking pre-validation")
        .connectTo("gw_afterDGJoin")
        .moveToNode("gw_isDG")
        .condition("No", "${isDG == false}")
        .connectTo("gw_afterDGJoin")

        // --- Availability validation (parallel fan-out/fan-in) ---
        .moveToNode("gw_afterDGJoin")
        .parallelGateway("pg_validateStart")
        .name("Start validations")
        // pre-create join
        .parallelGateway("pg_validateJoin")
        .name("End validations")
        .moveToNode("pg_validateStart")
        .serviceTask("validateVessel")
        .name("Validate vessel capacity")
        .connectTo("pg_validateJoin")
        .moveToNode("pg_validateStart")
        .serviceTask("validateInland")
        .name("Validate inland transport")
        .connectTo("pg_validateJoin")
        .moveToNode("pg_validateStart")
        .serviceTask("validateEquipment")
        .name("Validate equipment availability")
        .connectTo("pg_validateJoin")

        // --- Preliminary confirmation? ---
        .moveToNode("pg_validateJoin")
        .exclusiveGateway("gw_prelim")
        .name("Unconfirmed booking preliminarily?")
        .exclusiveGateway("gw_afterPrelimJoin")
        .name("After prelim")
        .moveToNode("gw_prelim")
        .condition("No", "${bookingPrelim == false}")
        .serviceTask("rejectBooking")
        .name("Reject booking")
        .endEvent("bookingRejected")
        .name("Booking rejected")
        .moveToNode("gw_prelim")
        .condition("Yes", "${bookingPrelim == true}")
        .connectTo("gw_afterPrelimJoin")

        // --- Constraint check and resolution ---
        .moveToNode("gw_afterPrelimJoin")
        .exclusiveGateway("gw_constraintIssue")
        .name("Constraint issue?")
        .exclusiveGateway("gw_afterConstraintJoin")
        .name("After constraint handling")

        // path: Yes -> type? -> capacity vs other -> check status -> resolved?
        .moveToNode("gw_constraintIssue")
        .condition("Yes", "${constraintIssue == true}")
        .serviceTask("confirmConstraintType")
        .name("Confirm constraint issue type")
        .exclusiveGateway("gw_issueType")
        .name("Issue type?")
        // prepare status check and resolved gateway so both branches can connect
        .serviceTask("checkConstraintStatus_anchor")
        .name("Check constraint issue status") // anchor to keep builder position
        .exclusiveGateway("gw_issueResolved")
        .name("Issue resolved?")
        .exclusiveGateway("gw_afterResolvedJoin")
        .name("After resolved split")
        // back to issueType for branches
        .moveToNode("gw_issueType")
        .condition("Capacity", "${issueType == 'capacity'}")
        .serviceTask("requestAdditionalCapacity")
        .name("Request additional capacity")
        .connectTo("checkConstraintStatus_anchor")
        .moveToNode("gw_issueType")
        .condition("Other", "${issueType == 'other'}")
        .serviceTask("identifyOptions")
        .name("Identify options to solve constraint issue")
        .connectTo("checkConstraintStatus_anchor")
        // resolved? yes -> join; no -> manage change -> end local branch
        .moveToNode("gw_issueResolved")
        .condition("No", "${issueResolved == false}")
        .callActivity("manageCarrierBookingChange")
        .name("4.1 Manage carrier booking change")
        .endEvent("bookingChangeEnd")
        .name("Booking change ended")
        .moveToNode("gw_issueResolved")
        .condition("Yes", "${issueResolved == true}")
        .connectTo("gw_afterConstraintJoin")

        // path: No issue -> straight to join
        .moveToNode("gw_constraintIssue")
        .condition("No", "${constraintIssue == false}")
        .connectTo("gw_afterConstraintJoin")

        // --- Special cargo/equipment ---
        .moveToNode("gw_afterConstraintJoin")
        .exclusiveGateway("gw_specialCargo")
        .name("Special cargo/equipment requirements?")
        .exclusiveGateway("gw_afterSpecialJoin")
        .name("After special cargo")
        .moveToNode("gw_specialCargo")
        .condition("Yes", "${specialCargo == true}")
        .serviceTask("completeSpecialDetails")
        .name("Complete special cargo/equipment details")
        .serviceTask("checkApprovalNeed")
        .name("Check special cargo/equipment approval need")
        .exclusiveGateway("gw_approvalNeeded")
        .name("Special cargo/equipment approval needed?")
        .moveToNode("gw_approvalNeeded")
        .condition("Yes", "${approvalNeeded == true}")
        .serviceTask("manageSpecialApproval")
        .name("Manage special cargo/equipment approval")
        .connectTo("gw_afterSpecialJoin")
        .moveToNode("gw_approvalNeeded")
        .condition("No", "${approvalNeeded == false}")
        .connectTo("gw_afterSpecialJoin")
        .moveToNode("gw_specialCargo")
        .condition("No", "${specialCargo == false}")
        .connectTo("gw_afterSpecialJoin")

        // --- Carrier haulage ---
        .moveToNode("gw_afterSpecialJoin")
        .exclusiveGateway("gw_carrierHaulage")
        .name("Carrier haulage needed?")
        .exclusiveGateway("gw_afterHaulageJoin")
        .name("After haulage")
        .moveToNode("gw_carrierHaulage")
        .condition("Yes", "${carrierHaulage == true}")
        .serviceTask("coordinateInlandTransport")
        .name("Coordinate inland transport")
        .connectTo("gw_afterHaulageJoin")
        .moveToNode("gw_carrierHaulage")
        .condition("No", "${carrierHaulage == false}")
        .connectTo("gw_afterHaulageJoin")

        // --- Planning and confirmation ---
        .moveToNode("gw_afterHaulageJoin")
        .serviceTask("registerPlan")
        .name("Register planned transport incl. transport modes")
        .serviceTask("createRoutePlan")
        .name("Create route plan")
        .serviceTask("generateBookingConfirmation")
        .name("Generate booking confirmation")
        .endEvent("bookingValidated")
        .name("Booking validated, planned and confirmed")
        .done();
  }

  public static void augment(BpmnModelInstance model) {
    // Assumes the executable process id from your builder
    final String processId = "ValidatePlanConfirmBooking";

    // 1) Collaboration + participants (pools)
    Collaboration collab = create(model, model.getDefinitions(), "collab", Collaboration.class);

    org.camunda.bpm.model.bpmn.instance.Process carrierProcess = model.getModelElementById(processId);
    Participant carrier = create(model, collab, "p_carrier", Participant.class);
    carrier.setName("Carrier");
    carrier.setProcess(carrierProcess);

    Participant customer = pool(model, collab, "p_customer", "Customer");
    Participant networkOps = pool(model, collab, "p_network", "Network/Vessel Capacity");
    Participant inlandProvider = pool(model, collab, "p_inland", "Inland Transport Provider");
    Participant equipmentMgr = pool(model, collab, "p_equip", "Equipment Availability");

    // 2) Messages
    Message msgSpaceReply  = message(model, "m_spaceReply",  "Space request reply");
    Message msgCapacityReq = message(model, "m_capacityReq", "Capacity request");
    Message msgInlandReq   = message(model, "m_inlandReq",   "Inland transport request");
    Message msgHaulierResp = message(model, "m_haulierResp", "Haulier response to transport request");
    Message msgRoutePlan   = message(model, "m_routePlan",   "Route plan");
    Message msgBookingInfo = message(model, "m_bookingInfo", "Booking request information");
    Message msgValidation  = message(model, "m_validation",  "Booking validation");
    Message msgRejection   = message(model, "m_rejection",   "Booking rejection");
    Message msgCustConf    = message(model, "m_custConf",    "Customer booking confirmation");
    Message msgConfirmed   = message(model, "m_confirmed",   "Confirmed booking");

    // 3) Data stores (read by validations)
    DataStoreReference dsVessel   = datastore(model, carrierProcess, "ds_vessel",   "Network/vessel capacity and schedules");
    DataStoreReference dsInland   = datastore(model, carrierProcess, "ds_inland",   "Inland transport agreement/availability");
    DataStoreReference dsEquip    = datastore(model, carrierProcess, "ds_equip",    "Equipment availability");
    DataStoreReference dsBooking  = datastore(model, carrierProcess, "ds_booking",  "Booking repository");

    // 4) Data objects (documents produced/consumed)
    DataObjectReference doBookingInfo = dataObject(model, carrierProcess, "do_bookingInfo", "Booking request information");
    DataObjectReference doRoutePlan   = dataObject(model, carrierProcess, "do_routePlan",   "Route plan");
    DataObjectReference doConfDoc     = dataObject(model, carrierProcess, "do_custConf",    "Customer booking confirmation");

    // 5) Wire message flows to key tasks/events
    // Helpers below find elements by id created in your builder
    ServiceTask tReqAddCap   = byId(model, "requestAdditionalCapacity");
    ServiceTask tIdentifyOpt = byId(model, "identifyOptions");
    ServiceTask tCoordInland = byId(model, "coordinateInlandTransport");
    ServiceTask tCreateRoute = byId(model, "createRoutePlan");
    ServiceTask tGenConf     = byId(model, "generateBookingConfirmation");
    ServiceTask tReject      = byId(model, "rejectBooking");
    EndEvent eRejected     = byId(model, "bookingRejected");
    EndEvent   eValidated    = byId(model, "bookingValidated");
    StartEvent eStart        = byId(model, "bookingRegistered");

    // inbound booking information
    messageFlow(model, collab, "mf_bookingInfo_in",
      customer, eStart, msgBookingInfo);

    // space/capacity management
    messageFlow(model, collab, "mf_capacity_request",
      carrier, tReqAddCap, msgCapacityReq, networkOps);
    messageFlow(model, collab, "mf_space_reply",
      networkOps, tReqAddCap, msgSpaceReply, carrier);

    // inland transport request and haulier response
    messageFlow(model, collab, "mf_inland_request",
      carrier, tCoordInland, msgInlandReq, inlandProvider);
    messageFlow(model, collab, "mf_haulier_response",
      inlandProvider, tCoordInland, msgHaulierResp, carrier);

    // route plan sent to customer as information artifact
    messageFlow(model, collab, "mf_route_plan_out",
      carrier, tCreateRoute, msgRoutePlan, customer);

    // validation or rejection notifications
    messageFlow(model, collab, "mf_validation_out",
      carrier, tGenConf, msgValidation, customer);
    messageFlow(model, collab, "mf_rejection_out",
      carrier, tReject, msgRejection, customer);

    // final confirmations
    messageFlow(model, collab, "mf_customer_booking_confirmation",
      carrier, tGenConf, msgCustConf, customer);
    messageFlow(model, collab, "mf_confirmed_booking",
      carrier, eValidated, msgConfirmed, customer);

    // 6) Data associations: tasks ↔ data stores / data objects

    // Validations read stores
    ServiceTask tValVessel = byId(model, "validateVessel");
    ServiceTask tValInland = byId(model, "validateInland");
    ServiceTask tValEquip  = byId(model, "validateEquipment");

    readFromStore(model, tValVessel, dsVessel);
    readFromStore(model, tValInland, dsInland);
    readFromStore(model, tValEquip,  dsEquip);

    // Booking repository updates on reject and confirm
    writeToStore(model, tReject,  dsBooking);
    writeToStore(model, tGenConf, dsBooking);

    // Route plan produced
    writeToObject(model, tCreateRoute, doRoutePlan);
    // Booking confirmation produced
    writeToObject(model, tGenConf, doConfDoc);
    // Booking request info consumed at start and throughout
    readFromObject(model, tCreateRoute, doBookingInfo);
    readFromObject(model, tGenConf,    doBookingInfo);

    // 7) Update the BPMNPlane to reference the collaboration and add visual layout
    // This makes the collaboration diagram with participants and message flows visible
    Collection<MessageFlow> allMessageFlows = model.getModelElementsByType(MessageFlow.class);
    updateDiagramToShowCollaboration(model, collab, carrier, customer, networkOps, inlandProvider, equipmentMgr, allMessageFlows);
  }

  // ---- helpers --------------------------------------------------------------

  /**
   * Updates the BPMN diagram to show the collaboration view instead of just the process.
   * This is critical - without this, BPMN viewers will only show the process elements
   * and not the collaboration context (participants/pools, message flows between them).
   * The BPMNPlane element determines what is visible in the diagram.
   * Also adds visual layout information (BPMNShape) for each participant pool and edges for message flows.
   */
  private static void updateDiagramToShowCollaboration(BpmnModelInstance model, Collaboration collab,
      Participant carrier, Participant customer, Participant networkOps,
      Participant inlandProvider, Participant equipmentMgr, Collection<MessageFlow> messageFlows) {
    // Find the BPMNDiagram and update its BPMNPlane to reference the collaboration
    Collection<org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnDiagram> diagrams =
        model.getModelElementsByType(org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnDiagram.class);

    if (!diagrams.isEmpty()) {
      org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnDiagram diagram = diagrams.iterator().next();
      org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnPlane plane = diagram.getBpmnPlane();
      if (plane != null) {
        // Update the plane to reference the collaboration instead of the process
        plane.setBpmnElement(collab);

        // Calculate bounding box of existing process elements
        double[] bbox = calculateProcessBoundingBox(plane);
        double minX = bbox[0];
        double minY = bbox[1];
        double maxX = bbox[2];
        double maxY = bbox[3];

        // Pool header width and padding
        double poolHeaderWidth = 30;
        double padding = 20;

        // Calculate carrier pool dimensions to contain the process
        double carrierX = minX - poolHeaderWidth - padding;
        double carrierY = minY - padding;
        double carrierWidth = (maxX - minX) + poolHeaderWidth + (2 * padding);
        double carrierHeight = (maxY - minY) + (2 * padding);

        // Other pool dimensions
        double poolHeight = 150;
        double poolSpacing = 20;

        // Add visual shapes for participants (pools)
        addParticipantShape(model, plane, carrier, carrierX, carrierY, carrierWidth, carrierHeight);

        double nextY = carrierY + carrierHeight + poolSpacing;
        addParticipantShape(model, plane, customer, carrierX, nextY, carrierWidth, poolHeight);
        nextY += poolHeight + poolSpacing;
        addParticipantShape(model, plane, networkOps, carrierX, nextY, carrierWidth, poolHeight);
        nextY += poolHeight + poolSpacing;
        addParticipantShape(model, plane, inlandProvider, carrierX, nextY, carrierWidth, poolHeight);
        nextY += poolHeight + poolSpacing;
        addParticipantShape(model, plane, equipmentMgr, carrierX, nextY, carrierWidth, poolHeight);

        // Add visual edges for message flows with calculated waypoints
        addMessageFlowEdges(model, plane, messageFlows, carrierY, carrierHeight,
            carrierY + carrierHeight + poolSpacing, poolHeight, poolSpacing, carrierX, carrierWidth);
      }
    }
  }

  private static double[] calculateProcessBoundingBox(org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnPlane plane) {
    double minX = Double.MAX_VALUE;
    double minY = Double.MAX_VALUE;
    double maxX = Double.MIN_VALUE;
    double maxY = Double.MIN_VALUE;

    Collection<org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape> shapes =
        plane.getChildElementsByType(org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape.class);

    for (org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape shape : shapes) {
      org.camunda.bpm.model.bpmn.instance.dc.Bounds bounds = shape.getBounds();
      if (bounds != null) {
        minX = Math.min(minX, bounds.getX());
        minY = Math.min(minY, bounds.getY());
        maxX = Math.max(maxX, bounds.getX() + bounds.getWidth());
        maxY = Math.max(maxY, bounds.getY() + bounds.getHeight());
      }
    }

    return new double[]{minX, minY, maxX, maxY};
  }

  private static void addParticipantShape(BpmnModelInstance model,
      org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnPlane plane,
      Participant participant, double x, double y, double width, double height) {
    org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape shape =
        model.newInstance(org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape.class);
    shape.setBpmnElement(participant);
    shape.setHorizontal(true);

    org.camunda.bpm.model.bpmn.instance.dc.Bounds bounds =
        model.newInstance(org.camunda.bpm.model.bpmn.instance.dc.Bounds.class);
    bounds.setX(x);
    bounds.setY(y);
    bounds.setWidth(width);
    bounds.setHeight(height);
    shape.addChildElement(bounds);

    plane.addChildElement(shape);
  }

  private static void addMessageFlowEdges(BpmnModelInstance model,
      org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnPlane plane,
      Collection<MessageFlow> messageFlows,
      double carrierY, double carrierHeight,
      double otherPoolsStartY, double poolHeight, double poolSpacing,
      double poolX, double poolWidth) {

    // Create a map of element ID to position for calculating waypoints
    Map<String, double[]> elementPositions = new HashMap<>();

    Collection<org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape> shapes =
        plane.getChildElementsByType(org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape.class);

    for (org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape shape : shapes) {
      BaseElement element = shape.getBpmnElement();
      if (element != null && !(element instanceof Participant)) {
        org.camunda.bpm.model.bpmn.instance.dc.Bounds bounds = shape.getBounds();
        if (bounds != null) {
          // Store center position of element
          double centerX = bounds.getX() + bounds.getWidth() / 2;
          double centerY = bounds.getY() + bounds.getHeight() / 2;
          elementPositions.put(element.getId(), new double[]{centerX, centerY, bounds.getY(), bounds.getY() + bounds.getHeight()});
        }
      }
    }

    // Calculate pool center Y positions
    double customerY = otherPoolsStartY + poolHeight / 2;
    double networkOpsY = otherPoolsStartY + poolHeight + poolSpacing + poolHeight / 2;
    double inlandProviderY = otherPoolsStartY + 2 * (poolHeight + poolSpacing) + poolHeight / 2;
    double poolCenterX = poolX + poolWidth / 2;

    // Add edges for each message flow
    for (MessageFlow mf : messageFlows) {
      org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge edge =
          model.newInstance(org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge.class);
      edge.setBpmnElement(mf);

      // Get source and target
      org.camunda.bpm.model.bpmn.instance.InteractionNode source = mf.getSource();
      org.camunda.bpm.model.bpmn.instance.InteractionNode target = mf.getTarget();

      // Calculate waypoints based on source and target
      double sourceX = poolCenterX;
      double sourceY = carrierY + carrierHeight; // Bottom of carrier pool
      double targetX = poolCenterX;
      double targetY = customerY; // Default to customer pool

      // Try to get more precise source position if it's a flow node
      if (source instanceof FlowNode) {
        double[] pos = elementPositions.get(source.getId());
        if (pos != null) {
          sourceX = pos[0];
          sourceY = pos[3]; // Bottom of element
        }
      }

      // Determine target pool Y position based on target participant
      if (target instanceof Participant) {
        String targetId = target.getId();
        if ("p_networkOps".equals(targetId) || "p_network".equals(targetId)) {
          targetY = networkOpsY;
        } else if ("p_inland".equals(targetId)) {
          targetY = inlandProviderY;
        }
      } else if (target instanceof FlowNode) {
        // Message coming into carrier pool from outside
        double[] pos = elementPositions.get(target.getId());
        if (pos != null) {
          targetX = pos[0];
          targetY = pos[2]; // Top of element
          sourceY = customerY; // Assume from customer
        }
      }

      // Create waypoints
      org.camunda.bpm.model.bpmn.instance.di.Waypoint wp1 =
          model.newInstance(org.camunda.bpm.model.bpmn.instance.di.Waypoint.class);
      wp1.setX(sourceX);
      wp1.setY(sourceY);

      org.camunda.bpm.model.bpmn.instance.di.Waypoint wp2 =
          model.newInstance(org.camunda.bpm.model.bpmn.instance.di.Waypoint.class);
      wp2.setX(targetX);
      wp2.setY(targetY);

      edge.addChildElement(wp1);
      edge.addChildElement(wp2);

      plane.addChildElement(edge);
    }
  }

  private static <T extends ModelElementInstance> T create(
    BpmnModelInstance model, ModelElementInstance parent, String id, Class<T> type) {
    T el = model.newInstance(type);
    el.setAttributeValue("id", id, true, false);
    parent.addChildElement(el);
    return el;
  }

  private static Participant pool(BpmnModelInstance m, Collaboration c, String id, String name) {
    Participant p = create(m, c, id, Participant.class);
    p.setName(name);
    return p;
  }

  private static Message message(BpmnModelInstance m, String id, String name) {
    Message msg = create(m, m.getDefinitions(), id, Message.class);
    msg.setName(name);
    return msg;
  }

  private static DataStoreReference datastore(BpmnModelInstance m, Process process, String id, String name) {
    DataStore dataStore = create(m, m.getDefinitions(), id + "_def", DataStore.class); // root element allowed under definitions
    dataStore.setName(name);
    DataStoreReference ref = create(m, process, id, DataStoreReference.class); // reference must be inside a Process
    ref.setName(name);
    ref.setDataStore(dataStore);
    return ref;
  }

  private static DataObjectReference dataObject(
    BpmnModelInstance m, Process process, String id, String name) {
    DataObject data = create(m, process, id + "_def", DataObject.class);
    data.setName(name);
    DataObjectReference ref = create(m, process, id, DataObjectReference.class);
    ref.setName(name);
    ref.setDataObject(data);
    return ref;
  }

  private static void messageFlow(
    BpmnModelInstance m,
    Collaboration collab,
    String id,
    Participant sourcePool, FlowNode sourceNode, Message msg, Participant targetPool) {

    MessageFlow mf = create(m, collab, id, MessageFlow.class);
    mf.setName(msg.getName());
    mf.setMessage(msg);
    mf.setSource(sourcePool);
    mf.setTarget(targetPool);
    // BPMN spec allows MessageFlow between participants; Camunda accepts FlowNode->Participant
  }

  // Overload when source and target pools are implied
  private static void messageFlow(
    BpmnModelInstance m,
    Collaboration collab,
    String id,
    Participant sourcePool, FlowNode sourceNode, Message msg) {
    MessageFlow mf = create(m, collab, id, MessageFlow.class);
    mf.setName(msg.getName());
    mf.setMessage(msg);
    mf.setSource(sourcePool);
    // Cast to InteractionNode - Events and Tasks implement this interface
    mf.setTarget((org.camunda.bpm.model.bpmn.instance.InteractionNode) sourceNode);
  }

  private static void readFromStore(BpmnModelInstance m, Activity task, DataStoreReference store) {
    String base = task.getId() + "_in_" + store.getId();
    IoSpecification ioSpec = ensureIoSpec(m, task);
    DataInput dataInput = m.newInstance(DataInput.class);
    dataInput.setAttributeValue("id", base, true, false);
    ioSpec.addChildElement(dataInput);
    ensureInputSet(m, ioSpec, dataInput);
    DataInputAssociation dia = create(m, task, base + "_assoc", DataInputAssociation.class);
    dia.getSources().add(store);
    dia.setTarget(dataInput);
  }

  private static void writeToStore(BpmnModelInstance m, Activity task, DataStoreReference store) {
    String base = task.getId() + "_out_" + store.getId();
    IoSpecification ioSpec = ensureIoSpec(m, task);
    DataOutput dataOutput = m.newInstance(DataOutput.class);
    dataOutput.setAttributeValue("id", base, true, false);
    ioSpec.addChildElement(dataOutput);
    ensureOutputSet(m, ioSpec, dataOutput);
    DataOutputAssociation doa = create(m, task, base + "_assoc", DataOutputAssociation.class);
    doa.getSources().add(dataOutput);
    doa.setTarget(store);
  }

  private static void readFromObject(BpmnModelInstance m, Activity task, DataObjectReference obj) {
    String base = task.getId() + "_in_" + obj.getId();
    IoSpecification ioSpec = ensureIoSpec(m, task);
    DataInput dataInput = m.newInstance(DataInput.class);
    dataInput.setAttributeValue("id", base, true, false);
    ioSpec.addChildElement(dataInput);
    ensureInputSet(m, ioSpec, dataInput);
    DataInputAssociation dia = create(m, task, base + "_assoc", DataInputAssociation.class);
    dia.getSources().add(obj);
    dia.setTarget(dataInput);
  }

  private static void writeToObject(BpmnModelInstance m, Activity task, DataObjectReference obj) {
    String base = task.getId() + "_out_" + obj.getId();
    IoSpecification ioSpec = ensureIoSpec(m, task);
    DataOutput dataOutput = m.newInstance(DataOutput.class);
    dataOutput.setAttributeValue("id", base, true, false);
    ioSpec.addChildElement(dataOutput);
    ensureOutputSet(m, ioSpec, dataOutput);
    DataOutputAssociation doa = create(m, task, base + "_assoc", DataOutputAssociation.class);
    doa.getSources().add(dataOutput);
    doa.setTarget(obj);
  }

  private static IoSpecification ensureIoSpec(BpmnModelInstance m, Activity task) {
    for (ModelElementInstance child : task.getChildElementsByType(IoSpecification.class)) {
      return (IoSpecification) child;
    }
    IoSpecification ioSpec = m.newInstance(IoSpecification.class);
    task.addChildElement(ioSpec);

    // BPMN schema requires both inputSet and outputSet to be present
    InputSet inputSet = m.newInstance(InputSet.class);
    ioSpec.addChildElement(inputSet);

    OutputSet outputSet = m.newInstance(OutputSet.class);
    ioSpec.addChildElement(outputSet);

    return ioSpec;
  }

  private static void ensureInputSet(BpmnModelInstance m, IoSpecification ioSpec, DataInput dataInput) {
    InputSet inputSet = null;
    for (ModelElementInstance child : ioSpec.getChildElementsByType(InputSet.class)) {
      inputSet = (InputSet) child;
      break;
    }
    if (inputSet == null) {
      inputSet = m.newInstance(InputSet.class);
      ioSpec.addChildElement(inputSet);
    }
    inputSet.getDataInputs().add(dataInput);
  }

  private static void ensureOutputSet(BpmnModelInstance m, IoSpecification ioSpec, DataOutput dataOutput) {
    OutputSet outputSet = null;
    for (ModelElementInstance child : ioSpec.getChildElementsByType(OutputSet.class)) {
      outputSet = (OutputSet) child;
      break;
    }
    if (outputSet == null) {
      outputSet = m.newInstance(OutputSet.class);
      ioSpec.addChildElement(outputSet);
    }
    outputSet.getDataOutputRefs().add(dataOutput);
  }

  private static <T extends BaseElement> T byId(BpmnModelInstance m, String id) {
    T el = m.getModelElementById(id);
    if (el == null) throw new IllegalStateException("Missing element: " + id);
    return el;
  }

  /**
   * Saves a BPMN model instance to a file.
   *
   * @param modelInstance the BPMN model to save
   * @param filePath the path where the BPMN file should be saved
   * @throws IOException if there's an error writing the file
   */
  public void saveBpmnToFile(BpmnModelInstance modelInstance, String filePath) throws IOException {
    File file = new File(filePath);
    File parentDir = file.getParentFile();
    if (parentDir != null && !parentDir.exists()) {
      parentDir.mkdirs();
    }
    Bpmn.writeModelToFile(file, modelInstance);
  }

  /** Main method to demonstrate BPMN process creation and saving. */
  public static void main(String[] args) {
    BlueprintModule blueprint = new BlueprintModule();

    // Create example process
    BpmnModelInstance modelInstance = blueprint.createExampleProcess();
    augment(modelInstance);

    // Define output path
    String bpmnOutputPath = "blueprint/generated-resources/example-process.bpmn";

    try {
      // Save BPMN to file
      blueprint.saveBpmnToFile(modelInstance, bpmnOutputPath);
      System.out.println("BPMN process successfully created and saved to: " + bpmnOutputPath);
    } catch (IOException e) {
      System.err.println("Error saving BPMN file: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
