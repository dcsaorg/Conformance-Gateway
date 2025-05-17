package org.dcsa.conformance.specifications.dataoverview;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTAutoFilter;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumn;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableColumns;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableStyleInfo;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STTableType;

@Slf4j
public abstract class DataOverviewSheet {
  private final String sheetName;
  private final String tableName;
  private final int primaryKeyColumnCount;
  private final List<String> csvHeaderTitles;
  private final List<String> excelHeaderTitles;
  private final List<Integer> excelColumnWidths;
  private final List<Boolean> excelWrapCellText;
  private final List<List<String>> csvDataValues;
  private final List<Map.Entry<DataOverviewDiffStatus, List<String>>> excelDataValues;

  protected DataOverviewSheet(
      String sheetName,
      String tableName,
      int primaryKeyColumnCount,
      List<String> headerTitles,
      List<Integer> columnWidths,
      List<Boolean> wrapCellText,
      List<List<String>> dataValues,
      Map<Class<? extends DataOverviewSheet>, List<List<String>>> oldDataValuesBySheetClass,
      Map<Class<? extends DataOverviewSheet>, Map<String, String>>
          changedPrimaryKeyByOldPrimaryKeyBySheetClass) {
    this.sheetName = sheetName;
    this.tableName = tableName;
    this.primaryKeyColumnCount = primaryKeyColumnCount;
    this.csvHeaderTitles = headerTitles;
    this.excelHeaderTitles = Stream.concat(Stream.of("Diff"), headerTitles.stream()).toList();
    this.excelColumnWidths = Stream.concat(Stream.of(12), columnWidths.stream()).toList();
    this.excelWrapCellText =
        Stream.concat(Stream.of(Boolean.FALSE), wrapCellText.stream()).toList();
    this.csvDataValues = dataValues;

    excelDataValues = new ArrayList<>();
    Map<String, List<String>> oldRowValuesByPrimaryKey =
        rowValuesByPrimaryKey(oldDataValuesBySheetClass.get(getClass()));
    Map<String, List<String>> newRowValuesByPrimaryKey = rowValuesByPrimaryKey(dataValues);

    Map<String, String> changedPrimaryKeyByOldPrimaryKey =
        new HashMap<>(changedPrimaryKeyByOldPrimaryKeyBySheetClass.get(getClass()));
    // expand old key - new key prefix mappings (skipping specified existing key mappings)
    changedPrimaryKeyByOldPrimaryKey.entrySet().stream()
        .filter(oldKeyNewKeyEntry -> oldKeyNewKeyEntry.getKey().endsWith("/"))
        .flatMap(
            oldKeyPrefixNewKeyPrefixEntry ->
                oldRowValuesByPrimaryKey.keySet().stream()
                    .filter(
                        oldKey ->
                            (oldKey.startsWith(oldKeyPrefixNewKeyPrefixEntry.getKey()))
                                && !changedPrimaryKeyByOldPrimaryKey.containsKey(oldKey))
                    .map(
                        oldKey ->
                            Map.entry(
                                oldKey,
                                oldKeyPrefixNewKeyPrefixEntry.getValue()
                                    + oldKey.substring(
                                        oldKeyPrefixNewKeyPrefixEntry.getKey().length()))))
        .toList()
        .forEach(
            expandedEntry ->
                changedPrimaryKeyByOldPrimaryKey.put(
                    expandedEntry.getKey(), expandedEntry.getValue()));

    Map<String, String> oldPrimaryKeysByNewPrimaryKey =
        changedPrimaryKeyByOldPrimaryKey.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    Set<String> sortedPrimaryKeys =
        Stream.concat(
                oldRowValuesByPrimaryKey.keySet().stream(),
                newRowValuesByPrimaryKey.keySet().stream())
            .collect(Collectors.toCollection(TreeSet::new));
    sortedPrimaryKeys.stream()
        // skip the old values of modified PKs
        .filter(key -> !changedPrimaryKeyByOldPrimaryKey.containsKey(key))
        .forEach(
            primaryKey -> {
              String newPrimaryKey =
                  newRowValuesByPrimaryKey.containsKey(primaryKey) ? primaryKey : null;
              List<String> newRowValues =
                  newPrimaryKey == null ? null : newRowValuesByPrimaryKey.get(newPrimaryKey);
              String oldPrimaryKey =
                  oldPrimaryKeysByNewPrimaryKey.computeIfAbsent(primaryKey, Function.identity());
              List<String> oldRowValues =
                  oldRowValuesByPrimaryKey.getOrDefault(oldPrimaryKey, null);
              if (newRowValues == null) {
                excelDataValues.add(Map.entry(DataOverviewDiffStatus.REMOVED, oldRowValues));
              } else if (oldRowValues == null) {
                excelDataValues.add(Map.entry(DataOverviewDiffStatus.ADDED, newRowValues));
              } else {
                AtomicBoolean anyValuesUpdated = new AtomicBoolean(false);
                AtomicInteger columnIndex = new AtomicInteger(0);
                List<String> updatedOldRowValues =
                    oldRowValues.stream()
                        .map(
                            oldValue -> {
                              String newValue = newRowValues.get(columnIndex.getAndIncrement());
                              if (newValue.equals(oldValue)
                                  || Objects.equals(
                                      oldValue, oldPrimaryKeysByNewPrimaryKey.get(newValue))) {
                                return "";
                              } else {
                                anyValuesUpdated.set(true);
                                return oldValue;
                              }
                            })
                        .toList();
                if (anyValuesUpdated.get()) {
                  excelDataValues.add(
                      Map.entry(DataOverviewDiffStatus.OLD_VALUE, updatedOldRowValues));
                  excelDataValues.add(Map.entry(DataOverviewDiffStatus.NEW_VALUE, newRowValues));
                } else {
                  excelDataValues.add(Map.entry(DataOverviewDiffStatus.UNMODIFIED, newRowValues));
                }
              }
            });
  }

