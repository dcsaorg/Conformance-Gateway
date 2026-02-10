package org.dcsa.conformance.standards.tnt.v220.party;

import java.util.List;

public record SortCriteria(String field, List<AttributeMapping> attributeMappings, SortDirection direction) {}
