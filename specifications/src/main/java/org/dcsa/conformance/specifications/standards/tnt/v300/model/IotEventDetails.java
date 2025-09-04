package org.dcsa.conformance.specifications.standards.tnt.v300.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.dcsa.conformance.specifications.generator.ClearParentProperties;
import org.dcsa.conformance.specifications.standards.tnt.v300.types.IotEventTypeCode;

@Schema(description = "IoT event details")
@Getter
@Setter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
@ClearParentProperties
public class IotEventDetails extends AbstractEventDetails {

  @Schema() private IotEventTypeCode iotEventTypeCode;
}
