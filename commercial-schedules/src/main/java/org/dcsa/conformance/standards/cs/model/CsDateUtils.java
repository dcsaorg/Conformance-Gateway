package org.dcsa.conformance.standards.cs.model;

import lombok.NoArgsConstructor;
import org.dcsa.conformance.core.toolkit.JsonToolkit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class CsDateUtils {
  public static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

  public static String getEndDateAfter3Months() {
    LocalDate futureDate = LocalDate.now().plusMonths(3);
    return futureDate.format(JsonToolkit.DEFAULT_DATE_FORMAT);
  }

  public static String getCurrentDate() {
    return LocalDate.now().format(JsonToolkit.DEFAULT_DATE_FORMAT);
  }

  public static void handleArrivalAndDepartureDates(
      Map<String, String> entryMap, Map<String, ? extends Collection<String>> queryParams) {
    String arrivalDate =
        getProcessedDate(queryParams, "arrivalStartDate", "arrivalEndDate", "ARRIVAL_DATE");
    String departureDate =
        getProcessedDate(queryParams, "departureStartDate", "departureEndDate", "DEPARTURE_DATE");

    entryMap.put("ARRIVAL_DATE", arrivalDate);
    entryMap.put("DEPARTURE_DATE", departureDate);
  }

  public static void handleSingleDate(
      Map<String, String> entryMap, Map<String, ? extends Collection<String>> queryParams) {
    String date =
        extractValue(queryParams, "date")
            .map(dateToProcess -> processDate(dateToProcess, "", "date"))
            .orElseGet(() -> CsDateUtils.DATE_TIME_FORMATTER.format(ZonedDateTime.now()));
    entryMap.put("DATE", date);
  }

  private static String getProcessedDate(
      Map<String, ? extends Collection<String>> queryParams,
      String startDateParam,
      String endDateParam,
      String mapKey) {
    Optional<String> startDateOpt = extractValue(queryParams, startDateParam);
    Optional<String> endDateOpt = extractValue(queryParams, endDateParam);

    if (startDateOpt.isPresent() || endDateOpt.isPresent()) {
      String startDate = startDateOpt.orElse(null);
      String endDate = endDateOpt.orElse(null);
      return "ARRIVAL_DATE".equals(mapKey)
          ? getArrivalDateTime(startDate, endDate)
          : getDepartureDateTime(startDate, endDate);
    } else {
      return CsDateUtils.DATE_TIME_FORMATTER.format(ZonedDateTime.now());
    }
  }

  private static Optional<String> extractValue(
      Map<String, ? extends Collection<String>> queryParams, String key) {
    return Optional.ofNullable(queryParams.get(key))
        .flatMap(collection -> collection.stream().findFirst());
  }

  private static String getArrivalDateTime(String arrivalStartDate, String arrivalEndDate) {
    if (arrivalStartDate != null && arrivalEndDate != null) {
      return processDate(arrivalStartDate, arrivalEndDate, "range");
    }
    if (arrivalStartDate != null) {
      return processDate(arrivalStartDate, "", "startDate");
    }
    if (arrivalEndDate != null) {
      return processDate("", arrivalEndDate, "endDate");
    }
    return "";
  }

  private static String getDepartureDateTime(String departureStartDate, String departureEndDate) {
    if (departureStartDate != null && departureEndDate != null) {
      return processDate(departureStartDate, departureEndDate, "range");
    }
    if (departureStartDate != null) {
      return processDate(departureStartDate, "", "startDate");
    }
    if (departureEndDate != null) {
      return processDate("", departureEndDate, "endDate");
    }
    return "";
  }

  private static String processDate(String startDate, String endDate, String type) {
    return switch (type) {
      case "startDate", "range", "date" ->
          convertDateToDateTime(LocalDate.parse(startDate, JsonToolkit.DEFAULT_DATE_FORMAT).plusDays(1));
      case "endDate" ->
          convertDateToDateTime(LocalDate.parse(endDate, JsonToolkit.DEFAULT_DATE_FORMAT).minusDays(1));
      default -> "";
    };
  }

  private static String convertDateToDateTime(LocalDate date) {
    LocalDateTime dateTime = date.atStartOfDay();
    ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.systemDefault());
    return zonedDateTime.format(CsDateUtils.DATE_TIME_FORMATTER);
  }
}
