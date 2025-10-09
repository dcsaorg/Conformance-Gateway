package org.dcsa.conformance.standards.bookingandebl;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.standards.bookingandebl.party.BookingAndEblCarrier;
import org.dcsa.conformance.standards.bookingandebl.party.BookingAndEblRole;
import org.dcsa.conformance.standards.bookingandebl.party.BookingAndEblShipper;

public class BookingAndEblComponentFactory extends AbstractComponentFactory {

  BookingAndEblComponentFactory(String standardName, String standardVersion, String scenarioSuite) {
    super(standardName, standardVersion, scenarioSuite, "Carrier", "Shipper");
  }

  @Override
  public List<ConformanceParty> createParties(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      JsonNodeMap persistentMap,
      PartyWebClient webClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {

    Map<String, PartyConfiguration> partyConfigurationsByRoleName =
        Arrays.stream(partyConfigurations)
            .collect(Collectors.toMap(PartyConfiguration::getRole, Function.identity()));
    Map<String, CounterpartConfiguration> counterpartConfigurationsByRoleName =
        Arrays.stream(counterpartConfigurations)
            .collect(Collectors.toMap(CounterpartConfiguration::getRole, Function.identity()));

    List<ConformanceParty> parties = new LinkedList<>();

    PartyConfiguration carrierConfiguration =
        partyConfigurationsByRoleName.get(BookingAndEblRole.CARRIER.getConfigName());
    if (carrierConfiguration != null) {
      parties.add(
          new BookingAndEblCarrier(
              standardVersion,
              carrierConfiguration,
              counterpartConfigurationsByRoleName.get(BookingAndEblRole.SHIPPER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    PartyConfiguration shipperConfiguration =
        partyConfigurationsByRoleName.get(BookingAndEblRole.SHIPPER.getConfigName());
    if (shipperConfiguration != null) {
      parties.add(
          new BookingAndEblShipper(
              standardVersion,
              shipperConfiguration,
              counterpartConfigurationsByRoleName.get(BookingAndEblRole.CARRIER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    return parties;
  }

  @Override
  protected Map<String, BookingAndEblScenarioListBuilder> createModuleScenarioListBuilders(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      boolean isWithNotifications) {
    return BookingAndEblScenarioListBuilder.createModuleScenarioListBuilders(
        this,
        isWithNotifications,
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, BookingAndEblRole::isCarrier),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, BookingAndEblRole::isShipper));
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(BookingAndEblRole.values())
        .map(BookingAndEblRole::getConfigName)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  @Override
  public Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return (partyConfigurations.length == BookingAndEblRole.values().length
            ? Arrays.stream(BookingAndEblRole.values()).map(BookingAndEblRole::getConfigName)
            : Arrays.stream(counterpartConfigurations)
                .map(CounterpartConfiguration::getRole)
                .filter(
                    counterpartRole ->
                        Arrays.stream(partyConfigurations)
                            .map(PartyConfiguration::getRole)
                            .noneMatch(partyRole -> Objects.equals(partyRole, counterpartRole))))
        .collect(Collectors.toSet());
  }

  public JsonSchemaValidator getBookingMessageSchemaValidator(String jsonSchema) {
    String[] versions = standardVersion.split("-\\+-");
    String schemaFilePath = "/standards/booking/schemas/BKG_v%s.yaml".formatted(versions[0]);
    return JsonSchemaValidator.getInstance(schemaFilePath, jsonSchema);
  }

  public JsonSchemaValidator getEblMessageSchemaValidator(String schema) {
    String[] versions = standardVersion.split("-\\+-");
    String schemaFilePath = "/standards/ebl/schemas/EBL_v%s.yaml".formatted(versions[1]);
    return JsonSchemaValidator.getInstance(schemaFilePath, schema);
  }
}
