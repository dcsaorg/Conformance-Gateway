package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Rail transport information")
@Data
public class RailTransport {

  @Schema(
      maxLength = 50,
      description =
"""
A railcar is a type of railway vehicle that is designed to transport freight or passengers on a railway track.
They are also known as rail vehicles, railcars, or rolling stock.
Railcars can be powered by an on-board locomotive or they can be pulled by a separate locomotive.
""")
  private String railCar;

  @Schema(
    maxLength = 50,
    description =
"""
A rail service number is a unique identifying number assigned to a specific rail service or train.
""")
  private String railService;

  @Schema(
    maxLength = 100,
    description =
"""
Unique identifying number or code that is assigned to a specific departure of a rail.
(Also known as a departure reference number.)
""")
  private String departureID;
}
