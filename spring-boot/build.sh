cd ../../Conformance-Gateway
find . -name "pom.xml" -exec mvn clean -U -B package -DskipTests -f '{}' \;
