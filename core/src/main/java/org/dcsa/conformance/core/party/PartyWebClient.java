package org.dcsa.conformance.core.party;

import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;

public interface PartyWebClient {
  void asyncRequest(ConformanceRequest conformanceRequest);
  ConformanceResponse syncRequest(ConformanceRequest conformanceRequest);
}
