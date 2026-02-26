package org.dcsa.conformance.core.util;

import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ReferenceGenerator {

  private static final String DEFAULT_PREFIX = "DCSA";

  public static String newReference() {
    return "%s%s".formatted(DEFAULT_PREFIX, UUID.randomUUID())
            .replace("-", "")
            .substring(0, 20);
  }
}
