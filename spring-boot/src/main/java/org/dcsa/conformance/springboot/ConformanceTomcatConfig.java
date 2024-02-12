package org.dcsa.conformance.springboot;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConformanceTomcatConfig {

  @Bean
  public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainerCustomizer() {
    return factory ->
        factory.addConnectorCustomizers(
            (Connector connector) -> connector.setEnforceEncodingInGetWriter(false));
  }
}
