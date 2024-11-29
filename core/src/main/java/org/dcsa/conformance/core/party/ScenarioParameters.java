package org.dcsa.conformance.core.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Interface to support Supplied and Dynamic Scenario Parameters.
 *
 * <p>SuppliedScenarioParameters (SSP): mechanism that allows an adopter to "parameterize" a
 * scenario by providing certain config values without which the synthetic party in the sandbox
 * could not send requests that the actual implementation would be able to accept. Usually provided
 * by one party, sometimes with separate implementations for both parties.
 *
 * <p>DynamicScenarioParameters (DSP): parameters that get captured by the orchestrator while the
 * scenario unfolds, by analyzing the traffic data. How to use them: when the Orchestrator calls the
 * handleExchange() of the Action class, that action probably needs to save these ids in the DSP so
 * that they can be picked up and embedded in the JSON description of subsequent actions.
 *
 * <p>SSP and DSP are both captured by actions and saved as part of the orchestrator's state.
 */
public interface ScenarioParameters {

  default ObjectNode toJson() {
    return OBJECT_MAPPER.valueToTree(this);
  }
}
