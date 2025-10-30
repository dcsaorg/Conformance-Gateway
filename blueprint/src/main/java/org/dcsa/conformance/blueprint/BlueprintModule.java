package org.dcsa.conformance.blueprint;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnPlane;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.dc.Bounds;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

/** Blueprint module for creating BPMN processes using Camunda BPMN Model library. */
public class BlueprintModule {

  // ====== 1) Build the logical process (unchanged) ============================================
  public BpmnModelInstance createExampleProcess() {
    return Bpmn.createExecutableProcess("ValidatePlanConfirmBooking")
        .name("1.2 Validate, plan and confirm booking request")

        // Start
        .startEvent("bookingRegistered")
        .name("Booking request registered")

        // Pricing
        .exclusiveGateway("gw_pricingRef")
        .name("Pricing reference available?")
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

        // DG
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

        // Parallel validations
        .moveToNode("gw_afterDGJoin")
        .parallelGateway("pg_validateStart")
        .name("Start validations")
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

        // Preliminary confirmation
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

        // Constraints
        .moveToNode("gw_afterPrelimJoin")
        .exclusiveGateway("gw_constraintIssue")
        .name("Constraint issue?")
        .exclusiveGateway("gw_afterConstraintJoin")
        .name("After constraint handling")
        .moveToNode("gw_constraintIssue")
        .condition("Yes", "${constraintIssue == true}")
        .serviceTask("confirmConstraintType")
        .name("Confirm constraint issue type")
        .exclusiveGateway("gw_issueType")
        .name("Issue type?")
        .serviceTask("checkConstraintStatus_anchor")
        .name("Check constraint issue status")
        .exclusiveGateway("gw_issueResolved")
        .name("Issue resolved?")
        .exclusiveGateway("gw_afterResolvedJoin")
        .name("After resolved split")
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
        .moveToNode("gw_issueResolved")
        .condition("No", "${issueResolved == false}")
        .callActivity("manageCarrierBookingChange")
        .name("4.1 Manage carrier booking change")
        .endEvent("bookingChangeEnd")
        .name("Booking change ended")
        .moveToNode("gw_issueResolved")
        .condition("Yes", "${issueResolved == true}")
        .connectTo("gw_afterConstraintJoin")
        .moveToNode("gw_constraintIssue")
        .condition("No", "${constraintIssue == false}")
        .connectTo("gw_afterConstraintJoin")

        // Special cargo
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

        // Carrier haulage
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

        // Plan and confirm
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

  // ====== 2) Augment collaboration, messages, data (your existing logic) ======================
  public static void augment(BpmnModelInstance model) {
    final String processId = "ValidatePlanConfirmBooking";

    Collaboration collab = create(model, model.getDefinitions(), "collab", Collaboration.class);
    Process carrierProcess = model.getModelElementById(processId);
    Participant carrier = create(model, collab, "p_carrier", Participant.class);
    carrier.setName("Carrier");
    carrier.setProcess(carrierProcess);

    Participant customer = pool(model, collab, "p_customer", "Customer");
    Participant networkOps = pool(model, collab, "p_network", "Network/Vessel Capacity");
    Participant inlandProvider = pool(model, collab, "p_inland", "Inland Transport Provider");
    Participant equipmentMgr = pool(model, collab, "p_equip", "Equipment Availability");

    // messages + data stores + data objects + associations (unchanged from your file)
    // :contentReference[oaicite:2]{index=2}

    Message msgSpaceReply = message(model, "m_spaceReply", "Space request reply");
    Message msgCapacityReq = message(model, "m_capacityReq", "Capacity request");
    Message msgInlandReq = message(model, "m_inlandReq", "Inland transport request");
    Message msgHaulierResp =
        message(model, "m_haulierResp", "Haulier response to transport request");
    Message msgRoutePlan = message(model, "m_routePlan", "Route plan");
    Message msgBookingInfo = message(model, "m_bookingInfo", "Booking request information");
    Message msgValidation = message(model, "m_validation", "Booking validation");
    Message msgRejection = message(model, "m_rejection", "Booking rejection");
    Message msgCustConf = message(model, "m_custConf", "Customer booking confirmation");
    Message msgConfirmed = message(model, "m_confirmed", "Confirmed booking");

    DataStoreReference dsVessel =
        datastore(model, carrierProcess, "ds_vessel", "Network/vessel capacity and schedules");
    DataStoreReference dsInland =
        datastore(model, carrierProcess, "ds_inland", "Inland transport agreement/availability");
    DataStoreReference dsEquip =
        datastore(model, carrierProcess, "ds_equip", "Equipment availability");
    DataStoreReference dsBooking =
        datastore(model, carrierProcess, "ds_booking", "Booking repository");

    DataObjectReference doBookingInfo =
        dataObject(model, carrierProcess, "do_bookingInfo", "Booking request information");
    DataObjectReference doRoutePlan =
        dataObject(model, carrierProcess, "do_routePlan", "Route plan");
    DataObjectReference doConfDoc =
        dataObject(model, carrierProcess, "do_custConf", "Customer booking confirmation");

    ServiceTask tReqAddCap = byId(model, "requestAdditionalCapacity");
    ServiceTask tIdentifyOpt = byId(model, "identifyOptions");
    ServiceTask tCoordInland = byId(model, "coordinateInlandTransport");
    ServiceTask tCreateRoute = byId(model, "createRoutePlan");
    ServiceTask tGenConf = byId(model, "generateBookingConfirmation");
    ServiceTask tReject = byId(model, "rejectBooking");
    EndEvent eValidated = byId(model, "bookingValidated");
    StartEvent eStart = byId(model, "bookingRegistered");

    messageFlow(model, collab, "mf_bookingInfo_in", customer, eStart, msgBookingInfo); // in
    messageFlow(
        model,
        collab,
        "mf_capacity_request",
        carrier,
        tReqAddCap,
        msgCapacityReq,
        networkOps); // out
    messageFlow(
        model, collab, "mf_space_reply", networkOps, tReqAddCap, msgSpaceReply, carrier); // in
    messageFlow(
        model,
        collab,
        "mf_inland_request",
        carrier,
        tCoordInland,
        msgInlandReq,
        inlandProvider); // out
    messageFlow(
        model,
        collab,
        "mf_haulier_response",
        inlandProvider,
        tCoordInland,
        msgHaulierResp,
        carrier); // in
    messageFlow(
        model, collab, "mf_route_plan_out", carrier, tCreateRoute, msgRoutePlan, customer); // out
    messageFlow(
        model, collab, "mf_validation_out", carrier, tGenConf, msgValidation, customer); // out
    messageFlow(model, collab, "mf_rejection_out", carrier, tReject, msgRejection, customer); // out
    messageFlow(
        model,
        collab,
        "mf_customer_booking_confirmation",
        carrier,
        tGenConf,
        msgCustConf,
        customer);
    messageFlow(model, collab, "mf_confirmed_booking", carrier, eValidated, msgConfirmed, customer);

    ServiceTask tValVessel = byId(model, "validateVessel");
    ServiceTask tValInland = byId(model, "validateInland");
    ServiceTask tValEquip = byId(model, "validateEquipment");

    readFromStore(model, tValVessel, dsVessel);
    readFromStore(model, tValInland, dsInland);
    readFromStore(model, tValEquip, dsEquip);
    writeToStore(model, tReject, dsBooking);
    writeToStore(model, tGenConf, dsBooking);

    writeToObject(model, tCreateRoute, doRoutePlan);
    writeToObject(model, tGenConf, doConfDoc);
    readFromObject(model, tCreateRoute, doBookingInfo);
    readFromObject(model, tGenConf, doBookingInfo);

    // ====== 3) NEW: auto-layout internal flow nodes, data objects/stores =======================
    autoLayoutProcess(model, carrierProcess); // compute shapes and bounds for all inner nodes

    // ====== 4) Reframe diagram as collaboration and draw pools + message flows ================
    Collection<MessageFlow> allMessageFlows = model.getModelElementsByType(MessageFlow.class);
    updateDiagramToShowCollaboration(
        model,
        collab,
        carrier,
        customer,
        networkOps,
        inlandProvider,
        equipmentMgr,
        allMessageFlows); // uses fresh bounds
  }

  private static void messageFlow(
      BpmnModelInstance model,
      Collaboration collab,
      String id,
      Participant sourcePool,
      FlowNode sourceNode,
      Message msg) {

    MessageFlow mf = model.newInstance(MessageFlow.class);
    mf.setAttributeValue("id", id, true, false);
    mf.setName(msg.getName());
    mf.setMessage(msg);
    mf.setSource(sourcePool);
    mf.setTarget((InteractionNode) sourceNode);
    collab.addChildElement(mf);
  }

  private static void messageFlow(
      BpmnModelInstance model,
      Collaboration collab,
      String id,
      Participant sourcePool,
      FlowNode sourceNode,
      Message msg,
      Participant targetPool) {

    MessageFlow mf = model.newInstance(MessageFlow.class);
    mf.setAttributeValue("id", id, true, false);
    mf.setName(msg.getName());
    mf.setMessage(msg);

    // BPMN spec allows MessageFlow between participants,
    // Camunda accepts FlowNode→Participant as well
    mf.setSource(sourcePool);
    mf.setTarget(targetPool);

    collab.addChildElement(mf);
  }

  // ====== Auto-Layout Engine ==============================================================
  private static final double NODE_W = 150, NODE_H = 70;
  private static final double COL_X = 200, ROW_Y = 110;
  private static final double START_X = 200, START_Y = 160;

  private static void autoLayoutProcess(BpmnModelInstance model, Process process) {
    final double NODE_W = 150, NODE_H = 70;
    final double COL_X = 350, ROW_Y = 150;
    final double START_X = 200, START_Y = 160;

    BpmnDiagram diagram = getOrCreateDiagram(model);
    BpmnPlane plane = diagram.getBpmnPlane();
    plane.setBpmnElement(process);

    // Build adjacency
    Map<String, FlowNode> nodes =
        process.getChildElementsByType(FlowNode.class).stream()
            .collect(Collectors.toMap(FlowNode::getId, n -> n));
    Collection<SequenceFlow> flows = process.getChildElementsByType(SequenceFlow.class);

    Map<FlowNode, List<FlowNode>> succ = new HashMap<>();
    for (FlowNode n : nodes.values()) succ.put(n, new ArrayList<>());
    for (SequenceFlow f : flows) succ.get((FlowNode) f.getSource()).add((FlowNode) f.getTarget());

    // BFS layering
    Map<FlowNode, Integer> col = new HashMap<>();
    Queue<FlowNode> q = new ArrayDeque<>();
    for (StartEvent s : process.getChildElementsByType(StartEvent.class)) {
      col.put(s, 0);
      q.add(s);
    }
    while (!q.isEmpty()) {
      FlowNode u = q.remove();
      int c = col.get(u);
      for (FlowNode v : succ.get(u)) {
        if (!col.containsKey(v)) {
          col.put(v, c + 1);
          q.add(v);
        }
      }
    }

    // Row placement
    Map<Integer, List<FlowNode>> cols = new HashMap<>();
    for (FlowNode n : nodes.values())
      cols.computeIfAbsent(col.getOrDefault(n, 0), k -> new ArrayList<>()).add(n);

    Map<FlowNode, Double> x = new HashMap<>(), y = new HashMap<>();
    for (Map.Entry<Integer, List<FlowNode>> e : cols.entrySet()) {
      int c = e.getKey();
      List<FlowNode> list = e.getValue();
      list.sort(Comparator.comparing(FlowNode::getId));
      double top = START_Y - ((list.size() - 1) * ROW_Y) / 2.0;
      for (int i = 0; i < list.size(); i++) {
        FlowNode n = list.get(i);
        x.put(n, START_X + c * COL_X);
        y.put(n, top + i * ROW_Y);
      }
    }

    // Clear old shapes/edges
    for (BpmnEdge e : new ArrayList<>(plane.getChildElementsByType(BpmnEdge.class))) {
      plane.removeChildElement(e);
    }

    // Draw shapes
    for (FlowNode n : nodes.values())
      upsertShape(model, plane, n, x.get(n), y.get(n), NODE_W, NODE_H);

    // Cache bounds
    Map<String, Bounds> bounds = new HashMap<>();
    for (BpmnShape s : plane.getChildElementsByType(BpmnShape.class)) {
      if (s.getBounds() != null) bounds.put(s.getBpmnElement().getId(), s.getBounds());
    }

    // Rebuild edges
    for (SequenceFlow f : flows) {
      FlowNode s = (FlowNode) f.getSource();
      FlowNode t = (FlowNode) f.getTarget();
      Bounds sb = bounds.get(s.getId());
      Bounds tb = bounds.get(t.getId());
      if (sb == null || tb == null) continue;

      double sx = sb.getX() + sb.getWidth();
      double sy = sb.getY() + sb.getHeight() / 2;
      double tx = tb.getX();
      double ty = tb.getY() + tb.getHeight() / 2;

      BpmnEdge edge = model.newInstance(BpmnEdge.class);
      edge.setBpmnElement(f);
      Waypoint w1 = model.newInstance(Waypoint.class);
      w1.setX(sx);
      w1.setY(sy);
      Waypoint w2 = model.newInstance(Waypoint.class);
      w2.setX(tx);
      w2.setY(ty);
      edge.addChildElement(w1);
      edge.addChildElement(w2);
      plane.addChildElement(edge);
    }

    placeArtifacts(model, plane, process);
  }

  private static void placeArtifacts(BpmnModelInstance model, BpmnPlane plane, Process process) {
    // cache element centers
    Map<String, double[]> centers = new HashMap<>();
    for (BpmnShape s : plane.getChildElementsByType(BpmnShape.class)) {
      BaseElement el = s.getBpmnElement();
      if (el instanceof FlowNode) {
        Bounds b = s.getBounds();
        centers.put(
            el.getId(), new double[] {b.getX() + b.getWidth() / 2, b.getY() + b.getHeight() / 2});
      }
    }
    // DataStores near tasks that write to them
    for (DataOutputAssociation doa : process.getChildElementsByType(DataOutputAssociation.class)) {
      if (doa.getTarget() instanceof DataStoreReference
          && doa.getParentElement() instanceof Activity) {
        Activity t = (Activity) doa.getParentElement();
        DataStoreReference ds = (DataStoreReference) doa.getTarget();
        double[] c = centers.getOrDefault(t.getId(), new double[] {START_X, START_Y});
        upsertShape(model, plane, ds, c[0] - 90, c[1] - 140, 120, 50);
      }
    }
    // DataObjects near tasks that write to them, or read from them
    for (DataOutputAssociation doa : process.getChildElementsByType(DataOutputAssociation.class)) {
      if (doa.getTarget() instanceof DataObjectReference
          && doa.getParentElement() instanceof Activity) {
        Activity t = (Activity) doa.getParentElement();
        DataObjectReference d = (DataObjectReference) doa.getTarget();
        double[] c = centers.getOrDefault(t.getId(), new double[] {START_X, START_Y});
        upsertShape(model, plane, d, c[0] - 60, c[1] + 110, 110, 45);
      }
    }
    for (DataInputAssociation dia : process.getChildElementsByType(DataInputAssociation.class)) {
      if (dia.getTarget() != null && dia.getParentElement() instanceof Activity) {
        Activity t = (Activity) dia.getParentElement();
        for (ModelElementInstance src : dia.getSources()) {
          if (src instanceof DataObjectReference) {
            DataObjectReference d = (DataObjectReference) src;
            double[] c = centers.getOrDefault(t.getId(), new double[] {START_X, START_Y});
            upsertShape(model, plane, d, c[0] - 60, c[1] + 110, 110, 45);
          } else if (src instanceof DataStoreReference) {
            DataStoreReference ds = (DataStoreReference) src;
            double[] c = centers.getOrDefault(t.getId(), new double[] {START_X, START_Y});
            upsertShape(model, plane, ds, c[0] - 90, c[1] - 140, 120, 50);
          }
        }
      }
    }
  }

  private static void upsertShape(
      BpmnModelInstance model,
      BpmnPlane plane,
      BaseElement el,
      double x,
      double y,
      double w,
      double h) {
    // find existing shape or create
    BpmnShape shape = null;
    for (BpmnShape s : plane.getChildElementsByType(BpmnShape.class)) {
      if (el.equals(s.getBpmnElement())) {
        shape = s;
        break;
      }
    }
    if (shape == null) {
      shape = model.newInstance(BpmnShape.class);
      shape.setBpmnElement(el);
      plane.addChildElement(shape);
    }
    Bounds b = shape.getBounds();
    if (b == null) {
      b = model.newInstance(Bounds.class);
      shape.addChildElement(b);
    }
    b.setX(x);
    b.setY(y);
    b.setWidth(w);
    b.setHeight(h);
  }

  private static BpmnDiagram getOrCreateDiagram(BpmnModelInstance model) {
    Collection<BpmnDiagram> ds = model.getModelElementsByType(BpmnDiagram.class);
    if (!ds.isEmpty()) return ds.iterator().next();
    BpmnDiagram d = model.newInstance(BpmnDiagram.class);
    BpmnPlane p = model.newInstance(BpmnPlane.class);
    d.setBpmnPlane(p);
    model.getDefinitions().addChildElement(d);
    return d;
  }

  // ====== Collaboration framing, message flow edges (updated to use fresh bounds) ============
  private static void updateDiagramToShowCollaboration(
      BpmnModelInstance model,
      Collaboration collab,
      Participant carrier,
      Participant customer,
      Participant networkOps,
      Participant inlandProvider,
      Participant equipmentMgr,
      Collection<MessageFlow> messageFlows) {

    BpmnDiagram diagram = getOrCreateDiagram(model);
    BpmnPlane plane = diagram.getBpmnPlane();
    plane.setBpmnElement(collab);

    // --- remove existing message/sequence edges ---
    List<BpmnEdge> edgesToRemove = new ArrayList<>();
    for (BpmnEdge e : plane.getChildElementsByType(BpmnEdge.class)) {
      BaseElement be = e.getBpmnElement();
      if (be instanceof SequenceFlow || be instanceof MessageFlow) {
        edgesToRemove.add(e);
      }
    }
    for (BpmnEdge e : edgesToRemove) {
      plane.removeChildElement(e);
    }

    // --- compute process bounding box ---
    double[] bbox = calculateProcessBoundingBox(plane);
    double minX = bbox[0], minY = bbox[1], maxX = bbox[2], maxY = bbox[3];

    double poolHeaderWidth = 30, padding = 40;
    double carrierX = minX - poolHeaderWidth - padding;
    double carrierY = minY - padding;
    double carrierWidth = (maxX - minX) + poolHeaderWidth + (2 * padding);
    double carrierHeight = (maxY - minY) + (2 * padding);

    double poolHeight = 150, poolSpacing = 20;

    // --- pools ---
    addParticipantShape(model, plane, carrier, carrierX, carrierY, carrierWidth, carrierHeight);
    double nextY = carrierY + carrierHeight + poolSpacing;
    addParticipantShape(model, plane, customer, carrierX, nextY, carrierWidth, poolHeight);
    nextY += poolHeight + poolSpacing;
    addParticipantShape(model, plane, networkOps, carrierX, nextY, carrierWidth, poolHeight);
    nextY += poolHeight + poolSpacing;
    addParticipantShape(model, plane, inlandProvider, carrierX, nextY, carrierWidth, poolHeight);
    nextY += poolHeight + poolSpacing;
    addParticipantShape(model, plane, equipmentMgr, carrierX, nextY, carrierWidth, poolHeight);

    // --- shape centers for connection points ---
    class Box {
      double x, y, w, h;
    }
    Map<String, Box> box = new HashMap<>();
    for (BpmnShape s : plane.getChildElementsByType(BpmnShape.class)) {
      if (s.getBpmnElement() instanceof Participant) continue;
      Bounds b = s.getBounds();
      if (b == null) continue;
      Box bx = new Box();
      bx.x = b.getX();
      bx.y = b.getY();
      bx.w = b.getWidth();
      bx.h = b.getHeight();
      box.put(s.getBpmnElement().getId(), bx);
    }

    // --- reconnect sequence flows with accurate anchors ---
    for (SequenceFlow sf : model.getModelElementsByType(SequenceFlow.class)) {
      FlowNode src = sf.getSource();
      FlowNode tgt = sf.getTarget();
      Box sb = box.get(src.getId());
      Box tb = box.get(tgt.getId());
      if (sb == null || tb == null) continue;

      double sx = sb.x + sb.w;
      double sy = sb.y + sb.h / 2;
      double tx = tb.x;
      double ty = tb.y + tb.h / 2;

      BpmnEdge edge = model.newInstance(BpmnEdge.class);
      edge.setBpmnElement(sf);
      Waypoint w1 = model.newInstance(Waypoint.class);
      w1.setX(sx);
      w1.setY(sy);
      Waypoint w2 = model.newInstance(Waypoint.class);
      w2.setX(tx);
      w2.setY(ty);
      edge.addChildElement(w1);
      edge.addChildElement(w2);
      plane.addChildElement(edge);
    }

    // --- message flows ---
    double customerY = carrierY + carrierHeight + poolSpacing + poolHeight / 2;
    double networkY = customerY + poolHeight + poolSpacing;
    double inlandY = networkY + poolHeight + poolSpacing;
    double equipY = inlandY + poolHeight + poolSpacing;

    for (MessageFlow mf : messageFlows) {
      InteractionNode src = mf.getSource();
      InteractionNode tgt = mf.getTarget();
      Box sb = (src instanceof BaseElement) ? box.get(((BaseElement) src).getId()) : null;
      Box tb = (tgt instanceof BaseElement) ? box.get(((BaseElement) tgt).getId()) : null;

      double sx = (sb != null) ? sb.x + sb.w / 2 : carrierX;
      double sy = (sb != null) ? sb.y + sb.h / 2 : carrierY;
      double tx = (tb != null) ? tb.x + tb.w / 2 : carrierX;
      double ty = (tb != null) ? tb.y + tb.h / 2 : equipY;

      BpmnEdge edge = model.newInstance(BpmnEdge.class);
      edge.setBpmnElement(mf);
      Waypoint w1 = model.newInstance(Waypoint.class);
      w1.setX(sx);
      w1.setY(sy);
      Waypoint w2 = model.newInstance(Waypoint.class);
      w2.setX(tx);
      w2.setY(ty);
      edge.addChildElement(w1);
      edge.addChildElement(w2);
      plane.addChildElement(edge);
    }
  }

  private static double[] calculateProcessBoundingBox(BpmnPlane plane) {
    double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

    for (BpmnShape s : plane.getChildElementsByType(BpmnShape.class)) {
      if (s.getBpmnElement() instanceof Participant) continue; // ignore pools for carrier bbox
      Bounds b = s.getBounds();
      if (b == null) continue;
      minX = Math.min(minX, b.getX());
      minY = Math.min(minY, b.getY());
      maxX = Math.max(maxX, b.getX() + b.getWidth());
      maxY = Math.max(maxY, b.getY() + b.getHeight());
    }
    if (!Double.isFinite(minX)) {
      minX = START_X;
      minY = START_Y;
      maxX = START_X + NODE_W;
      maxY = START_Y + NODE_H;
    }
    return new double[] {minX, minY, maxX, maxY};
  }

  private static void addParticipantShape(
      BpmnModelInstance model,
      BpmnPlane plane,
      Participant p,
      double x,
      double y,
      double w,
      double h) {
    BpmnShape shape = model.newInstance(BpmnShape.class);
    shape.setBpmnElement(p);
    shape.setHorizontal(true);
    Bounds b = model.newInstance(Bounds.class);
    b.setX(x);
    b.setY(y);
    b.setWidth(w);
    b.setHeight(h);
    shape.addChildElement(b);
    plane.addChildElement(shape);
  }

  private static void addMessageFlowEdges(
      BpmnModelInstance model,
      BpmnPlane plane,
      Collection<MessageFlow> mfs,
      double carrierY,
      double carrierH,
      double otherStartY,
      double poolH,
      double poolSpacing,
      double poolX,
      double poolW) {

    Map<String, double[]> pos = new HashMap<>();
    for (BpmnShape s : plane.getChildElementsByType(BpmnShape.class)) {
      Bounds b = s.getBounds();
      if (b == null) continue;
      BaseElement el = s.getBpmnElement();
      if (el != null && !(el instanceof Participant)) {
        pos.put(
            el.getId(),
            new double[] {
              b.getX() + b.getWidth() / 2,
              b.getY() + b.getHeight() / 2,
              b.getY(),
              b.getY() + b.getHeight()
            });
      }
    }

    double customerY = otherStartY + poolH / 2;
    double networkY = otherStartY + poolH + poolSpacing + poolH / 2;
    double inlandY = otherStartY + 2 * (poolH + poolSpacing) + poolH / 2;
    double centerX = poolX + poolW / 2;

    for (MessageFlow mf : mfs) {
      BpmnEdge edge = model.newInstance(BpmnEdge.class);
      edge.setBpmnElement(mf);

      InteractionNode s = mf.getSource();
      InteractionNode t = mf.getTarget();
      double sx = centerX, sy = carrierY + carrierH; // default bottom of carrier
      double tx = centerX, ty = customerY;

      if (s instanceof FlowNode) {
        double[] p = pos.get(((FlowNode) s).getId());
        if (p != null) {
          sx = p[0];
          sy = p[3];
        }
      }
      if (t instanceof Participant) {
        String id = ((Participant) t).getId();
        if ("p_network".equals(id) || "p_networkOps".equals(id)) ty = networkY;
        else if ("p_inland".equals(id)) ty = inlandY;
        else if ("p_customer".equals(id)) ty = customerY;
      } else if (t instanceof FlowNode) {
        double[] p = pos.get(((FlowNode) t).getId());
        if (p != null) {
          tx = p[0];
          ty = p[2];
          sy = customerY;
        }
      }

      Waypoint w1 = model.newInstance(Waypoint.class);
      w1.setX(sx);
      w1.setY(sy);
      Waypoint w2 = model.newInstance(Waypoint.class);
      w2.setX(tx);
      w2.setY(ty);
      edge.addChildElement(w1);
      edge.addChildElement(w2);
      plane.addChildElement(edge);
    }
  }

  // ====== Shared helpers (same as your file, plus small fixes) ==============================
  private static <T extends ModelElementInstance> T create(
      BpmnModelInstance m, ModelElementInstance parent, String id, Class<T> type) {
    T el = m.newInstance(type);
    el.setAttributeValue("id", id, true, false);
    if (parent != null) parent.addChildElement(el);
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

  private static DataStoreReference datastore(
      BpmnModelInstance m, Process process, String id, String name) {
    DataStore ds = create(m, m.getDefinitions(), id + "_def", DataStore.class);
    ds.setName(name);
    DataStoreReference ref = create(m, process, id, DataStoreReference.class);
    ref.setName(name);
    ref.setDataStore(ds);
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

  private static <T extends BaseElement> T byId(BpmnModelInstance m, String id) {
    T el = m.getModelElementById(id);
    if (el == null) throw new IllegalStateException("Missing element: " + id);
    return el;
  }

  // IO helpers (from your file) :contentReference[oaicite:3]{index=3}
  private static IoSpecification ensureIoSpec(BpmnModelInstance m, Activity task) {
    for (ModelElementInstance child : task.getChildElementsByType(IoSpecification.class))
      return (IoSpecification) child;
    IoSpecification ioSpec = m.newInstance(IoSpecification.class);
    task.addChildElement(ioSpec);
    InputSet is = m.newInstance(InputSet.class);
    ioSpec.addChildElement(is);
    OutputSet os = m.newInstance(OutputSet.class);
    ioSpec.addChildElement(os);
    return ioSpec;
  }

  private static void ensureInputSet(BpmnModelInstance m, IoSpecification ioSpec, DataInput di) {
    InputSet is =
        ioSpec.getChildElementsByType(InputSet.class).stream()
            .findFirst()
            .orElseGet(
                () -> {
                  InputSet x = m.newInstance(InputSet.class);
                  ioSpec.addChildElement(x);
                  return x;
                });
    is.getDataInputs().add(di);
  }

  private static void ensureOutputSet(
      BpmnModelInstance m, IoSpecification ioSpec, DataOutput dout) {
    OutputSet os =
        ioSpec.getChildElementsByType(OutputSet.class).stream()
            .findFirst()
            .orElseGet(
                () -> {
                  OutputSet x = m.newInstance(OutputSet.class);
                  ioSpec.addChildElement(x);
                  return x;
                });
    os.getDataOutputRefs().add(dout);
  }

  private static void readFromStore(BpmnModelInstance m, Activity t, DataStoreReference s) {
    String base = t.getId() + "_in_" + s.getId();
    IoSpecification io = ensureIoSpec(m, t);
    DataInput di = m.newInstance(DataInput.class);
    di.setAttributeValue("id", base, true, false);
    io.addChildElement(di);
    ensureInputSet(m, io, di);
    DataInputAssociation a = create(m, t, base + "_assoc", DataInputAssociation.class);
    a.getSources().add(s);
    a.setTarget(di);
  }

  private static void writeToStore(BpmnModelInstance m, Activity t, DataStoreReference s) {
    String base = t.getId() + "_out_" + s.getId();
    IoSpecification io = ensureIoSpec(m, t);
    DataOutput dout = m.newInstance(DataOutput.class);
    dout.setAttributeValue("id", base, true, false);
    io.addChildElement(dout);
    ensureOutputSet(m, io, dout);
    DataOutputAssociation a = create(m, t, base + "_assoc", DataOutputAssociation.class);
    a.getSources().add(dout);
    a.setTarget(s);
  }

  private static void readFromObject(BpmnModelInstance m, Activity t, DataObjectReference o) {
    String base = t.getId() + "_in_" + o.getId();
    IoSpecification io = ensureIoSpec(m, t);
    DataInput di = m.newInstance(DataInput.class);
    di.setAttributeValue("id", base, true, false);
    io.addChildElement(di);
    ensureInputSet(m, io, di);
    DataInputAssociation a = create(m, t, base + "_assoc", DataInputAssociation.class);
    a.getSources().add(o);
    a.setTarget(di);
  }

  private static void writeToObject(BpmnModelInstance m, Activity t, DataObjectReference o) {
    String base = t.getId() + "_out_" + o.getId();
    IoSpecification io = ensureIoSpec(m, t);
    DataOutput dout = m.newInstance(DataOutput.class);
    dout.setAttributeValue("id", base, true, false);
    io.addChildElement(dout);
    ensureOutputSet(m, io, dout);
    DataOutputAssociation a = create(m, t, base + "_assoc", DataOutputAssociation.class);
    a.getSources().add(dout);
    a.setTarget(o);
  }

  // ====== Persistence + demo runner (unchanged) =============================================
  public void saveBpmnToFile(BpmnModelInstance modelInstance, String filePath) throws IOException {
    File f = new File(filePath);
    File dir = f.getParentFile();
    if (dir != null && !dir.exists()) dir.mkdirs();
    Bpmn.writeModelToFile(f, modelInstance);
  }

  public static void main(String[] args) {
    BlueprintModule blueprint = new BlueprintModule();
    BpmnModelInstance model = blueprint.createExampleProcess();
    augment(model); // includes auto-layout + collaboration framing
    String out = "blueprint/generated-resources/example-process.bpmn";
    try {
      blueprint.saveBpmnToFile(model, out);
      System.out.println("Saved: " + out);
      String png = out.replace(".bpmn", ".png");
      runBpmnToImage(out, png);
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      Thread.currentThread().interrupt();
    }
  }

  private static void runBpmnToImage(String bpmnPath, String pngPath)
      throws IOException, InterruptedException {
    ProcessBuilder pb =
        new ProcessBuilder("cmd.exe", "/c", "npx", "bpmn-to-image", bpmnPath + ";" + pngPath);
    pb.directory(new File("."));
    pb.redirectErrorStream(true);
    java.lang.Process p = pb.start();
    try (java.io.BufferedReader r =
        new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
      String line;
      while ((line = r.readLine()) != null) System.out.println(line);
    }
    int code = p.waitFor();
    if (code != 0) System.err.println("bpmn-to-image exit: " + code);
  }
}
