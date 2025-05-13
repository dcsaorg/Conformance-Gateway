package org.dcsa.conformance.specifications.an.v100.dataoverview;

import java.util.List;
import java.util.Map;

public class AttributesNormalizedSheet extends DataOverviewSheet {
  protected AttributesNormalizedSheet(AttributesData attributesData) {
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
        importFromCsvFile(
            "https://raw.githubusercontent.com/dcsaorg/Conformance-Gateway/bd25914e5526aa7c8a67aba8a28010555c82d1ad/specifications/generated-resources/an-v1.0.0-data-overview-attributes-normalized.csv"),
        Map.ofEntries(
            Map.entry("ArrivalNotice,issueDate", "ArrivalNotice,issueDateTime"),
            Map.entry("CustomsReference,crType", "CustomsReference,typeCode"),
            Map.entry("CustomsReference,values", "CustomsReference,referenceValues"),
            Map.entry("DangerousGoods,codedVariantList", "DangerousGoods,codedVariant"),
            Map.entry("NationalCommodityCode,nccType", "NationalCommodityCode,typeCode"),
            Map.entry("NationalCommodityCode,nccValues", "NationalCommodityCode,codeValues"),
            Map.entry("VesselVoyage,carrierImportVoyageNumber", "VesselVoyage,carrierVoyageNumber"),
            Map.entry(
                "VesselVoyage,universalImportVoyageReference",
                "VesselVoyage,universalVoyageReference")));
  }
}
