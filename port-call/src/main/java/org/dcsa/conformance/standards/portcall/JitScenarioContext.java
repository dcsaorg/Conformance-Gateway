package org.dcsa.conformance.standards.portcall;

public record JitScenarioContext(
		String providerPartyName, String consumerPartyName, JitComponentFactory componentFactory) {}
