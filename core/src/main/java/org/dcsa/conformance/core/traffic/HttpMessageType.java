package org.dcsa.conformance.core.traffic;

import lombok.Getter;

public enum HttpMessageType {
  REQUEST("request"),
  RESPONSE("response");

  @Getter private final String name;

  HttpMessageType(String name) {
    this.name = name;
  }
}
