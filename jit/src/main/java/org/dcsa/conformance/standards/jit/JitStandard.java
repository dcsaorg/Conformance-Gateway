package org.dcsa.conformance.standards.jit;

import java.util.*;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.jit.party.JitRole;

public class JitStandard extends AbstractStandard {
  public static final JitStandard INSTANCE = new JitStandard();
  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  public static final String PORT_CALL_URL = "/port-calls/";
  public static final String TERMINAL_CALL_URL = "/terminal-calls/";
  public static final String PORT_CALL_SERVICES_URL = "/port-call-services/";
  public static final String VESSEL_STATUS_URL = "/vessel-statuses/";
  public static final String TIMESTAMP_URL = "/timestamps/";
  public static final String CANCEL_URL = "/port-call-services/{portCallServiceID}/cancel";
  public static final String DECLINE_URL = "/port-call-services/{portCallServiceID}/decline";
  public static final String OMIT_PORT_CALL_URL = "/port-calls/{portCallID}/omit";
  public static final String OMIT_TERMINAL_CALL_URL = "/terminal-calls/{terminalCallID}/omit";

  public static final String PUT = "PUT";
  public static final String POST = "POST";
  public static final String GET = "GET";
  public static final String PORT_CALL_SERVICE_ID = "{portCallServiceID}";
  public static final String PORT_CALL_ID = "{portCallID}";
  public static final String TERMINAL_CALL_ID = "{terminalCallID}";

  private JitStandard() {
    super("JIT");
  }

  @Override
  public SortedMap<String, SortedSet<String>> getScenarioSuitesByStandardVersion() {
    return new TreeMap<>(
        Map.ofEntries(Map.entry("2.0.0", new TreeSet<>(Set.of(SCENARIO_SUITE_CONFORMANCE)))));
  }

  @Override
  public Map<String, Map<String, SortedMap<String, SortedSet<String>>>>
      getEndpointUrisAndMethodsByScenarioSuiteAndRoleName() {

    return Map.ofEntries(
        Map.entry(
            SCENARIO_SUITE_CONFORMANCE,
            Map.ofEntries(
                Map.entry(
                    JitRole.PROVIDER.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(
                            createEntry(
                                PORT_CALL_SERVICES_URL + PORT_CALL_SERVICE_ID + "/decline", POST),
                            // Generic endpoints
                            createEntry(PORT_CALL_URL, GET),
                            createEntry(TERMINAL_CALL_URL, GET),
                            createEntry(PORT_CALL_SERVICES_URL, GET),
                            createEntry(TIMESTAMP_URL + "${timestampID}", PUT),
                            createEntry(TIMESTAMP_URL, GET),
                            createEntry(VESSEL_STATUS_URL, GET)))),
                Map.entry(
                    JitRole.CONSUMER.getConfigName(),
                    new TreeMap<>(
                        Map.ofEntries(
                            createEntry(PORT_CALL_URL + "{portCallID}", PUT),
                            createEntry(PORT_CALL_URL + "{portCallID}/omit", POST),
                            createEntry(TERMINAL_CALL_URL + "{terminalCallId}", PUT),
                            createEntry(TERMINAL_CALL_URL + "{terminalCallId}/omit", POST),
                            createEntry(PORT_CALL_SERVICES_URL + PORT_CALL_SERVICE_ID, PUT),
                            createEntry(
                                PORT_CALL_SERVICES_URL + PORT_CALL_SERVICE_ID + "/cancel", POST),
                            createEntry(VESSEL_STATUS_URL + PORT_CALL_SERVICE_ID, PUT),
                            // Generic endpoints
                            createEntry(PORT_CALL_URL, GET),
                            createEntry(TERMINAL_CALL_URL, GET),
                            createEntry(PORT_CALL_SERVICES_URL, GET),
                            createEntry(TIMESTAMP_URL + "${timestampID}", PUT),
                            createEntry(TIMESTAMP_URL, GET),
                            createEntry(VESSEL_STATUS_URL, GET)))))));
  }

  private static Map.Entry<String, TreeSet<String>> createEntry(String url, String httpMethod) {
    return Map.entry(url, new TreeSet<>(Set.of(httpMethod)));
  }

  @Override
  protected AbstractComponentFactory doCreateComponentFactory(
      String standardVersion, String scenarioSuite) {
    return new JitComponentFactory(getName(), standardVersion, scenarioSuite);
  }
}
