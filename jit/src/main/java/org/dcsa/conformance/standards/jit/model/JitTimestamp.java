package org.dcsa.conformance.standards.jit.model;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.With;

@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JitTimestamp(
    String timestampID,
    String replyToTimestampID,
    String portCallServiceID,
    JitClassifierCode classifierCode,
    String dateTime,
    String delayReasonCode,
    boolean isFYI,
    String remark) {

  public ObjectNode toJson() {
    return OBJECT_MAPPER.valueToTree(this);
  }

  public static JitTimestamp fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, JitTimestamp.class);
  }
}
