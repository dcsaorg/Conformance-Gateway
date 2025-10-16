package org.dcsa.conformance.specifications.standards.vgm.v100.messages;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import org.dcsa.conformance.specifications.standards.vgm.v100.model.VGM;

@Data
@Schema(description = "API message containing a list of VGMs.")
public class PostVGMsRequest {

  @Schema(name = "VGMs", description = "List of VGMs.")
  private List<VGM> vgms;
}
