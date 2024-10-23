package org.dcsa.conformance.standards.tnt.party;

import java.util.Collection;
import java.util.Map;

interface QueryParamRule {
  boolean validate(Map<String, ? extends Collection<String>> queryParams);
}
