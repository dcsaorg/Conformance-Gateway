package org.dcsa.conformance.springboot;

import org.dcsa.conformance.sandbox.configuration.SandboxConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationProperties(prefix = "conformance")
@ConfigurationPropertiesScan
public class ConformanceConfiguration extends SandboxConfiguration {}
