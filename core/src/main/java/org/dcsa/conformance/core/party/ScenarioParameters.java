package org.dcsa.conformance.core.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface ScenarioParameters {

  default ObjectNode toJson() {
    return OBJECT_MAPPER.valueToTree(this);
  }

}
