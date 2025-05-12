package org.dcsa.conformance.specifications.an.v100.dataoverview;

import java.util.List;
import java.util.Map;

public class AttributesHierarchicalSheet extends DataOverviewSheet {
  protected AttributesHierarchicalSheet(AttributesData attributesData) {
    super(
        "Attributes hierarchical",
        "AttributesHierarchicalTable",
        1,
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
        attributesData.getHierarchicalRows(),
        importFromCsvFile(
            "https://raw.githubusercontent.com/dcsaorg/Conformance-Gateway/bd25914e5526aa7c8a67aba8a28010555c82d1ad/specifications/generated-resources/an-v1.0.0-data-overview-attributes-hierarchical.csv"),
        Map.ofEntries(
            Map.entry(
                "ArrivalNotice / consignmentItems / cargoItems / outerPackaging / dangerousGoods / codedVariantList",
                "ArrivalNotice / consignmentItems / cargoItems / outerPackaging / dangerousGoods / codedVariant"),
            Map.entry(
                "ArrivalNotice / consignmentItems / customsReferences / crType",
                "ArrivalNotice / consignmentItems / customsReferences / typeCode"),
            Map.entry(
                "ArrivalNotice / consignmentItems / customsReferences / values",
                "ArrivalNotice / consignmentItems / customsReferences / referenceValues"),
            Map.entry(
                "ArrivalNotice / consignmentItems / nationalCommodityCodes / nccType",
                "ArrivalNotice / consignmentItems / nationalCommodityCodes / typeCode"),
            Map.entry(
                "ArrivalNotice / consignmentItems / nationalCommodityCodes / nccValues",
                "ArrivalNotice / consignmentItems / nationalCommodityCodes / codeValues"),
            Map.entry("ArrivalNotice / issueDate", "ArrivalNotice / issueDateTime"),
            Map.entry("ArrivalNotice / vesselVoyage", "ArrivalNotice / transport / vesselVoyages"),
            Map.entry(
                "ArrivalNotice / vesselVoyage / carrierImportVoyageNumber",
                "ArrivalNotice / transport / vesselVoyages / carrierVoyageNumber"),
            Map.entry(
                "ArrivalNotice / vesselVoyage / typeCode",
                "ArrivalNotice / transport / vesselVoyages / typeCode"),
            Map.entry(
                "ArrivalNotice / vesselVoyage / universalImportVoyageReference",
                "ArrivalNotice / transport / vesselVoyages / universalVoyageReference"),
            Map.entry(
                "ArrivalNotice / vesselVoyage / vesselFlag",
                "ArrivalNotice / transport / vesselVoyages / vesselFlag"),
            Map.entry(
                "ArrivalNotice / vesselVoyage / vesselIMONumber",
                "ArrivalNotice / transport / vesselVoyages / vesselIMONumber"),
            Map.entry(
                "ArrivalNotice / vesselVoyage / vesselName",
                "ArrivalNotice / transport / vesselVoyages / vesselName")));
  }
}
