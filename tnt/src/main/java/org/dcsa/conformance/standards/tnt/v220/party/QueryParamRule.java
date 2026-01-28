package org.dcsa.conformance.standards.tnt.v220.party;

import java.util.Collection;
import java.util.Map;

interface QueryParamRule {
  boolean validate(Map<String, ? extends Collection<String>> queryParams);
}
