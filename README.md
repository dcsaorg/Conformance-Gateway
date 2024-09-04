# Conformance Toolkit from DCSA

## Overview
This project evaluates standards conformance while acting as a gateway between collaborating applications.
Background information can be found at [DCSA Standards](https://dcsa.org/standards).
This tool runs locally with Spring Boot and Angular. Or hosted in AWS, as specified in the /cdk directory, without
Spring Boot.

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
    npm run build
    ng serve
    ```
3. Open a browser and navigate to `http://localhost:4200/environment`

## Contributing
1. Fork the repository in GitHub
2. Create a new branch (`git checkout -b feature-branch`)
3. Commit your changes (`git commit -am 'Add new feature'`)
4. Push to the branch (`git push origin feature-branch`)
5. Create a new Pull Request

## License
This project is licensed under the Apache License 2.0
