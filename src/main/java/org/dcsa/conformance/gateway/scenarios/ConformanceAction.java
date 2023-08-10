package org.dcsa.conformance.gateway.scenarios;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

@AllArgsConstructor
public class ConformanceAction {
  @Getter protected final UUID id = UUID.randomUUID();
  @Getter private final String sourcePartyName;
  @Getter private final String targetPartyName;

  public ObjectNode asJsonNode() {
    return new ObjectMapper()
        .createObjectNode()
        .put("actionId", id.toString())
        .put("actionType", getClass().getCanonicalName());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ConformanceAction that = (ConformanceAction) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
