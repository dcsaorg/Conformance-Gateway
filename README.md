# DCSA Conformance Framework

## Overview
The DCSA Conformance Framework is used by the adopters of each DCSA standard to measure the conformance of their
implementations. During standard development, DCSA itself uses the Conformance Framework to verify the correctness of
the standard by implementing all the relevant reference and conformance scenarios involving the interacting parties
defined in the standard.

Background information can be found at [DCSA Conformance](https://developer.dcsa.org/conformance).
DCSA developers can run the Conformance Framework locally, using Spring Boot and Angular.
Adopters use an AWS deployment of the Conformance Framework managed by DCSA, as specified in the /cdk directory.

## Prerequisites
- Java 21 or higher
- Maven 3.8 or higher
- Node.js and npm; works with v20 or higher

## Build and Run

### Java
1. Build the project using Maven:
    ```sh
    mvn package -DskipTests
    ```
2. Run the application:
    ```sh
    mvn spring-boot:run
    ```

### Angular and NodeJS Web UI
1. Navigate to the WebUI project directory:
    ```sh
    cd webui/
    ```
2. Install dependencies & build & run the Angular application:
    ```sh
    npm install
    ng serve
    ```
3. Open a browser and navigate to `http://localhost:4200/environment`

## Contributing
To be defined later.

## License
This project is licensed under the Apache License 2.0
