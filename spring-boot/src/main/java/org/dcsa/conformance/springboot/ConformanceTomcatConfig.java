package org.dcsa.conformance.springboot;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.tomcat.TomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConformanceTomcatConfig {

  @Bean
  public WebServerFactoryCustomizer<TomcatWebServerFactory> servletContainerCustomizer() {
    return factory ->
        factory.addConnectorCustomizers(
            (Connector connector) -> connector.setEnforceEncodingInGetWriter(false));
  }
}
