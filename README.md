# DCSA Conformance Framework

## Overview
The DCSA Conformance Framework is used by the adopters of each DCSA standard to measure the conformance of their
implementations. During standard development, DCSA itself uses the Conformance Framework to verify the correctness of
the standard by implementing all the relevant reference and conformance scenarios involving the interacting parties
defined in the standard.

Background information can be found at [DCSA Conformance](https://developer.dcsa.org/conformance).
DCSA developers can run the Conformance Framework locally, using Spring Boot and Angular.
Adopters use an AWS deployment of the Conformance Framework managed by DCSA.

## Prerequisites
- Java 25 or higher
- Maven 3.9 or higher (Maven wrapper included)
- Node.js and npm; works with v20 or higher

## Build and Run

### Java
1. Build the project using Maven:
    ```sh
    ./mvnw clean package -DskipTests
    ```
2. Run the application:
    ```sh
    ./mvnw -pl spring-boot -am spring-boot:run
    ```

### Docker Compose
Instead of building the project and running it locally, you can use Docker Compose to run the application.
Just run `docker compose up` in the root directory of the project. This will build the project and run the application.

### Angular and NodeJS Web UI
1. Navigate to the WebUI project directory:
    ```sh
    cd webui/
    ```
2. Install dependencies & build & run the Angular application:
    ```sh
    npm install
    npm run ng serve
    ```
3. Open a browser and navigate to `http://localhost:4200/environment`


## Testing Overview

There are 4 types of tests in the project:

1. Fully automated tests, running a whole standard. The orchestrator will run all the scenarios defined in the standard.
   The testing code only starts the test and verifies the results. This can be found in `ConformanceApplicationTest`
   class.
2. "Manual" tests, which the orchestrator will take only one side of the tests and the unit test code performs the
   "manual" actions needed. This can be found in the `ManualScenarioTest` class. All versions, suites and scenarios are
   tested in this way.
3. Web UI testing, which is done by using Selenium. This can be found in the `SeleniumTest` class. It requires NodeJS to
   be running. These tests take quite a while, because it often needs to wait before the UI is ready.
4. AWS testing, which only runs the EBL Conformance TD-only test on the Carrier role, by using the UI. This is also
   performed by Selenium. This can be found in the `AWSEnvironmentTest` class. It needs several environment variables to
   be set, which are not in the repository. Those you can set in your IDE, or by temporarily changing the code.

The GitHub Actions CI/CD pipeline runs the first 2 types of tests in every PR build. The 3rd type of test is run in
the pipeline of the `dev`, `test` and `master` branch. And the 4th type of tests is run in the pipeline of the `dev`
branch only.

## Testing while developing

While developing a standard, you can run the full standard in automatic mode by running the
`ConformanceApplicationTest`. Just temporarily command out the standards you don't need and start the test in your IDE.

### Manual testing
Running the 'Manual' test sometimes reveals issues that are not found in the automatic tests. You can either run your
full standard by commenting out the others in the `ManualScenarioTest` class, or run a single specific scenario by
running the method `testOnlyOneSpecificScenario` in the `ManualScenarioTest` class. Just remove the `@Disabled`
annotation from the test you want to run, and change the used variables to your liking. Just don't commit these changes.

**Tip**: In case there is something wrong with the standard, you'd probably like to inspect that situation. You can do this
by uncommenting the line with `// Note: developers` in `ManualTestBase` class, which will pause the test execution at
that point, keeping the UI open.

### Web UI testing
Running the Web UI tests can also be done in your IDE. You can either run your full standard by commenting out the
others in the `SeleniumTest` class, or run a single specific scenario by running the method
`testOnlyOneSpecificScenario`.

**Tip 1**: In order to see what Selenium is clicking around, in file `SeleniumTestBase` comment out the line with
`options.addArguments("--headless");`

**Tip 2**: After a failure, you can keep the UI running (Spring Boot), by uncommenting the `try catch` block, in method
`testOnlyOneSpecificScenario`.

## License
This project is licensed under the Apache License 2.0
