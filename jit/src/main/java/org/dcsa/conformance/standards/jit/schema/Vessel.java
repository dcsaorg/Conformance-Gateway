package org.dcsa.conformance.standards.jit.schema;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Represents a vessel with containers onboard")
public class Vessel {

  @Schema(description = "Vessel Name", example = "Ever Given")
  private String name;

  @Schema(description = "IMO Number (International Maritime Organization)", example = "9811000")
  private String imoNumber;

  @Schema(
      description = "List of containers on the vessel",
      example =
          """
          [
            {"type": "TEU", "equipmentReference": "TGHU1234567"}
          ]
          """)
  private List<Container> containers;

  public Vessel(String name, String imoNumber, List<Container> containers) {
    this.name = name;
    this.imoNumber = imoNumber;
    this.containers = containers;
  }

  public String getName() {
    return name;
  }

  public String getImoNumber() {
    return imoNumber;
  }

  public List<Container> getContainers() {
    return containers;
  }
}
