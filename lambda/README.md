# Conformance-Gateway
Evaluates the conformance of implementations of DCSA standards.

## Build
To build the project, run:
```
mvn package
```

## Configure

Configuration is in the `application*.properties` files in:
```
lambda/src/main/resources/standards/eblsurrender/v10
```
(The default configuration works for running everything on localhost as explained below.)

## Run

### eBL Surrender v1.0

#### All-in-one

Run the all-in-one configuration with the orchestrator, the synthetic carrier and synthetic platform in a single VM:
```
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.config.location=classpath:/standards/eblsurrender/v10/application.properties"
 ```

#### Carrier testing

To run the orchestrator and the synthetic platform in one VM for testing a carrier implementation, run:
```
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.config.location=classpath:/standards/eblsurrender/v10/application-carrier-testing-counterparts.properties"
 ```

To run only the synthetic carrier in a VM as the party being tested, run:
```
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.config.location=classpath:/standards/eblsurrender/v10/application-carrier-tested-party.properties"
 ```

#### Platform testing

To run the orchestrator and the synthetic carrier in one VM for testing a platform implementation, run:
```
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.config.location=classpath:/standards/eblsurrender/v10/application-platform-testing-counterparts.properties"
 ```

To run only the synthetic carrier in a VM as the party being tested, run:
```
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.config.location=classpath:/standards/eblsurrender/v10/application-platform-tested-party.properties"
 ```

## Test

### Conformance test

To start a test, visit in a browser:
```
http://localhost:8080/conformance/orchestrator/reset
```
(This returns an empty page and launches tests asynchronously; check the console from the "run" section above to watch test progress. eBL Surrender 1.0 tests currently take several seconds when both parties are synthetic.)

### Party input prompts
Synthetic parties automatically check during the test if they need to provide input, and provide it without requiring manual action.

For any other implementation, check during the test if the tested party needs to provide any input by visiting (if testing a carrier implementation):
```
http://localhost:8080/conformance/orchestrator/party/Carrier1/prompt/json
```
...or (if testing a platform implementation):
```
http://localhost:8080/conformance/orchestrator/party/Platform1/prompt/json
```

To provide the input for a party, for example to provide on behalf of the tested carrier the transportDocumentReference of an eBL that can be surrendered for delivery, send a POST to:
```
http://localhost:8080/conformance/orchestrator/party/Carrier1/input
```
...with the body (replacing the value of `actionId` with that from the input prompt and the value of `tdr` with the transportDocumentReference of the eBL to be used in this test scenario):
```
{
  "actionId" : "...",
  "tdr" : "..."
}
```

### Conformance report

To generate the conformance report for the party being tested, run:
```
http://localhost:8080/conformance/orchestrator/report
```
(This will return the HTML conformance test report for the party being tested, or for both parties if the all-in-one configuration is running.)
