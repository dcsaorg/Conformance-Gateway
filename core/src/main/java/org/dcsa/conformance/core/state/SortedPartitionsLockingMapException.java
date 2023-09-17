package org.dcsa.conformance.core.state;

public class SortedPartitionsLockingMapException extends Exception {
  public final SortedPartitionsLockingMapExceptionCode code;

  public SortedPartitionsLockingMapException(SortedPartitionsLockingMapExceptionCode code, Throwable cause) {
    super("%s: %s".formatted(SortedPartitionsLockingMapException.class.getSimpleName(), code.name()), cause);
    this.code = code;
  }
}
