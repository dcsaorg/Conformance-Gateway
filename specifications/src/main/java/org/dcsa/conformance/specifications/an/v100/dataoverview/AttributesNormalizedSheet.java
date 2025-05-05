package org.dcsa.conformance.specifications.an.v100.dataoverview;

import java.util.List;

public class AttributesNormalizedSheet extends DataOverviewSheet {
  protected AttributesNormalizedSheet(AttributesData attributesData) {
    super(
        "Attributes normalized",
        "AttributesNormalizedTable",
        List.of(
            "Object",
            "Attribute",
            "Type",
            "Required",
            "Size",
            "Pattern",
            "Example",
            "Description",
            "Constraints"),
        List.of(21, 33, 22, 11, 7, 17, 32, 96, 96),
        List.of(false, false, false, false, false, false, true, true, true),
        attributesData.getNormalizedRows());
  }
}
