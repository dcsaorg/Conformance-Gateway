cd ..
call mvn clean package

cd webui
call npm install
call ng build --configuration=%1%

cd ..\cdk
