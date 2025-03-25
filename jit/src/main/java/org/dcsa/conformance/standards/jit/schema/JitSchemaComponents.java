package org.dcsa.conformance.standards.jit.schema;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dcsa.conformance.standards.jit.schema.common.DCSABase;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JitSchemaComponents {

  public static final String PORT_CALL_ID_REF = "#/components/parameters/portCallIDPathParam";
  public static final String PORT_CALL_SERVICE_ID_REF =
      "#/components/parameters/portCallServiceIDPathParam";
  public static final String API_VERSION_MAJOR_REF = "#/components/parameters/Api-Version-Major";
  public static final String SENDING_PARTY_REF = "#/components/parameters/Sending-Party";
  public static final String RECEIVING_PARTY_REF = "#/components/parameters/Receiving-Party";
  public static final String REQUEST_SENDING_PARTY_REF =
      "#/components/parameters/Request-Sending-Party";
  public static final String REQUEST_RECEIVING_PARTY_REF =
      "#/components/parameters/Request-Receiving-Party";

  public static Parameter getPortCallIDPathParam() {
    return new Parameter()
        .name("portCallID")
        .in("path")
        .required(true)
        .description("The provider created identifier for the **Port Call**.")
        .schema(getUUIDSchema());
  }

  public static Parameter getPortCallServiceIDPathParam() {
    return new Parameter()
        .name("portCallServiceID")
        .in("path")
        .required(true)
        .description("The **Service Provider** created identifier for the **Port Call Service**.")
        .schema(getUUIDSchema());
  }

  public static Parameter getApiVersionMajorHeader() {
    return new Parameter()
        .name("API-Version")
        .description(
            "An API-Version header **MAY** be added to the request (optional); if added it **MUST** only contain **MAJOR** version. API-Version header **MUST** be aligned with the URI version.")
        .in("header")
        .required(false)
        .schema(new Schema<String>().type("string").example("2"));
  }

  public static Schema getUUIDSchema() {
    return new Schema<String>()
        .type("string")
        .format("uuid")
        .example("0342254a-5927-4856-b9c9-aa12e7c00563");
  }

  static void addHeaders(Components components) {
    components.addHeaders(
        "API-Version",
        new Header()
            .description(
                "SemVer used to indicate the version of the contract (API version) returned.")
            .schema(new Schema<>().type("string").example("2.0.0")));
    components.addHeaders("Response-Sending-Party", addResponseSendingPartyHeader());
    components.addHeaders("Response-Receiving-Party", addResponseReceivingPartyHeader());
  }

  private static Header addResponseSendingPartyHeader() {
    return new Header()
        .required(false)
        .description(
            """
  When communicating through an optional system that acts as an application level JIT communication proxy, forwarding API calls
  between JIT **Service Providers** and JIT **Service Consumers**, the API server sets this response header to identify itself
  to the JIT proxy and to the API client as the original sending party of the API response. The value of this response header
  must be the same as the value of the request header `Request-Receiving-Party`. The assignment of party identifiers by the JIT proxy
  and the distribution of identifiers to the parties connecting through the JIT proxy are out of scope.
  """)
        .schema(new Schema<String>().type("string").maxLength(4096).example("Terminal-456"));
  }

  private static Header addResponseReceivingPartyHeader() {
    return new Header()
        .required(false)
        .description(
            """
  When communicating through an optional system that acts as an application level JIT communication proxy, forwarding API calls
  between JIT **Service Providers** and JIT **Service Consumers**, the API server sets this response header to identify to
  the JIT proxy the target receiving party of the API response. The value of this response header must be the same as the value
  of the request header `Request-Sending-Party`. The assignment of party identifiers by the JIT proxy and the distribution of
  identifiers to the parties connecting through the JIT proxy are out of scope.
  """)
        .schema(new Schema<String>().type("string").maxLength(4096).example("'Carrier-123'"));
  }

  public static Map<String, Header> getDefaultJitHeaders() {
    var headers = new LinkedHashMap<String, Header>();
    headers.put(
        DCSABase.API_VERSION_HEADER.keySet().iterator().next(),
        DCSABase.API_VERSION_HEADER.values().iterator().next());
    headers.put(
        "Response-Sending-Party", new Header().$ref("#/components/headers/Response-Sending-Party"));
    headers.put(
        "Response-Receiving-Party",
        new Header().$ref("#/components/headers/Response-Receiving-Party"));
    return headers;
  }
}
