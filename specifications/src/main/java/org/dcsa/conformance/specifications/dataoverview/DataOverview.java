package org.dcsa.conformance.specifications.dataoverview;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dcsa.conformance.specifications.constraints.SchemaConstraint;
import org.dcsa.conformance.specifications.standards.an.v100.model.ArrivalNotice;
import org.dcsa.conformance.specifications.standards.an.v100.model.ArrivalNoticeNotification;

@Slf4j
public class DataOverview {

  private final AttributesHierarchicalSheet attributesHierarchicalSheet;
  private final AttributesNormalizedSheet attributesNormalizedSheet;
  private final QueryParametersSheet queryParametersSheet;
  private final QueryFiltersSheet queryFiltersSheet;

  public DataOverview(
      Map<String, Map<String, List<SchemaConstraint>>> constraintsByClassAndField,
      Map<String, Schema<?>> schemas,
      List<Parameter> queryParameters,
      Map<Boolean, List<List<Parameter>>> requiredAndOptionalFilters,
      Map<Class<? extends DataOverviewSheet>, List<List<String>>> oldDataValuesBySheetClass,
      Map<Class<? extends DataOverviewSheet>, Map<String, String>>
          changedPrimaryKeyByOldPrimaryKeyBySheetClass) {
    AttributesData attributesData =
        new AttributesData(
            constraintsByClassAndField,
            schemas,
            List.of(
                ArrivalNotice.class.getSimpleName(),
                ArrivalNoticeNotification.class.getSimpleName()));
    attributesHierarchicalSheet =
        new AttributesHierarchicalSheet(
            attributesData,
            oldDataValuesBySheetClass,
            changedPrimaryKeyByOldPrimaryKeyBySheetClass);
    attributesNormalizedSheet =
        new AttributesNormalizedSheet(
            attributesData,
            oldDataValuesBySheetClass,
            changedPrimaryKeyByOldPrimaryKeyBySheetClass);
    queryParametersSheet =
        new QueryParametersSheet(
            queryParameters,
            oldDataValuesBySheetClass,
            changedPrimaryKeyByOldPrimaryKeyBySheetClass);
    queryFiltersSheet =
        new QueryFiltersSheet(
            requiredAndOptionalFilters,
            oldDataValuesBySheetClass,
            changedPrimaryKeyByOldPrimaryKeyBySheetClass);
  }

  @SneakyThrows
  public void exportToExcelFile(String excelFilePath) {
    try (Workbook workbook = new XSSFWorkbook()) {
      AtomicLong nextId = new AtomicLong(0);
      Stream.of(
              attributesHierarchicalSheet,
              attributesNormalizedSheet,
              queryParametersSheet,
              queryFiltersSheet)
          .forEach(sheet -> sheet.addToExcelWorkbook(workbook, nextId::incrementAndGet));
      try (FileOutputStream fileOut = new FileOutputStream(excelFilePath)) {
        workbook.write(fileOut);
      }
    }
    log.info("Data Overview exported to {}", excelFilePath);
  }

  public void exportToCsvFiles(String csvFilePattern) {
    Stream.of(
            attributesHierarchicalSheet,
            attributesNormalizedSheet,
            queryParametersSheet,
            queryFiltersSheet)
        .forEach(sheet -> sheet.exportToCsvFile(csvFilePattern));
  }
}
