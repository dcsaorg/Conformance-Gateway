ls
cd home
ls
cd runner
ls
cd work
ls
find . -name "pom.xml" -exec mvn clean -U -B package -DskipTests -f '{}' \;
