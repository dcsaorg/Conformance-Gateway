package org.dcsa.conformance.standards.jit.model;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import lombok.With;
import org.dcsa.conformance.core.toolkit.JsonToolkit;

@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JitTimestamp(
    String timestampID,
    String replyToTimestampID,
    String portCallServiceID,
    JitClassifierCode classifierCode,
    String dateTime,
    String delayReasonCode,
    boolean isFYI,
    String remark) {

  public ObjectNode toJson() {
    return OBJECT_MAPPER.valueToTree(this);
  }

  public static JitTimestamp fromJson(JsonNode jsonNode) {
    return OBJECT_MAPPER.convertValue(jsonNode, JitTimestamp.class);
  }

  @SuppressWarnings("java:S2245") // Random is used for generating random timestamps. Secure enough.
  private static final Random RANDOM = new Random();

  public static JitTimestamp getTimestampForType(
      JitTimestampType timestampType, JitTimestamp previousTimestamp) {
    return switch (timestampType) {
      case ESTIMATED ->
          new JitTimestamp(
              UUID.randomUUID().toString(),
              previousTimestamp != null ? previousTimestamp.timestampID() : null,
              previousTimestamp != null
                  ? previousTimestamp.portCallServiceID()
                  : UUID.randomUUID().toString(),
              timestampType.getClassifierCode(),
              LocalDateTime.now().format(JsonToolkit.DEFAULT_DATE_FORMAT) + "T07:41:00+08:30",
              "STR",
              false,
              "Port closed due to strike");
      case PLANNED, ACTUAL ->
          previousTimestamp.withClassifierCode(timestampType.getClassifierCode());
      case REQUESTED ->
          previousTimestamp
              .withClassifierCode(timestampType.getClassifierCode())
              .withTimestampID(
                  UUID.randomUUID().toString()) // Create new ID, because it's a new timestamp
              .withReplyToTimestampID(
                  previousTimestamp.timestampID()) // Respond to the previous timestamp
              .withDateTime(generateRandomDateTime());
    };
  }

  // Random date/time in the future (3 - 7 hours from now), with a random offset of up to 4 hours.
  private static String generateRandomDateTime() {
    return LocalDateTime.now()
        .plusHours(3)
        .plusSeconds(RANDOM.nextInt(60 * 60 * 4))
        .format(JsonToolkit.ISO_8601_DATE_TIME_FORMAT);
  }
}
