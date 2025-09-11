package org.dcsa.conformance.standards.bookingandebl.party;

import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.state.JsonNodeMap;

import java.util.Collection;
import java.util.Map;

public abstract class BookingAndEblConformanceParty extends ConformanceParty {

    protected BookingAndEblConformanceParty(String apiVersion, PartyConfiguration partyConfiguration, CounterpartConfiguration counterpartConfiguration, JsonNodeMap persistentMap, PartyWebClient webClient, Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
        super(apiVersion, partyConfiguration, counterpartConfiguration, persistentMap, webClient, orchestratorAuthHeader);
    }
}
