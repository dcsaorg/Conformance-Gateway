package org.dcsa.conformance.specifications.dataoverview;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.dcsa.conformance.specifications.standards.ebl.v300.types.UnspecifiedType;
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
          changedPrimaryKeyByOldPrimaryKeyBySheetClass,
      boolean swapOldAndNew) {
    this.sheetName = sheetName;
    this.tableName = tableName;
    this.csvHeaderTitles = headerTitles;
    this.excelHeaderTitles = Stream.concat(Stream.of("Diff"), headerTitles.stream()).toList();
    this.excelColumnWidths = Stream.concat(Stream.of(12), columnWidths.stream()).toList();
    this.excelWrapCellText =
        Stream.concat(Stream.of(Boolean.FALSE), wrapCellText.stream()).toList();
    this.csvDataValues = dataValues;

    Map<String, List<String>> oldRowValuesByPrimaryKey =
        rowValuesByPrimaryKey(primaryKeyColumnCount, oldDataValuesBySheetClass.get(getClass()));
    Map<String, List<String>> newRowValuesByPrimaryKey =
        rowValuesByPrimaryKey(primaryKeyColumnCount, dataValues);

    excelDataValues =
        swapOldAndNew
            ? diff(
                newRowValuesByPrimaryKey,
                oldRowValuesByPrimaryKey,
                changedPrimaryKeyByOldPrimaryKeyBySheetClass.get(getClass()).entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey)))
            : diff(
                oldRowValuesByPrimaryKey,
                newRowValuesByPrimaryKey,
                changedPrimaryKeyByOldPrimaryKeyBySheetClass.get(getClass()));
  }

  private static List<Map.Entry<DataOverviewDiffStatus, List<String>>> diff(
      Map<String, List<String>> oldRowValuesByPrimaryKey,
      Map<String, List<String>> newRowValuesByPrimaryKey,
      Map<String, String> changedPrimaryKeyByOldPrimaryKey) {
    List<Map.Entry<DataOverviewDiffStatus, List<String>>> diffDataValues = new ArrayList<>();

    Map<String, String> expandedChangedPrimaryKeyByOldPrimaryKey =
        new TreeMap<>(changedPrimaryKeyByOldPrimaryKey);
    // expand old key - new key prefix mappings (skipping specified existing key mappings)
    new TreeSet<>(changedPrimaryKeyByOldPrimaryKey.keySet())
        .reversed().stream()
            .filter(oldKey -> oldKey.endsWith("/"))
            .flatMap(
                oldKeyPrefix ->
                    Stream.concat(
                        Stream.of(
                            Map.entry(
                                oldKeyPrefix.substring(0, oldKeyPrefix.length() - 2),
                                changedPrimaryKeyByOldPrimaryKey
                                    .get(oldKeyPrefix)
                                    .substring(
                                        0,
                                        changedPrimaryKeyByOldPrimaryKey.get(oldKeyPrefix).length()
                                            - 2))),
                        oldRowValuesByPrimaryKey.keySet().stream()
                            .filter(
                                oldKey ->
                                    (oldKey.startsWith(oldKeyPrefix))
                                        && !changedPrimaryKeyByOldPrimaryKey.containsKey(oldKey)
                                        && changedPrimaryKeyByOldPrimaryKey.keySet().stream()
                                            .filter(
                                                otherOldKeyPrefix ->
                                                    otherOldKeyPrefix.startsWith(oldKeyPrefix)
                                                        && !otherOldKeyPrefix.equals(oldKeyPrefix))
                                            .noneMatch(oldKey::startsWith))
                            .map(
                                oldKey ->
                                    Map.entry(
                                        oldKey,
                                        changedPrimaryKeyByOldPrimaryKey.get(oldKeyPrefix)
                                            + oldKey.substring(oldKeyPrefix.length())))))
            .toList()
            .forEach(
                expandedEntry ->
                    expandedChangedPrimaryKeyByOldPrimaryKey.put(
                        expandedEntry.getKey(), expandedEntry.getValue()));

    Map<String, String> oldPrimaryKeysByNewPrimaryKey =
        expandedChangedPrimaryKeyByOldPrimaryKey.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    Set<String> sortedPrimaryKeys =
        Stream.concat(
                oldRowValuesByPrimaryKey.keySet().stream(),
                newRowValuesByPrimaryKey.keySet().stream())
            .collect(
                Collectors.toCollection(
                    () ->
                        new TreeSet<>(
                            (leftPrimaryKey, rightPrimaryKey) -> {
                              AtomicReference<String> leftPKReference =
                                  new AtomicReference<>(leftPrimaryKey);
                              AtomicReference<String> rightPKReference =
                                  new AtomicReference<>(rightPrimaryKey);
                              changedPrimaryKeyByOldPrimaryKey.entrySet().stream()
                                  .filter(entry -> entry.getKey().endsWith("/"))
                                  .forEach(
                                      entry -> {
                                        if (leftPrimaryKey.startsWith(entry.getKey())) {
                                          leftPKReference.set(
                                              entry.getValue()
                                                  + leftPrimaryKey.substring(
                                                      entry.getKey().length()));
                                        }
                                        if (rightPrimaryKey.startsWith(entry.getKey())) {
                                          rightPKReference.set(
                                              entry.getValue()
                                                  + rightPrimaryKey.substring(
                                                      entry.getKey().length()));
                                        }
                                      });
                              int newPKComparison =
                                  leftPKReference.get().compareTo(rightPKReference.get());
                              return newPKComparison != 0
                                  ? newPKComparison
                                  : leftPrimaryKey.compareTo(rightPrimaryKey);
                            })));

    sortedPrimaryKeys.stream()
        // skip the old values of modified PKs
        .filter(
            key ->
                !expandedChangedPrimaryKeyByOldPrimaryKey.containsKey(key)
                    || !sortedPrimaryKeys.contains(
                        expandedChangedPrimaryKeyByOldPrimaryKey.get(key)))
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
                diffDataValues.add(Map.entry(DataOverviewDiffStatus.REMOVED, oldRowValues));
              } else if (oldRowValues == null) {
                diffDataValues.add(Map.entry(DataOverviewDiffStatus.ADDED, newRowValues));
              } else {
                AtomicBoolean anyValuesUpdated = new AtomicBoolean(false);
                AtomicInteger columnIndex = new AtomicInteger(0);
                List<String> updatedOldRowValues =
                    oldRowValues.stream()
                        .map(
                            oldValue -> {
                              String newValue = newRowValues.get(columnIndex.getAndIncrement());
                              if (newValue.equals(oldValue)) {
                                return "";
                              } else {
                                anyValuesUpdated.set(true);
                                return oldValue;
                              }
                            })
                        .toList();
                if (anyValuesUpdated.get()) {
                  if (updatedOldRowValues.size() > 3
                      && updatedOldRowValues.get(2).equals(UnspecifiedType.class.getSimpleName())) {
                    diffDataValues.add(Map.entry(DataOverviewDiffStatus.UNMODIFIED, newRowValues));
                  } else {
                    diffDataValues.add(
                        Map.entry(DataOverviewDiffStatus.OLD_VALUE, updatedOldRowValues));
                    diffDataValues.add(Map.entry(DataOverviewDiffStatus.NEW_VALUE, newRowValues));
                  }
                } else {
                  diffDataValues.add(Map.entry(DataOverviewDiffStatus.UNMODIFIED, newRowValues));
                }
              }
            });
    return diffDataValues;
  }

  private static Map<String, List<String>> rowValuesByPrimaryKey(
      int primaryKeyColumnCount, List<List<String>> dataValues) {
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
                                      cellStyle.setVerticalAlignment(VerticalAlignment.TOP);
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
  public static List<List<String>> importFromString(String csvContent) {
    try (MappingIterator<Map<String, String>> iterator =
        new CsvMapper()
            .readerFor(Map.class)
            .with(CsvSchema.builder().setUseHeader(true).build())
            .readValues(csvContent)) {
      return iterator.readAll().stream()
          .map(row -> new ArrayList<>(row.values()))
          .collect(Collectors.toList());
    }
  }
}
