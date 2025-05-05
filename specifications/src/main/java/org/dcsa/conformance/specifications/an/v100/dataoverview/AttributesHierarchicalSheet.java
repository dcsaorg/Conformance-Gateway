package org.dcsa.conformance.specifications.an.v100.dataoverview;

import java.util.List;

public class AttributesHierarchicalSheet extends DataOverviewSheet {
  protected AttributesHierarchicalSheet(AttributesData attributesData) {
    super(
        "Attributes hierarchical",
        "AttributesHierarchicalTable",
        List.of(
            "Path",
            "Attribute",
            "Type",
            "Required",
            "Size",
            "Pattern",
            "Example",
            "Description",
            "Constraints"),
        List.of(107, 33, 22, 11, 7, 17, 32, 96, 96),
        List.of(false, false, false, false, false, false, true, true, true),
        attributesData.getHierarchicalRows());
  }
}
