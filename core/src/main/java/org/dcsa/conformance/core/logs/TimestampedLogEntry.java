package org.dcsa.conformance.core.logs;

import java.time.Instant;

public record TimestampedLogEntry(String message, Instant timestamp) implements Comparable<TimestampedLogEntry> {

  public TimestampedLogEntry(String message) {
    this(message, Instant.now());
  }

  @Override
  public int compareTo(TimestampedLogEntry o) {
    return this.timestamp.compareTo(o.timestamp);
  }

}
