package org.dcsa.conformance.standards.jit.schema;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SchemaParams {

  public static final String PORT_CALL_ID_REF = "#/components/parameters/portCallIDPathParam";
  public static final String PORT_CALL_SERVICE_ID_REF =
      "#/components/parameters/portCallServiceIDPathParam";
  public static final String API_VERSION_MAJOR_REF = "#/components/parameters/Api-Version-Major";

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
        .$ref("#/components/parameters/portCallServiceIDPathParam")
        .name("portCallServiceID")
        .in("path")
        .required(true)
        .description("The provider created identifier for the **Port Call Service**.")
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
}
