package org.dcsa.conformance.specifications.dataoverview;

import java.util.List;
import java.util.Map;

public class AttributesNormalizedSheet extends DataOverviewSheet {
  protected AttributesNormalizedSheet(
      AttributesData attributesData,
      Map<Class<? extends DataOverviewSheet>, List<List<String>>> oldDataValuesBySheetClass,
      Map<Class<? extends DataOverviewSheet>, Map<String, String>>
          changedPrimaryKeyByOldPrimaryKeyBySheetClass) {
    super(
        "Attributes normalized",
        "AttributesNormalizedTable",
        2,
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
        attributesData.getNormalizedRows(),
        oldDataValuesBySheetClass,
        changedPrimaryKeyByOldPrimaryKeyBySheetClass);
  }
}
