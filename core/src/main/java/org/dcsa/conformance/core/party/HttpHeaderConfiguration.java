package org.dcsa.conformance.core.party;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class HttpHeaderConfiguration {
  private String headerName;
  private String headerValue;

  public JsonNode toJsonNode() {
    return OBJECT_MAPPER.valueToTree(this);
  }

  @SneakyThrows
  public static HttpHeaderConfiguration fromJsonNode(JsonNode jsonNode) {
    return OBJECT_MAPPER.treeToValue(jsonNode, HttpHeaderConfiguration.class);
  }
}
