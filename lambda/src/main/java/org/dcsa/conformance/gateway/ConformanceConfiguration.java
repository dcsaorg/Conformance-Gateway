package org.dcsa.conformance.gateway;

import org.dcsa.conformance.sandbox.configuration.SandboxConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationProperties(prefix = "conformance")
@ConfigurationPropertiesScan
public class ConformanceConfiguration extends SandboxConfiguration {}
