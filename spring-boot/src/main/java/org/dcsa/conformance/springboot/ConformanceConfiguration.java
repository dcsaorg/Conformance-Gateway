package org.dcsa.conformance.springboot;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationProperties(prefix = "conformance")
@ConfigurationPropertiesScan
@Getter
@Setter
public class ConformanceConfiguration {
    boolean useDynamoDb;
}