  private Map<String, List<String>> rowValuesByPrimaryKey(List<List<String>> dataValues) {
    return dataValues.stream()
        .collect(
            Collectors.toMap(
                rowValues ->
                    primaryKeyColumnCount == 1
                        ? rowValues.getFirst()
                        : rowValues.stream()
                            .limit(primaryKeyColumnCount)
                            .collect(Collectors.joining(",")),
                Function.identity()));
  }

  public void addToExcelWorkbook(Workbook workbook, Supplier<Long> idSupplier) {
    Sheet sheet = workbook.createSheet(sheetName);

    Map<Boolean, Map<DataOverviewDiffStatus, XSSFCellStyle>> cellStylesByWrapAndDiffStatus =
        Stream.of(Boolean.FALSE, Boolean.TRUE)
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    wrap ->
                        Arrays.stream(DataOverviewDiffStatus.values())
                            .collect(
                                Collectors.toMap(
                                    Function.identity(),
                                    diffStatus -> {
                                      XSSFCellStyle cellStyle =
                                          (XSSFCellStyle) workbook.createCellStyle();
                                      if (wrap) {
                                        cellStyle.setWrapText(true);
                                      }
                                      var font = workbook.createFont();
                                      switch (diffStatus) {
                                        case ADDED:
                                          font.setColor(IndexedColors.GREEN.getIndex());
                                          break;
                                        case REMOVED:
                                          font.setColor(IndexedColors.RED.getIndex());
                                          font.setStrikeout(true);
                                          break;
                                        case OLD_VALUE:
                                          font.setColor(IndexedColors.LIGHT_BLUE.getIndex());
                                          font.setStrikeout(true);
                                          break;
                                        case NEW_VALUE:
                                          font.setColor(IndexedColors.BLUE.getIndex());
                                          break;
                                      }
                                      cellStyle.setFont(font);
                                      return cellStyle;
                                    }))));

    AtomicInteger rowIndexReference = new AtomicInteger(0);
    Stream.concat(
            Stream.of(Map.entry(DataOverviewDiffStatus.UNMODIFIED, excelHeaderTitles)),
            excelDataValues.stream())
        .forEach(
            diffStatusAndValues -> {
              DataOverviewDiffStatus diffStatus = diffStatusAndValues.getKey();
              List<String> rowValues = diffStatusAndValues.getValue();
              int rowIndex = rowIndexReference.getAndIncrement();
              Row row = sheet.createRow(rowIndex);
              row.setHeight((short) -1);
              AtomicInteger columnIndexReference = new AtomicInteger(0);
              Stream.concat(
                      rowIndex == 0 ? Stream.of() : Stream.of(diffStatus.getDisplayName()),
                      rowValues.stream())
                  .forEach(
                      value -> {
                        int columnIndex = columnIndexReference.getAndIncrement();
                        Cell cell = row.createCell(columnIndex);
                        cell.setCellValue(value);
                        if (rowIndex > 0) {
                          cell.setCellStyle(
                              cellStylesByWrapAndDiffStatus
                                  .get(excelWrapCellText.get(columnIndex))
                                  .get(diffStatus));
                        }
                      });
            });
    AtomicInteger columnIndexReference = new AtomicInteger(0);
    excelColumnWidths.forEach(
        width -> {
          int columnIndex = columnIndexReference.getAndIncrement();
          if (width > 0) {
            sheet.setColumnWidth(columnIndex, 256 * width);
          } else {
            sheet.autoSizeColumn(columnIndex);
          }
        });

    XSSFSheet xssfSheet = (XSSFSheet) sheet;
    XSSFTable table = xssfSheet.createTable(null);

    String tableRange =
        "A1:%s%d"
            .formatted(
                CellReference.convertNumToColString(excelHeaderTitles.size() - 1),
                1 + excelDataValues.size());

    CTTable ctTable = table.getCTTable();
    ctTable.setDisplayName(tableName); // does not support spaces
    ctTable.setName(tableName);
    ctTable.setId(idSupplier.get());
    ctTable.setTotalsRowShown(false);
    ctTable.setTotalsRowCount(0L);
    ctTable.setRef(tableRange);
    ctTable.setTableType(STTableType.WORKSHEET);

    CTTableColumns columns = ctTable.addNewTableColumns();
    columns.setCount(excelHeaderTitles.size());
    excelHeaderTitles.forEach(
        title -> {
          CTTableColumn tableColumn = columns.addNewTableColumn();
          tableColumn.setId(idSupplier.get());
          tableColumn.setName(title);
        });

    CTTableStyleInfo styleInfo = ctTable.addNewTableStyleInfo();
    styleInfo.setName("TableStyleMedium2");
    styleInfo.setShowColumnStripes(false);
    styleInfo.setShowRowStripes(true);

    CTAutoFilter autoFilter = ctTable.addNewAutoFilter();
    autoFilter.setRef(tableRange);
  }

  @SneakyThrows
  public void exportToCsvFile(String csvFilePattern) {
    CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
    csvHeaderTitles.forEach(csvSchemaBuilder::addColumn);
    CsvSchema csvSchema = csvSchemaBuilder.build().withHeader();
    CsvMapper csvMapper = new CsvMapper();
    List<Map<String, String>> csvRows =
        csvDataValues.stream()
            .map(
                dataRow ->
                    IntStream.range(0, csvHeaderTitles.size())
                        .boxed()
                        .collect(Collectors.toMap(csvHeaderTitles::get, dataRow::get)))
            .toList();
    ObjectWriter csvWriter = csvMapper.writer(csvSchema);
    StringWriter stringWriter = new StringWriter();
    csvWriter.writeValue(stringWriter, csvRows);
    String csvFilePath = csvFilePattern.formatted(sheetName.toLowerCase().replaceAll(" ", "-"));
    Files.writeString(Paths.get(csvFilePath), stringWriter.toString());
    log.info("Data overview {} exported to {}", sheetName.toLowerCase(), csvFilePath);
  }

  @SneakyThrows
  public static List<List<String>> importFromCsvFile(String csvFileUrl) {
    String oldCsvContent;
    try (HttpClient httpClient = HttpClient.newHttpClient()) {
      oldCsvContent =
          httpClient
              .send(
                  HttpRequest.newBuilder(URI.create(csvFileUrl)).build(),
                  HttpResponse.BodyHandlers.ofString())
              .body();
    }
    List<List<String>> csvRows;
    try (MappingIterator<Map<String, String>> iterator =
        new CsvMapper()
            .readerFor(Map.class)
            .with(CsvSchema.builder().setUseHeader(true).build())
            .readValues(oldCsvContent)) {
      csvRows =
          iterator.readAll().stream()
              .map(row -> new ArrayList<>(row.values()))
              .collect(Collectors.toList());
    }
    return csvRows;
  }
}
