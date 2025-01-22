package org.dcsa.conformance.standards.jit;

public record JitScenarioContext(
		String providerPartyName, String consumerPartyName, JitComponentFactory componentFactory) {}
