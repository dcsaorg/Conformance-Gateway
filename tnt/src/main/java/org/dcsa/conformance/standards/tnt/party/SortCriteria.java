package org.dcsa.conformance.standards.tnt.party;

import lombok.Getter;

import java.util.List;

@Getter
public class SortCriteria {
  private final String field;
  private final List<AttributeMapping> attributeMappings;
  private final SortDirection direction;

  public SortCriteria(String field, List<AttributeMapping> attributeMappings, SortDirection direction) {
    this.field = field;
    this.attributeMappings = attributeMappings;
    this.direction = direction;
  }
}
