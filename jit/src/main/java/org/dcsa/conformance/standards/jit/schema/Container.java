package org.dcsa.conformance.standards.jit.schema;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a container on a vessel")
public class Container {

  @Schema(description = "Type of container (TEU/FEU)", example = "TEU")
  private String type;

  @Schema(description = "Equipment reference number", example = "TGHU1234567")
  private String equipmentReference;

  public Container(String type, String equipmentReference) {
    this.type = type;
    this.equipmentReference = equipmentReference;
  }

  public String getType() {
    return type;
  }

  public String getEquipmentReference() {
    return equipmentReference;
  }
}
